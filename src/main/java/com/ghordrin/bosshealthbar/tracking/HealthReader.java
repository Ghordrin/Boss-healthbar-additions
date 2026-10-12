package com.ghordrin.bosshealthbar.tracking;

import com.ghordrin.bosshealthbar.data.Health;
import com.ghordrin.bosshealthbar.data.NpcUtil;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import com.ghordrin.bosshealthbar.gamebars.GameBossBar;
import com.ghordrin.bosshealthbar.gamebars.TobBossBar;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.client.game.NPCManager;

// Where an opponent's health comes from, and what a reading says about whether it's down.
@Singleton
public class HealthReader
{
	public enum Source
	{
		GAME_BAR,
		RAID_BAR,
		OVERHEAD
	}

	public static final class Reading
	{
		public final Source source;
		public final Health health;

		public Reading(Source source, Health health)
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
	private Actor maxHealthActor;
	private int maxHealthForm = -1;
	private Integer maxHealth;

	@Inject
	HealthReader(GameBossBar gameBossBar, TobBossBar tobBossBar, NPCManager npcManager)
	{
		this.gameBossBar = gameBossBar;
		this.tobBossBar = tobBossBar;
		this.npcManager = npcManager;
	}

	// The game's bars have exact hitpoints, and some bosses stop sending overhead health updates while they're
	// shown, so they come first. Null while there's no reading at all.
	public Reading read(Actor actor)
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
		// A scale with a ratio below 0 is the client's "no value", not a reading.
		if (actor.getHealthScale() > 0 && actor.getHealthRatio() >= 0)
		{
			return new Reading(Source.OVERHEAD, new Health(actor.getHealthRatio(), actor.getHealthScale(), false,
				npcMaxHealth(actor), false));
		}
		return null;
	}

	void reset()
	{
		maxHealthActor = null;
		maxHealthForm = -1;
		maxHealth = null;
	}

	// Kept for the last NPC and form, since the overlay asks every frame.
	private Integer npcMaxHealth(Actor actor)
	{
		if (!(actor instanceof NPC))
		{
			return null;
		}
		final int form = NpcUtil.currentId((NPC) actor);
		if (actor == maxHealthActor && form == maxHealthForm)
		{
			return maxHealth;
		}
		final Integer health = npcManager.getHealth(form);
		// An unknown max health is asked again next time.
		if (health != null)
		{
			maxHealthActor = actor;
			maxHealthForm = form;
			maxHealth = health;
		}
		return health;
	}

	// At 0 for now, which isn't a kill on its own: some bosses sit at 0 or heal back from it. A boss bar's own
	// health wins over isDead(), which only means the overhead bar's last update started at 0.
	static boolean isDown(Reading reading, boolean dead, boolean npc, boolean attackable)
	{
		if (reading != null && reading.fromBossBar())
		{
			return reading.health.ratio <= 0;
		}
		// Some turn into a form that can't be attacked as they die, before their bar reaches 0.
		return dead || reading != null && reading.health.ratio == 1 && npc && !attackable;
	}

	// Back for another phase after being down. A boss bar refilling counts; on the overhead bar it has to be
	// attackable again with health to spare.
	static boolean isBackUp(Reading reading, boolean dead, boolean attackable)
	{
		if (reading == null)
		{
			return false;
		}
		if (reading.fromBossBar())
		{
			return reading.health.ratio > 0;
		}
		return !dead && attackable && reading.health.ratio > 1;
	}

	// Once down it stays so until back up, since some die by turning into a death form that still shows a bit of health.
	static boolean stillDown(boolean wasDown, boolean downNow, boolean backUp)
	{
		return downNow || wasDown && !backUp;
	}

	// The first tick of a run of ticks that it's been down and unable to be attacked.
	static int unfightableSince(boolean down, boolean attackable, int since, int tick)
	{
		if (!down || attackable)
		{
			return FightTimer.NO_TICK;
		}
		return since == FightTimer.NO_TICK ? tick : since;
	}

	// A kill once it's been down and unable to be attacked on two ticks in a row. A player at 0 is one at once.
	static boolean isDefeated(boolean down, boolean npc, int unfightableSince, int tick)
	{
		return down && (!npc || unfightableSince != FightTimer.NO_TICK && tick > unfightableSince);
	}

