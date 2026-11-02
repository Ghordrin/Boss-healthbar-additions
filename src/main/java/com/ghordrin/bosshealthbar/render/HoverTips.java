package com.ghordrin.bosshealthbar.render;

import java.util.Arrays;

// The tooltips of what was drawn this frame, each with the area it covers, so the one under the mouse can be shown.
// Cleared and refilled every frame, so the arrays only grow and nothing is made per frame.
public class HoverTips
{
	private int[] areas = new int[16];
	private String[] tips = new String[4];
	private int count;

	public void clear()
	{
		Arrays.fill(tips, 0, count, null);
		count = 0;
	}

	public void add(int left, int top, int right, int bottom, String tip)
	{
		if (tip == null || tip.isEmpty() || right <= left || bottom <= top)
		{
			return;
		}
		if (count == tips.length)
		{
			tips = Arrays.copyOf(tips, count * 2);
			areas = Arrays.copyOf(areas, count * 8);
		}
		areas[count * 4] = left;
		areas[count * 4 + 1] = top;
		areas[count * 4 + 2] = right;
		areas[count * 4 + 3] = bottom;
		tips[count++] = tip;
	}

	public String tipAt(int x, int y)
	{
		for (int i = 0; i < count; i++)
		{
			final int a = i * 4;
			if (x >= areas[a] && y >= areas[a + 1] && x < areas[a + 2] && y < areas[a + 3])
			{
				return tips[i];
			}
		}
		return null;
	}
}
