package com.ghordrin.bosshealthbar;

import com.google.common.annotations.VisibleForTesting;
import static com.ghordrin.bosshealthbar.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.ColorUtil.withAlpha;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.client.config.FontType;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

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
	private static final int HEADER_ICON_GAP = 4;
	private static final String DAMAGE_NUMBER_SIZING = "9999";
	private static final Color TEXT_SHADOW = new Color(0, 0, 0, 200);
	private static final Color DEFENCE_ARROW = new Color(220, 40, 40);
	private static final int DEFENCE_GAP = 3;
	private static final float RUNE_ICON_SCALE = 1.3f;
	private static final int NO_ROOM = -1;
	private static final int SPECIAL_ATTACK_GAP = 6;
	private static final int MAX_CACHED_ITEM_ICONS = 16;

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

	private BufferedImage defenceIconSource;
	private int defenceIconHeight;
	private BufferedImage defenceIconScaled;

	private final Map<BufferedImage, BufferedImage> itemIcons = new IdentityHashMap<>();
	private int itemIconHeight;

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

	void drawHeader(Graphics2D graphics, String name, int combatLevel, BufferedImage icon, int width, int capWidth,
		int baseline, ThemeColors colors)
	{
		int left = capWidth + TEXT_INSET;
		final int right = width - capWidth - TEXT_INSET;

		if (config.showBossName())
		{
			if (icon != null)
			{
				left = drawHeaderIcon(graphics, icon, left, baseline);
			}

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

	private int drawHeaderIcon(Graphics2D graphics, BufferedImage icon, int left, int baseline)
	{
		graphics.setFont(textFont);
		final int ascent = graphics.getFontMetrics().getAscent();
		final int size = ascent + 1;
		// Centred on the capital letters, which take up roughly the top 70% of the ascent.
		final int top = baseline - Math.round(ascent * 0.35f) - size / 2;
		final Object interpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		// Drawn scaled every frame, because ItemManager's image only fills in once the item has loaded.
		graphics.drawImage(icon, left, top, size, size, null);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation != null
			? interpolation : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		return left + size + HEADER_ICON_GAP;
	}

	String footerText(BarState state, boolean defeated)
	{
		return defeated ? DEFEATED_TEXT : buildHitpointsText(state);
	}

	void drawFooter(Graphics2D graphics, String text, String killCountText, PartyDefence.Reading defence,
		BufferedImage defenceIcon, List<SpecialAttackCounts.Reading> specialAttacks, String weaknessText,
		BufferedImage weaknessIcon, String drainCapText, boolean defeated, int width, int capWidth, int top,
		ThemeColors colors)
	{
		graphics.setFont(smallFont);
		final FontMetrics metrics = graphics.getFontMetrics();
		final int baseline = top + metrics.getAscent() + 1;
		int textX = width - capWidth - TEXT_INSET;
		if (text != null)
		{
			textX = defeated
				? (width - metrics.stringWidth(text)) / 2
				: width - capWidth - TEXT_INSET - metrics.stringWidth(text);
			final Color color = defeated ? colors.getDefeatedText() : colors.getHitpointsText();
			drawShadowedText(graphics, text, textX, baseline, color, 1f);
		}

		// Items end at the bar's edge, or a gap before the hitpoints text.
		final int limit = text != null ? textX - LEVEL_GAP : textX;
		// Once an item doesn't fit, the rest are left out too, so none of them moves into an earlier one's spot.
		int left = capWidth + TEXT_INSET;
		if (killCountText != null)
		{
			final int killCountWidth = metrics.stringWidth(killCountText);
			if (left + killCountWidth <= limit)
			{
				drawShadowedText(graphics, killCountText, left, baseline, colors.getLevelText(), 0.9f);
				left += killCountWidth + LEVEL_GAP;
			}
			else
			{
				left = NO_ROOM;
			}
		}

		if (defence != null && left != NO_ROOM)
		{
			left = drawDefence(graphics, defence, defenceIcon, metrics, left, limit, baseline, colors);
		}

		if (!specialAttacks.isEmpty() && left != NO_ROOM)
		{
			left = drawSpecialAttacks(graphics, specialAttacks, metrics, left, limit, baseline, colors);
		}

		if (weaknessText != null && left != NO_ROOM)
		{
			left = drawWeakness(graphics, weaknessText, weaknessIcon, metrics, left, limit, baseline, colors);
		}

		if (drainCapText != null && left != NO_ROOM)
		{
			drawDrainCap(graphics, drainCapText, defenceIcon, metrics, left, limit, baseline, colors);
		}
	}

	private int drawDefence(Graphics2D graphics, PartyDefence.Reading defence, BufferedImage icon, FontMetrics metrics,
		int left, int limit, int baseline, ThemeColors colors)
	{
		final int ascent = metrics.getAscent();
		final BufferedImage scaledIcon = scaledDefenceIcon(icon, ascent + 1);
		final int iconWidth = scaledIcon != null ? scaledIcon.getWidth() + DEFENCE_GAP : 0;
		final int arrowWidth = Math.max(5, Math.round(ascent * 0.6f));
		final int arrowHeight = Math.max(3, Math.round(arrowWidth * 0.6f));
		final int textWidth = metrics.stringWidth(defence.getText());
		final int end = left + iconWidth + arrowWidth + DEFENCE_GAP + textWidth;
		if (end > limit)
		{
			return NO_ROOM;
		}

		final int middle = baseline - ascent / 2;
		int x = left;
		if (scaledIcon != null)
		{
			graphics.drawImage(scaledIcon, x, middle - scaledIcon.getHeight() / 2, null);
			x += iconWidth;
		}

		final int arrowTop = middle - arrowHeight / 2;
		final int[] xs = {x, x + arrowWidth, x + arrowWidth / 2};
		final int[] ys = {arrowTop, arrowTop, arrowTop + arrowHeight};
		final Object antialiasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.translate(1, 1);
		graphics.setColor(TEXT_SHADOW);
		graphics.fillPolygon(xs, ys, 3);
		graphics.translate(-1, -1);
		graphics.setColor(DEFENCE_ARROW);
		graphics.fillPolygon(xs, ys, 3);
		if (antialiasing != null)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
		}
		x += arrowWidth + DEFENCE_GAP;

		final Color color = defence.getColor() != null ? defence.getColor() : colors.getText();
		drawShadowedText(graphics, defence.getText(), x, baseline, color, 1f);
		return end + LEVEL_GAP;
	}

	private int drawSpecialAttacks(Graphics2D graphics, List<SpecialAttackCounts.Reading> readings, FontMetrics metrics,
		int left, int limit, int baseline, ThemeColors colors)
	{
		final int ascent = metrics.getAscent();
		final int middle = baseline - ascent / 2;
		// Weapon images have a transparent border too, so they're sized like the rune.
		final int iconHeight = Math.round((ascent + 1) * RUNE_ICON_SCALE);

		// The weapons are shown all together or not at all.
		final int end = specialAttacksEnd(readings, metrics, left, iconHeight);
		if (end > limit)
		{
			return NO_ROOM;
		}

		int x = left;
		for (SpecialAttackCounts.Reading reading : readings)
		{
			final BufferedImage icon = reading.getImage();
			final int iconWidth = itemIconWidth(icon, iconHeight);
			final int iconPadding = itemIconPadding(iconWidth);
			final int iconAdvance = iconAdvance(icon, iconHeight);
			final int itemEnd = x + iconAdvance + metrics.stringWidth(reading.getText());

			if (iconWidth > 0)
			{
				final BufferedImage scaled = scaledItemIcon(icon, iconWidth, iconHeight);
				if (scaled != null)
				{
					graphics.drawImage(scaled, x - iconPadding, middle - iconHeight / 2, null);
				}
				x += iconAdvance;
			}

			final Color color = reading.getColor() != null ? reading.getColor() : colors.getText();
			drawShadowedText(graphics, reading.getText(), x, baseline, color, 1f);
			x = itemEnd + SPECIAL_ATTACK_GAP;
		}
		return end + LEVEL_GAP;
	}

	@VisibleForTesting
	static int specialAttacksEnd(List<SpecialAttackCounts.Reading> readings, FontMetrics metrics, int left, int iconHeight)
	{
		int end = left - SPECIAL_ATTACK_GAP;
		for (SpecialAttackCounts.Reading reading : readings)
		{
			end += SPECIAL_ATTACK_GAP + iconAdvance(reading.getImage(), iconHeight) + metrics.stringWidth(reading.getText());
		}
		return end;
	}

	private static int itemIconWidth(BufferedImage icon, int iconHeight)
	{
		return icon != null && icon.getWidth() > 0 && icon.getHeight() > 0
			? Math.max(1, Math.round(icon.getWidth() * iconHeight / (float) icon.getHeight())) : 0;
	}

	private static int itemIconPadding(int iconWidth)
	{
		return (iconWidth - Math.round(iconWidth / RUNE_ICON_SCALE)) / 2;
	}

	private static int iconAdvance(BufferedImage icon, int iconHeight)
	{
		final int iconWidth = itemIconWidth(icon, iconHeight);
		return iconWidth > 0 ? iconWidth - itemIconPadding(iconWidth) * 2 + DEFENCE_GAP : 0;
	}

	private BufferedImage scaledItemIcon(BufferedImage icon, int width, int height)
	{
		if (height != itemIconHeight || itemIcons.size() > MAX_CACHED_ITEM_ICONS)
		{
			itemIcons.clear();
			itemIconHeight = height;
		}

		BufferedImage scaled = itemIcons.get(icon);
		// ItemManager hands out a blank image that fills in once the item has loaded, so that one isn't cached yet.
		if (scaled == null && hasVisiblePixel(icon))
		{
			scaled = ImageUtil.resizeImage(icon, width, height);
			itemIcons.put(icon, scaled);
		}
		return scaled;
	}

	private static boolean hasVisiblePixel(BufferedImage image)
	{
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
				{
					return true;
				}
			}
		}
		return false;
	}

	private int drawWeakness(Graphics2D graphics, String text, BufferedImage icon, FontMetrics metrics,
		int left, int limit, int baseline, ThemeColors colors)
	{
		final int ascent = metrics.getAscent();
		// Item images have a transparent border around the rune, so draw them larger to match the skill icon.
		final int iconHeight = icon != null && icon.getHeight() > 0 ? Math.round((ascent + 1) * RUNE_ICON_SCALE) : 0;
		final int iconWidth = iconHeight > 0 ? Math.round(icon.getWidth() * iconHeight / (float) icon.getHeight()) : 0;
		final int iconPadding = (iconWidth - Math.round(iconWidth / RUNE_ICON_SCALE)) / 2;
		final int iconAdvance = iconWidth > 0 ? iconWidth - iconPadding * 2 + DEFENCE_GAP : 0;
		final int end = left + iconAdvance + metrics.stringWidth(text);
		if (end > limit)
		{
			return NO_ROOM;
		}

		int x = left;
		if (iconWidth > 0)
		{
			final int middle = baseline - ascent / 2;
			final Object interpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			// Drawn scaled every frame, because ItemManager's image only fills in once the item has loaded.
			graphics.drawImage(icon, x - iconPadding, middle - iconHeight / 2, iconWidth, iconHeight, null);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation != null
				? interpolation : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			x += iconAdvance;
		}

		drawShadowedText(graphics, text, x, baseline, colors.getLevelText(), 0.9f);
		return end + LEVEL_GAP;
	}

	private void drawDrainCap(Graphics2D graphics, String text, BufferedImage icon, FontMetrics metrics,
		int left, int limit, int baseline, ThemeColors colors)
	{
		final int ascent = metrics.getAscent();
		final BufferedImage scaledIcon = scaledDefenceIcon(icon, ascent + 1);
		final int iconWidth = scaledIcon != null ? scaledIcon.getWidth() + DEFENCE_GAP : 0;
		if (left + iconWidth + metrics.stringWidth(text) > limit)
		{
			return;
		}

		int x = left;
		if (scaledIcon != null)
		{
			graphics.drawImage(scaledIcon, x, baseline - ascent / 2 - scaledIcon.getHeight() / 2, null);
			x += iconWidth;
		}

		drawShadowedText(graphics, text, x, baseline, colors.getLevelText(), 0.9f);
	}

	private BufferedImage scaledDefenceIcon(BufferedImage icon, int height)
	{
		if (icon == null || icon.getWidth() <= 0 || icon.getHeight() <= 0)
		{
			return null;
		}
		if (icon != defenceIconSource || height != defenceIconHeight)
		{
			final int width = Math.max(1, Math.round(icon.getWidth() * height / (float) icon.getHeight()));
			defenceIconScaled = icon.getHeight() == height ? icon : ImageUtil.resizeImage(icon, width, height);
			defenceIconSource = icon;
			defenceIconHeight = height;
		}
		return defenceIconScaled;
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
