package com.ghordrin.bosshealthbar.render;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EnergyBarTest
{
	@Test
	public void theStripIsAThirdOfTheMainBarsHeightDownToAFloor()
	{
		assertEquals(12, EnergyBar.thickness(36));
		assertEquals(5, EnergyBar.thickness(16));
		assertEquals(3, EnergyBar.thickness(8));
		assertEquals(3, EnergyBar.thickness(1));
	}

	@Test
	public void theStripIsNeverThinnerThanItsFloor()
	{
		for (int barHeight = 1; barHeight <= 64; barHeight++)
		{
			assertEquals(Math.max(EnergyBar.MIN_THICKNESS, Math.round(barHeight / 3f)),
				EnergyBar.thickness(barHeight));
		}
	}

	@Test
	public void theFillFollowsTheGamesReading()
	{
		assertEquals(0f, EnergyBar.fraction(0, 96), 0f);
		assertEquals(0.5f, EnergyBar.fraction(48, 96), 0f);
		assertEquals(1f, EnergyBar.fraction(96, 96), 0f);
	}

	// The varbits are read a frame apart from the game writing them, so neither a bar with no maximum yet nor
	// one reading past its maximum may put the fill outside the bar.
	@Test
	public void aReadingThatCannotBeDrawnFallsInsideTheBar()
	{
		assertEquals(0f, EnergyBar.fraction(5, 0), 0f);
		assertEquals(0f, EnergyBar.fraction(5, -1), 0f);
		assertEquals(1f, EnergyBar.fraction(200, 96), 0f);
		assertEquals(0f, EnergyBar.fraction(-5, 96), 0f);
	}
}
