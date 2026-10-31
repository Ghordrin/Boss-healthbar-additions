package com.ghordrin.bosshealthbar.tracking;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.data.KnownBosses;
import com.ghordrin.bosshealthbar.data.NpcFilter;
import com.ghordrin.bosshealthbar.data.NpcUtil;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import com.ghordrin.bosshealthbar.gamebars.GameBossBar;
import com.ghordrin.bosshealthbar.gamebars.TobBossBar;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.InteractingChanged;

@Singleton
public class OpponentTracker
{
	// Past the longest attack range, with room to reposition, but close enough that walking away counts.
	private static final int COMBAT_DISTANCE = 15;

	public enum DespawnOutcome
	{
		DEFEATED,
		OUT_OF_SIGHT,
		LEFT
	}

	// Where the last scene load left the player, so the next one can tell whether they ended up somewhere else.
	static final class Area
	{
		private boolean sceneReloaded;
		private boolean sampled;
		int plane;
		int x;
		int y;
		boolean instance;

		void sceneLoading()
		{
			sceneReloaded = true;
		}

		void clear()
		{
			sceneReloaded = false;
			sampled = false;
			plane = 0;
			x = 0;
			y = 0;
			instance = false;
		}

		// Taken once per load. A no on arrival keeps the sample, in case the position isn't the destination yet, and
		// lets the next tick decide.
		boolean take(boolean onArrival, int toPlane, int toX, int toY, boolean toInstance)
		{
			final boolean left = leftArea(sceneReloaded && sampled, plane, x, y, instance, toPlane, toX, toY, toInstance);
			if (!left && onArrival)
			{
				return false;
			}
			plane = toPlane;
			x = toX;
			y = toY;
			instance = toInstance;
			sampled = true;
			sceneReloaded = false;
			return left;
		}
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
	private final HealthReader healthReader;
	private final DebugLog debugLog;

	@Getter
	private Actor opponent;
	private long interactionLostMillis;
	private long lastHitTakenMillis;
	private long lastOpponentHitMillis;

	private BossMemory.Entry heldEntry;
	private Actor resumedOpponent;
	private final HealthReader.DownState opponentState = new HealthReader.DownState();
	// The opponent, when picking it carried on the fight the previous one was part of.
	private Actor sameFightOpponent;
	private final HealthReader.Watch opponentWatch = new HealthReader.Watch();
	private Actor despawnedActor;
	private DespawnOutcome despawnedOutcome;
	private int zeroSinceTick = FightTimer.NO_TICK;
	// Counts up whenever the opponent starts a new life, so the bar can tell that apart from another phase.
	public int opponentLife;

	private Actor knownBossActor;
	private String knownBossName;
	private boolean knownBoss;

	private final Area area = new Area();

	private NpcFilter alsoShowFor = NpcFilter.EMPTY;
	private NpcFilter neverShowFor = NpcFilter.EMPTY;
	private final NpcFilter.LastMatch opponentListMatch = new NpcFilter.LastMatch();
	private final NpcFilter.LastMatch gameBarListMatch = new NpcFilter.LastMatch();
	private int gameBarNpcId = -1;
	private boolean gameBarNpcShown;
	private NPC raidBossActor;
	private int raidBossForm = -1;
	private boolean raidBossShown;

	@Inject
	OpponentTracker(Client client, BossHealthBarConfig config, GameBossBar gameBossBar, TobBossBar tobBossBar,
		SuperiorTracker superiors, DamageTracker damage, FightTimer fightTimer, FightGroup group, BossMemory memory,
		HealthReader healthReader, DebugLog debugLog)
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
		this.healthReader = healthReader;
		this.debugLog = debugLog;
	}

	public void onInteractingChanged(InteractingChanged event)
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
	public void followGameBars()
	{
		final NPC nativeBarBoss = gameBossBar.findNpc(opponent);
		final NPC gameBarBoss = nativeBarBoss != null ? nativeBarBoss : tobBossBar.findBoss();
		if (gameBarBoss != null && !gameBarBoss.isDead() && gameBarBoss != opponent && !isNeverShown(gameBarBoss))
		{
			setOpponent(gameBarBoss);
			interactionLostMillis = 0;
		}
	}

