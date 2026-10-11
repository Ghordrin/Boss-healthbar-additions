package com.ghordrin.bosshealthbar;

// One health reading: ratio out of scale, which is the exact hitpoints when exactHealth is set.
final class Health
{
	final int ratio;
	final int scale;
	final boolean exactHealth;
	final Integer maxHealth;
	final boolean percentOnly;

	Health(int ratio, int scale, boolean exactHealth, Integer maxHealth, boolean percentOnly)
	{
		this.ratio = ratio;
		this.scale = scale;
		this.exactHealth = exactHealth;
		this.maxHealth = maxHealth;
		this.percentOnly = percentOnly;
	}
}
