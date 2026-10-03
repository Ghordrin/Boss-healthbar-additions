package com.ghordrin.bosshealthbar;

import java.util.Arrays;

// Places the items of one text row around the bar. Item indexes double as importance: a lower index is
// handed space first and sits closer to its spot's edge.
class BarLayout
{
	static final int NOT_PLACED = Integer.MIN_VALUE;

	enum Spot
	{
		LEFT,
		CENTER,
		RIGHT
	}

	private final Spot[] spots;
	private final int[] widths;
	private final boolean[] kept;
	private final int[] xs;

	private int width;
	private int left;
	private int right;
	private int gap;

	private int leftEnd;
	private int rightStart;
	private int centerStart;
	private int centerWidth;
	private boolean leftUsed;
	private boolean rightUsed;
	private boolean centerUsed;

	BarLayout(int size)
	{
		spots = new Spot[size];
		widths = new int[size];
		kept = new boolean[size];
		xs = new int[size];
		clear();
	}

	void clear()
	{
		Arrays.fill(spots, null);
		Arrays.fill(kept, false);
		Arrays.fill(xs, NOT_PLACED);
	}

	void add(int item, Spot spot, int itemWidth)
	{
		spots[item] = spot;
		widths[item] = itemWidth;
	}

	void addKept(int item, Spot spot, int itemWidth)
	{
		add(item, spot, itemWidth);
		kept[item] = true;
	}

	void setWidth(int item, int itemWidth)
	{
		widths[item] = itemWidth;
	}

	Spot spot(int item)
	{
		return spots[item];
	}

	int x(int item)
	{
		return xs[item];
	}

	int width(int item)
	{
		return widths[item];
	}

	void layout(int rowWidth, int leftEdge, int rightEdge, int itemGap)
	{
		width = rowWidth;
		left = leftEdge;
		right = rightEdge;
		gap = itemGap;
		leftUsed = rightUsed = centerUsed = false;
		leftEnd = left;
		rightStart = right;
		centerWidth = 0;
		centerStart = 0;
		boolean leftOpen = true;
		boolean rightOpen = true;
		boolean centerOpen = true;
		Arrays.fill(xs, NOT_PLACED);

		for (int item = 0; item < spots.length; item++)
		{
			final Spot spot = spots[item];
			if (spot == null)
			{
				continue;
			}

			final int itemWidth = widths[item];
			if (spot == Spot.LEFT && leftOpen)
			{
				final int x = leftLimit();
				final int end = x + itemWidth;
				if (kept[item] || end <= rightLimit() && (!centerUsed || end + gap <= centerStart))
				{
					xs[item] = x;
					leftEnd = end;
					leftUsed = true;
				}
				else
				{
					leftOpen = false;
				}
			}
			else if (spot == Spot.RIGHT && rightOpen)
			{
				final int end = rightLimit();
				final int x = end - itemWidth;
				if (kept[item] || x >= leftLimit() && (!centerUsed || x >= centerStart + centerWidth + gap))
				{
					xs[item] = x;
					rightStart = x;
					rightUsed = true;
				}
				else
				{
					rightOpen = false;
				}
			}
			else if (spot == Spot.CENTER && centerOpen)
			{
				final int groupWidth = centerUsed ? centerWidth + gap + itemWidth : itemWidth;
				final int groupStart = Math.floorDiv(width - groupWidth, 2);
				if (kept[item] || groupStart >= leftLimit() && groupStart + groupWidth <= rightLimit())
				{
					// The real x is set below, once the group's final width is known.
					xs[item] = 0;
					centerWidth = groupWidth;
					centerStart = groupStart;
					centerUsed = true;
				}
				else
				{
					centerOpen = false;
				}
			}
		}

		int x = centerStart;
		for (int item = 0; item < spots.length; item++)
		{
			if (spots[item] == Spot.CENTER && xs[item] != NOT_PLACED)
			{
				xs[item] = x;
				x += widths[item] + gap;
			}
		}
	}

	// How much wider a placed item could be without moving into the space of anything else placed on the row.
	int room(int item)
	{
		if (xs[item] == NOT_PLACED)
		{
			return 0;
		}

		final int slack;
		switch (spots[item])
		{
			case LEFT:
				slack = Math.min(rightLimit(), centerUsed ? centerStart - gap : Integer.MAX_VALUE) - leftEnd;
				break;
			case RIGHT:
				slack = rightStart - Math.max(leftLimit(), centerUsed ? centerStart + centerWidth + gap : Integer.MIN_VALUE);
				break;
			default:
				// The group stays centred, so it can only grow as far as the closer side allows, on both sides.
				final int maxWidth = Math.min(width - 2 * leftLimit(), 2 * rightLimit() + 1 - width);
				slack = maxWidth - centerWidth;
				break;
		}
		return Math.max(0, slack);
	}

	// NOT_PLACED when, centred, it would run into a placed item other than ignoredItem.
	int centredX(int itemWidth, int ignoredItem)
	{
		final int x = Math.floorDiv(width - itemWidth, 2);
		final int end = x + itemWidth;
		for (int item = 0; item < spots.length; item++)
		{
			if (item != ignoredItem && xs[item] != NOT_PLACED && x < xs[item] + widths[item] + gap && xs[item] < end + gap)
			{
				return NOT_PLACED;
			}
		}
		return x;
	}

	private int leftLimit()
	{
		return leftUsed ? leftEnd + gap : left;
	}

	private int rightLimit()
	{
		return rightUsed ? rightStart - gap : right;
	}
}
