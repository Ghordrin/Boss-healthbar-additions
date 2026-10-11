package com.ghordrin.bosshealthbar;

import com.ghordrin.bosshealthbar.BossHealthBarOverlay.DefeatStep;
import com.ghordrin.bosshealthbar.BossHealthBarOverlay.Switch;
import static com.ghordrin.bosshealthbar.OpponentTracker.DespawnOutcome.DEFEATED;
import static com.ghordrin.bosshealthbar.OpponentTracker.DespawnOutcome.LEFT;
import static com.ghordrin.bosshealthbar.OpponentTracker.DespawnOutcome.OUT_OF_SIGHT;
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

	// The bar's defeat follows a confirmed kill, so the input is the tracker's isOpponentDefeated, not isDead.
	@Test
	public void theDefeatStartsOnAKillAndStopsWhenItIsNoLongerOne()
	{
		assertEquals(DefeatStep.START, BossHealthBarOverlay.defeatStep(true, false, true));
		assertEquals(DefeatStep.NONE, BossHealthBarOverlay.defeatStep(true, true, true));
		assertEquals(DefeatStep.NONE, BossHealthBarOverlay.defeatStep(true, false, false));
		assertEquals(DefeatStep.CANCEL, BossHealthBarOverlay.defeatStep(false, true, true));
	}

	@Test
	public void withoutADespawnReadingItIsADefeatWhenDeadOrLastReadAtZeroInView()
	{
		assertTrue(BossHealthBarOverlay.defeatedAtDespawn(null, true, 20, false));
		assertTrue(BossHealthBarOverlay.defeatedAtDespawn(null, false, 0, false));
		assertFalse(BossHealthBarOverlay.defeatedAtDespawn(null, false, 20, false));
		assertFalse(BossHealthBarOverlay.defeatedAtDespawn(null, true, 0, true));
	}

	@Test
	public void theTrackersDespawnReadingDecides()
	{
		assertTrue(BossHealthBarOverlay.defeatedAtDespawn(DEFEATED, false, 1, false));
		assertFalse(BossHealthBarOverlay.defeatedAtDespawn(LEFT, true, 0, false));
		assertFalse(BossHealthBarOverlay.defeatedAtDespawn(OUT_OF_SIGHT, false, 20, true));
		// The bar's own 0 wins over the distance.
		assertTrue(BossHealthBarOverlay.defeatedAtDespawn(OUT_OF_SIGHT, false, 0, false));
	}

	@Test
	public void theOutcomeAloneDecidesTheDefeatAtDespawn()
	{
		assertTrue(BossHealthBarOverlay.defeatedAtDespawn(DEFEATED, false, 1, outOfSight(DEFEATED)));
		assertFalse(BossHealthBarOverlay.defeatedAtDespawn(OUT_OF_SIGHT, false, 0, outOfSight(OUT_OF_SIGHT)));
		assertFalse(BossHealthBarOverlay.defeatedAtDespawn(LEFT, true, 0, outOfSight(LEFT)));
		assertTrue(BossHealthBarOverlay.defeatedAtDespawn(null, true, 20, outOfSight(null)));
		assertTrue(BossHealthBarOverlay.defeatedAtDespawn(null, false, 0, outOfSight(null)));
	}

	@Test
	public void aResumedOpponentWinsOverMovingAndStartingOver()
	{
		assertEquals(Switch.RESUME, BossHealthBarOverlay.switchTo(true, true, true, true, false, true));
		assertEquals(Switch.RESUME, BossHealthBarOverlay.switchTo(true, true, false, false, false, false));
		assertEquals(Switch.RESUME, BossHealthBarOverlay.switchTo(true, true, true, false, true, false));
		// Without an opponent there is nothing to resume.
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(false, true, true, false, false, false));
	}

	@Test
	public void noOpponentClearsTheBarUnlessTheDefeatIsPlaying()
	{
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(false, false, true, false, false, false));
		assertEquals(Switch.KEEP, BossHealthBarOverlay.switchTo(false, false, true, false, true, false));
	}

	@Test
	public void anOpponentThatLeftAliveHoldsTheBarForTheNextOne()
	{
		assertEquals(Switch.KEEP, BossHealthBarOverlay.switchTo(false, false, true, false, false, true));
		assertEquals(Switch.MOVE, BossHealthBarOverlay.switchTo(true, false, true, true, false, true));
		assertEquals(Switch.MOVE, BossHealthBarOverlay.switchTo(true, false, true, false, false, true));
		// Once the hold runs out, the bar clears or starts over as before.
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(false, false, true, false, false, false));
	}

	// The bar only moves when the fight timer carries on, so the two never disagree.
	@Test
	public void aNewOpponentMovesTheBarOnlyWhenItCarriesOnTheFight()
	{
		assertEquals(Switch.RESUME, BossHealthBarOverlay.switchTo(true, true, true, false, true, false));
		assertEquals(Switch.MOVE, BossHealthBarOverlay.switchTo(true, false, true, true, false, false));
		// Such as the next NPC clicked while the last one is still dying: a new fight, so a new bar.
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(true, false, true, false, false, false));
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(true, false, true, true, true, false));
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(true, false, false, true, false, false));
	}

	// An opponent back up after being defeated reports no carried-on fight, which is what the bar asks about here.
	@Test
	public void anOpponentThatDoesNotCarryOnTheFightGetsANewBarEvenWhileADefeatPlays()
	{
		assertEquals(Switch.RESET, BossHealthBarOverlay.switchTo(true, false, true, false, true, false));
	}

	private static boolean outOfSight(OpponentTracker.DespawnOutcome outcome)
	{
		return outcome == OUT_OF_SIGHT;
	}
}
