package com.ghordrin.bosshealthbar;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
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

		keyedColors(colors).forEach((key, color) ->
			configManager.setConfiguration(BossHealthBarConfig.GROUP, key, color));
	}

	static Map<String, Color> keyedColors(ThemeColors theme)
	{
		final Map<String, Color> colors = new LinkedHashMap<>();
		colors.put(BossHealthBarConfig.CUSTOM_FILL_HIGH_COLOR_KEY, theme.getFillHigh());
		colors.put(BossHealthBarConfig.CUSTOM_FILL_LOW_COLOR_KEY, theme.getFillLow());
		colors.put(BossHealthBarConfig.CUSTOM_TRAIL_COLOR_KEY, theme.getTrail());
		colors.put(BossHealthBarConfig.CUSTOM_FRAME_COLOR_KEY, theme.getFrame());
		colors.put(BossHealthBarConfig.CUSTOM_TEXT_COLOR_KEY, theme.getText());
		colors.put(BossHealthBarConfig.CUSTOM_LEVEL_TEXT_COLOR_KEY, theme.getLevelText());
		colors.put(BossHealthBarConfig.CUSTOM_HITPOINTS_TEXT_COLOR_KEY, theme.getHitpointsText());
		colors.put(BossHealthBarConfig.CUSTOM_DEFEATED_TEXT_COLOR_KEY, theme.getDefeatedText());
		return colors;
	}

	static ThemeColors fromConfig(BossHealthBarConfig config)
	{
		return ThemeColors.builder()
			.fillHigh(config.customFillHighColor())
			.fillLow(config.customFillLowColor())
			.trail(config.customTrailColor())
			.frame(config.customFrameColor())
			.text(config.customTextColor())
			.levelText(config.customLevelTextColor())
			.hitpointsText(config.customHitpointsTextColor())
			.defeatedText(config.customDefeatedTextColor())
			.build();
	}
}
