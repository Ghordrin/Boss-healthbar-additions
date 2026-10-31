package com.ghordrin.bosshealthbar.integrations;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.util.QuantityFormatter;

// A kill count milestone or a new personal best, taken from the moment RuneLite's Chat Commands plugin saves
// it, so the bar of that boss can mark it while it shows the kill. Both are saved under the boss name.
@Singleton
public class KillMoments
{
	public static final String PERSONAL_BEST_GROUP = "personalbest";
	// The game's message comes a tick or two either side of the death, so a moment waits a while for its bar.
	private static final long SHOWN_MILLIS = 15_000;

	private final BossHealthBarConfig config;

	private String key;
	private int milestone;
	private boolean personalBest;
	private long savedAt;

	@Inject
	KillMoments(BossHealthBarConfig config)
	{
		this.config = config;
	}

	public void killCountSaved(String bossKey, String oldValue, String newValue, long nowMillis)
	{
		if (!config.killMilestones())
		{
			return;
		}
		final Integer count = newValue != null ? Ints.tryParse(newValue) : null;
		final Integer before = oldValue != null ? Ints.tryParse(oldValue) : null;
		// Only a count going up is a kill. Chat Commands also writes counts it reads off the game's logs.
		if (count != null && (before == null || count > before) && isMilestone(count, config.killMilestoneEvery()))
		{
			note(bossKey, nowMillis);
			milestone = count;
		}
	}

	public void personalBestSaved(String bossKey, String oldValue, String newValue, long nowMillis)
	{
		final Double time = newValue != null ? Doubles.tryParse(newValue) : null;
		final Double before = oldValue != null ? Doubles.tryParse(oldValue) : null;
		// Only a faster time is a new best. Chat Commands also saves the best the game repeats after a kill,
		// and the one it reads off the adventure log, which would otherwise read as new the first time.
		if (config.personalBests() && time != null && before != null && time < before)
		{
			note(bossKey, nowMillis);
			personalBest = true;
		}
	}

	// Null when there is nothing to mark for this boss now.
	public String labelFor(String bossKey, long nowMillis)
	{
		if (bossKey == null || !bossKey.equals(key) || nowMillis - savedAt > SHOWN_MILLIS)
		{
			return null;
		}
		return label(milestone, personalBest);
	}

	private void note(String bossKey, long nowMillis)
	{
		if (!bossKey.equals(key) || nowMillis - savedAt > SHOWN_MILLIS)
		{
			milestone = 0;
			personalBest = false;
		}
		key = bossKey;
		savedAt = nowMillis;
	}

	@VisibleForTesting
	static boolean isMilestone(int count, int every)
	{
		return count == 1 || (every > 0 && count % every == 0);
	}

	@VisibleForTesting
	static String label(int milestone, boolean personalBest)
	{
		final String kill = milestone > 0 ? ordinal(milestone) + " kill" : null;
		if (kill != null && personalBest)
		{
			return kill + ", New PB";
		}
		return kill != null ? kill : personalBest ? "New PB" : null;
	}

	@VisibleForTesting
	static String ordinal(int number)
	{
		final int lastTwo = number % 100;
		final int last = number % 10;
		final String suffix = lastTwo >= 11 && lastTwo <= 13 ? "th"
			: last == 1 ? "st" : last == 2 ? "nd" : last == 3 ? "rd" : "th";
		return QuantityFormatter.formatNumber(number) + suffix;
	}
}
