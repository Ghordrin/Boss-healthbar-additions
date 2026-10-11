package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.ColorUtil.brighten;
import java.awt.Color;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
class ThemeColors
{
	static final Color DEFAULT_TEXT = new Color(0xE8E2D4);
	static final Color DEFAULT_HITPOINTS_TEXT = new Color(0xC8C0B0);
	private static final float MIN_TRAIL_CONTRAST = 1.5f;
	private static final float MIN_TRACK_CONTRAST = 1.6f;

	Color fillHigh;
	Color fillLow;
	Color trail;
	// Plain color behind the fill. Null keeps the default dark track.
	Color track;

	Color frame;

	Color text;
	Color levelText;
	Color hitpointsText;
	Color defeatedText;

	static ThemeColors of(Color fillHigh, Color fillLow, Color frame, Color trail)
	{
		return ThemeColors.builder()
			.fillHigh(fillHigh)
			.fillLow(fillLow)
			.trail(trail)
			.frame(frame)
			.text(DEFAULT_TEXT)
			.levelText(brighten(frame, 0.35f))
			.hitpointsText(DEFAULT_HITPOINTS_TEXT)
			.defeatedText(brighten(frame, 0.45f))
			.build();
	}

	// Follows how the built-in themes relate their colors: a darker fill at low health that leans red
	// for warm colors, a pale trail of the same hue, and a dark frame tinted with it.
	static ThemeColors fromBase(Color base)
	{
		final float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
		final float hue = hsb[0];
		final float saturation = hsb[1];
		final boolean grey = saturation < 0.12f;
		final boolean warm = hue >= 20f / 360f && hue <= 80f / 360f;

		final Color fillLow = Color.getHSBColor(hue + (warm ? -10f : 5f) / 360f,
			grey ? saturation : Math.min(1f, saturation * 1.03f + (saturation < 0.5f ? 0.14f : 0f)),
			hsb[2] * 0.68f);
		final Color frame = Color.getHSBColor(hue, grey ? 0.06f : 0.2f, 0.45f);
		return of(base, fillLow, frame, trailFor(base, hue, saturation));
	}

	// A flat theme drawn in the opponent's fill color. The frame and all the text stay the theme's, and the
	// track and trail only change when they would sit too close to that fill.
	static ThemeColors withBase(ThemeColors flat, Color base)
	{
		final float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
		final Color track = flat.getTrack() != null && contrast(base, flat.getTrack()) < MIN_TRACK_CONTRAST
			? trackFor(base, hsb[0], hsb[1]) : flat.getTrack();
		final Color trail = contrast(flat.getTrail(), base) < MIN_TRAIL_CONTRAST
			? trailFor(base, hsb[0], hsb[1]) : flat.getTrail();
		return flat.toBuilder()
			.fillHigh(base)
			.fillLow(base)
			.track(track)
			.trail(trail)
			.build();
	}

	// The color's own hue at a low brightness, dark enough for the fill to stand out against it.
	private static Color trackFor(Color base, float hue, float saturation)
	{
		for (float brightness = 0.3f; brightness > 0f; brightness -= 0.05f)
		{
			final Color track = Color.getHSBColor(hue, saturation, brightness);
			if (contrast(base, track) >= MIN_TRACK_CONTRAST)
			{
				return track;
			}
		}
		return Color.BLACK;
	}

	private static Color trailFor(Color base, float hue, float saturation)
	{
		final Color pale = Color.getHSBColor(hue, Math.min(0.5f, saturation * 0.4f), 0.97f);
		return contrast(pale, base) < MIN_TRAIL_CONTRAST ? Color.getHSBColor(hue + 0.5f, 0.45f, 0.9f) : pale;
	}

	static float contrast(Color a, Color b)
	{
		final float la = luminance(a);
		final float lb = luminance(b);
		return (Math.max(la, lb) + 0.05f) / (Math.min(la, lb) + 0.05f);
	}

	private static float luminance(Color c)
	{
		return 0.2126f * linear(c.getRed()) + 0.7152f * linear(c.getGreen()) + 0.0722f * linear(c.getBlue());
	}

	private static float linear(int channel)
	{
		final float c = channel / 255f;
		return c <= 0.03928f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
	}
}
