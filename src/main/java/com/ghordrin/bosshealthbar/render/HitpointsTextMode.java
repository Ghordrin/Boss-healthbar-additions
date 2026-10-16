package com.ghordrin.bosshealthbar.render;


public enum HitpointsTextMode
{
	NONE("None", Hitpoints.OFF, false),
	PERCENTAGE("Percentage", Hitpoints.OFF, true),
	HITPOINTS("Hitpoints", Hitpoints.OF_MAX, false),
	// Saved as BOTH since the first version that had it, so the name stays while the label says what it does.
	BOTH("Hitpoints and %", Hitpoints.OF_MAX, true),
	REMAINING("Hitpoints left", Hitpoints.LEFT, false),
	REMAINING_PERCENTAGE("Hitpoints left and %", Hitpoints.LEFT, true);

	enum Hitpoints
	{
		OFF,
		OF_MAX,
		LEFT
	}

	private final String label;
	private final Hitpoints hitpoints;
	private final boolean percentage;

	HitpointsTextMode(String label, Hitpoints hitpoints, boolean percentage)
	{
		this.label = label;
		this.hitpoints = hitpoints;
		this.percentage = percentage;
	}

	Hitpoints hitpoints()
	{
		return hitpoints;
	}

	boolean showsPercentage()
	{
		return percentage;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
