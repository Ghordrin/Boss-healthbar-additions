package com.ghordrin.bosshealthbar;

import java.util.List;

final class BarState
{
	static final float[] NO_PHASE_MARKERS = new float[0];

	final String name;
	final int combatLevel;
	final Integer maxHealth;
	final int ratio;
	final int scale;
	final boolean exactHealth;
	final boolean percentOnly;
	final float[] phaseMarkers;
	final HealthIndicatorMarkers.Marker[] userMarkers;
	final String killCountKey;
	final BossStats.Info stats;
	final List<SpecialAttackCounts.Reading> specialAttacks;
	final String fightTime;

	BarState(String name, int combatLevel, Integer maxHealth, int ratio, int scale,
		boolean exactHealth, boolean percentOnly, float[] phaseMarkers, HealthIndicatorMarkers.Marker[] userMarkers,
		String killCountKey, BossStats.Info stats, List<SpecialAttackCounts.Reading> specialAttacks, String fightTime)
	{
		this.name = name;
		this.combatLevel = combatLevel;
		this.maxHealth = maxHealth;
		this.ratio = ratio;
		this.scale = scale;
		this.exactHealth = exactHealth;
		this.percentOnly = percentOnly;
		this.phaseMarkers = phaseMarkers;
		this.userMarkers = userMarkers;
		this.killCountKey = killCountKey;
		this.stats = stats;
		this.specialAttacks = specialAttacks;
		this.fightTime = fightTime;
	}

	BarState withFightTime(String time)
	{
		return new BarState(name, combatLevel, maxHealth, ratio, scale, exactHealth, percentOnly, phaseMarkers,
			userMarkers, killCountKey, stats, specialAttacks, time);
	}
}
