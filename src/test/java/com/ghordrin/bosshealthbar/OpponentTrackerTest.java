package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.OpponentTracker.DespawnOutcome.DEFEATED;
import static com.ghordrin.bosshealthbar.OpponentTracker.DespawnOutcome.LEFT;
import static com.ghordrin.bosshealthbar.OpponentTracker.DespawnOutcome.OUT_OF_SIGHT;
import net.runelite.api.HitsplatID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class OpponentTrackerTest
{
	private static final long DELAY = 5000;

	@Test
	public void staysWhileStillInteracting()
	{
		assertFalse(OpponentTracker.isQuietFor(0, 0, 0, true, 100_000, DELAY));
	}

	@Test
	public void hidesOnceTheDelayHasPassedSinceInteractionStopped()
	{
		assertFalse(OpponentTracker.isQuietFor(10_000, 0, 0, true, 15_000, DELAY));
		assertTrue(OpponentTracker.isQuietFor(10_000, 0, 0, true, 15_001, DELAY));
	}

	@Test
	public void aHitTakenRestartsTheDelay()
	{
		assertFalse(OpponentTracker.isQuietFor(10_000, 14_000, 0, true, 18_000, DELAY));
		assertTrue(OpponentTracker.isQuietFor(10_000, 14_000, 0, true, 19_001, DELAY));
	}

	@Test
	public void aHitOnTheOpponentRestartsTheDelay()
	{
		assertFalse(OpponentTracker.isQuietFor(10_000, 0, 16_000, true, 21_000, DELAY));
		assertTrue(OpponentTracker.isQuietFor(10_000, 0, 16_000, true, 21_001, DELAY));
	}

	@Test
	public void theLatestHitCounts()
	{
		assertFalse(OpponentTracker.isQuietFor(10_000, 17_000, 13_000, true, 22_000, DELAY));
		assertTrue(OpponentTracker.isQuietFor(10_000, 17_000, 13_000, true, 22_001, DELAY));
	}

	@Test
	public void hitsBeforeInteractionStoppedDoNotExtendTheDelay()
	{
		assertTrue(OpponentTracker.isQuietFor(10_000, 2_000, 3_000, true, 15_001, DELAY));
	}

	@Test
	public void hitsAreIgnoredWhenTheOpponentIsNotNearby()
	{
		assertTrue(OpponentTracker.isQuietFor(10_000, 14_000, 14_500, false, 15_001, DELAY));
	}

	@Test
	public void damageAndBlocksCount()
	{
		assertTrue(OpponentTracker.isCombatHit(HitsplatID.DAMAGE_ME));
		assertTrue(OpponentTracker.isCombatHit(HitsplatID.DAMAGE_OTHER));
		assertTrue(OpponentTracker.isCombatHit(HitsplatID.DAMAGE_ME_POISE));
		assertTrue(OpponentTracker.isCombatHit(HitsplatID.BLOCK_ME));
		assertTrue(OpponentTracker.isCombatHit(HitsplatID.BLOCK_OTHER));
	}

	@Test
	public void damageOverTimeAndHealsDoNotCount()
	{
		assertFalse(OpponentTracker.isCombatHit(HitsplatID.POISON));
		assertFalse(OpponentTracker.isCombatHit(HitsplatID.VENOM));
		assertFalse(OpponentTracker.isCombatHit(HitsplatID.BURN));
		assertFalse(OpponentTracker.isCombatHit(HitsplatID.BLEED));
		assertFalse(OpponentTracker.isCombatHit(HitsplatID.DOOM));
		assertFalse(OpponentTracker.isCombatHit(HitsplatID.CORRUPTION));
		assertFalse(OpponentTracker.isCombatHit(HitsplatID.HEAL));
	}

	@Test
	public void aConfirmedDefeatIsAKillWhereverItDespawns()
	{
		assertEquals(DEFEATED, OpponentTracker.despawnOutcome(true, false, false, 5, -1, false));
		assertEquals(DEFEATED, OpponentTracker.despawnOutcome(true, false, false, 40, -1, false));
		assertEquals(DEFEATED, OpponentTracker.despawnOutcome(true, true, true, 40, -1, false));
	}

	@Test
	public void anUnconfirmedZeroIsOnlyAKillInView()
	{
		assertEquals(DEFEATED, OpponentTracker.despawnOutcome(false, true, false, 14, -1, false));
		assertEquals(DEFEATED, OpponentTracker.despawnOutcome(false, true, false, -1, -1, false));
		assertEquals(OUT_OF_SIGHT, OpponentTracker.despawnOutcome(false, true, false, 15, -1, false));
		assertEquals(LEFT, OpponentTracker.despawnOutcome(false, true, true, 15, -1, false));
	}

	@Test
	public void withoutAZeroItWentOutOfSightOrLeft()
	{
		assertEquals(LEFT, OpponentTracker.despawnOutcome(false, false, false, 14, -1, false));
		assertEquals(OUT_OF_SIGHT, OpponentTracker.despawnOutcome(false, false, false, 15, -1, false));
		assertEquals(LEFT, OpponentTracker.despawnOutcome(false, false, true, 30, -1, false));
	}

	@Test
	public void anExactZeroFarAwayIsAKillAndNotOutOfSight()
	{
		assertEquals(DEFEATED, OpponentTracker.despawnOutcome(false, false, false, 20, 0, true));
		assertEquals(DEFEATED, OpponentTracker.despawnOutcome(false, true, false, 20, 0, true));
	}

	@Test
	public void aLastReadingAboveZeroFarAwayIsOutOfSight()
	{
		assertEquals(OUT_OF_SIGHT, OpponentTracker.despawnOutcome(false, false, false, 20, 20, true));
		assertEquals(OUT_OF_SIGHT, OpponentTracker.despawnOutcome(false, false, false, 20, 1, true));
	}

	@Test
	public void anOverheadZeroFarAwayIsOutOfSight()
	{
		assertEquals(OUT_OF_SIGHT, OpponentTracker.despawnOutcome(false, true, false, 20, 0, false));
		assertEquals(OUT_OF_SIGHT, OpponentTracker.despawnOutcome(false, false, false, 20, 0, false));
	}

	// Dying while the opponent reads 0 isn't a kill, however exact that reading was.
	@Test
	public void anExactZeroIsStillNotAKillWhenThePlayerDied()
	{
		assertEquals(LEFT, OpponentTracker.despawnOutcome(false, true, true, 20, 0, true));
		assertEquals(LEFT, OpponentTracker.despawnOutcome(false, false, true, 20, 0, true));
	}

	@Test
	public void aNewLifeStartsTheFightOverAndLeavesTheOldGroupBehind()
	{
		final FightTimer timer = new FightTimer(new DebugLog(null));
		final FightGroup group = new FightGroup(null, new DebugLog(null));
		timer.opponentChanged(false, false, FightTimer.NO_TICK, 0);
		timer.onHit(0);
		group.join(1, "Boss", true, 0);
		group.join(2, "Minion", false, 20);
		timer.onGameTick(true, true, 600);
		assertEquals("6:00", timer.getText());
		final int fight = group.id();

		OpponentTracker.newLife(timer, group, null, true, 650);
		assertEquals("0:00", timer.getText());
		assertNotEquals(fight, group.id());
		assertEquals(0, group.size());

		timer.onGameTick(true, false, 700);
		assertEquals("0:30", timer.getText());
	}
}
