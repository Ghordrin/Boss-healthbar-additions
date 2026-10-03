package com.ghordrin.bosshealthbar;

import net.runelite.api.HitsplatID;
import static org.junit.Assert.assertFalse;
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
}
