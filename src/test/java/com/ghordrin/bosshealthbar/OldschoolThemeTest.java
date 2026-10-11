package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class OldschoolThemeTest
{
	@Test
	public void oldschoolIsFlatAndNotInTheDropdown()
	{
		assertTrue(FlatTheme.OLDSCHOOL.isFlat());
		for (HealthBarTheme theme : HealthBarTheme.values())
		{
			assertFalse(theme.name(), theme.isFlat());
			assertNotEquals(FlatTheme.OLDSCHOOL.name(), theme.name());
		}
		assertNull(HealthBarTheme.named(FlatTheme.OLDSCHOOL.name()));
	}

	@Test
	public void checkboxOverridesTheDropdown()
	{
		assertSame(FlatTheme.OLDSCHOOL, BarTheme.effective(true, HealthBarTheme.SARADOMIN));
		assertSame(FlatTheme.OLDSCHOOL, BarTheme.effective(true, HealthBarTheme.CUSTOM));
		assertSame(HealthBarTheme.SARADOMIN, BarTheme.effective(false, HealthBarTheme.SARADOMIN));
	}

	@Test
	public void oldschoolHasAPlainTrackAndOneFillColor()
	{
		final ThemeColors colors = FlatTheme.OLDSCHOOL.getColors();
		assertNotNull(colors.getTrack());
		assertEquals(colors.getFillHigh(), colors.getFillLow());
		assertNull(FlatTheme.OLDSCHOOL.getGodIconSpriteId());
		assertNull(HealthBarTheme.ZAMORAK.getColors().getTrack());
	}

	@Test
	public void flatBarsOnlyReserveRoomForTheOutline()
	{
		for (int height = 4; height <= 24; height++)
		{
			for (BarEnds ends : BarEnds.values())
			{
				assertEquals(1, BarPainter.capWidth(height, true, ends));
				assertEquals(2, BarPainter.capRise(height, true, ends));
			}
			assertEquals(BarPainter.scaledCapWidth(height), BarPainter.capWidth(height, false, BarEnds.CLASSIC));
			assertEquals(BarPainter.scaledCapRise(height), BarPainter.capRise(height, false, BarEnds.CLASSIC));
		}
	}
}
