package com.ghordrin.bosshealthbar;

interface BarTheme
{
	static BarTheme of(BossHealthBarConfig config)
	{
		return effective(config.oldschoolTheme(), config.theme());
	}

	static BarTheme effective(boolean oldschool, HealthBarTheme dropdownTheme)
	{
		return oldschool ? FlatTheme.OLDSCHOOL : dropdownTheme;
	}

	// Null for the Custom theme, whose colors come from the config.
	ThemeColors getColors();

	Integer getGodIconSpriteId();

	// A flat theme has a plain fill and outline: no ornaments, crest, shine, texture, boss colors or gold bar.
	boolean isFlat();
}
