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

	@Test
	public void aBaseAwayFromTheFlatTrackKeepsThatTrack()
	{
		final ThemeColors flat = FlatTheme.OLDSCHOOL.getColors();
		final ThemeColors colors = ThemeColors.withBase(flat, new Color(0x3CB043));
		assertEquals(flat.getTrack(), colors.getTrack());
		assertEquals(new Color(0x3CB043), colors.getFillHigh());
		assertEquals(new Color(0x3CB043), colors.getFillLow());
	}

	@Test
	public void aBaseTooCloseToTheFlatTrackGetsADarkerOneOfItsOwn()
	{
		final ThemeColors flat = FlatTheme.OLDSCHOOL.getColors();
		final Color base = new Color(0xB40000);
		final ThemeColors colors = ThemeColors.withBase(flat, base);
		assertTrue(brightness(colors.getTrack()) < brightness(base));
		assertTrue(ThemeColors.contrast(base, colors.getTrack()) >= 1.6f);
	}

	@Test
	public void theFlatBarStaysReadableAndKeepsItsOwnFrameAndTextForAnyBase()
	{
		final ThemeColors flat = FlatTheme.OLDSCHOOL.getColors();
		for (float hue = 0f; hue < 1f; hue += 1f / 72f)
		{
			for (float saturation : new float[]{0.05f, 0.3f, 0.6f, 0.9f, 1f})
			{
				for (float brightness = 0.3f; brightness <= 1f; brightness += 0.02f)
				{
					final Color base = Color.getHSBColor(hue, saturation, brightness);
					final ThemeColors colors = ThemeColors.withBase(flat, base);
					final String label = String.format("base %06X", base.getRGB() & 0xFFFFFF);

					assertEquals(label, base, colors.getFillHigh());
					assertEquals(label, base, colors.getFillLow());
					assertTrue(label, ThemeColors.contrast(base, colors.getTrack()) >= 1.6f);
					assertTrue(label, ThemeColors.contrast(colors.getTrail(), base) >= 1.5f
						|| hueGap(colors.getTrail(), base) >= 0.25f);

					assertEquals(label, flat.getFrame(), colors.getFrame());
					assertEquals(label, flat.getText(), colors.getText());
					assertEquals(label, flat.getLevelText(), colors.getLevelText());
					assertEquals(label, flat.getHitpointsText(), colors.getHitpointsText());
					assertEquals(label, flat.getDefeatedText(), colors.getDefeatedText());
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
