package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BossMemory.FORGET_TICKS;
import static com.ghordrin.bosshealthbar.BossMemory.HOLD_TICKS;
import static com.ghordrin.bosshealthbar.BossMemory.MAX_ENTRIES;
import static com.ghordrin.bosshealthbar.FightTimer.NO_TICK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BossMemoryTest
{
	private static final FightTimer.Fight FIGHT = new FightTimer.Fight(1, 10, NO_TICK);

	private final BossMemory memory = new BossMemory();

	@Test
	public void onlyAnOpponentFarAwayWentOutOfSight()
	{
		assertFalse(BossMemory.isOutOfSight(false, 10));
		assertFalse(BossMemory.isOutOfSight(false, 14));
		assertTrue(BossMemory.isOutOfSight(false, 15));
		assertTrue(BossMemory.isOutOfSight(false, Integer.MAX_VALUE));
		assertFalse(BossMemory.isOutOfSight(false, -1));
	}

	@Test
	public void dyingToTheOpponentIsNotWalkingAway()
	{
		assertFalse(BossMemory.isOutOfSight(true, 20));
	}

	@Test
	public void distanceIsFromTheSouthWestTile()
	{
		assertEquals(0, BossMemory.tileDistance(100, 100, 100, 100));
		assertEquals(14, BossMemory.tileDistance(100, 100, 114, 105));
		assertEquals(15, BossMemory.tileDistance(100, 100, 95, 115));
		assertEquals(16, BossMemory.tileDistance(100, 100, 84, 100));
	}

	@Test
	public void clearlyMoreHealthOnReturnIsANewFight()
	{
		assertFalse(BossMemory.healedSince(15, 30, false, 15, 30));
		assertFalse(BossMemory.healedSince(15, 30, false, 20, 30));
		assertTrue(BossMemory.healedSince(15, 30, false, 22, 30));
		assertTrue(BossMemory.healedSince(0, 1000, true, 900, 1000));
		assertFalse(BossMemory.healedSince(500, 1000, true, 10, 30));
		assertFalse(BossMemory.healedSince(0, 0, false, 30, 30));
	}

	@Test
	public void healthBackFromAnOverheadZeroIsNotAHeal()
	{
		assertFalse(BossMemory.healedSince(0, 30, false, 30, 30));
		assertTrue(BossMemory.healedSince(0, 30, true, 30, 30));
	}

	@Test
	public void remembersByIndexAndName()
	{
		memory.remember(5, "Boss", 100, FIGHT);
		assertNull(memory.find(6, "Boss", 101));
		final BossMemory.Entry entry = memory.find(5, "Boss", 101);
		assertNotNull(entry);
		assertSame(FIGHT, entry.fight);
	}

	@Test
	public void aDifferentNameAtTheSameIndexForgetsIt()
	{
		memory.remember(5, "Boss", 100, FIGHT);
		assertNull(memory.find(5, "Goblin", 101));
		assertNull(memory.find(5, "Boss", 102));
	}

	@Test
	public void forgetsAfterTheTimeLimit()
	{
		memory.remember(5, "Boss", 100, FIGHT);
		assertNotNull(memory.find(5, "Boss", 100 + FORGET_TICKS));
		assertNull(memory.find(5, "Boss", 101 + FORGET_TICKS));
		assertEquals(0, memory.size());
	}

	@Test
	public void dropsTheOldestWhenFull()
	{
		for (int i = 0; i <= MAX_ENTRIES; i++)
		{
			memory.remember(i, "Boss", 100 + i, FIGHT);
		}
		assertEquals(MAX_ENTRIES, memory.size());
		assertFalse(memory.contains(0));
		assertTrue(memory.contains(1));
		assertTrue(memory.contains(MAX_ENTRIES));
	}

	@Test
	public void rememberingTheSameIndexAgainReplacesIt()
	{
		memory.remember(5, "Boss", 100, FIGHT);
		final BossMemory.Entry later = memory.remember(5, "Boss", 200, FIGHT);
		assertEquals(1, memory.size());
		assertSame(later, memory.find(5, "Boss", 201));
	}

	@Test
	public void takingAnEntryForgetsIt()
	{
		memory.remember(5, "Boss", 100, FIGHT);
		assertNotNull(memory.take(5, "Boss", 101));
		assertNull(memory.take(5, "Boss", 102));
	}

	@Test
	public void forgetAndClear()
	{
		memory.remember(5, "Boss", 100, FIGHT);
		memory.remember(6, "Boss", 100, FIGHT);
		memory.forget(5);
		assertFalse(memory.contains(5));
		assertTrue(memory.contains(6));
		memory.clear();
		assertEquals(0, memory.size());
	}

	@Test
	public void heldOnlyShortlyAfterGoingOutOfSight()
	{
		assertTrue(BossMemory.isHeld(100, 100 + HOLD_TICKS));
		assertFalse(BossMemory.isHeld(100, 101 + HOLD_TICKS));
	}
}
