package com.ghordrin.bosshealthbar.theme;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

// Fills in the Custom theme's colors from the theme that was selected before, so they can be
// tweaked from there.
@Singleton
public class CustomColors
{
	private static final List<String> ITEM_TEXT_COLOR_KEYS = Arrays.asList(
		BossHealthBarConfig.CUSTOM_DAMAGE_TEXT_COLOR_KEY,
		BossHealthBarConfig.CUSTOM_DELVE_LEVEL_TEXT_COLOR_KEY,
		BossHealthBarConfig.CUSTOM_COMBAT_LEVEL_TEXT_COLOR_KEY,
		BossHealthBarConfig.CUSTOM_FIGHT_TIMER_TEXT_COLOR_KEY,
		BossHealthBarConfig.CUSTOM_KILL_COUNT_TEXT_COLOR_KEY,
		BossHealthBarConfig.CUSTOM_WEAKNESS_TEXT_COLOR_KEY,
		BossHealthBarConfig.CUSTOM_DRAIN_CAP_TEXT_COLOR_KEY,
		BossHealthBarConfig.CUSTOM_DEFENCE_TEXT_COLOR_KEY,
		BossHealthBarConfig.CUSTOM_SPECIAL_ATTACK_TEXT_COLOR_KEY);

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

	// Only shows on the Custom theme, so it switches to it, after the colors so that switching copies nothing over them.
	public void randomize()
	{
		copyFrom(randomColors(ThreadLocalRandom.current()));
		configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.THEME_KEY, HealthBarTheme.CUSTOM);
	}

	// The bar follows how the themes relate their colors, and the text takes hues that sit together on the color
	// wheel, so the set still looks chosen.
	@VisibleForTesting
	static ThemeColors randomColors(Random random)
	{
		final float hue = random.nextFloat();
		final Color base = Color.getHSBColor(hue, between(random, 0.55f, 0.95f), between(random, 0.6f, 0.9f));
		final float[] accents = Harmony.pick(random).accents;
		final float accent = hue + accents[random.nextInt(accents.length)];
		final float other = hue + accents[random.nextInt(accents.length)];
		return ThemeColors.fromBase(base).toBuilder()
			.track(random.nextBoolean() ? Color.getHSBColor(hue, between(random, 0.3f, 0.6f), 0.16f) : null)
			.text(Color.getHSBColor(hue, between(random, 0.06f, 0.18f), 0.96f))
			.levelText(Color.getHSBColor(accent, between(random, 0.3f, 0.5f), 0.9f))
			.hitpointsText(Color.getHSBColor(hue, between(random, 0.04f, 0.12f), 0.85f))
			.defeatedText(Color.getHSBColor(other, between(random, 0.4f, 0.6f), 0.92f))
			.build();
	}

	private static float between(Random random, float low, float high)
	{
		return low + random.nextFloat() * (high - low);
	}

	// Hue offsets from the fill, as shares of the wheel, with how often each set comes up.
	@VisibleForTesting
	enum Harmony
	{
		ANALOGOUS(35, -1f / 12, 1f / 12),
		COMPLEMENTARY(30, 0.5f),
		SPLIT_COMPLEMENTARY(20, 5f / 12, 7f / 12),
		TRIADIC(15, 1f / 3, 2f / 3);

		private static final int TOTAL_WEIGHT = Arrays.stream(values()).mapToInt(h -> h.weight).sum();

		private final int weight;
		final float[] accents;

		Harmony(int weight, float... accents)
		{
			this.weight = weight;
			this.accents = accents;
		}

		static Harmony pick(Random random)
		{
			int roll = random.nextInt(TOTAL_WEIGHT);
			for (Harmony harmony : values())
			{
				roll -= harmony.weight;
				if (roll < 0)
				{
					return harmony;
				}
			}
			return ANALOGOUS;
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
		// No theme colors its items one by one, so they go back to following the name and level colors.
		for (String key : ITEM_TEXT_COLOR_KEYS)
		{
			colors.put(key, BossHealthBarConfig.TRANSPARENT);
		}
		return colors;
	}

	// Null is what the painter reads as "draw the themed background" or "follow the shared text color", so a
	// color left transparent means that.
	@VisibleForTesting
	static Color picked(Color color)
	{
		return color == null || color.getAlpha() == 0 ? null : color;
	}

	public static ThemeColors fromConfig(BossHealthBarConfig config)
	{
		return ThemeColors.builder()
			.fillHigh(config.customFillHighColor())
			.fillLow(config.customFillLowColor())
			.track(picked(config.customTrackColor()))
			.trail(config.customTrailColor())
			.frame(config.customFrameColor())
			.text(config.customTextColor())
			.levelText(config.customLevelTextColor())
			.hitpointsText(config.customHitpointsTextColor())
			.defeatedText(config.customDefeatedTextColor())
			.damageText(picked(config.customDamageTextColor()))
			.delveLevelText(picked(config.customDelveLevelTextColor()))
			.combatLevelText(picked(config.customCombatLevelTextColor()))
			.fightTimerText(picked(config.customFightTimerTextColor()))
			.killCountText(picked(config.customKillCountTextColor()))
			.weaknessText(picked(config.customWeaknessTextColor()))
			.drainCapText(picked(config.customDrainCapTextColor()))
			.defenceText(picked(config.customDefenceTextColor()))
			.specialAttackText(picked(config.customSpecialAttackTextColor()))
			.build();
	}
}
