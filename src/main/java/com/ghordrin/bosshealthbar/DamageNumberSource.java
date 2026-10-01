package com.ghordrin.bosshealthbar;

public enum DamageNumberSource
{
	ME("Me"),
	PARTY("Party"),
	EVERYONE("Everyone");

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
