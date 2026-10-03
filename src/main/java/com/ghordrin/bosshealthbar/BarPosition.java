package com.ghordrin.bosshealthbar;

public enum BarPosition
{
	TOP_LEFT("Top left", true, BarLayout.Spot.LEFT),
	TOP_CENTER("Top centre", true, BarLayout.Spot.CENTER),
	TOP_RIGHT("Top right", true, BarLayout.Spot.RIGHT),
	BOTTOM_LEFT("Bottom left", false, BarLayout.Spot.LEFT),
	BOTTOM_CENTER("Bottom centre", false, BarLayout.Spot.CENTER),
	BOTTOM_RIGHT("Bottom right", false, BarLayout.Spot.RIGHT);

	private final String label;
	private final boolean top;
	private final BarLayout.Spot spot;

	BarPosition(String label, boolean top, BarLayout.Spot spot)
	{
		this.label = label;
		this.top = top;
		this.spot = spot;
	}

	boolean isTop()
	{
		return top;
	}

	BarLayout.Spot getSpot()
	{
		return spot;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
