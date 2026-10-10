package com.ghordrin.bosshealthbar;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.InteractingChanged;

@Singleton
class OpponentTracker
{
	// Past the longest attack range, with room to reposition, but close enough that walking away counts.
	private static final int COMBAT_DISTANCE = 15;

	enum DespawnOutcome
	{
		DEFEATED,
		OUT_OF_SIGHT,
		LEFT
	}

	private final Client client;
	private final BossHealthBarConfig config;
	private final GameBossBar gameBossBar;
	private final TobBossBar tobBossBar;
	private final SuperiorTracker superiors;
	private final DamageTracker damage;
	private final FightTimer fightTimer;
	private final FightGroup group;
	private final BossMemory memory;
	private final DebugLog debugLog;

	@Getter(AccessLevel.PACKAGE)
	private Actor opponent;
	private long interactionLostMillis;
	private long lastHitTakenMillis;
	private long lastOpponentHitMillis;

	private BossMemory.Entry heldEntry;
	private NPC outOfSightNpc;
	private BossMemory.Entry outOfSightEntry;
	private Actor resumedOpponent;
	private BarState resumedState;
	private boolean opponentSeenDefeated;
	private int zeroSinceTick = FightTimer.NO_TICK;

	private Actor knownBossActor;
	private String knownBossName;
	private boolean knownBoss;

	private NpcFilter alsoShowFor = NpcFilter.EMPTY;
	private NpcFilter neverShowFor = NpcFilter.EMPTY;
	private final NpcFilter.LastMatch opponentListMatch = new NpcFilter.LastMatch();
	private final NpcFilter.LastMatch gameBarListMatch = new NpcFilter.LastMatch();

	@Inject
	OpponentTracker(Client client, BossHealthBarConfig config, GameBossBar gameBossBar, TobBossBar tobBossBar,
		SuperiorTracker superiors, DamageTracker damage, FightTimer fightTimer, FightGroup group, BossMemory memory,
		DebugLog debugLog)
	{
		this.client = client;
		this.config = config;
		this.gameBossBar = gameBossBar;
		this.tobBossBar = tobBossBar;
		this.superiors = superiors;
		this.damage = damage;
		this.fightTimer = fightTimer;
		this.group = group;
		this.memory = memory;
		this.debugLog = debugLog;
	}

	void onInteractingChanged(InteractingChanged event)
	{
		if (event.getSource() != client.getLocalPlayer())
		{
			return;
		}

		final Actor target = event.getTarget();
		if (target == null)
		{
			interactionLostMillis = System.currentTimeMillis();
			if (opponent != null)
			{
				debugLog.add("Interaction lost with {}, will clear after {}s if not resumed", DebugLog.describe(opponent),
				config.hideDelay());
			}
			return;
		}

		interactionLostMillis = 0;

		if (target == opponent)
		{
			return;
		}

		// Attacking the smaller NPCs a boss spawns shouldn't take the bar off the boss.
		if (opponent != null && !opponent.isDead() && priority(target) < priority(opponent))
		{
			debugLog.add("Keeping {} over lower priority target {}", DebugLog.describe(opponent), DebugLog.describe(target));
			return;
		}

		setOpponent(target);
	}

	// While a game boss bar is up, the bar follows the NPC it shows.
	void followGameBars()
	{
		final NPC nativeBarBoss = gameBossBar.findNpc(opponent);
		final NPC gameBarBoss = nativeBarBoss != null ? nativeBarBoss : tobBossBar.findBoss();
		if (gameBarBoss != null && !gameBarBoss.isDead() && gameBarBoss != opponent && !isNeverShown(gameBarBoss))
		{
			setOpponent(gameBarBoss);
			interactionLostMillis = 0;
		}
	}

	void onNpcSpawned(NPC npc)
	{
		final int tick = client.getTickCount();
		final BossMemory.Entry entry = memory.find(npc.getIndex(), npc.getName(), tick);
		if (entry == null || entry != heldEntry || opponent != null || !BossMemory.isHeld(entry.tick, tick)
			|| isNeverShown(npc))
		{
			return;
		}

		// Back in sight soon after leaving it, with nothing else picked meanwhile, so the bar carries on.
		setOpponent(npc);
		interactionLostMillis = System.currentTimeMillis();
	}

