package com.ghordrin.bosshealthbar;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.InteractingChanged;

@Slf4j
@Singleton
class OpponentTracker
{
	private final Client client;
	private final BossHealthBarConfig config;
	private final GameBossBar gameBossBar;
	private final TobBossBar tobBossBar;
	private final SuperiorTracker superiors;
	private final DamageTracker damage;

	@Getter(AccessLevel.PACKAGE)
	private Actor opponent;
	private long interactionLostMillis;

	private Actor knownBossActor;
	private String knownBossName;
	private boolean knownBoss;

	@Inject
	OpponentTracker(Client client, BossHealthBarConfig config, GameBossBar gameBossBar, TobBossBar tobBossBar,
		SuperiorTracker superiors, DamageTracker damage)
	{
		this.client = client;
		this.config = config;
		this.gameBossBar = gameBossBar;
		this.tobBossBar = tobBossBar;
		this.superiors = superiors;
		this.damage = damage;
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
		if (gameBarBoss != null && !gameBarBoss.isDead() && gameBarBoss != opponent)
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
		opponent = null;
		interactionLostMillis = 0;
		damage.resetCombo();
	}

	// Clears the opponent once you've stopped interacting for longer than "Hide after", unless a
	// game boss bar still shows it.
	void onGameTick()
	{
		final Player player = client.getLocalPlayer();
		if (opponent != null
			&& player != null
			&& opponent != gameBossBar.findNpc(opponent)
			&& opponent != tobBossBar.findBoss()
			&& interactionLostMillis != 0
			&& player.getInteracting() == null
			&& System.currentTimeMillis() - interactionLostMillis > config.hideDelay() * 1000L)
		{
			log.debug("Opponent {} timed out after {}s with no interaction, clearing", opponent, config.hideDelay());
			opponent = null;
		}
	}

	boolean shouldShowBarFor(Actor actor)
	{
		return actor != null
			&& actor.getName() != null
			&& (!config.bossOnly()
				|| isKnownBoss(actor)
				|| isGameBarBoss(actor)
				|| (config.showAboveCombatLevel() && actor.getCombatLevel() >= config.minimumCombatLevel())
				|| (config.showSuperiors() && superiors.isSuperior(actor)));
	}

	void reset()
	{
		opponent = null;
		interactionLostMillis = 0;
		knownBossActor = null;
		knownBossName = null;
	}

	private void setOpponent(Actor target)
	{
		damage.resetCombo();
		opponent = target;
		log.debug("New opponent: {} (combat level {}, known boss: {}, game boss bar: {})",
			target.getName(), target.getCombatLevel(), KnownBosses.contains(target.getName()), isGameBarBoss(target));
	}

	private int priority(Actor actor)
	{
		if (actor == null)
		{
			return -1;
		}
		if (gameBossBar.isTracking(actor) || tobBossBar.isTracking(actor))
		{
			return 2;
		}
		return shouldShowBarFor(actor) ? 1 : 0;
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
}
