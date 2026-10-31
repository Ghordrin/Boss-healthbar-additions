package com.ghordrin.bosshealthbar.theme;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

// Fills in the Custom theme's colors from the theme that was selected before, so they can be
// tweaked from there.
@Singleton
public class CustomColors
{
	// Read off the list keyedColors writes, so a color added there can't be missed here.
	private static final Set<String> COLOR_KEYS =
		Collections.unmodifiableSet(keyedColors(ThemeColors.builder().build()).keySet());

	private final ConfigManager configManager;

	// Set while copyFrom writes, whose own writes would otherwise read as the user picking a color.
	private boolean copying;

	@Inject
	CustomColors(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	public static boolean isColorKey(String key)
	{
		return COLOR_KEYS.contains(key);
	}

	public boolean isCopying()
	{
		return copying;
	}

	public void copyFrom(ThemeColors colors)
	{
		if (colors == null)
		{
			return;
		}

		copying = true;
		try
		{
			keyedColors(colors).forEach((key, color) ->
				configManager.setConfiguration(BossHealthBarConfig.GROUP, key, color));
		}
		finally
		{
			copying = false;
		}
	}

	static Map<String, Color> keyedColors(ThemeColors theme)
	{
		final Map<String, Color> colors = new LinkedHashMap<>();
		colors.put(BossHealthBarConfig.CUSTOM_FILL_HIGH_COLOR_KEY, theme.getFillHigh());
		colors.put(BossHealthBarConfig.CUSTOM_FILL_LOW_COLOR_KEY, theme.getFillLow());
		// A theme without a background of its own leaves the picker transparent, so the dark one stays.
		colors.put(BossHealthBarConfig.CUSTOM_TRACK_COLOR_KEY,
			theme.getTrack() != null ? theme.getTrack() : BossHealthBarConfig.TRANSPARENT);
		colors.put(BossHealthBarConfig.CUSTOM_TRAIL_COLOR_KEY, theme.getTrail());
		colors.put(BossHealthBarConfig.CUSTOM_FRAME_COLOR_KEY, theme.getFrame());
		colors.put(BossHealthBarConfig.CUSTOM_TEXT_COLOR_KEY, theme.getText());
		colors.put(BossHealthBarConfig.CUSTOM_LEVEL_TEXT_COLOR_KEY, theme.getLevelText());
		colors.put(BossHealthBarConfig.CUSTOM_HITPOINTS_TEXT_COLOR_KEY, theme.getHitpointsText());
		colors.put(BossHealthBarConfig.CUSTOM_DEFEATED_TEXT_COLOR_KEY, theme.getDefeatedText());
		return colors;
	}

	// Null is what the painter reads as "draw the themed background", so a color left transparent means that.
	@VisibleForTesting
	static Color pickedTrack(Color track)
	{
		return track == null || track.getAlpha() == 0 ? null : track;
	}

	public static ThemeColors fromConfig(BossHealthBarConfig config)
	{
		return ThemeColors.builder()
			.fillHigh(config.customFillHighColor())
			.fillLow(config.customFillLowColor())
			.track(pickedTrack(config.customTrackColor()))
			.trail(config.customTrailColor())
			.frame(config.customFrameColor())
			.text(config.customTextColor())
			.levelText(config.customLevelTextColor())
			.hitpointsText(config.customHitpointsTextColor())
			.defeatedText(config.customDefeatedTextColor())
			.build();
	}
}
