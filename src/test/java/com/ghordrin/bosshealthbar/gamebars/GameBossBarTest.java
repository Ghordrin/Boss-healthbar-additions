package com.ghordrin.bosshealthbar.gamebars;

import static com.ghordrin.bosshealthbar.gamebars.GameBossBar.markerFraction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GameBossBarTest
{
	private static final int[] MAX_HEALTHS = {1, 2, 10, 99, 100, 255, 1000, 4000};

	@Test
	public void markersSitWhereTheGamePutsThem()
	{
		assertEquals(0f, markerFraction(1, 400), 0.0001f);
		assertEquals(1f, markerFraction(401, 400), 0.0001f);
		assertEquals(0.5f, markerFraction(201, 400), 0.0001f);
	}

	@Test
	public void aBarThatTracksNoNpcIsALeftoverWhileBarsAreReplaced()
	{
		assertTrue(GameBossBar.isLeftover(true, -1));
		assertFalse(GameBossBar.isLeftover(true, 100));
		assertFalse(GameBossBar.isLeftover(false, -1));
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
}
