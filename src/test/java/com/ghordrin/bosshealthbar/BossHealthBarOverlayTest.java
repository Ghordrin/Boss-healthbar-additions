package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BarPainter.capScale;
import static com.ghordrin.bosshealthbar.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.BarAnimation.easeOut;
import static com.ghordrin.bosshealthbar.BarTextPainter.estimateHealth;
import static com.ghordrin.bosshealthbar.BarTextPainter.hitpointsPercent;
import static com.ghordrin.bosshealthbar.BossHealthBarOverlay.markerFraction;
import static com.ghordrin.bosshealthbar.BarAnimation.progress;
import static com.ghordrin.bosshealthbar.BarPainter.scaledCapWidth;
import static com.ghordrin.bosshealthbar.BarPainter.scaledFrameStroke;
import java.time.Duration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BossHealthBarOverlayTest
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
	public void markersSitWhereTheGamePutsThem()
	{
		assertEquals(0f, markerFraction(1, 400), 0.0001f);
		assertEquals(1f, markerFraction(401, 400), 0.0001f);
		assertEquals(0.5f, markerFraction(201, 400), 0.0001f);
	}

	@Test
	public void markersStayOnTheBar()
	{
		for (int max : MAX_HEALTHS)
		{
			for (int value = 0; value <= max + 2; value++)
			{
				final float fraction = markerFraction(value, max);
				assertTrue("value " + value + " of " + max + " gave " + fraction,
					fraction >= 0f && fraction <= 1f);
			}
		}
	}

	@Test
	public void clampKeepsFractionsInRange()
	{
		assertEquals(0f, clamp01(-5f), 0f);
		assertEquals(1f, clamp01(5f), 0f);
		assertEquals(0.25f, clamp01(0.25f), 0f);
	}

	@Test
	public void progressRunsFromZeroToOneAcrossTheDuration()
	{
		final Duration delay = Duration.ofMillis(100);
		final Duration duration = Duration.ofMillis(400);

		assertEquals(0f, progress(Duration.ofMillis(50).toNanos(), delay, duration), 0.0001f);
		assertEquals(0f, progress(delay.toNanos(), delay, duration), 0.0001f);
		assertEquals(0.5f, progress(Duration.ofMillis(300).toNanos(), delay, duration), 0.0001f);
		assertEquals(1f, progress(Duration.ofMillis(500).toNanos(), delay, duration), 0.0001f);
		assertEquals(1f, progress(Duration.ofSeconds(10).toNanos(), delay, duration), 0.0001f);
		assertEquals(1f, progress(Long.MAX_VALUE, delay, duration), 0.0001f);
	}

	@Test
	public void easeOutKeepsItsEndsAndOnlyMovesForward()
	{
		assertEquals(0f, easeOut(0f), 0.0001f);
		assertEquals(1f, easeOut(1f), 0.0001f);

		float previous = -1f;
		for (int i = 0; i <= 100; i++)
		{
			final float eased = easeOut(i / 100f);
			assertTrue("eased value went backwards at " + i, eased > previous);
			assertTrue("eased value left the range at " + i, eased >= 0f && eased <= 1f);
			previous = eased;
		}

		assertTrue(easeOut(0.5f) > 0.5f);
	}

	@Test
	public void capsOnlyGrowOnceTheBarIsTallerThanTheReferenceHeight()
	{
		assertEquals(1f, capScale(4), 0.0001f);
		assertEquals(1f, capScale(8), 0.0001f);
		assertTrue(capScale(24) > capScale(9));
		assertTrue(capScale(9) > capScale(8));
	}

	@Test
	public void scaledCapWidthGrowsWithBarHeight()
	{
		assertEquals(scaledCapWidth(4), scaledCapWidth(8));
		assertTrue(scaledCapWidth(24) > scaledCapWidth(8));
	}

	@Test
	public void scaledFrameStrokeNeverGoesBelowAHairline()
	{
		assertEquals(1f, scaledFrameStroke(4), 0.0001f);
		assertEquals(1f, scaledFrameStroke(8), 0.0001f);
		assertTrue(scaledFrameStroke(24) > 1f);
	}
}
