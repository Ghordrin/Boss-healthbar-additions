package com.ghordrin.bosshealthbar;

import java.time.Duration;

final class BarAnimation
{
	private static final Duration TRAIL_HOLD = Duration.ofMillis(800);
	private static final float TRAIL_CATCH_UP_RATE = 3.5f;
	private static final float TRAIL_MIN_DRAIN_PER_SECOND = 0.15f;
	private static final Duration FADE_IN_DURATION = Duration.ofMillis(300);
	private static final int INTRO_SLIDE_DISTANCE = 14;
	private static final Duration INTRO_SLIDE_DURATION = Duration.ofMillis(400);
	private static final Duration INTRO_EXPAND_DELAY = Duration.ofMillis(80);
	private static final Duration INTRO_EXPAND_DURATION = Duration.ofMillis(400);
	private static final Duration INTRO_FILL_DELAY = Duration.ofMillis(380);
	private static final Duration INTRO_FILL_DURATION = Duration.ofMillis(450);
	private static final Duration INTRO_TEXT_DELAY = Duration.ofMillis(400);
	private static final Duration INTRO_TEXT_DURATION = Duration.ofMillis(250);
	private static final Duration DEFEAT_HOLD = Duration.ofMillis(1600);
	private static final Duration DEFEAT_FADE = Duration.ofMillis(700);
	private static final Duration LOW_HEALTH_PULSE_PERIOD = Duration.ofMillis(1100);

	private float displayedFraction = -1f;
	private float trailFraction = -1f;
	private float actualFraction = -1f;
	private long lastRenderNanos;
	private long fadeStartNanos;
	private long defeatStartNanos;
	private boolean snapToTarget;

	void reset()
	{
		displayedFraction = -1f;
		trailFraction = -1f;
		actualFraction = -1f;
		fadeStartNanos = 0;
		defeatStartNanos = 0;
		snapToTarget = false;
	}

	// The next tick jumps straight to the new health without replaying the intro, and without
	// showing the difference from the old target as a damage trail.
	void retarget()
	{
		snapToTarget = true;
	}

	void resetFrameTime()
	{
		lastRenderNanos = 0;
	}

	void tick(float targetFraction, float healSpeed, boolean showTrail, long lastHitMillis)
	{
		long now = System.nanoTime();
		float dt = lastRenderNanos == 0 ? 0f : Math.min(0.25f, (now - lastRenderNanos) / 1_000_000_000f);
		lastRenderNanos = now;
		actualFraction = targetFraction;

		if (displayedFraction < 0f)
		{
			displayedFraction = targetFraction;
			trailFraction = targetFraction;
			fadeStartNanos = now;
			snapToTarget = false;
			return;
		}

		if (snapToTarget)
		{
			displayedFraction = targetFraction;
			trailFraction = targetFraction;
			snapToTarget = false;
			return;
		}

		if (targetFraction < displayedFraction)
		{
			displayedFraction = targetFraction;
		}
		else
		{
			displayedFraction += (targetFraction - displayedFraction) * Math.min(1f, dt * healSpeed);
			if (Math.abs(displayedFraction - targetFraction) < 0.001f)
			{
				displayedFraction = targetFraction;
			}
		}

		if (!showTrail || displayedFraction >= trailFraction)
		{
			trailFraction = displayedFraction;
			return;
		}

		if (lastHitMillis != 0 && System.currentTimeMillis() - lastHitMillis < TRAIL_HOLD.toMillis())
		{
			return;
		}

		final float gap = trailFraction - displayedFraction;
		trailFraction = Math.max(displayedFraction,
			trailFraction - Math.max(TRAIL_MIN_DRAIN_PER_SECOND, gap * TRAIL_CATCH_UP_RATE) * dt);
	}

	float getDisplayedFraction()
	{
		return displayedFraction;
	}

	float getTrailFraction()
	{
		return trailFraction;
	}

	float getActualFraction()
	{
		return actualFraction;
	}

	boolean isDefeatPlaying()
	{
		return defeatStartNanos != 0;
	}

	void startDefeat(long now)
	{
		defeatStartNanos = now;
	}

	void cancelDefeat()
	{
		defeatStartNanos = 0;
	}

	float defeatOpacity(long now)
	{
		if (defeatStartNanos == 0)
		{
			return 1f;
		}

		final long elapsed = now - defeatStartNanos;
		final long hold = DEFEAT_HOLD.toNanos();
		final long fade = DEFEAT_FADE.toNanos();
		if (elapsed >= hold + fade)
		{
			return -1f;
		}

		return elapsed > hold ? 1f - (elapsed - hold) / (float) fade : 1f;
	}

	private long introElapsed(long now)
	{
		return fadeStartNanos == 0 ? Long.MAX_VALUE : now - fadeStartNanos;
	}

	long introElapsedMillis(long now)
	{
		final long elapsed = introElapsed(now);
		return elapsed == Long.MAX_VALUE ? elapsed : elapsed / 1_000_000L;
	}

	float fadeInOpacity(long now)
	{
		return progress(introElapsed(now), Duration.ZERO, FADE_IN_DURATION);
	}

	int slideOffset(IntroAnimation intro, long now)
	{
		return intro.slide
			? Math.round(INTRO_SLIDE_DISTANCE * (1f - easeOut(progress(introElapsed(now), Duration.ZERO, INTRO_SLIDE_DURATION))))
			: 0;
	}

	float expandProgress(IntroAnimation intro, long now)
	{
		return intro.expand ? easeOut(progress(introElapsed(now), INTRO_EXPAND_DELAY, INTRO_EXPAND_DURATION)) : 1f;
	}

	float fillProgress(IntroAnimation intro, long now)
	{
		return intro.expand ? easeOut(progress(introElapsed(now), INTRO_FILL_DELAY, INTRO_FILL_DURATION)) : 1f;
	}

	float textOpacity(IntroAnimation intro, long now)
	{
		return intro.expand ? progress(introElapsed(now), INTRO_TEXT_DELAY, INTRO_TEXT_DURATION) : 1f;
	}

	float lowHealthPulse(boolean defeated, boolean enabled, int thresholdPercent)
	{
		if (defeated || !enabled || displayedFraction <= 0f || displayedFraction > thresholdPercent / 100f)
		{
			return 0f;
		}

		final long period = LOW_HEALTH_PULSE_PERIOD.toNanos();
		final double phase = (System.nanoTime() % period) / (double) period * 2 * Math.PI;
		return (float) (0.5 - 0.5 * Math.cos(phase));
	}

	static float progress(long elapsedNanos, Duration delay, Duration duration)
	{
		return clamp01((elapsedNanos - delay.toNanos()) / (float) duration.toNanos());
	}

	static float easeOut(float t)
	{
		final float inverse = 1f - t;
		return 1f - inverse * inverse * inverse;
	}

	static float clamp01(float value)
	{
		return Math.max(0f, Math.min(1f, value));
	}
}
