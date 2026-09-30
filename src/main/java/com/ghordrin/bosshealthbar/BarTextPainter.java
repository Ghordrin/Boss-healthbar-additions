package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.ColorUtil.withAlpha;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.inject.Inject;
import net.runelite.client.config.FontType;
import net.runelite.client.ui.FontManager;

class BarTextPainter
{
	private static final String DEFEATED_TEXT = "Defeated";
	private static final long DAMAGE_NUMBER_FADE_MILLIS = 400;
	private static final int REFERENCE_FONT_SIZE = 16;
	private static final int MIN_FONT_SIZE = 8;
	private static final int MAX_FONT_SIZE = 40;
	private static final float SMALL_TEXT_SCALE = 0.72f;
	private static final int HEADER_HEIGHT = 24;
	private static final int HEADER_BASELINE_GAP = 6;
	private static final int FOOTER_HEIGHT = 16;
	private static final int TEXT_INSET = 2;
	private static final int LEVEL_GAP = 12;
	private static final String DAMAGE_NUMBER_SIZING = "9999";
	private static final Color TEXT_SHADOW = new Color(0, 0, 0, 200);

	private final DamageTracker damageTracker;
	private final BossHealthBarConfig config;

	private String cachedFontFamily;
	private int cachedFontSize;
	private boolean cachedFontBold;
	private boolean cachedFontItalic;
	private Font textFont;
	private Font smallFont;
	private int layoutFontSize = REFERENCE_FONT_SIZE;
	private boolean pixelFont;

	private String ellipsizedSource;
	private Font ellipsizedFont;
	private int ellipsizedWidth;
	private String ellipsizedName;

	@Inject
	BarTextPainter(DamageTracker damageTracker, BossHealthBarConfig config)
	{
		this.damageTracker = damageTracker;
		this.config = config;
	}

