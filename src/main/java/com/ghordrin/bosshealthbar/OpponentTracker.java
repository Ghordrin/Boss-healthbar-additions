package com.ghordrin.bosshealthbar;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.InteractingChanged;

@Slf4j
@Singleton
class OpponentTracker
{
	// Past the longest attack range, with room to reposition, but close enough that walking away counts.
	private static final int COMBAT_DISTANCE = 15;

	private final Client client;
	private final BossHealthBarConfig config;
	private final GameBossBar gameBossBar;
	private final TobBossBar tobBossBar;
	private final SuperiorTracker superiors;
	private final DamageTracker damage;
	private final FightTimer fightTimer;

	@Getter(AccessLevel.PACKAGE)
	private Actor opponent;
	private long interactionLostMillis;
	private long lastHitTakenMillis;
	private long lastOpponentHitMillis;

	private Actor knownBossActor;
	private String knownBossName;
	private boolean knownBoss;

	private NpcFilter alsoShowFor = NpcFilter.EMPTY;
	private NpcFilter neverShowFor = NpcFilter.EMPTY;
	private final NpcFilter.LastMatch opponentListMatch = new NpcFilter.LastMatch();
	private final NpcFilter.LastMatch gameBarListMatch = new NpcFilter.LastMatch();

	@Inject
	OpponentTracker(Client client, BossHealthBarConfig config, GameBossBar gameBossBar, TobBossBar tobBossBar,
		SuperiorTracker superiors, DamageTracker damage, FightTimer fightTimer)
	{
		this.client = client;
		this.config = config;
		this.gameBossBar = gameBossBar;
		this.tobBossBar = tobBossBar;
		this.superiors = superiors;
		this.damage = damage;
		this.fightTimer = fightTimer;
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
			log.debug("Interaction lost with {}, will clear after {}s if not resumed", opponent, config.hideDelay());
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
			log.debug("Keeping {} over lower priority target {}", opponent.getName(), target.getName());
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

	void onNpcDespawned(NPC npc)
	{
		if (npc != opponent)
		{
			return;
		}

		log.debug("Opponent {} despawned, clearing", opponent);
		fightTimer.opponentDespawned(isDefeated(npc), client.getTickCount());
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
			log.debug("Opponent {} timed out after {}s with no combat, clearing", opponent, config.hideDelay());
			opponent = null;
			fightTimer.reset();
		}
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

	// Matches the bar's Defeated: some bosses on the game's bar never set isDead() while they die, so 0 health counts too.
	boolean isDefeated(Actor actor)
	{
		if (actor.isDead())
		{
			return true;
		}
		final int nativeMaxHealth = gameBossBar.isTracking(actor) ? gameBossBar.maxHealth() : 0;
		if (nativeMaxHealth > 0)
		{
			return gameBossBar.health() <= 0;
		}
		final int tobMaxHealth = tobBossBar.isTracking(actor) ? tobBossBar.maxHealth() : 0;
		if (tobMaxHealth > 0)
		{
			return tobBossBar.health(tobMaxHealth) <= 0;
		}
		return actor.getHealthScale() > 0 && actor.getHealthRatio() == 0;
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
		damage.resetCombo();
		fightTimer.opponentChanged(target, opponent != null && isDefeated(opponent), client.getTickCount());
		opponent = target;
		lastOpponentHitMillis = 0;
		log.debug("New opponent: {} (combat level {}, known boss: {}, game boss bar: {})",
			target.getName(), target.getCombatLevel(), KnownBosses.contains(target.getName()), isGameBarBoss(target));
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
