package com.ghordrin.bosshealthbar;

public enum NativeBossBarMode
{
	REPLACE("Replace it"),
	BOTH("Show both"),
	HIDE_OURS("Hide this bar");

	private final String label;

	NativeBossBarMode(String label)
	{
		this.label = label;
	}

	boolean hidesGameBar()
	{
		return this == REPLACE;
	}

	boolean hidesOurBar()
	{
		return this == HIDE_OURS;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