	public void onNpcSpawned(NPC npc)
	{
		final int tick = client.getTickCount();
		final BossMemory.Entry entry = memory.find(npc.getIndex(), npc.getName(), tick);
		// Only the spawns the bar could pick up, so a busy area doesn't push the current fight out of the log.
		if (entry != null || (opponent != null && npc.getName() != null && npc.getName().equals(opponent.getName())))
		{
			debugLog.add("{} spawned on tick {}", DebugLog.describe(npc), tick);
		}
		if (entry == null || entry != heldEntry || opponent != null || !BossMemory.isHeld(entry.tick, tick)
			|| isNeverShown(npc))
		{
			return;
		}

		// Back in sight soon after leaving it, with nothing else picked meanwhile, so the bar carries on.
		setOpponent(npc);
		interactionLostMillis = System.currentTimeMillis();
	}

	public void onNpcDespawned(NPC npc)
	{
		if (npc != opponent)
		{
			if (memory.contains(npc.getIndex()) && isDown(npc))
			{
				memory.forget(npc.getIndex());
			}
			return;
		}

		final int tick = client.getTickCount();
		// A bar that despawns with the boss can take its health with it, so the last tick's reading counts too.
		final boolean confirmed = opponentState.down || isDown(npc);
		final boolean zero = zeroSinceTick != FightTimer.NO_TICK || readsZero(npc);
		final boolean playerDied = isLocalPlayerDead();
		final int distance = distanceToPlayer(npc);
		// The fight group still holds this NPC's last reading, since it drops it after this.
		final FightGroup.Member member = group.find(npc.getIndex());
		final int lastRatio = member != null ? member.ratio : -1;
		final boolean lastExact = member != null && member.exact;
		final DespawnOutcome outcome = despawnOutcome(confirmed, zero, playerDied, distance, lastRatio, lastExact);
		debugLog.add("Opponent {} despawned (health {}/{}, dead {}, down {}, read 0 since tick {}, player died {}, distance {}): {}",
			DebugLog.describe(npc), npc.getHealthRatio(), npc.getHealthScale(), npc.isDead(), opponentState.down,
			zeroSinceTick, playerDied, distance, outcome);
		// A line of its own when it was the last reading, and not the distance, that made the despawn a kill.
		if (outcome == DespawnOutcome.DEFEATED
			&& despawnOutcome(confirmed, zero, playerDied, distance, -1, false) == DespawnOutcome.OUT_OF_SIGHT)
		{
			debugLog.add("Opponent {} was last read at 0 health, counting its despawn as a kill", DebugLog.describe(npc));
		}
		// A kill ends the fight where the 0 was first read, confirmed then or only at the despawn.
		final int defeatTick = zeroSinceTick != FightTimer.NO_TICK ? zeroSinceTick : tick;
		clearDown();
		despawnedActor = npc;
		despawnedOutcome = outcome;
		fightTimer.opponentDespawned(outcome == DespawnOutcome.DEFEATED, defeatTick, tick);
		if (outcome == DespawnOutcome.OUT_OF_SIGHT)
		{
			heldEntry = memory.remember(npc.getIndex(), npc.getName(), tick, fightTimer.save(group.id()));
			heldEntry.health = member != null ? member.lastAboveZero : null;
		}
		else
		{
			memory.forget(npc.getIndex());
		}
		resumedOpponent = null;
		opponent = null;
		interactionLostMillis = 0;
		lastOpponentHitMillis = 0;
		damage.resetCombo();
	}

