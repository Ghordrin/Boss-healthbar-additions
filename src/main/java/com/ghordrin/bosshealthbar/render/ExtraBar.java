package com.ghordrin.bosshealthbar.render;

import static com.ghordrin.bosshealthbar.render.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.render.BarPainter.capRise;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.data.BarState;
import com.ghordrin.bosshealthbar.data.Health;
import com.ghordrin.bosshealthbar.gamebars.GameBossBar;
import com.ghordrin.bosshealthbar.theme.PhaseColors;
import com.ghordrin.bosshealthbar.theme.ThemeColors;
import com.google.common.annotations.VisibleForTesting;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;

// The second bar the game drives for a boss that carries something besides its health, such as an energy bar.
// Hiding the game's bar takes that one with it, so it is drawn again as a thin strip under the health, in the
// colors the game paints it in. It follows the game's bar and has nothing of its own to set, so there is no
// setting for it: it shows whenever the game shows one.
@Singleton
public class ExtraBar
{
	// The room left between the health bar and this one, tighter than the gap before another boss's bar since
	// this one belongs to the same boss.
	public static final int GAP = 2;

	@VisibleForTesting
	static final int MIN_THICKNESS = 3;
	private static final int THICKNESS_SHARE = 3;

	// drawBar reads the phase markers off a state and takes the fraction it fills to from the animation, so
	// one state with no markers serves every frame and nothing is allocated per frame.
	private static final BarState STATE = BarState.healthOnly(null, new Health(0, 1, true, null, false));

	private final BossHealthBarConfig config;
	private final BarPainter barPainter;
	private final GameBossBar gameBossBar;
	private final BarAnimation animation = new BarAnimation();

	@Inject
	private ExtraBar(BossHealthBarConfig config, BarPainter barPainter, GameBossBar gameBossBar)
	{
		this.config = config;
		this.barPainter = barPainter;
		this.gameBossBar = gameBossBar;
	}

	// The preview is a bar of our own making, so a boss being fought behind it doesn't put a strip on it.
	public boolean isLive(boolean preview)
	{
		return !preview && gameBossBar.showsExtraBar() && gameBossBar.extraMaxHealth() > 0;
	}

	// A share of the main bar's height, so the strip keeps its proportion whatever that height is set to.
	public static int thickness(int barHeight)
	{
		return Math.max(MIN_THICKNESS, Math.round(barHeight / (float) THICKNESS_SHARE));
	}

	// The room it takes, without the gap before it.
	public static int height(int barHeight, boolean flat, BarEnds ends)
	{
		final int thickness = thickness(barHeight);
		return capRise(thickness, flat, ends) * 2 + thickness;
	}

	public void reset()
	{
		animation.reset();
	}

	public void draw(Graphics2D graphics, ThemeColors themeColors, int barY, int shownWidth, int shownInset,
		int barHeight, float fillProgress, boolean flat, BarEnds ends)
	{
		animation.tick(fraction(gameBossBar.extraHealth(), gameBossBar.extraMaxHealth()), config.animationSpeed(),
			config.showDamageTrail(), 0);
		graphics.translate(shownInset, 0);
		barPainter.drawBar(graphics, colors(themeColors, flat), STATE, animation, false, barY, shownWidth,
			thickness(barHeight), fillProgress, false, flat, ends, false, false, -1);
		graphics.translate(-shownInset, 0);
	}

	@VisibleForTesting
	static float fraction(int current, int base)
	{
		return base <= 0 ? 0f : clamp01(current / (float) base);
	}

	// The game's own colors for this bar, which tell it apart from the health above it. They are this bar's
	// colors rather than a phase of the health, so the phase color setting and the fortify tint stay out of
	// it. A boss whose extra bar the game paints in the usual boss colors keeps the theme's.
	private ThemeColors colors(ThemeColors themeColors, boolean flat)
	{
		final PhaseColors colors = gameBossBar.extraBarColors();
		return colors != null ? colors.applyTo(themeColors, null, false) : themeColors;
	}
}
