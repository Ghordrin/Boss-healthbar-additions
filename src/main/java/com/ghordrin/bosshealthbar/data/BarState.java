package com.ghordrin.bosshealthbar.data;

import com.ghordrin.bosshealthbar.integrations.HealthIndicatorMarkers;
import com.ghordrin.bosshealthbar.integrations.SpecialAttackCounts;
import java.util.Collections;
import java.util.List;

public final class BarState
{
	public static final float[] NO_PHASE_MARKERS = new float[0];

	// No delve level to show.
	public static final int NO_DELVE_LEVEL = 0;

	public final String name;
	// The shorter name to fall back on when the full one doesn't fit, or null when the boss has none.
	public final String shortName;
	public final int combatLevel;
	public final int delveLevel;
	public final Health health;
	public final float[] phaseMarkers;
	public final HealthIndicatorMarkers.Marker[] userMarkers;
	public final String killCountKey;
	public final BossStats.Info stats;
	public final List<SpecialAttackCounts.Reading> specialAttacks;
	public final String fightTime;

	public BarState(String name, String shortName, int combatLevel, int delveLevel, Health health, float[] phaseMarkers,
		HealthIndicatorMarkers.Marker[] userMarkers, String killCountKey, BossStats.Info stats,
		List<SpecialAttackCounts.Reading> specialAttacks, String fightTime)
	{
		this.name = name;
		this.shortName = shortName;
		this.combatLevel = combatLevel;
		this.delveLevel = delveLevel;
		this.health = health;
		this.phaseMarkers = phaseMarkers;
		this.userMarkers = userMarkers;
		this.killCountKey = killCountKey;
		this.stats = stats;
		this.specialAttacks = specialAttacks;
		this.fightTime = fightTime;
	}

	// A bar with a name and health but none of the extras.
	public static BarState healthOnly(String name, Health health)
	{
		return new BarState(name, null, 0, NO_DELVE_LEVEL, health, NO_PHASE_MARKERS, HealthIndicatorMarkers.NONE, null,
			null, Collections.emptyList(), null);
	}

	public BarState withHealth(Health newHealth)
	{
		return new BarState(name, shortName, combatLevel, delveLevel, newHealth, phaseMarkers, userMarkers,
			killCountKey, stats, specialAttacks, fightTime);
	}

	public BarState withFightTime(String time)
	{
		return new BarState(name, shortName, combatLevel, delveLevel, health, phaseMarkers, userMarkers, killCountKey,
			stats, specialAttacks, time);
	}
}
