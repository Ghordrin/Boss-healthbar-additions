package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class LastHealthTest
{
	private static final int FIGHT = 100;

	private static Health reading(int ratio)
	{
		return new Health(ratio, 500, true, 500, false);
	}

	@Test
	public void findsTheSameNpcByIndexAndName()
	{
		final LastHealth health = new LastHealth();
		final Health first = reading(300);
		final Health second = reading(200);
		health.remember(1, "Boss", FIGHT, first);
		health.remember(2, "Boss", FIGHT, second);
		assertSame(first, health.find(1, "Boss", FIGHT));
		assertSame(second, health.find(2, "Boss", FIGHT));
		assertNull(health.find(1, "Other", FIGHT));
	}

	@Test
	public void aSingleEntryWithTheNameCoversANewIndex()
	{
		final LastHealth health = new LastHealth();
		final Health state = reading(300);
		health.remember(1, "Boss", FIGHT, state);
		health.remember(2, "Minion", FIGHT, reading(50));
		assertSame(state, health.find(9, "Boss", FIGHT));
	}

	@Test
	public void severalEntriesWithTheNameDontGuess()
	{
		final LastHealth health = new LastHealth();
		health.remember(1, "Boss", FIGHT, reading(300));
		health.remember(2, "Boss", FIGHT, reading(200));
		assertNull(health.find(9, "Boss", FIGHT));
	}

	@Test
	public void aNewReadingReplacesTheOldOne()
	{
		final LastHealth health = new LastHealth();
		health.remember(1, "Boss", FIGHT, reading(300));
		final Health newer = reading(250);
		health.remember(1, "Boss", FIGHT, newer);
		assertSame(newer, health.find(1, "Boss", FIGHT));
		assertSame(newer, health.find(5, "Boss", FIGHT));
	}

	@Test
	public void keepsOnlyTheNewestEntries()
	{
		final LastHealth health = new LastHealth();
		for (int i = 0; i <= LastHealth.CAPACITY; i++)
		{
			health.remember(i, "Npc " + i, FIGHT, reading(100 + i));
		}
		assertNull(health.find(0, "Npc 0", FIGHT));
		assertNotNull(health.find(LastHealth.CAPACITY, "Npc " + LastHealth.CAPACITY, FIGHT));
		assertNotNull(health.find(1, "Npc 1", FIGHT));
	}

	@Test
	public void anotherFightDoesntSeeTheReading()
	{
		final LastHealth health = new LastHealth();
		health.remember(1, "Boss", FIGHT, reading(300));
		assertNull(health.find(1, "Boss", FIGHT + 50));
		assertNull(health.find(2, "Boss", FIGHT + 50));
		health.remember(3, "Boss", -1, reading(200));
		assertNull(health.find(3, "Boss", -1));
	}

	// Flips once a 0 is never remembered.
	@Test
	public void remembersAZeroForNow()
	{
		final LastHealth health = new LastHealth();
		final Health zero = reading(0);
		health.remember(1, "Boss", FIGHT, zero);
		assertSame(zero, health.find(1, "Boss", FIGHT));
	}

	@Test
	public void ignoresStatesWithoutHealth()
	{
		final LastHealth health = new LastHealth();
		health.remember(2, "Boss", FIGHT, new Health(-1, -1, false, null, false));
		assertNull(health.find(2, "Boss", FIGHT));
		health.remember(1, "Boss", FIGHT, reading(300));
		health.clear();
		assertNull(health.find(1, "Boss", FIGHT));
	}
}
