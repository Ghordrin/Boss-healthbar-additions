package com.ghordrin.bosshealthbar;

public enum TextOutline
{
	SHADOW("Shadow"),
	OUTLINE("Outline"),
	NONE("None");

	private final String label;

	TextOutline(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
