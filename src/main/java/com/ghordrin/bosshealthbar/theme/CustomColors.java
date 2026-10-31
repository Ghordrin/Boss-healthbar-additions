package com.ghordrin.bosshealthbar.theme;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

// Fills in the Custom theme's colors from the theme that was selected before, so they can be
// tweaked from there.
@Singleton
public class CustomColors
{
	// Every Custom color, in the order the panel lists them and color codes carry them, which is why a new one
	// goes on the end. Each says where it is saved, which theme color it starts from, and where it is drawn.
	private enum Slot
	{
		FILL_HIGH(BossHealthBarConfig.CUSTOM_FILL_HIGH_COLOR_KEY, BossHealthBarConfig::customFillHighColor,
			ThemeColors::getFillHigh, ThemeColors.ThemeColorsBuilder::fillHigh),
		FILL_LOW(BossHealthBarConfig.CUSTOM_FILL_LOW_COLOR_KEY, BossHealthBarConfig::customFillLowColor,
			ThemeColors::getFillLow, ThemeColors.ThemeColorsBuilder::fillLow),
		// A theme without a background of its own leaves the picker transparent, so the dark one stays.
		TRACK(BossHealthBarConfig.CUSTOM_TRACK_COLOR_KEY, BossHealthBarConfig::customTrackColor,
			ThemeColors::getTrack, ThemeColors.ThemeColorsBuilder::track, true),
		TRAIL(BossHealthBarConfig.CUSTOM_TRAIL_COLOR_KEY, BossHealthBarConfig::customTrailColor,
			ThemeColors::getTrail, ThemeColors.ThemeColorsBuilder::trail),
		FRAME(BossHealthBarConfig.CUSTOM_FRAME_COLOR_KEY, BossHealthBarConfig::customFrameColor,
			ThemeColors::getFrame, ThemeColors.ThemeColorsBuilder::frame),
		TEXT(BossHealthBarConfig.CUSTOM_TEXT_COLOR_KEY, BossHealthBarConfig::customTextColor,
			ThemeColors::getText, ThemeColors.ThemeColorsBuilder::text),
		LEVEL_TEXT(BossHealthBarConfig.CUSTOM_LEVEL_TEXT_COLOR_KEY, BossHealthBarConfig::customLevelTextColor,
			ThemeColors::getLevelText, ThemeColors.ThemeColorsBuilder::levelText),
		HITPOINTS_TEXT(BossHealthBarConfig.CUSTOM_HITPOINTS_TEXT_COLOR_KEY, BossHealthBarConfig::customHitpointsTextColor,
			ThemeColors::getHitpointsText, ThemeColors.ThemeColorsBuilder::hitpointsText),
		DEFEATED_TEXT(BossHealthBarConfig.CUSTOM_DEFEATED_TEXT_COLOR_KEY, BossHealthBarConfig::customDefeatedTextColor,
			ThemeColors::getDefeatedText, ThemeColors.ThemeColorsBuilder::defeatedText),
		// No theme colors its items one by one, so they start out following the name and level colors.
		DAMAGE_TEXT(BossHealthBarConfig.CUSTOM_DAMAGE_TEXT_COLOR_KEY, BossHealthBarConfig::customDamageTextColor,
			ThemeColors.ThemeColorsBuilder::damageText),
		DELVE_LEVEL_TEXT(BossHealthBarConfig.CUSTOM_DELVE_LEVEL_TEXT_COLOR_KEY,
			BossHealthBarConfig::customDelveLevelTextColor, ThemeColors.ThemeColorsBuilder::delveLevelText),
		COMBAT_LEVEL_TEXT(BossHealthBarConfig.CUSTOM_COMBAT_LEVEL_TEXT_COLOR_KEY,
			BossHealthBarConfig::customCombatLevelTextColor, ThemeColors.ThemeColorsBuilder::combatLevelText),
		FIGHT_TIMER_TEXT(BossHealthBarConfig.CUSTOM_FIGHT_TIMER_TEXT_COLOR_KEY,
			BossHealthBarConfig::customFightTimerTextColor, ThemeColors.ThemeColorsBuilder::fightTimerText),
		KILL_COUNT_TEXT(BossHealthBarConfig.CUSTOM_KILL_COUNT_TEXT_COLOR_KEY,
			BossHealthBarConfig::customKillCountTextColor, ThemeColors.ThemeColorsBuilder::killCountText),
		WEAKNESS_TEXT(BossHealthBarConfig.CUSTOM_WEAKNESS_TEXT_COLOR_KEY, BossHealthBarConfig::customWeaknessTextColor,
			ThemeColors.ThemeColorsBuilder::weaknessText),
		DRAIN_CAP_TEXT(BossHealthBarConfig.CUSTOM_DRAIN_CAP_TEXT_COLOR_KEY, BossHealthBarConfig::customDrainCapTextColor,
			ThemeColors.ThemeColorsBuilder::drainCapText),
		DEFENCE_TEXT(BossHealthBarConfig.CUSTOM_DEFENCE_TEXT_COLOR_KEY, BossHealthBarConfig::customDefenceTextColor,
			ThemeColors.ThemeColorsBuilder::defenceText),
		SPECIAL_ATTACK_TEXT(BossHealthBarConfig.CUSTOM_SPECIAL_ATTACK_TEXT_COLOR_KEY,
			BossHealthBarConfig::customSpecialAttackTextColor, ThemeColors.ThemeColorsBuilder::specialAttackText);

