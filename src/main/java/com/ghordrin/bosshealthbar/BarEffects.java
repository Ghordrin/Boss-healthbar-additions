package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BarAnimation.clamp01;
import java.awt.Color;
import java.time.Duration;

final class BarEffects
{
	static final Duration BURN_DURATION = Duration.ofMillis(1300);
	static final int BURN_SPARK_COUNT = 30;
	private static final int BURN_SEED = 13;

	private static final int COLOR_STEPS = 16;
	private static final Color[] SPARK_COLORS = fireColors(210, 80, 50);

	private BarEffects()
	{
	}

	static final class Particle
	{
		float x;
		float y;
		float size;
		float progress;
		float along;
	}

	static float easeIn(float t)
	{
		final float k = clamp01(t);
		return k * k;
	}

	// Deterministic noise in [0, 1), so particles move the same way every frame without being stored.
	static float random(int seed, int index)
	{
		int h = seed * 0x9E3779B1 + index * 0x85EBCA6B;
		h ^= h >>> 16;
		h *= 0x7FEB352D;
		h ^= h >>> 15;
		h *= 0x846CA68B;
		h ^= h >>> 16;
		return (h >>> 8) / (float) (1 << 24);
	}

	// along is how far the spark started along the burn (0 at the right end, 1 at the left), x and y are
	// offsets from there and from the middle of the bar. False before the spark is thrown and once it's out.
	static boolean burnSpark(int index, long burnMillis, Particle out)
	{
		final int base = index * 5;
		final float start = random(BURN_SEED, base);
		final float life = 400f + random(BURN_SEED, base + 1) * 400f;
		final float age = burnMillis - start * BURN_DURATION.toMillis();
		if (age < 0f || age > life)
		{
			return false;
		}

		final float k = age / life;
		out.along = easeIn(start);
		out.x = (random(BURN_SEED, base + 2) - 0.5f) * 16f * k;
		out.y = -(10f + random(BURN_SEED, base + 3) * 22f) * k;
		out.size = 1f + random(BURN_SEED, base + 4) * 1.5f;
		out.progress = k;
		return true;
	}

	static float burnProgress(long millis)
	{
		return easeIn(millis / (float) BURN_DURATION.toMillis());
	}

	static Color sparkColor(float progress)
	{
		return SPARK_COLORS[Math.min(COLOR_STEPS - 1, (int) (clamp01(progress) * COLOR_STEPS))];
	}

	// Yellow-orange cooling to red as it fades out.
	private static Color[] fireColors(int startGreen, int endGreen, int blue)
	{
		final Color[] colors = new Color[COLOR_STEPS];
		for (int i = 0; i < COLOR_STEPS; i++)
		{
			final float k = i / (float) (COLOR_STEPS - 1);
			colors[i] = new Color(255, Math.round(startGreen + (endGreen - startGreen) * k), blue, Math.round(255 * (1f - k)));
		}
		return colors;
	}
}
