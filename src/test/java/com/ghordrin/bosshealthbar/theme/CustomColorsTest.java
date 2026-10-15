package com.ghordrin.bosshealthbar.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

public class CustomColorsTest
{
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
			"customDefeatedTextColor"),
			new ArrayList<>(keyed.keySet()));
		assertEquals(Arrays.asList(new Color(1), new Color(2), new Color(9), new Color(3), new Color(4), new Color(5),
			new Color(6), new Color(7), new Color(8)), new ArrayList<>(keyed.values()));
	}

	// Only the Oldschool theme has a background of its own, so switching from any other has to clear it
	// rather than leave the one the user picked last time under the new colours.
	@Test
	public void aThemeWithoutABackgroundLeavesThePickerTransparent()
	{
		assertEquals(BossHealthBarConfig.TRANSPARENT,
			CustomColors.keyedColors(COLORS).get("customTrackColor"));
	}

	// The painter reads null as "draw the themed background", which is what an unpicked color has to mean.
	@Test
	public void onlyAnOpaqueBackgroundReachesTheBar()
	{
		assertNull(CustomColors.pickedTrack(BossHealthBarConfig.TRANSPARENT));
		assertNull(CustomColors.pickedTrack(null));
		assertNull(CustomColors.pickedTrack(new Color(180, 0, 0, 0)));
		assertEquals(new Color(0xB40000), CustomColors.pickedTrack(new Color(0xB40000)));
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
