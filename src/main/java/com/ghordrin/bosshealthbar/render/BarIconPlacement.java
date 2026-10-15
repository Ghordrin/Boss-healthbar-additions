package com.ghordrin.bosshealthbar.render;


public enum BarIconPlacement
{
	CRESTS("Crests", true, true),
	CREST_LEFT("Crest left", true, false),
	CREST_RIGHT("Crest right", false, true),
	BEFORE_NAME("Before the name", false, false),
	NONE("No icon", false, false);

	private final String label;
	private final boolean left;
	private final boolean right;

	BarIconPlacement(String label, boolean left, boolean right)
	{
		this.label = label;
		this.left = left;
		this.right = right;
	}

	public boolean hasLeftCrest()
	{
		return left;
	}

	public boolean hasRightCrest()
	{
		return right;
	}

	public boolean hasCrest()
	{
		return left || right;
	}

	// The Oldschool theme draws no crests to hold an icon, so a crest choice goes before the name there instead.
	public BarIconPlacement onTheme(boolean flat)
	{
		return flat && hasCrest() ? BEFORE_NAME : this;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