	void updateFonts()
	{
		final FontType fontType = config.font();
		final String family = fontType.getFamily() != null ? fontType.getFamily() : BossHealthBarConfig.DEFAULT_FONT.getFamily();
		final int size = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontType.getSize()));
		if (family.equals(cachedFontFamily) && size == cachedFontSize
			&& fontType.isBold() == cachedFontBold && fontType.isItalic() == cachedFontItalic)
		{
			return;
		}

		// The RuneScape fonts are bitmaps made for one size and blur when resized, so they keep their
		// own size and the small text uses RuneScape Small.
		final Font pixelBase = runescapeFont(family);
		pixelFont = pixelBase != null;

		final int italic = fontType.isItalic() ? Font.ITALIC : Font.PLAIN;
		final int style = (fontType.isBold() ? Font.BOLD : Font.PLAIN) | italic;
		textFont = FontManager.getFallbackFont(family, style, pixelFont ? pixelBase.getSize() : size);

		if (pixelFont)
		{
			final Font runescapeSmall = FontManager.getRunescapeSmallFont();
			smallFont = FontManager.getFallbackFont(runescapeSmall.getFamily(), Font.PLAIN, runescapeSmall.getSize());
		}
		else
		{
			smallFont = FontManager.getFallbackFont(family, italic,
				Math.max(MIN_FONT_SIZE, Math.round(size * SMALL_TEXT_SCALE)));
		}

		layoutFontSize = textFont.getSize();
		cachedFontFamily = family;
		cachedFontSize = size;
		cachedFontBold = fontType.isBold();
		cachedFontItalic = fontType.isItalic();
	}

	private static Font runescapeFont(String family)
	{
		final Font regular = FontManager.getRunescapeFont();
		if (family.equals(regular.getFamily()))
		{
			return regular;
		}

		final Font small = FontManager.getRunescapeSmallFont();
		return family.equals(small.getFamily()) ? small : null;
	}

	private float textScale()
	{
		return layoutFontSize / (float) REFERENCE_FONT_SIZE;
	}

	int headerHeight()
	{
		return Math.round(HEADER_HEIGHT * textScale());
	}

	int headerBaselineGap()
	{
		return Math.round(HEADER_BASELINE_GAP * textScale());
	}

	int footerHeight()
	{
		return Math.round(FOOTER_HEIGHT * textScale());
	}

	void applyTextHints(Graphics2D graphics)
	{
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, pixelFont
			? RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
			: RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		// Fractional metrics place glyphs between pixels, which makes smoothed text look soft.
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	}

	void drawHeader(Graphics2D graphics, String name, int combatLevel, int width, int capWidth, int baseline, ThemeColors colors)
	{
		final int left = capWidth + TEXT_INSET;
		final int right = width - capWidth - TEXT_INSET;

		if (config.showBossName())
		{
			String levelText = null;
			int levelWidth = 0;
			if (config.showCombatLevel() && combatLevel > 0)
			{
				levelText = "LV " + combatLevel;
				graphics.setFont(smallFont);
				levelWidth = LEVEL_GAP + graphics.getFontMetrics().stringWidth(levelText);
			}

			int reserved = 0;
			if (config.showDamageNumber())
			{
				graphics.setFont(textFont);
				reserved = graphics.getFontMetrics().stringWidth(DAMAGE_NUMBER_SIZING) + LEVEL_GAP;
			}

			graphics.setFont(textFont);
			FontMetrics nameMetrics = graphics.getFontMetrics();
			String nameText = ellipsizeName(name, nameMetrics, right - left - reserved - levelWidth);
			drawShadowedText(graphics, nameText, left, baseline, colors.getText(), 1f);

			if (levelText != null)
			{
				int levelX = left + nameMetrics.stringWidth(nameText) + LEVEL_GAP;
				graphics.setFont(smallFont);
				drawShadowedText(graphics, levelText, levelX, baseline, colors.getLevelText(), 0.9f);
			}
		}

		if (config.showDamageNumber())
		{
			drawDamageNumber(graphics, right, baseline, colors.getText());
		}
	}

	String footerText(BarState state, boolean defeated)
	{
		return defeated ? DEFEATED_TEXT : buildHitpointsText(state);
	}

	void drawFooter(Graphics2D graphics, String text, boolean defeated, int width, int capWidth, int top, ThemeColors colors)
	{
		graphics.setFont(smallFont);
		final FontMetrics metrics = graphics.getFontMetrics();
		final int textX = defeated
			? (width - metrics.stringWidth(text)) / 2
			: width - capWidth - TEXT_INSET - metrics.stringWidth(text);
		final int baseline = top + metrics.getAscent() + 1;
		final Color color = defeated ? colors.getDefeatedText() : colors.getHitpointsText();
		drawShadowedText(graphics, text, textX, baseline, color, 1f);
	}

	private String ellipsizeName(String name, FontMetrics metrics, int maxWidth)
	{
		if (!name.equals(ellipsizedSource) || metrics.getFont() != ellipsizedFont || maxWidth != ellipsizedWidth)
		{
			ellipsizedName = ellipsize(name, metrics, maxWidth);
			ellipsizedSource = name;
			ellipsizedFont = metrics.getFont();
			ellipsizedWidth = maxWidth;
		}
		return ellipsizedName;
	}

	private static String ellipsize(String text, FontMetrics metrics, int maxWidth)
	{
		if (metrics.stringWidth(text) <= maxWidth)
		{
			return text;
		}
		for (int end = text.length() - 1; end > 0; end--)
		{
			String candidate = text.substring(0, end).trim() + "…";
			if (metrics.stringWidth(candidate) <= maxWidth)
			{
				return candidate;
			}
		}
		return "…";
	}

	private void drawDamageNumber(Graphics2D graphics, int right, int baseline, Color color)
	{
		final long lastDamage = damageTracker.getLastDamageDealtMillis();
		final int damage = damageTracker.getComboDamage();
		if (lastDamage == 0 || damage <= 0)
		{
			return;
		}

		final long elapsed = System.currentTimeMillis() - lastDamage;
		final long window = DamageTracker.COMBO_WINDOW.toMillis();
		if (elapsed >= window)
		{
			return;
		}

		float alpha = 1f;
		if (elapsed > window - DAMAGE_NUMBER_FADE_MILLIS)
		{
			alpha = (window - elapsed) / (float) DAMAGE_NUMBER_FADE_MILLIS;
		}

		String text = String.valueOf(damage);
		graphics.setFont(textFont);
		FontMetrics metrics = graphics.getFontMetrics();
		drawShadowedText(graphics, text, right - metrics.stringWidth(text), baseline, color, alpha);
	}

	private static void drawShadowedText(Graphics2D graphics, String text, int x, int y, Color color, float alpha)
	{
		alpha = clamp01(alpha);
		final boolean opaque = alpha >= 1f;
		graphics.setColor(opaque ? TEXT_SHADOW : withAlpha(TEXT_SHADOW, Math.round(TEXT_SHADOW.getAlpha() * alpha)));
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(opaque ? color : withAlpha(color, Math.round(255 * alpha)));
		graphics.drawString(text, x, y);
	}

	private String buildHitpointsText(BarState state)
	{
		HitpointsTextMode mode = config.hitpointsTextMode();
		if (mode == HitpointsTextMode.NONE)
		{
			return null;
		}

		String percentText = hitpointsPercent(state.ratio, state.scale) + "%";

		if (mode == HitpointsTextMode.PERCENTAGE || state.maxHealth == null || state.percentOnly)
		{
			return percentText;
		}

		int currentHealth = state.exactHealth ? state.ratio : estimateHealth(state.ratio, state.scale, state.maxHealth);
		String hpText = currentHealth + " / " + state.maxHealth;

		if (mode == HitpointsTextMode.HITPOINTS)
		{
			return hpText;
		}

		return hpText + "   " + percentText;
	}

	// Like the game's own bar, 0% and 100% are only shown when the opponent is really dead or full.
	static int hitpointsPercent(int ratio, int scale)
	{
		int percent = (int) Math.round(100.0 * ratio / scale);
		if (ratio > 0 && ratio < scale)
		{
			percent = Math.max(1, Math.min(99, percent));
		}
		return percent;
	}

	// The game sends ratio = 1 + (scale - 1) * health / maxHealth, rounded down, for health above 0.
	// This returns the middle of the range of health values that give that ratio.
	static int estimateHealth(int ratio, int healthScale, int maxHealth)
	{
		if (ratio <= 0)
		{
			return 0;
		}

		int minHealth = 1;
		int maxHealthForRatio;
		if (healthScale > 1)
		{
			if (ratio > 1)
			{
				minHealth = (maxHealth * (ratio - 1) + healthScale - 2) / (healthScale - 1);
			}
			maxHealthForRatio = (maxHealth * ratio - 1) / (healthScale - 1);
			if (maxHealthForRatio > maxHealth)
			{
				maxHealthForRatio = maxHealth;
			}
		}
		else
		{
			maxHealthForRatio = maxHealth;
		}

		return (minHealth + maxHealthForRatio + 1) / 2;
	}
}
