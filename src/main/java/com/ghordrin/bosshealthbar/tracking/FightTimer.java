package com.ghordrin.bosshealthbar.tracking;

import com.ghordrin.bosshealthbar.debug.DebugLog;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Constants;
import net.runelite.client.util.Text;

// How long the fight with the current opponent has lasted, counted in game ticks from when the player goes for it and
// is near it, or from the first hit on it if that comes first.
@Singleton
public class FightTimer
{
	public static final int NO_TICK = -1;
	// A boss that swaps to a new NPC between phases can be gone for a moment before the new one shows up.
	static final int SWAP_TICKS = 10;
	// Counted from the first 0: a despawn after that follows within a moment, past which the clock stops where it is.
	// It matches SWAP_TICKS today, so the clock stops on the last tick a return would still be the same fight, but the
	// two measure different things and either can be tuned on its own.
	static final int DEATH_TICKS = 10;
	// How far the game's time may sit from ours before it is about something else than this fight.
	static final int REPORT_GAP_TICKS = 100;

	// The wordings the game puts in front of a time it kept for a fight, and the time itself. Fight,
	// Challenge and Corrupted challenge duration all end in the same word, which stands for them all.
	// "Total completion time" is the exception: it is the time of everything before it, not of this fight.
	private static final Pattern REPORTED_TIME = Pattern.compile(
		"(?i)(?:duration:|subdued in|(?<!total )completion time:)"
			+ "\\s*(?:(\\d+):)?(\\d+):(\\d{2})(?:\\.(\\d{2}))?");

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
	// Where the death sequence of a kill made in view began, i.e. the first 0 the opponent read.
	private int deadSince = NO_TICK;
	private Actor lastHitActor;
	private int lastHitTick = NO_TICK;

