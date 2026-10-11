package com.ghordrin.bosshealthbar;

import com.ghordrin.bosshealthbar.BossHealthBarOverlay.DefeatStep;
import com.ghordrin.bosshealthbar.BossHealthBarOverlay.Switch;
import static org.junit.Assert.assertEquals;
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

	// Flips once a bar follows confirmed kills: today isDead alone starts the defeat and its end cancels it.
	@Test
	public void theDefeatFollowsIsDeadForNow()
	{
		assertEquals(DefeatStep.START, BossHealthBarOverlay.defeatStep(true, false, true));
		assertEquals(DefeatStep.NONE, BossHealthBarOverlay.defeatStep(true, true, true));
		assertEquals(DefeatStep.NONE, BossHealthBarOverlay.defeatStep(true, false, false));
		assertEquals(DefeatStep.CANCEL, BossHealthBarOverlay.defeatStep(false, true, true));
	}

	@Test
	public void aDespawnIsADefeatWhenDeadOrLastReadAtZeroInView()
	{
		assertTrue(BossHealthBarOverlay.defeatedAtDespawn(true, 20, false));
		assertTrue(BossHealthBarOverlay.defeatedAtDespawn(false, 0, false));
		assertFalse(BossHealthBarOverlay.defeatedAtDespawn(false, 20, false));
		assertFalse(BossHealthBarOverlay.defeatedAtDespawn(true, 0, true));
	}

	// Flips once an opponent that leaves alive holds the bar: today no opponent clears it.
	@Test
	public void noOpponentClearsTheBarUnlessTheDefeatIsPlayingForNow()
	{
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(false, false, true, false, false));
		assertEquals(Switch.KEEP, BossHealthBarOverlay.switchTo(false, false, true, true, true));
	}

	@Test
	public void aNewOpponentResumesMovesOrStartsANewBar()
	{
		assertEquals(Switch.RESUME, BossHealthBarOverlay.switchTo(true, true, true, true, true));
		assertEquals(Switch.MOVE, BossHealthBarOverlay.switchTo(true, false, true, false, false));
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(true, false, true, true, false));
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(true, false, true, false, true));
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(true, false, false, false, false));
	}
}
