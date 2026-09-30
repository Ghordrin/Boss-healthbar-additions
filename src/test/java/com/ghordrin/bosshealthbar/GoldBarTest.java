package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class GoldBarTest
{
	@Test
	public void gildingOnlyChangesTheMetal()
	{
		final ThemeColors theme = HealthBarTheme.GUTHIX.getColors();
		final ThemeColors gold = GoldBar.gilded(theme);

		assertEquals(GoldBar.GOLD, gold.getFrame());
		assertEquals(theme.getFillHigh(), gold.getFillHigh());
		assertEquals(theme.getFillLow(), gold.getFillLow());
		assertEquals(theme.getTrail(), gold.getTrail());
		assertEquals(theme.getText(), gold.getText());
	}

	@Test
	public void reusesTheGoldColorsForTheSameTheme()
	{
		final GoldBar goldBar = new GoldBar(false);
		final ThemeColors theme = HealthBarTheme.SEREN.getColors();
		assertSame(goldBar.colors(theme), goldBar.colors(theme));
	}
}
