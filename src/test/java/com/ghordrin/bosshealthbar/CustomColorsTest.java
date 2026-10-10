package com.ghordrin.bosshealthbar;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import static org.junit.Assert.assertEquals;
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
		final Map<String, Color> keyed = CustomColors.keyedColors(COLORS);
		// Saved key names: changing one resets that colour for every user.
		assertEquals(Arrays.asList("customFillHighColor", "customFillLowColor", "customTrailColor", "customFrameColor",
			"customTextColor", "customLevelTextColor", "customHitpointsTextColor", "customDefeatedTextColor"),
			new ArrayList<>(keyed.keySet()));
		assertEquals(Arrays.asList(new Color(1), new Color(2), new Color(3), new Color(4), new Color(5), new Color(6),
			new Color(7), new Color(8)), new ArrayList<>(keyed.values()));
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
