package com.ghordrin.bosshealthbar;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.client.game.NPCManager;

// Where an opponent's health comes from, and what a reading says about whether it's down.
@Singleton
class HealthReader
{
	enum Source
	{
		GAME_BAR,
		RAID_BAR,
		OVERHEAD
	}

	static final class Reading
	{
		final Source source;
		final Health health;

		Reading(Source source, Health health)
		{
			this.source = source;
			this.health = health;
		}

		boolean fromBossBar()
		{
			return source != Source.OVERHEAD;
		}
	}

	private final GameBossBar gameBossBar;
	private final TobBossBar tobBossBar;
	private final NPCManager npcManager;

	@Inject
	HealthReader(GameBossBar gameBossBar, TobBossBar tobBossBar, NPCManager npcManager)
	{
		this.gameBossBar = gameBossBar;
		this.tobBossBar = tobBossBar;
		this.npcManager = npcManager;
	}

	// The game's bars have exact hitpoints, and some bosses stop sending overhead health updates while they're
	// shown, so they come first. Null while there's no reading at all.
	Reading read(Actor actor)
	{
		if (actor == null)
		{
			return null;
		}
		if (gameBossBar.isTracking(actor))
		{
			final int max = gameBossBar.maxHealth();
			if (max > 0 && gameBossBar.hasHealth())
			{
				return new Reading(Source.GAME_BAR,
					new Health(gameBossBar.health(), max, true, max, gameBossBar.isPercentOnly()));
			}
		}
		if (tobBossBar.isTracking(actor))
		{
			final int max = tobBossBar.maxHealth();
			if (max > 0)
			{
				return new Reading(Source.RAID_BAR, new Health(tobBossBar.health(max), max, false, npcMaxHealth(actor),
					false));
			}
		}
		if (actor.getHealthScale() > 0)
		{
			return new Reading(Source.OVERHEAD, new Health(actor.getHealthRatio(), actor.getHealthScale(), false,
				npcMaxHealth(actor), false));
		}
		return null;
	}

	private Integer npcMaxHealth(Actor actor)
	{
		return actor instanceof NPC ? npcManager.getHealth(NpcUtil.currentId((NPC) actor)) : null;
	}

	// A boss bar at 0 counts. An overhead 0 doesn't, since a stun meter that empties looks the same.
	static boolean isDown(Reading reading, boolean dead, boolean npc, boolean attackable)
	{
		if (dead)
		{
			return true;
		}
		if (reading == null)
		{
			return false;
		}
		if (reading.fromBossBar())
		{
			return reading.health.ratio <= 0;
		}
		// Some turn into a form that can't be attacked as they die, before their bar reaches 0.
		return reading.health.ratio == 1 && npc && !attackable;
	}

	// Back for another phase after being down: it can be attacked again with health to spare.
	static boolean isBackUp(int ratio, int scale, boolean dead, boolean npc, boolean attackable)
	{
		return npc && !dead && attackable && scale > 0 && ratio > 1;
	}

	// Once down it stays so, since some die by turning into a death form that still shows a bit of health.
	static boolean stillDown(boolean wasDown, boolean downNow, boolean backUp)
	{
		return downNow || wasDown && !backUp;
	}

	static boolean canBeFought(Actor actor)
	{
		return actor instanceof NPC && NpcUtil.isAttackable((NPC) actor);
	}

	static String describe(Reading reading)
	{
		if (reading == null)
		{
			return "no reading";
		}
		final String source;
		switch (reading.source)
		{
			case GAME_BAR:
				source = "game boss bar";
				break;
			case RAID_BAR:
				source = "raid boss bar";
				break;
			default:
				source = "overhead bar";
				break;
		}
		return source + " at " + reading.health.ratio + "/" + reading.health.scale;
	}
}
