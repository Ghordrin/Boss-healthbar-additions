package com.ghordrin.bosshealthbar;

import javax.inject.Inject;
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
		final int group;
		final int startTick;
		final int endTick;

		Fight(int group, int startTick, int endTick)
		{
			this.group = group;
			this.startTick = startTick;
			this.endTick = endTick;
		}
	}

	private final DebugLog debugLog;

	private Actor opponent;
	private boolean inFight;
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

	@Inject
	FightTimer(DebugLog debugLog)
	{
		this.debugLog = debugLog;
	}

	// Returns whether the fight carries on with the new opponent.
	boolean opponentChanged(Actor next, boolean aliveMember, boolean previousDefeated, int tick)
	{
		final boolean carried = opponentChanged(aliveMember, previousDefeated,
			next == lastHitActor ? lastHitTick : NO_TICK, tick);
		opponent = next;
		return carried;
	}

	// earlierHitTick is the latest hit on the new opponent from before it became the opponent, if any.
	// aliveMember is whether the new opponent was already fought in this fight and is still alive.
	boolean opponentChanged(boolean aliveMember, boolean previousDefeated, int earlierHitTick, int tick)
	{
		final boolean carried = keepsCounting(inFight, aliveMember, previousDefeated || endTick != NO_TICK, clearedTick,
			tick);
		if (carried)
		{
			debugLog.add("Fight timer carried over to the new opponent ({})", elapsedText(tick));
			if (aliveMember && endTick != NO_TICK)
			{
				endTick = NO_TICK;
				debugLog.add("Fight timer running again, the fight continues");
			}
		}
		else
		{
			if (startTick != NO_TICK)
			{
				debugLog.add("Fight timer: new fight, previous one cleared ({})", elapsedText(tick));
			}
			clearFight();
		}
		opponent = null;
		inFight = true;
		clearedTick = NO_TICK;
		if (startTick == NO_TICK)
		{
			startTick = recentHitTick(earlierHitTick, tick);
			if (startTick != NO_TICK)
			{
				debugLog.add("Fight timer started from a hit on tick {}", startTick);
			}
		}
		updateText(tick);
		return carried;
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
		inFight = true;
		startTick = fight.startTick;
		endTick = fight.endTick;
		clearedTick = NO_TICK;
		if (startTick == NO_TICK && endTick == NO_TICK)
		{
			startTick = recentHitTick(earlierHitTick, tick);
		}
		debugLog.add("Fight timer resumed a remembered fight (start tick {}, end tick {})", startTick, endTick);
		updateText(tick);
	}

	void startOver(int tick)
	{
		debugLog.add("Fight timer started over (was {})", elapsedText(tick));
		startTick = tick;
		endTick = NO_TICK;
		updateText(tick);
	}

	Fight save(int group)
	{
		return new Fight(group, startTick, endTick);
	}

	void opponentDespawned(boolean defeated, int tick)
	{
		opponentDespawned(defeated, tick, tick);
	}

	// defeatTick is when it was defeated, which can be before the despawn.
	void opponentDespawned(boolean defeated, int defeatTick, int tick)
	{
		if (defeated)
		{
			defeat(defeatTick);
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
			debugLog.add("Fight timer started");
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
			else if (endTick != NO_TICK)
			{
				// Some opponents drop to 0 and come back for another phase.
				endTick = NO_TICK;
				debugLog.add("Fight timer running again, the opponent is back up");
			}
		}
		updateText(tick);
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
		inFight = false;
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
			debugLog.add("Fight timer stopped, opponent defeated ({})", elapsedText(tick));
		}
	}

	private String elapsedText(int tick)
	{
		final int ticks = elapsedTicks(startTick, endTick, tick);
		return ticks == NO_TICK ? "not started" : ticks + " ticks";
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

	// An NPC already fought in this fight carries it on while it's alive. Any other does too, unless the last opponent
	// was defeated or has been gone too long.
	static boolean keepsCounting(boolean inFight, boolean aliveMember, boolean previousEnded, int clearedTick, int tick)
	{
		return inFight
			&& (aliveMember || (!previousEnded && (clearedTick == NO_TICK || tick - clearedTick <= SWAP_TICKS)));
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