	// Up again after a confirmed kill. A phase change takes about as long as a boss swapping to its next NPC, so only
	// a longer gap than that means the one that comes back is the next one rather than the same fight carrying on.
	static boolean isRevived(int downSince, int tick)
	{
		return downSince != FightTimer.NO_TICK && tick - downSince > FightTimer.SWAP_TICKS;
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

	// The overhead bar is up but its ratio is the client's "no value".
	static boolean hasNoValue(int ratio, int scale)
	{
		return scale > 0 && ratio < 0;
	}

	static int form(Actor actor)
	{
		return actor instanceof NPC ? NpcUtil.currentId((NPC) actor) : -1;
	}

	// One actor's down latch and confirmed kill, updated once a tick by whoever reads its health.
	static final class DownState
	{
		boolean down;
		int unfightableSince = FightTimer.NO_TICK;
		boolean defeated;
		private int downSince = FightTimer.NO_TICK;
		private boolean revived;

		// Logs what changed when given a log, so only one owner per NPC passes one. owner starts each line.
		void update(Reading reading, boolean dead, boolean npc, boolean attackable, int tick, DebugLog debugLog,
			String owner, Actor actor)
		{
			final boolean wasDown = down;
			final boolean wasDefeated = defeated;
			final int wasDownSince = downSince;
			down = stillDown(down, isDown(reading, dead, npc, attackable), isBackUp(reading, dead, attackable));
			downSince = !down ? FightTimer.NO_TICK : wasDown ? downSince : tick;
			unfightableSince = HealthReader.unfightableSince(down, npc && attackable, unfightableSince, tick);
			// A kill stays confirmed while it's down, since some die by turning into a form with nothing to read.
			// Being attackable again means it was never a kill.
			defeated = down && (wasDefeated && !(npc && attackable)
				|| isDefeated(down, npc, unfightableSince, tick));
			// Up again long after a confirmed kill, so this is the next one and not another phase.
			final boolean back = wasDefeated && !down && isRevived(wasDownSince, tick);
			revived |= back;
			if (debugLog == null)
			{
				return;
			}
			if (down != wasDown)
			{
				debugLog.add("{}{} {} ({}, dead {})", owner, DebugLog.describe(actor), down ? "down" : "back up",
					describe(reading), dead);
			}
			if (defeated && !wasDefeated)
			{
				debugLog.add("{}{} defeated, can no longer be attacked", owner, DebugLog.describe(actor));
			}
			if (back)
			{
				debugLog.add("{}{} is up again after being defeated", owner, DebugLog.describe(actor));
			}
		}

		// Whether health came back after a confirmed kill, read once by the owner so it acts on it one time.
		boolean takeRevived()
		{
			final boolean revived = this.revived;
			this.revived = false;
			return revived;
		}

		void clear()
		{
			down = false;
			unfightableSince = FightTimer.NO_TICK;
			defeated = false;
			downSince = FightTimer.NO_TICK;
			revived = false;
		}
	}

	// What the debug log last said about one actor's health, so it only gets a line when something that matters changes:
	// the reading's source, a ratio of 0 or 1, an overhead bar with no value, isDead(), being attackable, or the form.
	static final class Watch
	{
		private boolean seen;
		private Source source;
		private int level;
		private boolean dead;
		private boolean attackable;
		private int form;

		boolean changed(Reading reading, boolean noValue, boolean dead, boolean attackable, int form)
		{
			final Source source = reading != null ? reading.source : null;
			final int level = reading != null ? Math.max(-1, Math.min(2, reading.health.ratio)) : noValue ? -2 : -1;
			if (seen && source == this.source && level == this.level && dead == this.dead
				&& attackable == this.attackable && form == this.form)
			{
				return false;
			}
			seen = true;
			this.source = source;
			this.level = level;
			this.dead = dead;
			this.attackable = attackable;
			this.form = form;
			return true;
		}

		void clear()
		{
			seen = false;
		}

		void log(DebugLog debugLog, Actor actor, Reading reading, boolean dead, boolean attackable)
		{
			final int form = form(actor);
			final int ratio = actor.getHealthRatio();
			final int scale = actor.getHealthScale();
			final boolean noValue = reading == null && hasNoValue(ratio, scale);
			if (changed(reading, noValue, dead, attackable, form))
			{
				debugLog.add("Health of {}: {}, dead {}, attackable {}, form {}", DebugLog.describe(actor),
					noValue ? "overhead bar with no value (" + ratio + "/" + scale + "), ignored" : describe(reading),
					dead, attackable, form);
			}
		}
	}
}
