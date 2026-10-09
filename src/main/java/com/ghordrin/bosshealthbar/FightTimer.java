package com.ghordrin.bosshealthbar;

import java.util.Objects;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Constants;

// How long the fight with the current opponent has lasted, counted in game ticks from the first hit on it.
@Singleton
class FightTimer
{
	static final int NO_TICK = -1;
	// A boss that swaps to a new NPC between phases can be gone for a moment before the new one shows up.
	static final int SWAP_TICKS = 10;

	static final class Fight
	{
		final String name;
		final int startTick;
		final int endTick;

		Fight(String name, int startTick, int endTick)
		{
			this.name = name;
			this.startTick = startTick;
			this.endTick = endTick;
		}
	}

	private Actor opponent;
	private String name;
	private int endTick = NO_TICK;
	private int clearedTick = NO_TICK;
	private Actor lastHitActor;
	private int lastHitTick = NO_TICK;

	// Identifies the current fight, or NO_TICK before its first hit.
	@Getter(AccessLevel.PACKAGE)
	private int startTick = NO_TICK;
	private int shownTicks = NO_TICK;
	@Getter(AccessLevel.PACKAGE)
	private String text;

	void opponentChanged(Actor next, String nextName, boolean previousDefeated, boolean pairHeld, int tick)
	{
		opponentChanged(nextName, previousDefeated, pairHeld, next == lastHitActor ? lastHitTick : NO_TICK, tick);
		opponent = next;
	}

	// earlierHitTick is the latest hit on the new opponent from before it became the opponent, if any.
	// pairHeld is whether the new opponent's pair is still being kept from before.
	void opponentChanged(String nextName, boolean previousDefeated, boolean pairHeld, int earlierHitTick, int tick)
	{
		if (!keepsCounting(name, previousDefeated || endTick != NO_TICK, clearedTick, nextName, pairHeld, tick))
		{
			clearFight();
		}
		opponent = null;
		name = nextName;
		clearedTick = NO_TICK;
		if (startTick == NO_TICK)
		{
			startTick = recentHitTick(earlierHitTick, tick);
		}
		updateText(tick);
	}

	void opponentReturned(Actor next, Fight fight, int tick)
	{
		opponentReturned(fight, next == lastHitActor ? lastHitTick : NO_TICK, tick);
		opponent = next;
	}

	// An opponent that went out of sight carries on its own fight, whatever was fought in between.
	void opponentReturned(Fight fight, int earlierHitTick, int tick)
	{
		opponent = null;
		name = fight.name;
		startTick = fight.startTick;
		endTick = fight.endTick;
		clearedTick = NO_TICK;
		if (startTick == NO_TICK && endTick == NO_TICK)
		{
			startTick = recentHitTick(earlierHitTick, tick);
		}
		updateText(tick);
	}

	void startOver(int tick)
	{
		startTick = tick;
		endTick = NO_TICK;
		updateText(tick);
	}

	Fight save()
	{
		return new Fight(name, startTick, endTick);
	}

	void opponentDespawned(boolean defeated, int tick)
	{
		if (defeated)
		{
			defeat(tick);
		}
		opponent = null;
		clearedTick = tick;
		updateText(tick);
	}

	// The opponent can be picked after the hit that started the fight, e.g. when the game's boss bar comes up.
	void onCombatHit(Actor actor, int tick)
	{
		lastHitActor = actor;
		lastHitTick = tick;
		if (actor != null && actor == opponent)
		{
			onHit(tick);
		}
	}

	void onHit(int tick)
	{
		if (startTick == NO_TICK && endTick == NO_TICK)
		{
			startTick = tick;
			updateText(tick);
		}
	}

	void onGameTick(Actor current, boolean defeated, int tick)
	{
		final boolean tracked = current != null && current == opponent;
		onGameTick(tracked, tracked && defeated, tick);
	}

	void onGameTick(boolean tracked, boolean dead, int tick)
	{
		if (tracked)
		{
			if (dead)
			{
				defeat(tick);
			}
			else
			{
				// Some opponents drop to 0 and come back for another phase.
				endTick = NO_TICK;
			}
		}
		updateText(tick);
	}

	// The pair was forgotten, so its next member starts a new fight. The time shown stays until then.
	void pairCleared(String pair)
	{
		if (Objects.equals(name, pair))
		{
			name = null;
		}
	}

	void reset()
	{
		clearFight();
		lastHitActor = null;
		lastHitTick = NO_TICK;
	}

	private void clearFight()
	{
		opponent = null;
		name = null;
		startTick = NO_TICK;
		endTick = NO_TICK;
		clearedTick = NO_TICK;
		shownTicks = NO_TICK;
		text = null;
	}

	private void defeat(int tick)
	{
		if (startTick != NO_TICK && endTick == NO_TICK)
		{
			endTick = tick;
		}
	}

	private void updateText(int tick)
	{
		final int ticks = elapsedTicks(startTick, endTick, tick);
		if (ticks != shownTicks)
		{
			shownTicks = ticks;
			text = ticks == NO_TICK ? null : format(ticks);
		}
	}

	static int elapsedTicks(int startTick, int endTick, int tick)
	{
		if (startTick == NO_TICK)
		{
			return NO_TICK;
		}
		return Math.max(0, (endTick != NO_TICK ? endTick : tick) - startTick);
	}

	static int recentHitTick(int hitTick, int tick)
	{
		return hitTick != NO_TICK && tick - hitTick >= 0 && tick - hitTick <= 1 ? hitTick : NO_TICK;
	}

	// A new NPC with the same name carries on the fight, unless the last one was defeated or has been gone too long.
	// The members of a pair can be out of reach for longer between phases, so they don't time out while the pair is kept.
	static boolean keepsCounting(String previousName, boolean previousDefeated, int clearedTick, String nextName,
		boolean pairHeld, int tick)
	{
		return previousName != null
			&& !previousDefeated
			&& Objects.equals(previousName, nextName)
			&& (clearedTick == NO_TICK || tick - clearedTick <= SWAP_TICKS || (pairHeld && PairBosses.isPairKey(nextName)));
	}

	static String format(int ticks)
	{
		final long seconds = (long) ticks * Constants.GAME_TICK_LENGTH / 1000;
		final long hours = seconds / 3600;
		final long minutes = seconds / 60 % 60;
		final long secs = seconds % 60;
		final StringBuilder builder = new StringBuilder(8);
		if (hours > 0)
		{
			builder.append(hours).append(':');
			if (minutes < 10)
			{
				builder.append('0');
			}
		}
		builder.append(minutes).append(':');
		if (secs < 10)
		{
			builder.append('0');
		}
		return builder.append(secs).toString();
	}
}
