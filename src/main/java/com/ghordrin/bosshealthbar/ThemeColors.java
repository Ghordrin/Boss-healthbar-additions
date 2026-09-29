package com.ghordrin.bosshealthbar;

import java.awt.Color;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
class ThemeColors
{
	static final Color DEFAULT_TEXT = new Color(0xE8E2D4);
	static final Color DEFAULT_HITPOINTS_TEXT = new Color(0xC8C0B0);

	Color fillHigh;
	Color fillLow;
	Color trail;

	Color frame;

	Color text;
	Color levelText;
	Color hitpointsText;
	Color defeatedText;
}
