package com.ghordrin.bosshealthbar.data;


// One health reading: ratio out of scale, which is the exact hitpoints when exactHealth is set.
public final class Health
{
	public final int ratio;
	public final int scale;
	public final boolean exactHealth;
	public final Integer maxHealth;
	public final boolean percentOnly;

	public Health(int ratio, int scale, boolean exactHealth, Integer maxHealth, boolean percentOnly)
	{
		this.ratio = ratio;
		this.scale = scale;
		this.exactHealth = exactHealth;
		this.maxHealth = maxHealth;
		this.percentOnly = percentOnly;
	}
}