		final String key;
		final Function<BossHealthBarConfig, Color> saved;
		final Function<ThemeColors, Color> fromTheme;
		final BiConsumer<ThemeColors.ThemeColorsBuilder, Color> drawn;
		// Transparent is how the picker says "none": the themed background, or the shared text color.
		final boolean transparentIsNone;

		Slot(String key, Function<BossHealthBarConfig, Color> saved, Function<ThemeColors, Color> fromTheme,
			BiConsumer<ThemeColors.ThemeColorsBuilder, Color> drawn)
		{
			this(key, saved, fromTheme, drawn, false);
		}

		Slot(String key, Function<BossHealthBarConfig, Color> saved, BiConsumer<ThemeColors.ThemeColorsBuilder, Color> drawn)
		{
			this(key, saved, theme -> null, drawn, true);
		}

		Slot(String key, Function<BossHealthBarConfig, Color> saved, Function<ThemeColors, Color> fromTheme,
			BiConsumer<ThemeColors.ThemeColorsBuilder, Color> drawn, boolean transparentIsNone)
		{
			this.key = key;
			this.saved = saved;
			this.fromTheme = fromTheme;
			this.drawn = drawn;
			this.transparentIsNone = transparentIsNone;
		}
	}

	private static final Set<String> COLOR_KEYS = Collections.unmodifiableSet(new LinkedHashSet<>(
		Arrays.stream(Slot.values()).map(slot -> slot.key).collect(Collectors.toList())));

	private final ConfigManager configManager;

	// Set while copyFrom writes, whose own writes would otherwise read as the user picking a color.
	private boolean copying;

	@Inject
	CustomColors(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	public static String code(BossHealthBarConfig config)
	{
		return PaletteCode.encode(configuredColors(config).values(), config.customIconItemId(),
			config.customIconSpriteId());
	}

	// The theme goes to Custom first, so nothing it sets off copies a theme over the pasted colors. Colors an
	// older code doesn't have go back to following the others.
	public void use(PaletteCode.Palette palette)
	{
		configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.THEME_KEY, HealthBarTheme.CUSTOM);
		final List<Color> colors = palette.getColors();
		int i = 0;
		for (String key : COLOR_KEYS)
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, key,
				i < colors.size() ? colors.get(i) : BossHealthBarConfig.TRANSPARENT);
			i++;
		}
		configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.CUSTOM_ICON_ITEM_ID_KEY,
			palette.getIconItemId());
		configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.CUSTOM_ICON_SPRITE_ID_KEY,
			palette.getIconSpriteId());
	}

	// What each Custom color is set to now.
	@VisibleForTesting
	static Map<String, Color> configuredColors(BossHealthBarConfig config)
	{
		final Map<String, Color> colors = new LinkedHashMap<>();
		for (Slot slot : Slot.values())
		{
			colors.put(slot.key, slot.saved.apply(config));
		}
		return colors;
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

	// The theme's colors as the Custom colors would hold them, a color it has none of left transparent.
	static Map<String, Color> keyedColors(ThemeColors theme)
	{
		final Map<String, Color> colors = new LinkedHashMap<>();
		for (Slot slot : Slot.values())
		{
			final Color color = slot.fromTheme.apply(theme);
			colors.put(slot.key, color != null ? color : BossHealthBarConfig.TRANSPARENT);
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
		final ThemeColors.ThemeColorsBuilder colors = ThemeColors.builder();
		for (Slot slot : Slot.values())
		{
			final Color color = slot.saved.apply(config);
			slot.drawn.accept(colors, slot.transparentIsNone ? picked(color) : color);
		}
		return colors.build();
	}
}
