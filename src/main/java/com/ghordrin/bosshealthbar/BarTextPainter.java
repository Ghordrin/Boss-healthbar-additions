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
	static final int TEXT_INSET = 2;
	static final int LEVEL_GAP = 12;
	private static final int HEADER_ICON_GAP = 4;
	private static final String DAMAGE_NUMBER_SIZING = "9999";
	private static final Color TEXT_SHADOW = new Color(0, 0, 0, 200);
	private static final Color DEFENCE_ARROW = new Color(220, 40, 40);
	private static final int DEFENCE_GAP = 3;
	private static final float RUNE_ICON_SCALE = 1.3f;
	private static final int SPECIAL_ATTACK_GAP = 6;
	private static final int MAX_CACHED_ITEM_ICONS = 16;
	private static final String ELLIPSIS = "…";

	// The items around the bar, most important first.
	static final int NAME = 0;
	static final int HITPOINTS = 1;
	static final int DAMAGE_NUMBER = 2;
	static final int KILL_COUNT = 3;
	static final int PARTY_DEFENCE = 4;
	static final int SPECIAL_ATTACKS = 5;
	static final int WEAKNESS = 6;
	static final int DRAIN_CAP = 7;
	static final int ITEM_COUNT = 8;

	private final DamageTracker damageTracker;
	private final BossHealthBarConfig config;

	private final BarPosition[] positions = new BarPosition[ITEM_COUNT];
	private final boolean[] available = new boolean[ITEM_COUNT];
	private final BarLayout topRow = new BarLayout(ITEM_COUNT);
	private final BarLayout bottomRow = new BarLayout(ITEM_COUNT);
	private boolean topLarge;
	private boolean bottomLarge;
	private int topHeight;
	private int bottomHeight;

	// What layoutText measured and placed, for drawRow in the same frame.
	private FontMetrics textMetrics;
	private FontMetrics smallMetrics;
	private int topBaseline;
	private int bottomBaseline;
	private String nameText;
	private String levelText;
	private BufferedImage nameIcon;
	private String hitpointsText;
	private boolean defeated;
	private int defeatedX;
	private int previewDamage;
	private String killCountText;
	private PartyDefence.Reading defence;
	private BufferedImage scaledDefenceIcon;
	private int defenceIconWidth;
	private int arrowWidth;
	private List<SpecialAttackCounts.Reading> specialAttacks;
	private int itemIconSize;
	private String weaknessText;
	private BufferedImage weaknessIcon;
	private int weaknessIconWidth;
	private int weaknessIconHeight;
	private int weaknessIconPadding;
	private int weaknessIconAdvance;
	private String drainCapText;

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

	private int headerHeight()
	{
		return Math.round(HEADER_HEIGHT * textScale());
	}

	private int headerBaselineGap()
	{
		return Math.round(HEADER_BASELINE_GAP * textScale());
	}

	private int footerHeight()
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

	void updateRows(boolean partyDefenceAvailable, boolean specialAttacksAvailable, boolean hasWeakness,
		boolean hasDrainCap)
	{
		positions[NAME] = config.namePosition();
		positions[HITPOINTS] = config.hitpointsPosition();
		positions[DAMAGE_NUMBER] = config.damageNumberPosition();
		positions[KILL_COUNT] = config.killCountPosition();
		positions[PARTY_DEFENCE] = config.partyDefencePosition();
		positions[SPECIAL_ATTACKS] = config.specialAttackCountsPosition();
		positions[WEAKNESS] = config.weaknessPosition();
		positions[DRAIN_CAP] = config.drainCapPosition();

		itemsAvailable(config, partyDefenceAvailable, specialAttacksAvailable, hasWeakness, hasDrainCap, available);
		topLarge = rowLarge(positions, available, true);
		bottomLarge = rowLarge(positions, available, false);
		topHeight = rowHeight(positions, available, true, headerHeight(), footerHeight());
		bottomHeight = rowHeight(positions, available, false, headerHeight(), footerHeight());
	}

	// Rows are sized from the settings rather than what's showing, so they don't jump during a fight.
	@VisibleForTesting
	static void itemsAvailable(BossHealthBarConfig config, boolean partyDefenceAvailable,
		boolean specialAttacksAvailable, boolean hasWeakness, boolean hasDrainCap, boolean[] available)
	{
		available[NAME] = config.showBossName();
		available[HITPOINTS] = config.hitpointsTextMode() != HitpointsTextMode.NONE || config.showDefeatAnimation();
		available[DAMAGE_NUMBER] = config.showDamageNumber();
		available[KILL_COUNT] = config.showKillCount();
		available[PARTY_DEFENCE] = partyDefenceAvailable;
		available[SPECIAL_ATTACKS] = specialAttacksAvailable;
		available[WEAKNESS] = hasWeakness;
		available[DRAIN_CAP] = hasDrainCap;
	}

	@VisibleForTesting
	static boolean rowLarge(BarPosition[] positions, boolean[] available, boolean top)
	{
		return available[NAME] && positions[NAME].isTop() == top
			|| available[DAMAGE_NUMBER] && positions[DAMAGE_NUMBER].isTop() == top;
	}

	@VisibleForTesting
	static int rowHeight(BarPosition[] positions, boolean[] available, boolean top, int largeHeight, int smallHeight)
	{
		if (rowLarge(positions, available, top))
		{
			return largeHeight;
		}
		for (int item = 0; item < ITEM_COUNT; item++)
		{
			if (available[item] && positions[item].isTop() == top)
			{
				return smallHeight;
			}
		}
		return 0;
	}

	int topRowHeight()
	{
		return topHeight;
	}

	int bottomRowHeight()
	{
		return bottomHeight;
	}

	void layoutText(Graphics2D graphics, String name, int combatLevel, BufferedImage nameIcon, String hitpointsText,
		boolean defeated, int previewDamage, String killCountText, PartyDefence.Reading defence,
		BufferedImage defenceIcon, List<SpecialAttackCounts.Reading> specialAttacks, String weaknessText,
		BufferedImage weaknessIcon, String drainCapText, int width, int capWidth, int bottomTop)
	{
		graphics.setFont(textFont);
		textMetrics = graphics.getFontMetrics();
		graphics.setFont(smallFont);
		smallMetrics = graphics.getFontMetrics();
		final int ascent = smallMetrics.getAscent();
		final int left = capWidth + TEXT_INSET;
		final int right = width - capWidth - TEXT_INSET;

		this.nameIcon = nameIcon;
		this.hitpointsText = hitpointsText;
		this.defeated = defeated;
		this.previewDamage = previewDamage;
		this.killCountText = killCountText;
		this.defence = defence;
		this.specialAttacks = specialAttacks;
		this.weaknessText = weaknessText;
		this.weaknessIcon = weaknessIcon;
		this.drainCapText = drainCapText;

		topRow.clear();
		bottomRow.clear();

		final boolean showName = config.showBossName();
		levelText = null;
		nameText = null;
		int nameFixedWidth = 0;
		final int ellipsisWidth = textMetrics.stringWidth(ELLIPSIS);
		if (showName)
		{
			if (nameIcon != null)
			{
				nameFixedWidth = headerIconSize(textMetrics) + HEADER_ICON_GAP;
			}
			if (config.showCombatLevel() && combatLevel > 0)
			{
				levelText = "LV " + combatLevel;
				nameFixedWidth += LEVEL_GAP + smallMetrics.stringWidth(levelText);
			}
			row(NAME).addKept(NAME, positions[NAME].getSpot(), nameFixedWidth + ellipsisWidth);
		}

		if (config.showDamageNumber())
		{
			add(DAMAGE_NUMBER, textMetrics.stringWidth(DAMAGE_NUMBER_SIZING));
		}

		// While "Defeated" shows, the hitpoints keep their slot so the other items stay where they were.
		if (hitpointsText != null)
		{
			add(HITPOINTS, smallMetrics.stringWidth(hitpointsText));
		}

		if (killCountText != null)
		{
			add(KILL_COUNT, smallMetrics.stringWidth(killCountText));
		}

		scaledDefenceIcon = defence != null || drainCapText != null ? scaledDefenceIcon(defenceIcon, ascent + 1) : null;
		defenceIconWidth = scaledDefenceIcon != null ? scaledDefenceIcon.getWidth() + DEFENCE_GAP : 0;
		arrowWidth = Math.max(5, Math.round(ascent * 0.6f));
		if (defence != null)
		{
			add(PARTY_DEFENCE, defenceIconWidth + arrowWidth + DEFENCE_GAP + smallMetrics.stringWidth(defence.getText()));
		}

		// Item images have a transparent border around the rune or weapon, so draw them larger to match the skill icon.
		itemIconSize = Math.round((ascent + 1) * RUNE_ICON_SCALE);
		if (!specialAttacks.isEmpty())
		{
			add(SPECIAL_ATTACKS, specialAttacksEnd(specialAttacks, smallMetrics, 0, itemIconSize));
		}

		weaknessIconHeight = weaknessIcon != null && weaknessIcon.getHeight() > 0 ? itemIconSize : 0;
		weaknessIconWidth = weaknessIconHeight > 0
			? Math.round(weaknessIcon.getWidth() * weaknessIconHeight / (float) weaknessIcon.getHeight()) : 0;
		weaknessIconPadding = (weaknessIconWidth - Math.round(weaknessIconWidth / RUNE_ICON_SCALE)) / 2;
		weaknessIconAdvance = weaknessIconWidth > 0 ? weaknessIconWidth - weaknessIconPadding * 2 + DEFENCE_GAP : 0;
		if (weaknessText != null)
		{
			add(WEAKNESS, weaknessIconAdvance + smallMetrics.stringWidth(weaknessText));
		}

		if (drainCapText != null)
		{
			add(DRAIN_CAP, defenceIconWidth + smallMetrics.stringWidth(drainCapText));
		}

		topRow.layout(width, left, right, LEVEL_GAP);
		bottomRow.layout(width, left, right, LEVEL_GAP);

		// The name never drops out. It's laid out at its shortest first, so the items sharing its row keep
		// their space, then gets whatever room is left and is laid out again at the width it ends up with.
		if (showName)
		{
			final BarLayout nameRow = row(NAME);
			nameText = ellipsizeName(name, textMetrics, ellipsisWidth + nameRow.room(NAME));
			nameRow.setWidth(NAME, nameFixedWidth + textMetrics.stringWidth(nameText));
			nameRow.layout(width, left, right, LEVEL_GAP);
		}

		defeatedX = defeated
			? row(HITPOINTS).centredX(smallMetrics.stringWidth(DEFEATED_TEXT), HITPOINTS) : BarLayout.NOT_PLACED;

		topBaseline = topLarge ? headerHeight() - headerBaselineGap() : ascent + 1;
		bottomBaseline = bottomTop + (bottomLarge ? headerHeight() - headerBaselineGap() : ascent + 1);
	}

	void drawRow(Graphics2D graphics, boolean top, ThemeColors colors)
	{
		final int baseline = top ? topBaseline : bottomBaseline;
		final int ascent = smallMetrics.getAscent();

		if (onRow(NAME, top))
		{
			int x = x(NAME);
			if (nameIcon != null)
			{
				x = drawHeaderIcon(graphics, nameIcon, textMetrics, x, baseline);
			}
			graphics.setFont(textFont);
			drawShadowedText(graphics, nameText, x, baseline, colors.getText(), 1f);
			if (levelText != null)
			{
				graphics.setFont(smallFont);
				drawShadowedText(graphics, levelText, x + textMetrics.stringWidth(nameText) + LEVEL_GAP, baseline,
					colors.getLevelText(), 0.9f);
			}
		}

		if (onRow(DAMAGE_NUMBER, top))
		{
			drawDamageNumber(graphics, textMetrics, row(DAMAGE_NUMBER), baseline, previewDamage, colors.getText());
		}

		graphics.setFont(smallFont);
		if (defeated)
		{
			if (defeatedX != BarLayout.NOT_PLACED && positions[HITPOINTS].isTop() == top)
			{
				drawShadowedText(graphics, DEFEATED_TEXT, defeatedX, baseline, colors.getDefeatedText(), 1f);
			}
		}
		else if (onRow(HITPOINTS, top))
		{
			drawShadowedText(graphics, hitpointsText, x(HITPOINTS), baseline, colors.getHitpointsText(), 1f);
		}

		if (onRow(KILL_COUNT, top))
		{
			drawShadowedText(graphics, killCountText, x(KILL_COUNT), baseline, colors.getLevelText(), 0.9f);
		}

		if (onRow(PARTY_DEFENCE, top))
		{
			drawDefence(graphics, defence, scaledDefenceIcon, defenceIconWidth, arrowWidth, ascent, x(PARTY_DEFENCE),
				baseline, colors);
		}

		if (onRow(SPECIAL_ATTACKS, top))
		{
			drawSpecialAttacks(graphics, specialAttacks, smallMetrics, itemIconSize, x(SPECIAL_ATTACKS), baseline, colors);
		}

		if (onRow(WEAKNESS, top))
		{
			int x = x(WEAKNESS);
			if (weaknessIconWidth > 0)
			{
				drawWeaknessIcon(graphics, weaknessIcon, x - weaknessIconPadding, baseline - ascent / 2 - weaknessIconHeight / 2,
					weaknessIconWidth, weaknessIconHeight);
				x += weaknessIconAdvance;
			}
			drawShadowedText(graphics, weaknessText, x, baseline, colors.getLevelText(), 0.9f);
		}

		if (onRow(DRAIN_CAP, top))
		{
			int x = x(DRAIN_CAP);
			if (scaledDefenceIcon != null)
			{
				graphics.drawImage(scaledDefenceIcon, x, baseline - ascent / 2 - scaledDefenceIcon.getHeight() / 2, null);
				x += defenceIconWidth;
			}
			drawShadowedText(graphics, drainCapText, x, baseline, colors.getLevelText(), 0.9f);
		}
	}

	private BarLayout row(int item)
	{
		return positions[item].isTop() ? topRow : bottomRow;
	}

	private void add(int item, int width)
	{
		row(item).add(item, positions[item].getSpot(), width);
	}

	private boolean onRow(int item, boolean top)
	{
		return positions[item].isTop() == top && x(item) != BarLayout.NOT_PLACED;
	}

	private int x(int item)
	{
		return row(item).x(item);
	}

	private static int headerIconSize(FontMetrics textMetrics)
	{
		return textMetrics.getAscent() + 1;
	}

	private int drawHeaderIcon(Graphics2D graphics, BufferedImage icon, FontMetrics textMetrics, int left, int baseline)
	{
		final int ascent = textMetrics.getAscent();
		final int size = headerIconSize(textMetrics);
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

	private void drawDefence(Graphics2D graphics, PartyDefence.Reading defence, BufferedImage scaledIcon, int iconWidth,
		int arrowWidth, int ascent, int left, int baseline, ThemeColors colors)
	{
		final int arrowHeight = Math.max(3, Math.round(arrowWidth * 0.6f));
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
	}

	private void drawSpecialAttacks(Graphics2D graphics, List<SpecialAttackCounts.Reading> readings, FontMetrics metrics,
		int iconHeight, int left, int baseline, ThemeColors colors)
	{
		final int middle = baseline - metrics.getAscent() / 2;
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

	private static void drawWeaknessIcon(Graphics2D graphics, BufferedImage icon, int x, int y, int width, int height)
	{
		final Object interpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		// Drawn scaled every frame, because ItemManager's image only fills in once the item has loaded.
		graphics.drawImage(icon, x, y, width, height, null);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation != null
			? interpolation : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
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
			String candidate = text.substring(0, end).trim() + ELLIPSIS;
			if (metrics.stringWidth(candidate) <= maxWidth)
			{
				return candidate;
			}
		}
		return ELLIPSIS;
	}

	private void drawDamageNumber(Graphics2D graphics, FontMetrics metrics, BarLayout row, int baseline,
		int previewDamage, Color color)
	{
		int damage = previewDamage;
		float alpha = 1f;
		if (damage <= 0)
		{
			final long lastDamage = damageTracker.getLastDamageDealtMillis();
			damage = damageTracker.getComboDamage();
			if (lastDamage == 0 || damage <= 0)
			{
				return;
			}

			final long elapsed = System.currentTimeMillis() - lastDamage;
			final long window = DamageTracker.DISPLAY_HOLD.toMillis();
			if (elapsed >= window)
			{
				return;
			}

			if (elapsed > window - DAMAGE_NUMBER_FADE_MILLIS)
			{
				alpha = (window - elapsed) / (float) DAMAGE_NUMBER_FADE_MILLIS;
			}
		}

		final String text = String.valueOf(damage);
		graphics.setFont(textFont);
		drawShadowedText(graphics, text, damageNumberX(row.spot(DAMAGE_NUMBER), row.x(DAMAGE_NUMBER),
			row.width(DAMAGE_NUMBER), metrics.stringWidth(text)), baseline, color, alpha);
	}

	// The number sits in a slot as wide as the widest usual number, against the side its spot is on.
	@VisibleForTesting
	static int damageNumberX(BarLayout.Spot spot, int slotX, int slotWidth, int textWidth)
	{
		switch (spot)
		{
			case LEFT:
				return slotX;
			case CENTER:
				return slotX + (slotWidth - textWidth) / 2;
			default:
				return slotX + slotWidth - textWidth;
		}
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

	String hitpointsText(BarState state)
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
