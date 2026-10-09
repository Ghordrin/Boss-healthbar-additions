package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import java.util.Collections;
import org.junit.Test;

public class LastHealthTest
{
	private static final int FIGHT = 100;

	private static BarState state(int ratio)
	{
		return new BarState("Boss", 100, 500, ratio, 500, true, false, BarState.NO_PHASE_MARKERS,
			HealthIndicatorMarkers.NONE, null, null, Collections.emptyList(), null);
	}

	@Test
	public void findsTheSameNpcByIndexAndName()
	{
		final LastHealth health = new LastHealth();
		final BarState first = state(300);
		final BarState second = state(200);
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
		final BarState state = state(300);
		health.remember(1, "Boss", FIGHT, state);
		health.remember(2, "Minion", FIGHT, state(50));
		assertSame(state, health.find(9, "Boss", FIGHT));
	}

	@Test
	public void severalEntriesWithTheNameDontGuess()
	{
		final LastHealth health = new LastHealth();
		health.remember(1, "Boss", FIGHT, state(300));
		health.remember(2, "Boss", FIGHT, state(200));
		assertNull(health.find(9, "Boss", FIGHT));
	}

	@Test
	public void aNewReadingReplacesTheOldOne()
	{
		final LastHealth health = new LastHealth();
		health.remember(1, "Boss", FIGHT, state(300));
		final BarState newer = state(250);
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
			health.remember(i, "Npc " + i, FIGHT, state(100 + i));
		}
		assertNull(health.find(0, "Npc 0", FIGHT));
		assertNotNull(health.find(LastHealth.CAPACITY, "Npc " + LastHealth.CAPACITY, FIGHT));
		assertNotNull(health.find(1, "Npc 1", FIGHT));
	}

	@Test
	public void anotherFightDoesntSeeTheReading()
	{
		final LastHealth health = new LastHealth();
		health.remember(1, "Boss", FIGHT, state(300));
		assertNull(health.find(1, "Boss", FIGHT + 50));
		assertNull(health.find(2, "Boss", FIGHT + 50));
		health.remember(3, "Boss", -1, state(200));
		assertNull(health.find(3, "Boss", -1));
	}

	@Test
	public void ignoresStatesWithoutHealth()
	{
		final LastHealth health = new LastHealth();
		health.remember(2, "Boss", FIGHT, new BarState("Boss", 100, null, -1, -1, false, false,
			BarState.NO_PHASE_MARKERS, HealthIndicatorMarkers.NONE, null, null, Collections.emptyList(), null));
		assertNull(health.find(2, "Boss", FIGHT));
		health.remember(1, "Boss", FIGHT, state(300));
		health.clear();
		assertNull(health.find(1, "Boss", FIGHT));
	}
}
