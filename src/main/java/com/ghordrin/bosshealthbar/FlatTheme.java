package com.ghordrin.bosshealthbar;

import java.awt.Color;

// Themes that aren't in the Theme dropdown. Oldschool is turned on with its own checkbox.
enum FlatTheme implements BarTheme
{
	OLDSCHOOL(ThemeColors.builder()
		.fillHigh(new Color(0x00C000))
		.fillLow(new Color(0x00C000))
		.track(new Color(0xB40000))
		.trail(new Color(0xD07A20))
		.frame(new Color(0x0A0A0A))
		.text(new Color(0xF4F4F4))
		.levelText(new Color(0xF2E08A))
		.hitpointsText(new Color(0xDCDCDC))
		.defeatedText(new Color(0xF07070))
		.build());

	private final ThemeColors colors;

	FlatTheme(ThemeColors colors)
	{
		this.colors = colors;
	}

	@Override
	public ThemeColors getColors()
	{
		return colors;
	}

	@Override
	public Integer getGodIconSpriteId()
	{
		return null;
	}

	@Override
	public boolean isFlat()
	{
		return true;
	}
}
