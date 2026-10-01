package com.ghordrin.bosshealthbar;

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

	BarState(String name, int combatLevel, Integer maxHealth, int ratio, int scale,
		boolean exactHealth, boolean percentOnly, float[] phaseMarkers, HealthIndicatorMarkers.Marker[] userMarkers)
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
	}
}
