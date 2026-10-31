package com.ghordrin.bosshealthbar.render;

import static com.ghordrin.bosshealthbar.render.HealthText.estimateHealth;
import static com.ghordrin.bosshealthbar.render.HealthText.hitpointsPercent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.data.Health;
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
		assertEquals("50", hitpointsPercent(50, 100, 0));
		assertEquals("99", hitpointsPercent(99, 100, 0));
	}

	@Test
	public void hitpointsPercentOnlyShowsZeroOrFullWhenTrulyEmptyOrFull()
	{
		for (int decimals = 0; decimals <= HealthText.MAX_DECIMALS; decimals++)
		{
			assertEquals(0.0, Double.parseDouble(hitpointsPercent(0, 100, decimals)), 0.0);
			assertEquals(100.0, Double.parseDouble(hitpointsPercent(100, 100, decimals)), 0.0);
		}
	}

	@Test
	public void hitpointsPercentClampsAwayFromZeroAndFullWhileStillAliveOrDamaged()
	{
		assertEquals("1", hitpointsPercent(1, 1_000_000, 0));
		assertEquals("99", hitpointsPercent(999_999, 1_000_000, 0));
		assertEquals("0.1", hitpointsPercent(1, 1_000_000, 1));
		assertEquals("99.9", hitpointsPercent(999_999, 1_000_000, 1));
		assertEquals("0.01", hitpointsPercent(1, 1_000_000, 2));
		assertEquals("99.99", hitpointsPercent(999_999, 1_000_000, 2));
	}

	@Test
	public void hitpointsPercentShowsTheDecimalsAsked()
	{
		assertEquals("62.5", hitpointsPercent(250, 400, 1));
		assertEquals("62.50", hitpointsPercent(250, 400, 2));
		assertEquals("33.33", hitpointsPercent(1, 3, 2));
		assertEquals("5.08", hitpointsPercent(127, 2500, 2));
		assertEquals("100.00", hitpointsPercent(400, 400, 2));
		assertEquals("0.00", hitpointsPercent(0, 400, 2));
	}

	@Test
	public void hitpointsPercentKeepsDecimalsInRange()
	{
		assertEquals("50", hitpointsPercent(1, 2, -1));
		assertEquals("50.00", hitpointsPercent(1, 2, 7));
	}

	private static Health health(Integer maxHealth, int ratio, int scale, boolean exactHealth, boolean percentOnly)
	{
		return new Health(ratio, scale, exactHealth, maxHealth, percentOnly);
	}

	private static String text(HitpointsTextMode mode, Health health)
	{
		return HealthText.hitpointsText(mode, health, 0);
	}

	@Test
	public void hitpointsTextCarriesTheDecimals()
	{
		final Health exact = health(400, 250, 400, true, false);
		assertEquals("62.5%", HealthText.hitpointsText(HitpointsTextMode.PERCENTAGE, exact, 1));
		assertEquals("250 / 400   62.50%", HealthText.hitpointsText(HitpointsTextMode.BOTH, exact, 2));
		assertEquals("250 / 400", HealthText.hitpointsText(HitpointsTextMode.HITPOINTS, exact, 2));
	}

	@Test
	public void hitpointsTextFollowsTheMode()
	{
		final Health estimated = health(400, 15, 30, false, false);
		assertNull(text(HitpointsTextMode.NONE, estimated));
		assertEquals("50%", text(HitpointsTextMode.PERCENTAGE, estimated));
		assertEquals("200 / 400", text(HitpointsTextMode.HITPOINTS, estimated));
		assertEquals("200", text(HitpointsTextMode.REMAINING, estimated));
		assertEquals("200 / 400   50%", text(HitpointsTextMode.BOTH, estimated));
		assertEquals("200   50%", text(HitpointsTextMode.REMAINING_PERCENTAGE, estimated));
	}

	@Test
	public void hitpointsTextUsesExactHealthWhenKnown()
	{
		final Health exact = health(400, 250, 400, true, false);
		assertEquals("63%", text(HitpointsTextMode.PERCENTAGE, exact));
		assertEquals("250 / 400", text(HitpointsTextMode.HITPOINTS, exact));
		assertEquals("250", text(HitpointsTextMode.REMAINING, exact));
		assertEquals("250 / 400   63%", text(HitpointsTextMode.BOTH, exact));
		assertEquals("250   63%", text(HitpointsTextMode.REMAINING_PERCENTAGE, exact));
	}

	@Test
	public void hitpointsTextFallsBackToThePercentage()
	{
		final Health percentOnly = health(400, 15, 30, false, true);
		assertEquals("50%", text(HitpointsTextMode.HITPOINTS, percentOnly));
		assertEquals("50%", text(HitpointsTextMode.REMAINING, percentOnly));
		assertEquals("50%", text(HitpointsTextMode.BOTH, percentOnly));
		assertEquals("50%", text(HitpointsTextMode.REMAINING_PERCENTAGE, percentOnly));
		assertNull(text(HitpointsTextMode.NONE, percentOnly));

		final Health unknownMax = health(null, 15, 30, false, false);
		assertEquals("50%", text(HitpointsTextMode.HITPOINTS, unknownMax));
		assertEquals("50%", text(HitpointsTextMode.BOTH, unknownMax));
		assertNull(text(HitpointsTextMode.NONE, unknownMax));
	}
}
