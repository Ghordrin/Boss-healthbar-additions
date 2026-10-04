package com.ghordrin.bosshealthbar;

public enum DamageNumberSource
{
	ME("Me"),
	PARTY("Party");

	private final String label;

	DamageNumberSource(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