	void onNpcDespawned(NPC npc)
	{
		if (npc != opponent)
		{
			if (memory.contains(npc.getIndex()) && isDefeated(npc))
			{
				memory.forget(npc.getIndex());
			}
			return;
		}

		final int tick = client.getTickCount();
		// A bar that despawns with the boss can take its health with it, so the last tick's reading counts too.
		final boolean confirmed = opponentSeenDefeated || isDefeated(npc);
		final boolean zero = zeroSinceTick != FightTimer.NO_TICK || readsZero(npc);
		final boolean playerDied = isLocalPlayerDead();
		final int distance = distanceToPlayer(npc);
		final DespawnOutcome outcome = despawnOutcome(confirmed, zero, playerDied, distance);
		debugLog.add("Opponent {} despawned (health {}/{}, dead {}, seen defeated {}, read 0 since tick {}, player died {}, distance {}): {}",
			DebugLog.describe(npc), npc.getHealthRatio(), npc.getHealthScale(), npc.isDead(), opponentSeenDefeated,
			zeroSinceTick, playerDied, distance, outcome);
		// An unconfirmed 0 that turns out to be a kill ends the fight where the 0 was first read.
		final int defeatTick = !confirmed && zeroSinceTick != FightTimer.NO_TICK ? zeroSinceTick : tick;
		opponentSeenDefeated = false;
		zeroSinceTick = FightTimer.NO_TICK;
		fightTimer.opponentDespawned(outcome == DespawnOutcome.DEFEATED, defeatTick, tick);
		if (outcome == DespawnOutcome.OUT_OF_SIGHT)
		{
			heldEntry = memory.remember(npc.getIndex(), npc.getName(), tick, fightTimer.save(group.id()));
			outOfSightNpc = npc;
			outOfSightEntry = heldEntry;
		}
		else
		{
			memory.forget(npc.getIndex());
		}
		resumedOpponent = null;
		resumedState = null;
		opponent = null;
		interactionLostMillis = 0;
		lastOpponentHitMillis = 0;
		damage.resetCombo();
	}

	void onHitsplatApplied(Actor actor, Hitsplat hitsplat)
	{
		if (!isCombatHit(hitsplat.getHitsplatType()))
		{
			return;
		}
		if (actor == client.getLocalPlayer())
		{
			lastHitTakenMillis = System.currentTimeMillis();
		}
		else if (actor != null && actor == opponent)
		{
			lastOpponentHitMillis = System.currentTimeMillis();
		}
	}

	// Clears the opponent once the fight has been quiet for longer than "Hide after", unless a
	// game boss bar still shows it.
	void onGameTick()
	{
		final Player player = client.getLocalPlayer();
		if (opponent == null || player == null || interactionLostMillis == 0)
		{
			return;
		}

		final boolean nearby = !opponent.isDead()
			&& opponent.getWorldArea().distanceTo(player.getWorldLocation()) <= COMBAT_DISTANCE;

		if (opponent != gameBossBar.findNpc(opponent)
			&& opponent != tobBossBar.findBoss()
			&& player.getInteracting() == null
			&& isQuietFor(interactionLostMillis, lastHitTakenMillis, lastOpponentHitMillis, nearby,
				System.currentTimeMillis(), config.hideDelay() * 1000L))
		{
			debugLog.add("Opponent {} timed out after {}s with no combat, clearing", DebugLog.describe(opponent),
				config.hideDelay());
			opponent = null;
			fightTimer.reset();
			group.clear();
			opponentSeenDefeated = false;
			zeroSinceTick = FightTimer.NO_TICK;
		}
	}

	// A confirmed defeat counts straight away. An overhead 0 that isn't confirmed, such as a stun meter that empties,
	// only counts if the NPC then despawns in view. Far away it went out of sight.
	static DespawnOutcome despawnOutcome(boolean confirmed, boolean zero, boolean playerDied, int distance)
	{
		if (confirmed || zero && distance < BossMemory.OUT_OF_SIGHT_DISTANCE)
		{
			return DespawnOutcome.DEFEATED;
		}
		return BossMemory.isOutOfSight(playerDied, distance) ? DespawnOutcome.OUT_OF_SIGHT : DespawnOutcome.LEFT;
	}

