package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BarTextPainter.estimateHealth;
import static com.ghordrin.bosshealthbar.BarTextPainter.hitpointsPercent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BarTextPainterTest
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

	@Test
	public void thePartnerRowFitsTheHitpointsFontWhileItHasText()
	{
		assertEquals(14, BarTextPainter.partnerRowHeight(true, false, 10, 4));
		assertEquals(14, BarTextPainter.partnerRowHeight(false, true, 10, 4));
		assertEquals(0, BarTextPainter.partnerRowHeight(false, false, 10, 4));
	}

	@Test
	public void theSlotSizingSwapsEveryDigit()
	{
		assertEquals("0:00", BarTextPainter.withDigits("1:23", '0'));
		assertEquals("4:44:44", BarTextPainter.withDigits("1:05:09", '4'));
	}
}
