package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BarEffectsTest
{
	@Test
	public void randomIsRepeatableAndInRange()
	{
		for (int i = 0; i < 1000; i++)
		{
			final float value = BarEffects.random(42, i);
			assertTrue(value >= 0f && value < 1f);
			assertEquals(value, BarEffects.random(42, i), 0f);
		}
		assertTrue(BarEffects.random(1, 0) != BarEffects.random(2, 0));
	}

	@Test
	public void burnSparksOnlyFlyWhileTheBarBurns()
	{
		final BarEffects.Particle spark = new BarEffects.Particle();
		for (int i = 0; i < BarEffects.BURN_SPARK_COUNT; i++)
		{
			assertFalse(BarEffects.burnSpark(i, -1, spark));
			assertFalse(BarEffects.burnSpark(i, BarEffects.BURN_DURATION.toMillis() + 1000, spark));
		}

		int thrown = 0;
		for (int i = 0; i < BarEffects.BURN_SPARK_COUNT; i++)
		{
			for (long millis = 0; millis <= BarEffects.BURN_DURATION.toMillis(); millis += 25)
			{
				if (BarEffects.burnSpark(i, millis, spark))
				{
					thrown++;
					assertTrue(spark.along >= 0f && spark.along <= 1f);
					// A spark starts where the edge was when it was thrown, so never ahead of the edge.
					assertTrue(spark.along <= BarEffects.burnProgress(millis) + 0.0001f);
					assertTrue(spark.y <= 0f);
				}
			}
		}
		assertTrue(thrown > 0);
	}

	@Test
	public void burnRunsTheWholeBar()
	{
		assertEquals(0f, BarEffects.burnProgress(-100), 0f);
		assertEquals(0f, BarEffects.burnProgress(0), 0f);
		assertEquals(1f, BarEffects.burnProgress(BarEffects.BURN_DURATION.toMillis()), 0f);
		assertTrue(BarEffects.burnProgress(400) < BarEffects.burnProgress(800));
	}

	@Test
	public void sparkColorsFadeOut()
	{
		assertEquals(255, BarEffects.sparkColor(0f).getAlpha());
		assertEquals(0, BarEffects.sparkColor(1f).getAlpha());
		assertTrue(BarEffects.sparkColor(0.5f).getAlpha() < BarEffects.sparkColor(0.1f).getAlpha());
	}
}