	// Identifies the current fight, or NO_TICK before it has started.
	@Getter
	private int startTick = NO_TICK;
	private int shownTicks = NO_TICK;
	@Getter
	private String text;
	@Getter
	private boolean preciseTiming;
	private int pendingReport = NO_TICK;
	private int pendingSince = NO_TICK;

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
		// Whatever is the opponent now, it isn't the one that was defeated.
		deadSince = NO_TICK;
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
		deadSince = NO_TICK;
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
		deadSince = NO_TICK;
		updateText(tick);
	}

	public int getElapsedTicks(int tick)
	{
		return elapsedTicks(startTick, endTick, tick);
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
			defeat(endOfFight(deadSince != NO_TICK ? deadSince : defeatTick, tick));
		}
		opponent = null;
		clearedTick = tick;
		updateText(tick);
	}

	// The opponent can be picked after the hit that started the fight, e.g. when the game's boss bar comes up.
	public void onCombatHit(Actor actor, int tick)
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

	void onEngaged(int tick)
	{
		if (inFight && startTick == NO_TICK && endTick == NO_TICK)
		{
			startTick = tick;
			debugLog.add("Fight timer started, going for the opponent");
			updateText(tick);
		}
	}

	public void onGameTick(Actor current, boolean defeated, int zeroSince, int tick)
	{
		final boolean tracked = current != null && current == opponent;
		onGameTick(tracked, tracked && defeated, tracked ? zeroSince : NO_TICK, tick);
	}

	// zeroSince is the tick the opponent's health first read 0, where its death sequence began.
	void onGameTick(boolean tracked, boolean defeated, int zeroSince, int tick)
	{
		if (tracked)
		{
			if (defeated)
			{
				if (deadSince == NO_TICK)
				{
					deadSince = zeroSince != NO_TICK ? Math.min(zeroSince, tick) : tick;
					debugLog.add("Fight timer: the opponent is defeated, the clock runs until it is gone");
				}
				if (tick - deadSince >= DEATH_TICKS)
				{
					defeat(deadSince + DEATH_TICKS);
				}
			}
			else
			{
				deadSince = NO_TICK;
				if (endTick != NO_TICK)
				{
					// Some opponents drop to 0 and come back for another phase.
					endTick = NO_TICK;
					debugLog.add("Fight timer running again, the opponent is back up");
				}
			}
		}
		usePendingReport(tracked && defeated, tick);
		updateText(tick);
	}

	// The game shows its own durations with hundredths while the player has precise timing on, so the fight
	// time follows that setting instead of a setting of ours.
	public void setPreciseTiming(boolean precise)
	{
		if (precise != preciseTiming)
		{
			preciseTiming = precise;
			// The tick count hasn't changed, so the text has to be asked for again.
			shownTicks = NO_TICK;
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
		inFight = false;
		startTick = NO_TICK;
		endTick = NO_TICK;
		clearedTick = NO_TICK;
		deadSince = NO_TICK;
		shownTicks = NO_TICK;
		pendingReport = NO_TICK;
		pendingSince = NO_TICK;
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
			text = ticks == NO_TICK ? null : format(ticks, preciseTiming);
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

	// The fight ends when the opponent is gone, but never more than the death window after the kill was known.
	static int endOfFight(int knownDeadSince, int goneTick)
	{
		return knownDeadSince == NO_TICK ? goneTick : Math.min(goneTick, knownDeadSince + DEATH_TICKS);
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

	// The game prints several wordings of a fight or challenge time, so match on the word they share.
	public static boolean mentionsDuration(String message)
	{
		return message != null && message.toLowerCase(Locale.ROOT).contains("duration");
	}

	// The time the game reports for a kill, in ticks, or NO_TICK when the message holds none. The wordings
	// are the ones the game uses for a fight it times; a total, a personal best and a lap are left out, so
	// only the first time after the wording counts.
	public static int reportedTicks(String message)
	{
		if (message == null)
		{
			return NO_TICK;
		}
		final Matcher matcher = REPORTED_TIME.matcher(Text.removeTags(message));
		if (!matcher.find())
		{
			return NO_TICK;
		}
		final long hours = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0;
		final long minutes = Long.parseLong(matcher.group(2));
		final long seconds = Long.parseLong(matcher.group(3));
		final long hundredths = matcher.group(4) != null ? Long.parseLong(matcher.group(4)) : 0;
		final long millis = ((hours * 60 + minutes) * 60 + seconds) * 1000 + hundredths * 10;
		// Without precise timing the game prints whole seconds, which is its own time rounded down, so the
		// tick count rounds up to land inside the second it printed instead of the one before it.
		final long rounded = matcher.group(4) != null
			? millis : millis + Constants.GAME_TICK_LENGTH - 1;
		return (int) (rounded / Constants.GAME_TICK_LENGTH);
	}

	// The game can print its time on the tick the boss goes down, before the kill is confirmed a tick later,
	// so a time that can't be used yet is kept and tried again on the next ticks.
	public void onReportedTime(int ticks, boolean defeated, int tick)
	{
		if (ticks != NO_TICK && !takeOverTicks(ticks, defeated, tick))
		{
			pendingReport = ticks;
			pendingSince = tick;
		}
	}

	private void usePendingReport(boolean defeated, int tick)
	{
		if (pendingReport != NO_TICK
			&& (takeOverTicks(pendingReport, defeated, tick) || tick - pendingSince >= DEATH_TICKS))
		{
			pendingReport = NO_TICK;
		}
	}

	// The game stopping its own clock is the end of the fight, and its time is the one the player compares
	// against, so the clock stops here and takes it over. A time too far from ours belongs to something
	// larger than this fight, such as a whole raid reported on its last boss, and is left alone.
	boolean takeOverTicks(int ticks, boolean defeated, int tick)
	{
		if (ticks == NO_TICK || startTick == NO_TICK || !(defeated || endTick != NO_TICK))
		{
			return false;
		}
		final int ours = elapsedTicks(startTick, endTick, tick);
		if (Math.abs(ticks - ours) > REPORT_GAP_TICKS)
		{
			debugLog.add("The game's {} ticks are too far from our {}, the fight time is left as it is", ticks, ours);
			return false;
		}
		endTick = startTick + ticks;
		debugLog.add("Fight time taken from the game: {} ticks, ours was {}", ticks, ours);
		updateText(tick);
		return true;
	}

	// With precise timing the hundredths come straight off the ticks, so they land on .00, .20, .40, .60 or
	// .80, the same values the game prints.
	public static String format(int ticks, boolean precise)
	{
		final long millis = (long) ticks * Constants.GAME_TICK_LENGTH;
		final long seconds = millis / 1000;
		final long hours = seconds / 3600;
		final long minutes = seconds / 60 % 60;
		final long secs = seconds % 60;
		final StringBuilder builder = new StringBuilder(11);
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
		builder.append(secs);
		if (precise)
		{
			final long hundredths = millis % 1000 / 10;
			builder.append('.');
			if (hundredths < 10)
			{
				builder.append('0');
			}
			builder.append(hundredths);
		}
		return builder.toString();
	}
}
