package com.ghordrin.bosshealthbar.theme;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;

public interface BarTheme
{
	static BarTheme of(BossHealthBarConfig config)
	{
		return effective(config.oldschoolTheme(), config.theme());
	}

	static BarTheme effective(boolean oldschool, HealthBarTheme dropdownTheme)
	{
		if (!oldschool)
		{
			return dropdownTheme;
		}
		return dropdownTheme == HealthBarTheme.CUSTOM ? FlatTheme.CUSTOM : FlatTheme.OLDSCHOOL;
	}

	// Null for a Custom theme, whose colors come from the config.
	ThemeColors getColors();

	// Null for a theme with no icon of its own.
	ThemeIcon getIcon();

	// A flat theme has a plain fill and outline: no ornaments, crest, shine, texture, boss colors or gold bar.
	boolean isFlat();
}
