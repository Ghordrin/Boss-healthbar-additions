package com.ghordrin.bosshealthbar;

public enum BarPosition
{
	TOP_LEFT("Top left", Row.TOP, BarLayout.Spot.LEFT),
	TOP_CENTER("Top centre", Row.TOP, BarLayout.Spot.CENTER),
	TOP_RIGHT("Top right", Row.TOP, BarLayout.Spot.RIGHT),
	IN_BAR_LEFT("Inside left", Row.IN_BAR, BarLayout.Spot.LEFT),
	IN_BAR_CENTER("Inside centre", Row.IN_BAR, BarLayout.Spot.CENTER),
	IN_BAR_RIGHT("Inside right", Row.IN_BAR, BarLayout.Spot.RIGHT),
	BOTTOM_LEFT("Bottom left", Row.BOTTOM, BarLayout.Spot.LEFT),
	BOTTOM_CENTER("Bottom centre", Row.BOTTOM, BarLayout.Spot.CENTER),
	BOTTOM_RIGHT("Bottom right", Row.BOTTOM, BarLayout.Spot.RIGHT),
	SECOND_BOTTOM_LEFT("Second bottom left", Row.BOTTOM_2, BarLayout.Spot.LEFT),
	SECOND_BOTTOM_CENTER("Second bottom centre", Row.BOTTOM_2, BarLayout.Spot.CENTER),
	SECOND_BOTTOM_RIGHT("Second bottom right", Row.BOTTOM_2, BarLayout.Spot.RIGHT);

	// The rows around the bar, in the order they're drawn from the top down.
	enum Row
	{
		TOP,
		IN_BAR,
		BOTTOM,
		BOTTOM_2
	}

	static final int ROW_COUNT = Row.values().length;

	private final String label;
	private final Row row;
	private final BarLayout.Spot spot;

	BarPosition(String label, Row row, BarLayout.Spot spot)
	{
		this.label = label;
		this.row = row;
		this.spot = spot;
	}

	Row getRow()
	{
		return row;
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
