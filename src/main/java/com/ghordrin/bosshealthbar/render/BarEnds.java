package com.ghordrin.bosshealthbar.render;


public enum BarEnds
{
	CLASSIC("Classic"),
	SUBTLE("Subtle");

	private final String label;

	BarEnds(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
