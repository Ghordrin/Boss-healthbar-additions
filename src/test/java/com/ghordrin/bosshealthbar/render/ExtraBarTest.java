package com.ghordrin.bosshealthbar.render;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ExtraBarTest
{
	@Test
	public void theStripIsAThirdOfTheMainBarsHeightDownToAFloor()
	{
		assertEquals(12, ExtraBar.thickness(36));
		assertEquals(5, ExtraBar.thickness(16));
		assertEquals(3, ExtraBar.thickness(8));
		assertEquals(3, ExtraBar.thickness(1));
	}

	@Test
	public void theStripIsNeverThinnerThanItsFloor()
	{
		for (int barHeight = 1; barHeight <= 64; barHeight++)
		{
			assertEquals(Math.max(ExtraBar.MIN_THICKNESS, Math.round(barHeight / 3f)),
				ExtraBar.thickness(barHeight));
		}
	}

	@Test
	public void theFillFollowsTheGamesReading()
	{
		assertEquals(0f, ExtraBar.fraction(0, 96), 0f);
		assertEquals(0.5f, ExtraBar.fraction(48, 96), 0f);
		assertEquals(1f, ExtraBar.fraction(96, 96), 0f);
	}

	// The varbits are read a frame apart from the game writing them, so neither a bar with no maximum yet nor
	// one reading past its maximum may put the fill outside the bar.
	@Test
	public void aReadingThatCannotBeDrawnFallsInsideTheBar()
	{
		assertEquals(0f, ExtraBar.fraction(5, 0), 0f);
		assertEquals(0f, ExtraBar.fraction(5, -1), 0f);
		assertEquals(1f, ExtraBar.fraction(200, 96), 0f);
		assertEquals(0f, ExtraBar.fraction(-5, 96), 0f);
	}
}
