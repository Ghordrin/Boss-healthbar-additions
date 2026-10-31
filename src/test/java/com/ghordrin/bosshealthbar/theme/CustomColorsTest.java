package com.ghordrin.bosshealthbar.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

public class CustomColorsTest
{
	// Text has to stay readable over the game, and every color has to be set for the panel to show it.
	@Test
	public void randomColorsAreCompleteAndReadable()
	{
		for (long seed = 0; seed < 2000; seed++)
		{
			final ThemeColors colors = CustomColors.randomColors(new Random(seed));
			for (Map.Entry<String, Color> entry : CustomColors.keyedColors(colors).entrySet())
			{
				assertNotNull(entry.getKey(), entry.getValue());
			}
			for (Color text : Arrays.asList(colors.getText(), colors.getLevelText(), colors.getHitpointsText(),
				colors.getDefeatedText()))
			{
				assertTrue("seed " + seed, Color.RGBtoHSB(text.getRed(), text.getGreen(), text.getBlue(), null)[2] >= 0.8f);
			}
		}
	}

	@Test
	public void everyHarmonyComesUpAndTheWeightsHold()
	{
		final Map<CustomColors.Harmony, Integer> counts = new EnumMap<>(CustomColors.Harmony.class);
		final Random random = new Random(1);
		for (int i = 0; i < 10000; i++)
		{
			counts.merge(CustomColors.Harmony.pick(random), 1, Integer::sum);
		}
		assertEquals(CustomColors.Harmony.values().length, counts.size());
		assertTrue(counts.get(CustomColors.Harmony.ANALOGOUS) > counts.get(CustomColors.Harmony.TRIADIC));
	}

	private static final ThemeColors COLORS = ThemeColors.builder()
		.fillHigh(new Color(1))
		.fillLow(new Color(2))
		.trail(new Color(3))
		.frame(new Color(4))
		.text(new Color(5))
		.levelText(new Color(6))
		.hitpointsText(new Color(7))
		.defeatedText(new Color(8))
		.build();

	@Test
	public void everyCustomColorKeyGetsItsOwnColor()
	{
		final Map<String, Color> keyed = CustomColors.keyedColors(COLORS.toBuilder().track(new Color(9)).build());
		// Saved key names: changing one resets that colour for every user.
		assertEquals(Arrays.asList("customFillHighColor", "customFillLowColor", "customTrackColor", "customTrailColor",
			"customFrameColor", "customTextColor", "customLevelTextColor", "customHitpointsTextColor",
			"customDefeatedTextColor", "customDamageTextColor", "customDelveLevelTextColor",
			"customCombatLevelTextColor", "customFightTimerTextColor", "customKillCountTextColor",
			"customWeaknessTextColor", "customDrainCapTextColor", "customDefenceTextColor",
			"customSpecialAttackTextColor"),
			new ArrayList<>(keyed.keySet()));
		final Color none = BossHealthBarConfig.TRANSPARENT;
		assertEquals(Arrays.asList(new Color(1), new Color(2), new Color(9), new Color(3), new Color(4), new Color(5),
			new Color(6), new Color(7), new Color(8), none, none, none, none, none, none, none, none, none),
			new ArrayList<>(keyed.values()));
	}

	// Only the Oldschool theme has a background of its own, so switching from any other has to clear it
	// rather than leave the one the user picked last time under the new colours.
	@Test
	public void aThemeWithoutABackgroundLeavesThePickerTransparent()
	{
		assertEquals(BossHealthBarConfig.TRANSPARENT,
			CustomColors.keyedColors(COLORS).get("customTrackColor"));
	}

	// The prompt to switch themes hangs off this, so a color the set doesn't know about would change
	// nothing and say nothing.
	@Test
	public void everyColorTheThemeWritesCountsAsAColorKey()
	{
		for (String key : CustomColors.keyedColors(COLORS).keySet())
		{
			assertTrue(key, CustomColors.isColorKey(key));
		}
		assertFalse(CustomColors.isColorKey(BossHealthBarConfig.THEME_KEY));
		assertFalse(CustomColors.isColorKey(BossHealthBarConfig.CUSTOM_ICON_ITEM_ID_KEY));
	}

	// The painter reads null as "draw the themed background", which is what an unpicked color has to mean.
	@Test
	public void onlyAnOpaqueBackgroundReachesTheBar()
	{
		assertNull(CustomColors.picked(BossHealthBarConfig.TRANSPARENT));
		assertNull(CustomColors.picked(null));
		assertNull(CustomColors.picked(new Color(180, 0, 0, 0)));
		assertEquals(new Color(0xB40000), CustomColors.picked(new Color(0xB40000)));
	}

	@Test
	public void theCustomThemeReadsEveryCustomColor()
	{
		final BossHealthBarConfig config = new BossHealthBarConfig()
		{
			@Override
			public Color customFillHighColor()
			{
				return COLORS.getFillHigh();
			}

			@Override
			public Color customFillLowColor()
			{
				return COLORS.getFillLow();
			}

			@Override
			public Color customTrackColor()
			{
				return BossHealthBarConfig.TRANSPARENT;
			}

			@Override
			public Color customTrailColor()
			{
				return COLORS.getTrail();
			}

			@Override
			public Color customFrameColor()
			{
				return COLORS.getFrame();
			}

			@Override
			public Color customTextColor()
			{
				return COLORS.getText();
			}

			@Override
			public Color customLevelTextColor()
			{
				return COLORS.getLevelText();
			}

			@Override
			public Color customHitpointsTextColor()
			{
				return COLORS.getHitpointsText();
			}

			@Override
			public Color customDefeatedTextColor()
			{
				return COLORS.getDefeatedText();
			}
		};
		assertEquals(COLORS, CustomColors.fromConfig(config));
	}
}
