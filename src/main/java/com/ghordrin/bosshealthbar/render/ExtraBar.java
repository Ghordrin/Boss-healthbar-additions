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
// Hiding the game's bar takes that one with it, so it is drawn again as a thin strip under the rows below the
// health, in the colors the game paints it in. Under the rows rather than right under the health, so the
// health's own text stays next to it. It follows the game's bar and has nothing of its own to set, so there is
// no setting for it: it shows whenever the game shows one. Its value is written on a line above the strip, as
// the other boss's bar writes its own, in the form the hitpoints setting picks.
@Singleton
public class ExtraBar
{
	// The room left between the rows above and this one.
	public static final int GAP = 3;

	@VisibleForTesting
	static final int MIN_THICKNESS = 3;
	private static final int THICKNESS_SHARE = 3;

	// drawBar reads the phase markers off a state and takes the fraction it fills to from the animation, so
	// one state with no markers serves every frame and nothing is allocated per frame.
	private static final BarState STATE = BarState.healthOnly(null, new Health(0, 1, true, null, false));

	private final BossHealthBarConfig config;
	private final BarPainter barPainter;
	private final GameBossBar gameBossBar;
	private final BarTextPainter textPainter;
	private final BarAnimation animation = new BarAnimation();

	// The text is only made again when something in it changes, since the overlay asks every frame.
	private int textCurrent;
	private int textBase;
	private boolean textPercentOnly;
	private HitpointsTextMode textMode;
	private int textDecimals;
	private String text;

	@Inject
	private ExtraBar(BossHealthBarConfig config, BarPainter barPainter, GameBossBar gameBossBar,
		BarTextPainter textPainter)
	{
		this.config = config;
		this.barPainter = barPainter;
		this.gameBossBar = gameBossBar;
		this.textPainter = textPainter;
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
	public int height(int barHeight, boolean flat, BarEnds ends)
	{
		// Asked first every frame, so the fonts are brought up to date here, as the partner bar does.
		textPainter.updateFonts();
		return height(barHeight, flat, ends, textHeight());
	}

	@VisibleForTesting
	static int height(int barHeight, boolean flat, BarEnds ends, int textHeight)
	{
		return textHeight + stripHeight(barHeight, flat, ends);
	}

	private static int stripHeight(int barHeight, boolean flat, BarEnds ends)
	{
		final int thickness = thickness(barHeight);
		return capRise(thickness, flat, ends) * 2 + thickness;
	}

	private int textHeight()
	{
		return config.hitpointsTextMode() == HitpointsTextMode.NONE ? 0 : textPainter.hitpointsHeight();
	}

	public void reset()
	{
		animation.reset();
		textMode = null;
		text = null;
	}

	// top is where the room under the rows starts, so the gap and this bar's own ends are kept here.
	public void draw(Graphics2D graphics, ThemeColors themeColors, int top, int shownWidth, int shownInset,
		int barHeight, float fillProgress, boolean flat, BarEnds ends)
	{
		animation.tick(fraction(gameBossBar.extraHealth(), gameBossBar.extraMaxHealth()), config.animationSpeed(),
			config.showDamageTrail(), 0);
		final int thickness = thickness(barHeight);
		graphics.translate(shownInset, 0);
		barPainter.drawBar(graphics, colors(themeColors), STATE, animation, false,
			top + GAP + textHeight() + capRise(thickness, flat, ends), shownWidth, thickness, fillProgress,
			false, flat, ends, false, false, -1);
		graphics.translate(-shownInset, 0);
	}

	// Drawn apart from the strip, so it fades in with the rest of the text.
	public void drawText(Graphics2D graphics, ThemeColors colors, int top, int width)
	{
		final String value = text();
		if (value != null)
		{
			textPainter.drawCentredHitpoints(graphics, value, width / 2, top + GAP + textHeight() / 2, colors);
		}
	}

	private String text()
	{
		final HitpointsTextMode mode = config.hitpointsTextMode();
		final int decimals = config.percentDecimals();
		final int base = gameBossBar.extraMaxHealth();
		final int current = Math.max(0, Math.min(gameBossBar.extraHealth(), base));
		final boolean percentOnly = gameBossBar.isPercentOnly();
		if (mode != textMode || decimals != textDecimals || current != textCurrent || base != textBase
			|| percentOnly != textPercentOnly)
		{
			text = base > 0
				? HealthText.hitpointsText(mode, new Health(current, base, true, base, percentOnly), decimals) : null;
			textMode = mode;
			textDecimals = decimals;
			textCurrent = current;
			textBase = base;
			textPercentOnly = percentOnly;
		}
		return text;
	}

	@VisibleForTesting
	static float fraction(int current, int base)
	{
		return base <= 0 ? 0f : clamp01(current / (float) base);
	}

	// The game's own colors for this bar, which tell it apart from the health above it. They are this bar's
	// colors rather than a phase of the health, so the phase color setting and the fortify tint stay out of
	// it. A boss whose extra bar the game paints in the usual boss colors keeps the theme's.
	private ThemeColors colors(ThemeColors themeColors)
	{
		final PhaseColors colors = gameBossBar.extraBarColors();
		return colors != null ? colors.applyTo(themeColors, null, false) : themeColors;
	}
}
