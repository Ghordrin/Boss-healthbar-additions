package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.theme.FlatTheme;
import com.ghordrin.bosshealthbar.theme.HealthBarTheme;
import com.ghordrin.bosshealthbar.theme.ThemeColors;
import org.junit.Test;

public class BossHealthBarOverlayTest
{
	@Test
	public void aFlatThemeOnlyTakesTheBossFillColor()
	{
		final ThemeColors theme = FlatTheme.OLDSCHOOL.getColors();
		final ThemeColors boss = HealthBarTheme.BANDOS.getColors();
		assertEquals(ThemeColors.withBase(theme, boss.getFillHigh()),
			BossHealthBarOverlay.barColors(theme, boss, true));
		assertSame(boss, BossHealthBarOverlay.barColors(theme, boss, false));
		assertSame(theme, BossHealthBarOverlay.barColors(theme, null, false));
		assertSame(theme, BossHealthBarOverlay.barColors(theme, null, true));
	}

	@Test
	public void flatThemesNeverTurnGold()
	{
		assertFalse(BossHealthBarOverlay.showsGold(true, true));
		assertTrue(BossHealthBarOverlay.showsGold(true, false));
		assertFalse(BossHealthBarOverlay.showsGold(false, false));
	}








}