	static boolean isQuietFor(long interactionLostMillis, long lastHitTakenMillis, long lastOpponentHitMillis,
		boolean nearby, long now, long delayMillis)
	{
		if (interactionLostMillis == 0)
		{
			return false;
		}
		long lastCombat = interactionLostMillis;
		if (nearby)
		{
			lastCombat = Math.max(lastCombat, Math.max(lastHitTakenMillis, lastOpponentHitMillis));
		}
		return now - lastCombat > delayMillis;
	}

	// Damage over time is left out on purpose, so ticks that linger after a fight don't keep the bar up.
	static boolean isCombatHit(int hitsplatType)
	{
		switch (hitsplatType)
		{
			case HitsplatID.BLOCK_ME:
			case HitsplatID.BLOCK_OTHER:
			case HitsplatID.DAMAGE_ME:
			case HitsplatID.DAMAGE_OTHER:
			case HitsplatID.DAMAGE_ME_CYAN:
			case HitsplatID.DAMAGE_OTHER_CYAN:
			case HitsplatID.DAMAGE_ME_ORANGE:
			case HitsplatID.DAMAGE_OTHER_ORANGE:
			case HitsplatID.DAMAGE_ME_YELLOW:
			case HitsplatID.DAMAGE_OTHER_YELLOW:
			case HitsplatID.DAMAGE_ME_WHITE:
			case HitsplatID.DAMAGE_OTHER_WHITE:
			case HitsplatID.DAMAGE_MAX_ME:
			case HitsplatID.DAMAGE_MAX_ME_CYAN:
			case HitsplatID.DAMAGE_MAX_ME_ORANGE:
			case HitsplatID.DAMAGE_MAX_ME_YELLOW:
			case HitsplatID.DAMAGE_MAX_ME_WHITE:
			case HitsplatID.DAMAGE_ME_POISE:
			case HitsplatID.DAMAGE_OTHER_POISE:
			case HitsplatID.DAMAGE_MAX_ME_POISE:
				return true;
			default:
				return false;
		}
	}

	boolean shouldShowBarFor(Actor actor)
	{
		if (actor == null || actor.getName() == null)
		{
			return false;
		}

		final NpcFilter.Match match = listMatch(opponentListMatch, actor);
		return match != NpcFilter.Match.NEVER
			&& (match == NpcFilter.Match.ALSO
				|| !config.bossOnly()
				|| isKnownBoss(actor)
				|| isGameBarBoss(actor)
				|| (config.showAboveCombatLevel() && actor.getCombatLevel() >= config.minimumCombatLevel())
				|| (config.showSuperiors() && superiors.isSuperior(actor)));
	}

	// A confirmed defeat. Some bosses on the game's bar never set isDead() while they die, so 0 health there counts too.
	// An overhead 0 doesn't, since a stun meter that empties looks the same.
	boolean isDefeated(Actor actor)
	{
		if (actor.isDead())
		{
			return true;
		}
		final int nativeMaxHealth = gameBossBar.isTracking(actor) && gameBossBar.hasHealth() ? gameBossBar.maxHealth() : 0;
		if (nativeMaxHealth > 0)
		{
			return gameBossBar.health() <= 0;
		}
		final int tobMaxHealth = tobBossBar.isTracking(actor) ? tobBossBar.maxHealth() : 0;
		if (tobMaxHealth > 0)
		{
			return tobBossBar.health(tobMaxHealth) <= 0;
		}
		if (actor.getHealthScale() <= 0)
		{
			return false;
		}
		// Some turn into a form that can't be attacked as they die, before their bar reaches 0.
		return actor.getHealthRatio() == 1 && actor instanceof NPC && !NpcUtil.isAttackable((NPC) actor);
	}

	private static boolean readsZero(Actor actor)
	{
		return actor.getHealthScale() > 0 && actor.getHealthRatio() == 0;
	}

	// Back for another phase after being defeated: it can be attacked again with health to spare.
	private static boolean isBackUp(Actor actor)
	{
		return actor instanceof NPC && !actor.isDead() && NpcUtil.isAttackable((NPC) actor)
			&& actor.getHealthScale() > 0 && actor.getHealthRatio() > 1;
	}

