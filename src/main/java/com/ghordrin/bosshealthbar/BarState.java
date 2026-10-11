package com.ghordrin.bosshealthbar;

import java.util.Collections;
import java.util.List;

final class BarState
{
	static final float[] NO_PHASE_MARKERS = new float[0];

	final String name;
	final int combatLevel;
	final Health health;
	final float[] phaseMarkers;
	final HealthIndicatorMarkers.Marker[] userMarkers;
	final String killCountKey;
	final BossStats.Info stats;
	final List<SpecialAttackCounts.Reading> specialAttacks;
	final String fightTime;

	BarState(String name, int combatLevel, Health health, float[] phaseMarkers, HealthIndicatorMarkers.Marker[] userMarkers,
		String killCountKey, BossStats.Info stats, List<SpecialAttackCounts.Reading> specialAttacks, String fightTime)
	{
		this.name = name;
		this.combatLevel = combatLevel;
		this.health = health;
		this.phaseMarkers = phaseMarkers;
		this.userMarkers = userMarkers;
		this.killCountKey = killCountKey;
		this.stats = stats;
		this.specialAttacks = specialAttacks;
		this.fightTime = fightTime;
	}

	// A bar with a name and health but none of the extras.
	static BarState healthOnly(String name, Health health)
	{
		return new BarState(name, 0, health, NO_PHASE_MARKERS, HealthIndicatorMarkers.NONE, null, null,
			Collections.emptyList(), null);
	}

	BarState withFightTime(String time)
	{
		return new BarState(name, combatLevel, health, phaseMarkers, userMarkers, killCountKey, stats, specialAttacks,
			time);
	}
}
