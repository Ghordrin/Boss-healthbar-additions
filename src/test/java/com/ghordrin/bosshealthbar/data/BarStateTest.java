package com.ghordrin.bosshealthbar.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.ghordrin.bosshealthbar.integrations.HealthIndicatorMarkers;
import java.util.Collections;
import org.junit.Test;

public class BarStateTest
{
	private static BarState doom()
	{
		return new BarState("Doom of Mokhaiotl (Burrowed)", "Doom (Burrowed)", 1200, 7,
			new Health(30, 60, true, 60, false), BarState.NO_PHASE_MARKERS, HealthIndicatorMarkers.NONE,
			"doom of mokhaiotl", null, Collections.emptyList(), null);
	}

	// The bar keeps showing after the kill, so the level has to survive the fight time being filled in.
	@Test
	public void theDelveLevelAndShortNameSurviveTheFightTime()
	{
		final BarState withTime = doom().withFightTime("1:23");
		assertEquals(7, withTime.delveLevel);
		assertEquals("Doom (Burrowed)", withTime.shortName);
		assertEquals("Doom of Mokhaiotl (Burrowed)", withTime.name);
		assertEquals("1:23", withTime.fightTime);
	}

	@Test
	public void aHealthOnlyBarHasNeither()
	{
		final BarState state = BarState.healthOnly("Rat", new Health(1, 2, true, 2, false));
		assertEquals(BarState.NO_DELVE_LEVEL, state.delveLevel);
		assertNull(state.shortName);
	}
}
