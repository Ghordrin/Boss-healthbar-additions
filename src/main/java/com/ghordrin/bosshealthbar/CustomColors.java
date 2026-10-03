package com.ghordrin.bosshealthbar;

import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

// Fills in the Custom theme's colors from the theme that was selected before, so they can be
// tweaked from there.
@Singleton
class CustomColors
{
	private final ConfigManager configManager;

	@Inject
	CustomColors(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	// previousThemeName is the saved name of the previous theme, or null when it was the default.
	void copyFrom(String previousThemeName)
	{
		final HealthBarTheme previous = previousThemeName != null
			? HealthBarTheme.named(previousThemeName) : BossHealthBarConfig.DEFAULT_THEME;
		final ThemeColors colors = previous != null ? previous.getColors() : null;
		if (colors == null)
		{
			return;
		}

		set("customFillHighColor", colors.getFillHigh());
		set("customFillLowColor", colors.getFillLow());
		set("customTrailColor", colors.getTrail());
		set("customFrameColor", colors.getFrame());
		set("customTextColor", colors.getText());
		set("customLevelTextColor", colors.getLevelText());
		set("customHitpointsTextColor", colors.getHitpointsText());
		set("customDefeatedTextColor", colors.getDefeatedText());
	}

	private void set(String key, Color color)
	{
		configManager.setConfiguration(BossHealthBarConfig.GROUP, key, color);
	}
}
