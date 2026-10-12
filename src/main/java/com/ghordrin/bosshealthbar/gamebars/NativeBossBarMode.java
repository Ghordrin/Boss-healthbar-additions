package com.ghordrin.bosshealthbar.gamebars;


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

	public boolean hidesGameBar()
	{
		return this == REPLACE;
	}

	public boolean hidesOurBar()
	{
		return this == HIDE_OURS;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