	// Which reading isDefeated went by, for the debug log.
	private String defeatSource(Actor actor)
	{
		if (actor.isDead())
		{
			return "dead";
		}
		if (gameBossBar.isTracking(actor) && gameBossBar.hasHealth() && gameBossBar.maxHealth() > 0)
		{
			return "game boss bar at " + gameBossBar.health() + "/" + gameBossBar.maxHealth();
		}
		if (tobBossBar.isTracking(actor) && tobBossBar.maxHealth() > 0)
		{
			return "raid boss bar at 0/" + tobBossBar.maxHealth();
		}
		return "overhead bar at " + actor.getHealthRatio() + "/" + actor.getHealthScale();
	}

	void reset()
	{
		opponent = null;
		interactionLostMillis = 0;
		lastHitTakenMillis = 0;
		lastOpponentHitMillis = 0;
		knownBossActor = null;
		knownBossName = null;
		opponentListMatch.clear();
		gameBarListMatch.clear();
		fightTimer.reset();
		group.clear();
		memory.clear();
		heldEntry = null;
		outOfSightNpc = null;
		outOfSightEntry = null;
		opponentSeenDefeated = false;
		zeroSinceTick = FightTimer.NO_TICK;
		resumedOpponent = null;
		resumedState = null;
	}

	boolean wentOutOfSight(Actor actor)
	{
		return actor != null && actor == outOfSightNpc;
	}

	// The bar's last reading of an opponent that went out of sight, shown again if it comes back.
	void rememberState(Actor actor, BarState state)
	{
		if (actor == outOfSightNpc && outOfSightEntry != null)
		{
			outOfSightEntry.state = state;
		}
		outOfSightNpc = null;
		outOfSightEntry = null;
	}

	// The bar's last reading was 0, so the despawn was a kill after all.
	void forgetOutOfSight(Actor actor)
	{
		if (actor != outOfSightNpc)
		{
			return;
		}
		debugLog.add("Opponent {} was last read at 0 health, counting its despawn as a kill", DebugLog.describe(actor));
		memory.forget(outOfSightNpc.getIndex());
		if (heldEntry == outOfSightEntry)
		{
			heldEntry = null;
		}
		if (opponent == null)
		{
			fightTimer.opponentDespawned(true, client.getTickCount());
		}
		outOfSightNpc = null;
		outOfSightEntry = null;
	}

	void healedOnReturn(Actor actor)
	{
		debugLog.add("Remembered opponent {} came back with more health, starting a new fight", DebugLog.describe(actor));
		fightTimer.startOver(client.getTickCount());
	}

	BarState resumedState(Actor actor)
	{
		return actor != null && actor == resumedOpponent ? resumedState : null;
	}

	private int distanceToPlayer(NPC npc)
	{
		final Player player = client.getLocalPlayer();
		if (player == null)
		{
			return -1;
		}
		final WorldPoint tile = npc.getWorldLocation();
		final WorldPoint location = player.getWorldLocation();
		if (tile.getPlane() != location.getPlane())
		{
			return Integer.MAX_VALUE;
		}
		return BossMemory.tileDistance(tile.getX(), tile.getY(), location.getX(), location.getY());
	}

	private boolean isLocalPlayerDead()
	{
		final Player player = client.getLocalPlayer();
		return player != null && (player.isDead() || client.getBoostedSkillLevel(Skill.HITPOINTS) <= 0);
	}

	// Called once a tick. Kept for the despawn, when a boss bar may already have lost the health.
	boolean updateOpponentDefeated()
	{
		// Once defeated it stays so, since some die by turning into a death form that still shows a bit of health.
		final boolean defeated = opponent != null
			&& (isDefeated(opponent) || opponentSeenDefeated && !isBackUp(opponent));
		if (defeated && !opponentSeenDefeated)
		{
			debugLog.add("Opponent {} seen defeated ({})", DebugLog.describe(opponent), defeatSource(opponent));
		}
		opponentSeenDefeated = defeated;
		updateZero();
		return opponentSeenDefeated;
	}

