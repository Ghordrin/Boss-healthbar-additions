package com.ghordrin.bosshealthbar;

import java.awt.Color;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ThemeColorsTest
{
	@Test
	public void themesKeepTheirColors()
	{
		final ThemeColors zamorak = HealthBarTheme.ZAMORAK.getColors();
		assertEquals(new Color(0xB01818), zamorak.getFillHigh());
		assertEquals(new Color(0x6A0C0C), zamorak.getFillLow());
		assertEquals(new Color(0xF0A030), zamorak.getTrail());
		assertEquals(new Color(0x5A4E4E), zamorak.getFrame());
		assertEquals(ColorUtil.brighten(new Color(0x5A4E4E), 0.35f), zamorak.getLevelText());
	}

	@Test
	public void derivedColorsStayReadableForAnyBase()
	{
		for (float hue = 0f; hue < 1f; hue += 1f / 36f)
		{
			for (float saturation : new float[]{0.05f, 0.3f, 0.6f, 0.9f})
			{
				for (float brightness : new float[]{0.45f, 0.7f, 0.95f})
				{
					final Color base = Color.getHSBColor(hue, saturation, brightness);
					final ThemeColors colors = ThemeColors.fromBase(base);
					final String label = String.format("base %06X", base.getRGB() & 0xFFFFFF);

					assertEquals(label, base, colors.getFillHigh());
					assertTrue(label, brightness(colors.getFillLow()) < brightness(base));
					assertTrue(label, ThemeColors.contrast(colors.getTrail(), base) >= 1.5f
						|| hueGap(colors.getTrail(), base) >= 0.25f);
				}
			}
		}
	}

	private static float brightness(Color c)
	{
		return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[2];
	}

	private static float hueGap(Color a, Color b)
	{
		final float ha = Color.RGBtoHSB(a.getRed(), a.getGreen(), a.getBlue(), null)[0];
		final float hb = Color.RGBtoHSB(b.getRed(), b.getGreen(), b.getBlue(), null)[0];
		final float gap = Math.abs(ha - hb);
		return Math.min(gap, 1f - gap);
	}
}
