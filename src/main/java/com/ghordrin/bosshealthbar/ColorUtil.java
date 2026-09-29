package com.ghordrin.bosshealthbar;

import java.awt.Color;

final class ColorUtil
{
	private ColorUtil()
	{
	}

	static Color lerp(Color a, Color b, float t)
	{
		return new Color(
			mix(a.getRed(), b.getRed(), t),
			mix(a.getGreen(), b.getGreen(), t),
			mix(a.getBlue(), b.getBlue(), t),
			mix(a.getAlpha(), b.getAlpha(), t));
	}

	static Color brighten(Color c, float amount)
	{
		return lerp(c, Color.WHITE, amount);
	}

	static Color darken(Color c, float amount)
	{
		return lerp(c, Color.BLACK, amount);
	}

	static Color withAlpha(Color c, int alpha)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	private static int mix(int a, int b, float t)
	{
		return Math.max(0, Math.min(255, Math.round(a + (b - a) * t)));
	}
}
