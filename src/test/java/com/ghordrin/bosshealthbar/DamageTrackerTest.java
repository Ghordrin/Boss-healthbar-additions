package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.DamageTracker.COMBO_WINDOW;
import static com.ghordrin.bosshealthbar.DamageTracker.nextComboDamage;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DamageTrackerTest
{
	private static final long WINDOW_MILLIS = COMBO_WINDOW.toMillis();

	@Test
	public void firstHitStartsTheCombo()
	{
		assertEquals(50, nextComboDamage(0, 0, 1_000, 50));
	}

	@Test
	public void hitsWithinTheWindowAddUp()
	{
		final int afterFirstHit = nextComboDamage(0, 0, 1_000, 50);
		final int afterSecondHit = nextComboDamage(afterFirstHit, 1_000, 1_000 + WINDOW_MILLIS - 1, 30);

		assertEquals(80, afterSecondHit);
	}

	@Test
	public void aHitRightAtTheWindowEdgeStillCounts()
	{
		assertEquals(80, nextComboDamage(50, 1_000, 1_000 + WINDOW_MILLIS, 30));
	}

	@Test
	public void aHitPastTheWindowStartsAFreshCombo()
	{
		assertEquals(30, nextComboDamage(50, 1_000, 1_000 + WINDOW_MILLIS + 1, 30));
	}

	@Test
	public void aStalePreviousComboIsIgnoredWhenThereWasNoRecentHit()
	{
		assertEquals(10, nextComboDamage(999, 0, 5_000, 10));
	}
}