	// The overhead bar going away doesn't end the 0, only a reading above it does.
	private void updateZero()
	{
		if (opponent == null || opponent.getHealthScale() <= 0)
		{
			return;
		}
		if (opponent.getHealthRatio() > 0)
		{
			zeroSinceTick = FightTimer.NO_TICK;
		}
		else if (zeroSinceTick == FightTimer.NO_TICK)
		{
			zeroSinceTick = client.getTickCount();
			debugLog.add("Opponent {} reads 0 on its overhead bar", DebugLog.describe(opponent));
		}
	}

	void loadLists()
	{
		alsoShowFor = NpcFilter.parse(config.alsoShowFor());
		neverShowFor = NpcFilter.parse(config.neverShowFor());
		opponentListMatch.clear();
		gameBarListMatch.clear();
	}

	private void setOpponent(Actor target)
	{
		final int tick = client.getTickCount();
		final BossMemory.Entry returned = target instanceof NPC
			? memory.take(((NPC) target).getIndex(), target.getName(), tick) : null;
		final boolean previousDefeated = opponent != null && (opponentSeenDefeated || isDefeated(opponent));
		final boolean bossLike = isBossLike(target);
		damage.resetCombo();
		heldEntry = null;
		opponentSeenDefeated = false;
		zeroSinceTick = FightTimer.NO_TICK;
		if (returned != null)
		{
			debugLog.add("Remembered opponent {} came back after {} ticks (remembered health {}/{}, fight start tick {})",
				DebugLog.describe(target), tick - returned.tick, returned.state != null ? returned.state.ratio : -1,
				returned.state != null ? returned.state.scale : -1, returned.fight.startTick);
			fightTimer.opponentReturned(target, returned.fight, tick);
			group.restore(returned.fight.group, target, bossLike, tick);
			resumedOpponent = target;
			resumedState = returned.state;
		}
		else
		{
			if (fightTimer.opponentChanged(target, group.isAliveMember(target), previousDefeated, tick))
			{
				group.join(target, bossLike, tick);
			}
			else
			{
				group.startFight(target, bossLike, tick);
			}
			resumedOpponent = null;
			resumedState = null;
		}
		opponent = target;
		lastOpponentHitMillis = 0;
		debugLog.add("New opponent: {} (combat level {}, known boss: {}, game boss bar: {})",
			DebugLog.describe(target), target.getCombatLevel(), KnownBosses.contains(target.getName()), isGameBarBoss(target));
	}

	private int priority(Actor actor)
	{
		if (actor == null)
		{
			return -1;
		}
		if (!shouldShowBarFor(actor))
		{
			return 0;
		}
		return gameBossBar.isTracking(actor) || tobBossBar.isTracking(actor) ? 2 : 1;
	}

	// Opponents that get a bar however "Only show for bosses" is set, apart from the combat level option.
	private boolean isBossLike(Actor actor)
	{
		return actor != null && actor.getName() != null
			&& (listMatch(opponentListMatch, actor) == NpcFilter.Match.ALSO
				|| isKnownBoss(actor)
				|| isGameBarBoss(actor)
				|| (config.showSuperiors() && superiors.isSuperior(actor)));
	}

	private boolean isGameBarBoss(Actor actor)
	{
		return actor != null && (gameBossBar.isTracking(actor) || actor == tobBossBar.cachedBoss());
	}

	// Checked every frame, so the answer is kept for the last actor and name.
	private boolean isKnownBoss(Actor actor)
	{
		final String name = actor.getName();
		if (actor != knownBossActor || !name.equals(knownBossName))
		{
			knownBossActor = actor;
			knownBossName = name;
			knownBoss = KnownBosses.contains(name);
		}
		return knownBoss;
	}

	// Kept apart from the opponent's cached answer, so checking both each frame doesn't redo the match.
	private boolean isNeverShown(NPC npc)
	{
		return npc.getName() != null && listMatch(gameBarListMatch, npc) == NpcFilter.Match.NEVER;
	}

	private NpcFilter.Match listMatch(NpcFilter.LastMatch last, Actor actor)
	{
		if (!(actor instanceof NPC) || (alsoShowFor.isEmpty() && neverShowFor.isEmpty()))
		{
			return NpcFilter.Match.NONE;
		}
		return last.get((NPC) actor, neverShowFor, alsoShowFor);
	}
}
