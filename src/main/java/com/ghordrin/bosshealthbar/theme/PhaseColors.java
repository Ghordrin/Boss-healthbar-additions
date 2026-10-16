package com.ghordrin.bosshealthbar.theme;

import java.awt.Color;

// The colors the game paints its own boss bar in. They are green until a boss shields itself or changes phase,
// when the game switches the whole bar to another hue. Our bar covers the game's, so it follows that switch to
// keep saying what the game was saying.
public final class PhaseColors
{
	private static final Color DEFAULT_FILL = new Color(0x00C800);
	private static final Color DEFAULT_TRAIL = new Color(0x009000);
	private static final Color DEFAULT_TRACK = new Color(0xC80000);

	private final Color fill;
	private final Color trail;
	private final Color track;

	private PhaseColors(Color fill, Color trail, Color track)
	{
		this.fill = fill;
		this.trail = trail;
		this.track = track;
	}

	// Null while the game paints the colors it uses for every boss, which are the ones our themes replace.
	// A bar that hasn't been filled in yet reads as black, and says nothing about a phase either.
	public static PhaseColors of(int fillRgb, int trailRgb, int trackRgb)
	{
		if ((fillRgb & 0xFFFFFF) == 0)
		{
			return null;
		}
		final Color fill = new Color(fillRgb);
		final Color trail = new Color(trailRgb);
		final Color track = new Color(trackRgb);
		if (DEFAULT_FILL.equals(fill) && DEFAULT_TRAIL.equals(trail) && DEFAULT_TRACK.equals(track))
		{
			return null;
		}
		return new PhaseColors(fill, trail, track);
	}

	// The frame and the text stay the theme's, so only what the game recolors is recolored here. An override
	// is a single color, so the trail and the track come off it the way a boss color's do.
	public ThemeColors applyTo(ThemeColors theme, Color override)
	{
		if (override != null && override.getAlpha() > 0)
		{
			return ThemeColors.withBase(theme, override);
		}
		return theme.toBuilder()
			.fillHigh(fill)
			.fillLow(fill)
			.trail(trail)
			.track(track)
			.build();
	}
}
