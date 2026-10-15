package com.ghordrin.bosshealthbar.theme;

import lombok.Value;

// A theme's icon: a game sprite for most of them, an item for the ones the game has no sprite for.
@Value
public class ThemeIcon
{
	boolean item;
	int id;

	static ThemeIcon sprite(int id)
	{
		return new ThemeIcon(false, id);
	}

	static ThemeIcon item(int id)
	{
		return new ThemeIcon(true, id);
	}
}
