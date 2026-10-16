package com.ghordrin.bosshealthbar.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import com.ghordrin.bosshealthbar.integrations.HealthIndicatorMarkers;
import com.ghordrin.bosshealthbar.integrations.SpecialAttackCounts;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class BarStateTest
{
	private static final Health HEALTH = new Health(30, 60, true, 60, false);
	private static final float[] PHASE_MARKERS = {0.25f, 0.5f};
	private static final HealthIndicatorMarkers.Marker[] USER_MARKERS = new HealthIndicatorMarkers.Marker[0];
	private static final List<SpecialAttackCounts.Reading> SPECIAL_ATTACKS = Collections.emptyList();

	private static BarState state()
	{
		return new BarState("Doom of Mokhaiotl (Burrowed)", "Doom (Burrowed)", 1200, 7,
			HEALTH, PHASE_MARKERS, USER_MARKERS, "doom of mokhaiotl", null, SPECIAL_ATTACKS, null);
	}

	// The bar keeps showing after the kill, so every field has to survive the fight time being filled in.
	// withFightTime copies all eleven by hand, which is where one gets dropped.
	@Test
	public void everyFieldSurvivesTheFightTime()
	{
		final BarState withTime = state().withFightTime("1:23");

		assertEquals("Doom of Mokhaiotl (Burrowed)", withTime.name);
		assertEquals("Doom (Burrowed)", withTime.shortName);
		assertEquals(1200, withTime.combatLevel);
		assertEquals(7, withTime.delveLevel);
		assertSame(HEALTH, withTime.health);
		assertSame(PHASE_MARKERS, withTime.phaseMarkers);
		assertSame(USER_MARKERS, withTime.userMarkers);
		assertEquals("doom of mokhaiotl", withTime.killCountKey);
		assertNull(withTime.stats);
		assertSame(SPECIAL_ATTACKS, withTime.specialAttacks);
		assertEquals("1:23", withTime.fightTime);
	}

	// Same hand-copy as withFightTime, for a bar held on the game's own bar while its health keeps moving.
	@Test
	public void everyFieldSurvivesNewHealth()
	{
		final Health health = new Health(30, 60, true, 60, false);
		final BarState withHealth = state().withFightTime("1:23").withHealth(health);

		assertEquals("Doom of Mokhaiotl (Burrowed)", withHealth.name);
		assertEquals("Doom (Burrowed)", withHealth.shortName);
		assertEquals(1200, withHealth.combatLevel);
		assertEquals(7, withHealth.delveLevel);
		assertSame(health, withHealth.health);
		assertSame(PHASE_MARKERS, withHealth.phaseMarkers);
		assertSame(USER_MARKERS, withHealth.userMarkers);
		assertEquals("doom of mokhaiotl", withHealth.killCountKey);
		assertNull(withHealth.stats);
		assertSame(SPECIAL_ATTACKS, withHealth.specialAttacks);
		assertEquals("1:23", withHealth.fightTime);
	}

	@Test
	public void aHealthOnlyBarHasNoExtras()
	{
		final BarState state = BarState.healthOnly("Rat", HEALTH);

		assertEquals("Rat", state.name);
		assertNull(state.shortName);
		assertEquals(0, state.combatLevel);
		assertEquals(BarState.NO_DELVE_LEVEL, state.delveLevel);
		assertSame(HEALTH, state.health);
		assertSame(BarState.NO_PHASE_MARKERS, state.phaseMarkers);
		assertSame(HealthIndicatorMarkers.NONE, state.userMarkers);
		assertNull(state.killCountKey);
		assertNull(state.stats);
		assertEquals(Collections.emptyList(), state.specialAttacks);
		assertNull(state.fightTime);
	}
}