	public void onHitsplatApplied(Actor actor, Hitsplat hitsplat)
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
	public void onGameTick()
	{
		final Player player = client.getLocalPlayer();
		if (opponent == null || player == null || interactionLostMillis == 0)
		{
			return;
		}

		final boolean nearby = !opponent.isDead() && isNearby(combatDistanceTo(opponent));

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
			clearDown();
		}
	}

	public void onSceneLoading()
	{
		area.sceneLoading();
	}

	// Whether the fight is to be forgotten: the player ended up somewhere else over the last scene load and nothing of
	// the fight is still here. Taken once per load.
	public boolean takeLeftArea(boolean onArrival)
	{
		final Player player = client.getLocalPlayer();
		final WorldView worldView = client.getTopLevelWorldView();
		if (player == null || worldView == null)
		{
			return false;
		}

		final WorldPoint location = player.getWorldLocation();
		final boolean instance = worldView.isInstance();
		final int fromPlane = area.plane;
		final int fromX = area.x;
		final int fromY = area.y;
		final boolean fromInstance = area.instance;
		final boolean left = area.take(onArrival, location.getPlane(), location.getX(), location.getY(), instance);
		if (left)
		{
			debugLog.add("Left the area over a scene load ({} tiles, plane {} to {}, instance {} to {})",
				BossMemory.tileDistance(fromX, fromY, location.getX(), location.getY()),
				fromPlane, location.getPlane(), fromInstance, instance);
		}
		final boolean forgets = forgetsFight(left, opponent != null);
		if (left && !forgets)
		{
			debugLog.add("Opponent {} is still here, so the fight carries on", DebugLog.describe(opponent));
		}
		return forgets;
	}

	// A fight itself can move the player far enough to reload the scene, so nothing is forgotten while the opponent is
	// still here.
	static boolean forgetsFight(boolean left, boolean opponentPresent)
	{
		return left && !opponentPresent;
	}

	// Far enough over a scene load that nothing we remembered can still be in sight.
	static boolean leftArea(boolean sampled, int fromPlane, int fromX, int fromY, boolean fromInstance,
		int toPlane, int toX, int toY, boolean toInstance)
	{
		return sampled && (fromPlane != toPlane || fromInstance != toInstance
			|| BossMemory.tileDistance(fromX, fromY, toX, toY) >= BossMemory.OUT_OF_SIGHT_DISTANCE);
	}

	// A confirmed defeat counts straight away. An overhead 0 that isn't confirmed, such as a stun meter that empties,
	// only counts if the NPC then despawns in view. Far away it went out of sight, unless the last reading was an
	// exact 0, which wins over the distance.
	static DespawnOutcome despawnOutcome(boolean confirmed, boolean zero, boolean playerDied, int distance,
		int lastRatio, boolean lastExact)
	{
		if (confirmed || zero && distance < BossMemory.OUT_OF_SIGHT_DISTANCE)
		{
			return DespawnOutcome.DEFEATED;
		}
		if (!BossMemory.isOutOfSight(playerDied, distance))
		{
			return DespawnOutcome.LEFT;
		}
		return lastExact && lastRatio == 0 ? DespawnOutcome.DEFEATED : DespawnOutcome.OUT_OF_SIGHT;
	}

	// Close enough for the fight to count.
	static boolean isNearby(int distance)
	{
		return distance >= 0 && distance <= COMBAT_DISTANCE;
	}

	// Whether the player is fighting this opponent right now, which is when its fight starts: going for it, near it,
	// and it can still be fought. One only picked up because a game bar follows it, or already read at 0, isn't fought.
	static boolean startsFight(boolean engaged, boolean attackable, boolean dead, boolean down, int distance)
	{
		return engaged && attackable && !dead && !down && isNearby(distance);
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
	public static boolean isCombatHit(int hitsplatType)
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

	public boolean shouldShowBarFor(Actor actor)
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

	private boolean isDown(Actor actor)
	{
		return HealthReader.isDown(healthReader.read(actor), actor.isDead(), actor instanceof NPC,
			NpcUtil.isAttackable(actor));
	}

	private static boolean readsZero(Actor actor)
	{
		return actor.getHealthScale() > 0 && actor.getHealthRatio() == 0;
	}

	// A confirmed kill, unlike a 0 the opponent can still come back from.
	public boolean isOpponentDefeated()
	{
		return opponentState.defeated;
	}

	// The tick the opponent's health first read 0, which is where its death sequence began.
	public int zeroSince()
	{
		return zeroSinceTick;
	}

	// Changes when the opponent is back up after a confirmed kill, so the bar starts over for the next one.
	public int opponentLife()
	{
		return opponentLife;
	}

	// Whether the given actor carried on the previous opponent's fight when it was picked, as the fight timer decided.
	public boolean continuesFight(Actor actor)
	{
		return actor != null && actor == sameFightOpponent;
	}

	// How the given opponent's despawn was read, or null if it wasn't the last opponent to despawn.
	public DespawnOutcome despawnOutcome(Actor actor)
	{
		return actor != null && actor == despawnedActor ? despawnedOutcome : null;
	}

	private void clearDown()
	{
		opponentState.clear();
		zeroSinceTick = FightTimer.NO_TICK;
	}

	public void reset()
	{
		opponent = null;
		interactionLostMillis = 0;
		lastHitTakenMillis = 0;
		lastOpponentHitMillis = 0;
		knownBossActor = null;
		knownBossName = null;
		opponentListMatch.clear();
		gameBarListMatch.clear();
		gameBarNpcId = -1;
		raidBossActor = null;
		fightTimer.reset();
		group.clear();
		memory.clear();
		heldEntry = null;
		clearDown();
		despawnedActor = null;
		sameFightOpponent = null;
		healthReader.reset();
		despawnedOutcome = null;
		resumedOpponent = null;
		area.clear();
	}

	public void healedOnReturn(Actor actor)
	{
		debugLog.add("Remembered opponent {} came back with more health, starting a new fight", DebugLog.describe(actor));
		fightTimer.startOver(client.getTickCount());
	}

	// Whether the given opponent was picked up again from memory, carrying on the fight it went out of sight in.
	public boolean resumedFromMemory(Actor actor)
	{
		return actor != null && actor == resumedOpponent;
	}

	private int combatDistanceTo(Actor actor)
	{
		final Player player = client.getLocalPlayer();
		return player == null ? -1 : actor.getWorldArea().distanceTo(player.getWorldLocation());
	}

	// Hits start the fight through the fight timer's own hit path, so this only covers the player going for the opponent.
	private void updateFightStart(Actor actor, boolean attackable, boolean dead, boolean down, int tick)
	{
		// Saves the work below once the fight is under way; onEngaged is what refuses to restart it.
		if (fightTimer.getStartTick() != FightTimer.NO_TICK)
		{
			return;
		}
		// Only the local player's own target counts, and it is read live so an interaction from before counts too.
		final Player player = client.getLocalPlayer();
		final boolean engaged = player != null && actor == player.getInteracting();
		final int distance = engaged ? combatDistanceTo(actor) : -1;
		if (startsFight(engaged, attackable, dead, down, distance) && shouldShowBarFor(actor))
		{
			fightTimer.onEngaged(tick);
		}
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
	public void updateOpponentDown()
	{
		if (opponent == null)
		{
			opponentState.clear();
			return;
		}

		final HealthReader.Reading reading = healthReader.read(opponent);
		final boolean dead = opponent.isDead();
		final boolean npc = opponent instanceof NPC;
		final boolean attackable = NpcUtil.isAttackable(opponent);
		// A fight group member's health is logged by the group.
		final DebugLog log = !npc || group.find(((NPC) opponent).getIndex()) == null ? debugLog : null;
		if (log != null)
		{
			opponentWatch.log(log, opponent, reading, dead, attackable);
		}
		final int tick = client.getTickCount();
		opponentState.update(reading, dead, npc, attackable, tick, log, "Opponent ", opponent);
		updateFightStart(opponent, attackable, dead, opponentState.down, tick);
		if (opponentState.takeRevived())
		{
			startNewLife(tick);
		}
		updateZero();
	}

	// The same NPC back up after its kill was confirmed is the next one, so nothing of the previous life carries over.
	private void startNewLife(int tick)
	{
		newLife(fightTimer, group, opponent, isBossLike(opponent), tick);
		sameFightOpponent = null;
		resumedOpponent = null;
		opponentLife++;
		damage.resetCombo();
		clearDown();
		opponentWatch.clear();
	}

	static void newLife(FightTimer fightTimer, FightGroup group, Actor opponent, boolean bossLike, int tick)
	{
		fightTimer.startOver(tick);
		group.startFight(opponent, bossLike, tick);
	}

	// The overhead bar going away, or reading no value, doesn't end the 0, only a reading above it does.
	private void updateZero()
	{
		if (opponent == null || opponent.getHealthScale() <= 0 || opponent.getHealthRatio() < 0)
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

	public void loadLists()
	{
		alsoShowFor = NpcFilter.parse(config.alsoShowFor());
		neverShowFor = NpcFilter.parse(config.neverShowFor());
		opponentListMatch.clear();
		gameBarListMatch.clear();
		gameBarNpcId = -1;
		raidBossActor = null;
	}

	private void setOpponent(Actor target)
	{
		final int tick = client.getTickCount();
		final BossMemory.Entry returned = target instanceof NPC
			? memory.take(((NPC) target).getIndex(), target.getName(), tick) : null;
		final boolean previousDefeated = opponent != null && (opponentState.down || isDown(opponent));
		final boolean bossLike = isBossLike(target);
		damage.resetCombo();
		heldEntry = null;
		clearDown();
		opponentWatch.clear();
		if (returned != null)
		{
			debugLog.add("Remembered opponent {} came back after {} ticks (remembered health {}/{}, fight start tick {})",
				DebugLog.describe(target), tick - returned.tick, returned.health != null ? returned.health.ratio : -1,
				returned.health != null ? returned.health.scale : -1, returned.fight.startTick);
			fightTimer.opponentReturned(target, returned.fight, tick);
			sameFightOpponent = null;
			group.restore(returned.fight.group, target, bossLike, tick);
			// A fight fought in between leaves the group without this NPC, so the remembered health comes back with it.
			group.restoreHealth(((NPC) target).getIndex(), returned.health);
			resumedOpponent = target;
		}
		else
		{
			if (fightTimer.opponentChanged(target, group.isAliveMember(target), previousDefeated, tick))
			{
				sameFightOpponent = target;
				group.join(target, bossLike, tick);
			}
			else
			{
				sameFightOpponent = null;
				group.startFight(target, bossLike, tick);
			}
			resumedOpponent = null;
		}
		opponent = target;
		lastOpponentHitMillis = 0;
		debugLog.add("New opponent: {} (combat level {}, known boss: {}, game boss bar: {})",
			DebugLog.describe(target), target.getCombatLevel(), KnownBosses.contains(target.getName()), isGameBarBoss(target));
		updateFightStart(target, NpcUtil.isAttackable(target), target.isDead(), isDown(target), tick);
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

	// A game bar can be up for a boss whose NPC isn't in the scene yet, right after a kill for instance, so
	// this answers by the tracked ID when there is none. The bar counts anything a game bar tracks as a boss,
	// which leaves Never show for as the only setting that can keep it off.
	public boolean gameBarNpcGetsBar(int npcId)
	{
		if (npcId == -1)
		{
			return false;
		}
		if (neverShowFor.isEmpty())
		{
			return true;
		}
		final NPC npc = gameBossBar.trackedNpc();
		if (npc != null && NpcUtil.baseId(npc) == npcId)
		{
			// The NPC in the scene carries the name the lists are matched against everywhere else.
			return !isNeverShown(npc);
		}
		// Asked every frame, so the answer is kept for the last ID.
		if (npcId != gameBarNpcId)
		{
			gameBarNpcId = npcId;
			final NPCComposition composition = client.getNpcDefinition(npcId);
			final String name = composition != null ? composition.getName() : null;
			// A boss with several forms has no name on the ID its bar tracks, only on the forms, so with no
			// NPC in the scene only an entry by ID can answer for it.
			gameBarNpcShown = NpcFilter.check(neverShowFor, NpcFilter.EMPTY, NpcUtil.hasName(name) ? name : "", npcId)
				!= NpcFilter.Match.NEVER;
		}
		return gameBarNpcShown;
	}

	// The raid bar's boss usually isn't the opponent, and the question comes every frame, so the whole answer
	// is kept until that boss or its form changes.
	public boolean raidBarBossGetsBar(NPC boss)
	{
		final int form = boss != null ? NpcUtil.currentId(boss) : -1;
		if (boss != raidBossActor || form != raidBossForm)
		{
			raidBossActor = boss;
			raidBossForm = form;
			raidBossShown = shouldShowBarFor(boss);
		}
		return raidBossShown;
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
