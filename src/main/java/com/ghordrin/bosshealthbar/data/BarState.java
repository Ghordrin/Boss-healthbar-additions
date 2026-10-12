package com.ghordrin.bosshealthbar.data;

import com.ghordrin.bosshealthbar.integrations.HealthIndicatorMarkers;
import com.ghordrin.bosshealthbar.integrations.SpecialAttackCounts;
import java.util.Collections;
import java.util.List;

public final class BarState
{
	public static final float[] NO_PHASE_MARKERS = new float[0];

	public final String name;
	public final int combatLevel;
	public final Health health;
	public final float[] phaseMarkers;
	public final HealthIndicatorMarkers.Marker[] userMarkers;
	public final String killCountKey;
	public final BossStats.Info stats;
	public final List<SpecialAttackCounts.Reading> specialAttacks;
	public final String fightTime;

	public BarState(String name, int combatLevel, Health health, float[] phaseMarkers, HealthIndicatorMarkers.Marker[] userMarkers,
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
	public static BarState healthOnly(String name, Health health)
	{
		return new BarState(name, 0, health, NO_PHASE_MARKERS, HealthIndicatorMarkers.NONE, null, null,
			Collections.emptyList(), null);
	}

	public BarState withFightTime(String time)
	{
		return new BarState(name, combatLevel, health, phaseMarkers, userMarkers, killCountKey, stats, specialAttacks,
			time);
	}
}
