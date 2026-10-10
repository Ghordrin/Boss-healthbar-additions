package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BossHealthBarOverlayTest
{
	@Test
	public void bossColorsDontApplyToFlatThemes()
	{
		final ThemeColors theme = FlatTheme.OLDSCHOOL.getColors();
		final ThemeColors boss = HealthBarTheme.BANDOS.getColors();
		assertSame(theme, BossHealthBarOverlay.barColors(theme, boss, true));
		assertSame(boss, BossHealthBarOverlay.barColors(theme, boss, false));
		assertSame(theme, BossHealthBarOverlay.barColors(theme, null, false));
	}

	@Test
	public void flatThemesNeverTurnGold()
	{
		assertFalse(BossHealthBarOverlay.showsGold(true, true));
		assertTrue(BossHealthBarOverlay.showsGold(true, false));
		assertFalse(BossHealthBarOverlay.showsGold(false, false));
	}
}
