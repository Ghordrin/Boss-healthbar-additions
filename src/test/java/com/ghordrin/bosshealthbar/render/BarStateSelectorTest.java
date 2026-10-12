package com.ghordrin.bosshealthbar.render;

import static com.ghordrin.bosshealthbar.tracking.OpponentTracker.DespawnOutcome.DEFEATED;
import static com.ghordrin.bosshealthbar.tracking.OpponentTracker.DespawnOutcome.LEFT;
import static com.ghordrin.bosshealthbar.tracking.OpponentTracker.DespawnOutcome.OUT_OF_SIGHT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.render.BarStateSelector.DefeatStep;
import com.ghordrin.bosshealthbar.render.BarStateSelector.Switch;
import com.ghordrin.bosshealthbar.tracking.FightTimer;
import com.ghordrin.bosshealthbar.tracking.OpponentTracker;
import org.junit.Test;

public class BarStateSelectorTest
{
	// The bar's defeat follows a confirmed kill, so the input is the tracker's isOpponentDefeated, not isDead.
	@Test
	public void theDefeatStartsOnAKillAndStopsWhenItIsNoLongerOne()
	{
		assertEquals(DefeatStep.START, BarStateSelector.defeatStep(true, false, true));
		assertEquals(DefeatStep.NONE, BarStateSelector.defeatStep(true, true, true));
		assertEquals(DefeatStep.NONE, BarStateSelector.defeatStep(true, false, false));
		assertEquals(DefeatStep.CANCEL, BarStateSelector.defeatStep(false, true, true));
	}

	@Test
	public void withoutADespawnReadingItIsADefeatWhenDeadOrLastReadAtZeroInView()
	{
		assertTrue(BarStateSelector.defeatedAtDespawn(null, true, 20, false));
		assertTrue(BarStateSelector.defeatedAtDespawn(null, false, 0, false));
		assertFalse(BarStateSelector.defeatedAtDespawn(null, false, 20, false));
		assertFalse(BarStateSelector.defeatedAtDespawn(null, true, 0, true));
	}

	@Test
	public void theTrackersDespawnReadingDecides()
	{
		assertTrue(BarStateSelector.defeatedAtDespawn(DEFEATED, false, 1, false));
		assertFalse(BarStateSelector.defeatedAtDespawn(LEFT, true, 0, false));
		assertFalse(BarStateSelector.defeatedAtDespawn(OUT_OF_SIGHT, false, 20, true));
		// The bar's own 0 wins over the distance.
		assertTrue(BarStateSelector.defeatedAtDespawn(OUT_OF_SIGHT, false, 0, false));
	}

	@Test
	public void theOutcomeAloneDecidesTheDefeatAtDespawn()
	{
		assertTrue(BarStateSelector.defeatedAtDespawn(DEFEATED, false, 1, outOfSight(DEFEATED)));
		assertFalse(BarStateSelector.defeatedAtDespawn(OUT_OF_SIGHT, false, 0, outOfSight(OUT_OF_SIGHT)));
		assertFalse(BarStateSelector.defeatedAtDespawn(LEFT, true, 0, outOfSight(LEFT)));
		assertTrue(BarStateSelector.defeatedAtDespawn(null, true, 20, outOfSight(null)));
		assertTrue(BarStateSelector.defeatedAtDespawn(null, false, 0, outOfSight(null)));
	}

	@Test
	public void aResumedOpponentWinsOverMovingAndStartingOver()
	{
		assertEquals(Switch.RESUME, BarStateSelector.switchTo(true, true, true, true, false, true));
		assertEquals(Switch.RESUME, BarStateSelector.switchTo(true, true, false, false, false, false));
		assertEquals(Switch.RESUME, BarStateSelector.switchTo(true, true, true, false, true, false));
		// Without an opponent there is nothing to resume.
		assertEquals(Switch.RESET, BarStateSelector.switchTo(false, true, true, false, false, false));
	}

	@Test
	public void noOpponentClearsTheBarUnlessTheDefeatIsPlaying()
	{
		assertEquals(Switch.RESET, BarStateSelector.switchTo(false, false, true, false, false, false));
		assertEquals(Switch.KEEP, BarStateSelector.switchTo(false, false, true, false, true, false));
	}

	@Test
	public void anOpponentThatLeftAliveHoldsTheBarForTheNextOne()
	{
		assertEquals(Switch.KEEP, BarStateSelector.switchTo(false, false, true, false, false, true));
		assertEquals(Switch.MOVE, BarStateSelector.switchTo(true, false, true, true, false, true));
		assertEquals(Switch.MOVE, BarStateSelector.switchTo(true, false, true, false, false, true));
		// Once the hold runs out, the bar clears or starts over as before.
		assertEquals(Switch.RESET, BarStateSelector.switchTo(false, false, true, false, false, false));
	}

	// The bar only moves when the fight timer carries on, so the two never disagree.
	@Test
	public void aNewOpponentMovesTheBarOnlyWhenItCarriesOnTheFight()
	{
		assertEquals(Switch.RESUME, BarStateSelector.switchTo(true, true, true, false, true, false));
		assertEquals(Switch.MOVE, BarStateSelector.switchTo(true, false, true, true, false, false));
		// Such as the next NPC clicked while the last one is still dying: a new fight, so a new bar.
		assertEquals(Switch.RESET, BarStateSelector.switchTo(true, false, true, false, false, false));
		assertEquals(Switch.RESET, BarStateSelector.switchTo(true, false, true, true, true, false));
		assertEquals(Switch.RESET, BarStateSelector.switchTo(true, false, false, true, false, false));
	}

	// An opponent back up after being defeated reports no carried-on fight, which is what the bar asks about here.
	@Test
	public void anOpponentThatDoesNotCarryOnTheFightGetsANewBarEvenWhileADefeatPlays()
	{
		assertEquals(Switch.RESET, BarStateSelector.switchTo(true, false, true, false, true, false));
	}

	@Test
	public void aHeldBarFollowsTheTimerOfItsOwnFight()
	{
		assertEquals("1:30", BarStateSelector.heldFightTime("1:28", "1:30", 100, 100));
	}

	// Attacking something else while the defeated bar is still up starts another fight, whose clock isn't this bar's.
	@Test
	public void aHeldBarKeepsItsTimeOnceAnotherFightHasStarted()
	{
		assertEquals("1:30", BarStateSelector.heldFightTime("1:30", "0:00", 100, 420));
		assertEquals("1:30", BarStateSelector.heldFightTime("1:30", null, 100, FightTimer.NO_TICK));
	}

	private static boolean outOfSight(OpponentTracker.DespawnOutcome outcome)
	{
		return outcome == OUT_OF_SIGHT;
	}
}
