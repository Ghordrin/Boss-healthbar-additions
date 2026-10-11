package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.HealthText.estimateHealth;
import static com.ghordrin.bosshealthbar.HealthText.hitpointsPercent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class HealthTextTest
{
	private static final int[] MAX_HEALTHS = {1, 2, 10, 99, 100, 255, 1000, 4000};
	private static final int[] SCALES = {1, 2, 30, 100, 255};

	@Test
	public void estimateHealthRoundTripsToTheObservedRatio()
	{
		for (int max : MAX_HEALTHS)
		{
			for (int scale : SCALES)
			{
				for (int health = 1; health <= max; health++)
				{
					final int ratio = 1 + (scale - 1) * health / max;
					final int estimate = estimateHealth(ratio, scale, max);

					assertTrue("estimate " + estimate + " out of range for max " + max,
						estimate >= 1 && estimate <= max);
					assertEquals("max " + max + " scale " + scale + " health " + health,
						ratio, 1 + (scale - 1) * estimate / max);
				}
			}
		}
	}

	@Test
	public void estimateHealthReturnsZeroWhenTheBarIsEmpty()
	{
		assertEquals(0, estimateHealth(0, 30, 400));
		assertEquals(0, estimateHealth(-1, 30, 400));
	}

	@Test
	public void estimateHealthReturnsFullAtTheTopOfTheScale()
	{
		assertEquals(400, estimateHealth(30, 30, 400));
	}

	@Test
	public void estimateHealthHandlesAScaleOfOne()
	{
		final int estimate = estimateHealth(1, 1, 100);
		assertTrue(estimate >= 1 && estimate <= 100);
	}

	@Test
	public void hitpointsPercentMatchesTheRawRatioInTheMiddleOfTheBar()
	{
		assertEquals(50, hitpointsPercent(50, 100));
		assertEquals(99, hitpointsPercent(99, 100));
	}

	@Test
	public void hitpointsPercentOnlyShowsZeroOrFullWhenTrulyEmptyOrFull()
	{
		assertEquals(0, hitpointsPercent(0, 100));
		assertEquals(100, hitpointsPercent(100, 100));
	}

	@Test
	public void hitpointsPercentClampsAwayFromZeroAndFullWhileStillAliveOrDamaged()
	{
		assertEquals(1, hitpointsPercent(1, 1_000_000));
		assertEquals(99, hitpointsPercent(999_999, 1_000_000));
	}

	private static Health health(Integer maxHealth, int ratio, int scale, boolean exactHealth, boolean percentOnly)
	{
		return new Health(ratio, scale, exactHealth, maxHealth, percentOnly);
	}

	private static String text(HitpointsTextMode mode, Health health)
	{
		return HealthText.hitpointsText(mode, health);
	}

	@Test
	public void hitpointsTextFollowsTheMode()
	{
		final Health estimated = health(400, 15, 30, false, false);
		assertNull(text(HitpointsTextMode.NONE, estimated));
		assertEquals("50%", text(HitpointsTextMode.PERCENTAGE, estimated));
		assertEquals("200 / 400", text(HitpointsTextMode.HITPOINTS, estimated));
		assertEquals("200 / 400   50%", text(HitpointsTextMode.BOTH, estimated));
	}

	@Test
	public void hitpointsTextUsesExactHealthWhenKnown()
	{
		final Health exact = health(400, 250, 400, true, false);
		assertEquals("63%", text(HitpointsTextMode.PERCENTAGE, exact));
		assertEquals("250 / 400", text(HitpointsTextMode.HITPOINTS, exact));
		assertEquals("250 / 400   63%", text(HitpointsTextMode.BOTH, exact));
	}

	@Test
	public void hitpointsTextFallsBackToThePercentage()
	{
		final Health percentOnly = health(400, 15, 30, false, true);
		assertEquals("50%", text(HitpointsTextMode.HITPOINTS, percentOnly));
		assertEquals("50%", text(HitpointsTextMode.BOTH, percentOnly));
		assertNull(text(HitpointsTextMode.NONE, percentOnly));

		final Health unknownMax = health(null, 15, 30, false, false);
		assertEquals("50%", text(HitpointsTextMode.HITPOINTS, unknownMax));
		assertEquals("50%", text(HitpointsTextMode.BOTH, unknownMax));
		assertNull(text(HitpointsTextMode.NONE, unknownMax));
	}
}
