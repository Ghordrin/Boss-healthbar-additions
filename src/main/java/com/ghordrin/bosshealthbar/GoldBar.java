package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.ColorUtil.brighten;
import static com.ghordrin.bosshealthbar.ColorUtil.withAlpha;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Named;

class GoldBar
{
	static final Color GOLD = new Color(0xE0B84A);
	private static final int CHANCE = 250;
	private static final int DEVELOPER_CHANCE = 2;

	// Plays once, just after the intro animation has filled the bar.
	private static final long SHINE_DELAY_MILLIS = 850;
	private static final long SHINE_MILLIS = 900;
	private static final Color SHINE = new Color(255, 248, 225);

	private static final long SPARKLE_PERIOD_MILLIS = 1800;
	private static final float SPARKLE_VISIBLE = 0.35f;
	private static final Color SPARKLE = new Color(255, 246, 214);
	// x and y in crest radii from its centre, then a phase offset.
	private static final float[][] SPARKLES = {
		{-0.95f, -0.75f, 0f},
		{0.9f, -0.95f, 0.33f},
		{-0.15f, 1.05f, 0.66f},
	};

	private final Random random = new Random();
	private final int chance;

	private ThemeColors source;
	private ThemeColors gold;

	@Inject
	GoldBar(@Named("developerMode") boolean developerMode)
	{
		chance = developerMode ? DEVELOPER_CHANCE : CHANCE;
	}

	boolean roll()
	{
		return random.nextInt(chance) == 0;
	}

	ThemeColors colors(ThemeColors colors)
	{
		if (colors != source)
		{
			gold = gilded(colors);
			source = colors;
		}
		return gold;
	}

	static ThemeColors gilded(ThemeColors colors)
	{
		return colors.toBuilder()
			.frame(GOLD)
			.levelText(brighten(GOLD, 0.35f))
			.defeatedText(brighten(GOLD, 0.45f))
			.build();
	}

	void drawShine(Graphics2D graphics, int x, int y, int width, int height, long barAgeMillis)
	{
		final long time = barAgeMillis - SHINE_DELAY_MILLIS;
		if (time < 0 || time >= SHINE_MILLIS || width <= 0)
		{
			return;
		}

		final float progress = time / (float) SHINE_MILLIS;
		final float band = Math.max(24f, height * 3f);
		final float centre = x - band + (width + band * 2) * progress;
		graphics.setPaint(new LinearGradientPaint(
			centre - band / 2, y, centre + band / 2, y + height,
			new float[]{0f, 0.5f, 1f},
			new Color[]{withAlpha(SHINE, 0), withAlpha(SHINE, 150), withAlpha(SHINE, 0)}));
		graphics.fillRect(x, y, width, height);
	}

	void drawSparkles(Graphics2D graphics, float centreX, float centreY, float radius, boolean mirrored, long nowMillis)
	{
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final float cycle = (nowMillis % SPARKLE_PERIOD_MILLIS) / (float) SPARKLE_PERIOD_MILLIS;
		for (float[] sparkle : SPARKLES)
		{
			final float phase = (cycle + sparkle[2] + (mirrored ? 0.5f : 0f)) % 1f;
			if (phase >= SPARKLE_VISIBLE)
			{
				continue;
			}

			final float strength = (float) Math.sin(Math.PI * phase / SPARKLE_VISIBLE);
			final float x = centreX + (mirrored ? -sparkle[0] : sparkle[0]) * radius;
			final float y = centreY + sparkle[1] * radius;
			graphics.setColor(withAlpha(SPARKLE, Math.round(230 * strength)));
			graphics.fill(star(x, y, Math.max(1.5f, radius * 0.4f * strength)));
		}
	}

	private static Shape star(float x, float y, float size)
	{
		final float waist = size * 0.22f;
		final Path2D.Float path = new Path2D.Float();
		path.moveTo(x, y - size);
		path.lineTo(x + waist, y - waist);
		path.lineTo(x + size, y);
		path.lineTo(x + waist, y + waist);
		path.lineTo(x, y + size);
		path.lineTo(x - waist, y + waist);
		path.lineTo(x - size, y);
		path.lineTo(x - waist, y - waist);
		path.closePath();
		return path;
	}
}
