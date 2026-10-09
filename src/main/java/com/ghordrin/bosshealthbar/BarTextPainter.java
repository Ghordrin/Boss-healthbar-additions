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
	private static final String EMPTY_FIGHT_TIME = "0:00";
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
	static final int FIGHT_TIMER = 3;
	static final int KILL_COUNT = 4;
	static final int PARTY_DEFENCE = 5;
	static final int MAGIC_DEFENCE = 6;
	static final int SPECIAL_ATTACKS = 7;
	static final int WEAKNESS = 8;
	static final int DRAIN_CAP = 9;
	static final int ITEM_COUNT = 10;

	private final DamageTracker damageTracker;
	private final BossHealthBarConfig config;

	private final BarPosition[] positions = new BarPosition[ITEM_COUNT];
	private final boolean[] available = new boolean[ITEM_COUNT];
	private final BarLayout topRow = new BarLayout(ITEM_COUNT);
	private final BarLayout bottomRow = new BarLayout(ITEM_COUNT);
	private final int[] rowAbove = new int[ITEM_COUNT];
	private final int[] rowBelow = new int[ITEM_COUNT];
	private int topHeight;
	private int bottomHeight;
	private int topRowBaseline;
	private int bottomRowBaseline;

	// Each font's metrics, measured with its own smoothing, which changes the glyph widths.
	private final FontMetrics[] metrics = new FontMetrics[ITEM_COUNT];
	private FontMetrics levelMetrics;

	// What layoutText placed, for drawRow in the same frame.
	private int topBaseline;
	private int bottomBaseline;
	private String nameText;
	private String levelText;
	private BufferedImage nameIcon;
	private String hitpointsText;
	private boolean defeated;
	private int defeatedX;
	private int previewDamage;
	private String fightTimeText;
	private int fightTimeWidth;
	private int fightTimeSlotWidth;
	private String sizedFightTime;
	private FontMetrics sizedFightTimeMetrics;
	private char widestDigit;
	private String killCountText;
	private PartyDefence.Reading defence;
	private BufferedImage scaledDefenceIcon;
	private int defenceIconWidth;
	private PartyDefence.Reading magicDefence;
	private BufferedImage scaledMagicIcon;
	private int magicIconWidth;
	private int arrowWidth;
	private List<SpecialAttackCounts.Reading> specialAttacks;
	private int specialAttackIconSize;
	private BufferedImage scaledDrainIcon;
	private int drainIconWidth;
	private String weaknessText;
	private BufferedImage weaknessIcon;
	private int weaknessIconWidth;
	private int weaknessIconHeight;
	private int weaknessIconPadding;
	private int weaknessIconAdvance;
	private String drainCapText;

	private final FontType[] requestedFonts = new FontType[ITEM_COUNT];
	private final ItemFont[] itemFonts = new ItemFont[ITEM_COUNT];
	private final ItemFont levelItemFont = new ItemFont();
	private final Font[] fonts = new Font[ITEM_COUNT];
	private final boolean[] pixelFonts = new boolean[ITEM_COUNT];
	private Font levelFont;
	private boolean smoothText;
	private boolean measured;

	// How far each item's text reaches above and below the shared baseline of its row.
	private final int[] itemAbove = new int[ITEM_COUNT];
	private final int[] itemBelow = new int[ITEM_COUNT];
	private int levelAbove;
	private int levelBelow;

	private String ellipsizedSource;
	private Font ellipsizedFont;
	private int ellipsizedWidth;
	private String ellipsizedName;

	private final ScaledIcon defenceIconCache = new ScaledIcon();
	private final ScaledIcon magicIconCache = new ScaledIcon();
	private final ScaledIcon drainIconCache = new ScaledIcon();

	private final Map<BufferedImage, BufferedImage> itemIcons = new IdentityHashMap<>();
	private int itemIconHeight;

	@Inject
	BarTextPainter(DamageTracker damageTracker, BossHealthBarConfig config)
	{
		this.damageTracker = damageTracker;
		this.config = config;
		for (int item = 0; item < ITEM_COUNT; item++)
		{
			itemFonts[item] = new ItemFont();
		}
		itemFonts[MAGIC_DEFENCE] = itemFonts[PARTY_DEFENCE];
	}

	void updateFonts()
	{
		final boolean smooth = config.smoothText();
		readItemFonts(config, requestedFonts);
		boolean changed = !measured || smooth != smoothText;
		for (int item = 0; item < ITEM_COUNT; item++)
		{
			if (item != MAGIC_DEFENCE)
			{
				changed |= itemFonts[item].update(requestedFonts[item], defaultFont(item));
			}
		}
		changed |= levelItemFont.update(config.combatLevelFont(), FontType.SMALL);
		if (!changed)
		{
			return;
		}

		smoothText = smooth;
		for (int item = 0; item < ITEM_COUNT; item++)
		{
			fonts[item] = itemFonts[item].font;
			pixelFonts[item] = itemFonts[item].pixel;
		}
		levelFont = levelItemFont.font;
		measureItems();
		measured = true;
	}

	@VisibleForTesting
	static void readItemFonts(BossHealthBarConfig config, FontType[] fonts)
	{
		fonts[NAME] = config.font();
		fonts[HITPOINTS] = config.hitpointsFont();
		fonts[DAMAGE_NUMBER] = config.damageNumberFont();
		fonts[FIGHT_TIMER] = config.fightTimerFont();
		fonts[KILL_COUNT] = config.killCountFont();
		fonts[PARTY_DEFENCE] = config.partyDefenceFont();
		fonts[MAGIC_DEFENCE] = fonts[PARTY_DEFENCE];
		fonts[SPECIAL_ATTACKS] = config.specialAttackCountsFont();
		fonts[WEAKNESS] = config.weaknessFont();
		fonts[DRAIN_CAP] = config.drainCapFont();
	}

	private static FontType defaultFont(int item)
	{
		switch (item)
		{
			case NAME:
				return BossHealthBarConfig.DEFAULT_FONT;
			case DAMAGE_NUMBER:
				return FontType.REGULAR;
			default:
				return FontType.SMALL;
		}
	}

	// The RuneScape fonts are bitmaps, so they're never smoothed.
	static boolean isRunescapeFont(String family)
	{
		return family.equals(FontManager.getRunescapeFont().getFamily())
			|| family.equals(FontManager.getRunescapeSmallFont().getFamily())
			|| family.equals(FontManager.getRunescapeBoldFont().getFamily());
	}

	static int clampFontSize(int size)
	{
		return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
	}

	// What the smaller text used to be with other fonts, before each item had its own font.
	static int smallTextSize(int nameSize)
	{
		return Math.max(MIN_FONT_SIZE, Math.round(clampFontSize(nameSize) * SMALL_TEXT_SCALE));
	}

	@VisibleForTesting
	static int scaledToFont(int height, int fontSize)
	{
		return Math.round(height * (fontSize / (float) REFERENCE_FONT_SIZE));
	}

	// The name and damage number get a taller row with a gap under the baseline.
	@VisibleForTesting
	static int largeAbove(int fontSize)
	{
		return scaledToFont(HEADER_HEIGHT, fontSize) - largeBelow(fontSize);
	}

	@VisibleForTesting
	static int largeBelow(int fontSize)
	{
		return scaledToFont(HEADER_BASELINE_GAP, fontSize);
	}

	// Other fonts keep the row height they had when the small text was sized from the name font: as tall as
	// the name font when it's still the name's small text, else about that.
	@VisibleForTesting
	static int smallRowHeight(int fontSize, boolean pixel, String family, String nameFamily, int nameSize)
	{
		if (pixel)
		{
			return scaledToFont(FOOTER_HEIGHT, fontSize);
		}
		if (family.equals(nameFamily) && fontSize == smallTextSize(nameSize))
		{
			return clampFontSize(nameSize);
		}
		return Math.round(fontSize / SMALL_TEXT_SCALE);
	}

	private int smallRowHeight(ItemFont font)
	{
		final ItemFont name = itemFonts[NAME];
		return smallRowHeight(font.size, font.pixel, font.family, name.family, name.size);
	}

	@VisibleForTesting
	static int smallBelow(int rowHeight, int above)
	{
		return Math.max(0, rowHeight - above);
	}

	// Measured off screen with the same hints as the overlay, which only translates, so the metrics are
	// known before the overlay lays out its frame and are reused for every frame.
	private void measureItems()
	{
		final Graphics2D scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			applyTextHints(scratch);
			for (int item = 0; item < ITEM_COUNT; item++)
			{
				setTextAntialiasing(scratch, pixelFonts[item]);
				metrics[item] = scratch.getFontMetrics(fonts[item]);
				final int size = fonts[item].getSize();
				if (item == NAME || item == DAMAGE_NUMBER)
				{
					itemAbove[item] = largeAbove(size);
					itemBelow[item] = largeBelow(size);
				}
				else
				{
					itemAbove[item] = metrics[item].getAscent() + 1;
					itemBelow[item] = smallBelow(smallRowHeight(itemFonts[item]), itemAbove[item]);
				}
			}
			setTextAntialiasing(scratch, levelItemFont.pixel);
			levelMetrics = scratch.getFontMetrics(levelFont);
			levelAbove = levelMetrics.getAscent() + 1;
			levelBelow = smallBelow(smallRowHeight(levelItemFont), levelAbove);
		}
		finally
		{
			scratch.dispose();
		}
	}

	@VisibleForTesting
	Font font(int item)
	{
		return fonts[item];
	}

	@VisibleForTesting
	Font levelFont()
	{
		return levelFont;
	}

	FontMetrics hitpointsMetrics()
	{
		return metrics[HITPOINTS];
	}

	void drawHitpointsStyleText(Graphics2D graphics, String text, int x, int baseline, Color color)
	{
		useFont(graphics, HITPOINTS);
		drawShadowedText(graphics, text, x, baseline, color, 1f);
	}

	private void setTextAntialiasing(Graphics2D graphics, boolean pixel)
	{
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, smoothText && !pixel
			? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
			: RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	}

	private void useFont(Graphics2D graphics, int item)
	{
		graphics.setFont(fonts[item]);
		setTextAntialiasing(graphics, pixelFonts[item]);
	}

	private static final class ItemFont
	{
		private String family;
		private int size;
		private boolean bold;
		private boolean italic;
		private Font font;
		private boolean pixel;

		boolean update(FontType type, FontType fallback)
		{
			final FontType setting = type != null ? type : fallback;
			final String newFamily = setting.getFamily() != null ? setting.getFamily() : fallback.getFamily();
			final int newSize = clampFontSize(setting.getSize());
			if (font != null && newFamily.equals(family) && newSize == size
				&& setting.isBold() == bold && setting.isItalic() == italic)
			{
				return false;
			}

			family = newFamily;
			size = newSize;
			bold = setting.isBold();
			italic = setting.isItalic();
			pixel = isRunescapeFont(family);
			font = FontManager.getFallbackFont(family, (bold ? Font.BOLD : Font.PLAIN) | (italic ? Font.ITALIC : Font.PLAIN), size);
			return true;
		}
	}

	void applyTextHints(Graphics2D graphics)
	{
		setTextAntialiasing(graphics, pixelFonts[NAME]);
		// Fractional metrics place glyphs between pixels, which makes smoothed text look soft.
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	}

	void updateRows(boolean partyDefenceAvailable, boolean magicDefenceAvailable, boolean specialAttacksAvailable,
		boolean hasWeakness, boolean hasDrainCap)
	{
		positions[NAME] = config.namePosition();
		positions[HITPOINTS] = config.hitpointsPosition();
		positions[DAMAGE_NUMBER] = config.damageNumberPosition();
		positions[FIGHT_TIMER] = config.fightTimerPosition();
		positions[KILL_COUNT] = config.killCountPosition();
		positions[PARTY_DEFENCE] = config.partyDefencePosition();
		positions[MAGIC_DEFENCE] = config.partyDefencePosition();
		positions[SPECIAL_ATTACKS] = config.specialAttackCountsPosition();
		positions[WEAKNESS] = config.weaknessPosition();
		positions[DRAIN_CAP] = config.drainCapPosition();

		itemsAvailable(config, partyDefenceAvailable, magicDefenceAvailable, specialAttacksAvailable, hasWeakness,
			hasDrainCap, available);
		System.arraycopy(itemAbove, 0, rowAbove, 0, ITEM_COUNT);
		System.arraycopy(itemBelow, 0, rowBelow, 0, ITEM_COUNT);
		if (config.showCombatLevel())
		{
			rowAbove[NAME] = Math.max(rowAbove[NAME], levelAbove);
			rowBelow[NAME] = Math.max(rowBelow[NAME], levelBelow);
		}
		topRowBaseline = rowBaseline(positions, available, true, rowAbove);
		bottomRowBaseline = rowBaseline(positions, available, false, rowAbove);
		topHeight = rowHeight(positions, available, true, rowAbove, rowBelow);
		bottomHeight = rowHeight(positions, available, false, rowAbove, rowBelow);
	}

	// Rows are sized from the settings rather than what's showing, so they don't jump during a fight.
	@VisibleForTesting
	static void itemsAvailable(BossHealthBarConfig config, boolean partyDefenceAvailable, boolean magicDefenceAvailable,
		boolean specialAttacksAvailable, boolean hasWeakness, boolean hasDrainCap, boolean[] available)
	{
		available[NAME] = config.showBossName();
		available[HITPOINTS] = config.hitpointsTextMode() != HitpointsTextMode.NONE || config.showDefeatAnimation();
		available[DAMAGE_NUMBER] = config.showDamageNumber();
		available[FIGHT_TIMER] = config.showFightTimer();
		available[KILL_COUNT] = config.showKillCount();
		available[PARTY_DEFENCE] = partyDefenceAvailable;
		available[MAGIC_DEFENCE] = magicDefenceAvailable;
		available[SPECIAL_ATTACKS] = specialAttacksAvailable;
		available[WEAKNESS] = hasWeakness;
		available[DRAIN_CAP] = hasDrainCap;
	}

	// Everything on a row shares one baseline, low enough for the tallest text.
	@VisibleForTesting
	static int rowBaseline(BarPosition[] positions, boolean[] available, boolean top, int[] above)
	{
		int baseline = 0;
		for (int item = 0; item < ITEM_COUNT; item++)
		{
			if (available[item] && positions[item].isTop() == top)
			{
				baseline = Math.max(baseline, above[item]);
			}
		}
		return baseline;
	}

	@VisibleForTesting
	static int rowHeight(BarPosition[] positions, boolean[] available, boolean top, int[] above, int[] below)
	{
		final int baseline = rowBaseline(positions, available, top, above);
		if (baseline == 0)
		{
			return 0;
		}
		int descent = 0;
		for (int item = 0; item < ITEM_COUNT; item++)
		{
			if (available[item] && positions[item].isTop() == top)
			{
				descent = Math.max(descent, below[item]);
			}
		}
		return baseline + descent;
	}

	@VisibleForTesting
	int topRowBaseline()
	{
		return topRowBaseline;
	}

	@VisibleForTesting
	int bottomRowBaseline()
	{
		return bottomRowBaseline;
	}

	int topRowHeight()
	{
		return topHeight;
	}

	int bottomRowHeight()
	{
		return bottomHeight;
	}

	void layoutText(String name, int combatLevel, BufferedImage nameIcon, String hitpointsText,
		boolean defeated, int previewDamage, boolean showFightTime, String fightTimeText, String killCountText,
		PartyDefence.Reading defence,
		BufferedImage defenceIcon, PartyDefence.Reading magicDefence, BufferedImage magicIcon,
		List<SpecialAttackCounts.Reading> specialAttacks, String weaknessText,
		BufferedImage weaknessIcon, String drainCapText, int width, int capWidth, int bottomTop)
	{
		final FontMetrics textMetrics = metrics[NAME];
		final int left = capWidth + TEXT_INSET;
		final int right = width - capWidth - TEXT_INSET;

		this.nameIcon = nameIcon;
		this.hitpointsText = hitpointsText;
		this.defeated = defeated;
		this.previewDamage = previewDamage;
		this.fightTimeText = fightTimeText;
		this.killCountText = killCountText;
		this.defence = defence;
		this.magicDefence = magicDefence;
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
				nameFixedWidth += LEVEL_GAP + levelMetrics.stringWidth(levelText);
			}
			row(NAME).addKept(NAME, positions[NAME].getSpot(), nameFixedWidth + ellipsisWidth);
		}

		if (config.showDamageNumber())
		{
			add(DAMAGE_NUMBER, metrics[DAMAGE_NUMBER].stringWidth(DAMAGE_NUMBER_SIZING));
		}

		// While "Defeated" shows, the hitpoints keep their slot so the other items stay where they were.
		if (hitpointsText != null)
		{
			add(HITPOINTS, metrics[HITPOINTS].stringWidth(hitpointsText));
		}

		// The slot is kept before the first hit too, so the name doesn't get shorter when the time appears.
		if (showFightTime)
		{
			sizeFightTime(fightTimeText != null ? fightTimeText : EMPTY_FIGHT_TIME, metrics[FIGHT_TIMER]);
			add(FIGHT_TIMER, fightTimeSlotWidth);
		}

		if (killCountText != null)
		{
			add(KILL_COUNT, metrics[KILL_COUNT].stringWidth(killCountText));
		}

		final int defenceIconSize = metrics[PARTY_DEFENCE].getAscent() + 1;
		scaledDefenceIcon = defence != null ? defenceIconCache.get(defenceIcon, defenceIconSize) : null;
		defenceIconWidth = scaledDefenceIcon != null ? scaledDefenceIcon.getWidth() + DEFENCE_GAP : 0;
		arrowWidth = Math.max(5, Math.round(metrics[PARTY_DEFENCE].getAscent() * 0.6f));
		if (defence != null)
		{
			add(PARTY_DEFENCE, defenceIconWidth + arrowWidth + DEFENCE_GAP
				+ metrics[PARTY_DEFENCE].stringWidth(defence.getText()));
		}

		scaledMagicIcon = magicDefence != null ? magicIconCache.get(magicIcon, defenceIconSize) : null;
		magicIconWidth = scaledMagicIcon != null ? scaledMagicIcon.getWidth() + DEFENCE_GAP : 0;
		if (magicDefence != null)
		{
			add(MAGIC_DEFENCE, magicIconWidth + arrowWidth + DEFENCE_GAP
				+ metrics[MAGIC_DEFENCE].stringWidth(magicDefence.getText()));
		}

		specialAttackIconSize = itemIconSize(metrics[SPECIAL_ATTACKS]);
		if (!specialAttacks.isEmpty())
		{
			add(SPECIAL_ATTACKS, specialAttacksEnd(specialAttacks, metrics[SPECIAL_ATTACKS], 0, specialAttackIconSize));
		}

		weaknessIconHeight = weaknessIcon != null && weaknessIcon.getHeight() > 0 ? itemIconSize(metrics[WEAKNESS]) : 0;
		weaknessIconWidth = weaknessIconHeight > 0
			? Math.round(weaknessIcon.getWidth() * weaknessIconHeight / (float) weaknessIcon.getHeight()) : 0;
		weaknessIconPadding = (weaknessIconWidth - Math.round(weaknessIconWidth / RUNE_ICON_SCALE)) / 2;
		weaknessIconAdvance = weaknessIconWidth > 0 ? weaknessIconWidth - weaknessIconPadding * 2 + DEFENCE_GAP : 0;
		if (weaknessText != null)
		{
			add(WEAKNESS, weaknessIconAdvance + metrics[WEAKNESS].stringWidth(weaknessText));
		}

		// The drain limit shows the Defence icon too, at its own size.
		scaledDrainIcon = drainCapText != null ? drainIconCache.get(defenceIcon, metrics[DRAIN_CAP].getAscent() + 1) : null;
		drainIconWidth = scaledDrainIcon != null ? scaledDrainIcon.getWidth() + DEFENCE_GAP : 0;
		if (drainCapText != null)
		{
			add(DRAIN_CAP, drainIconWidth + metrics[DRAIN_CAP].stringWidth(drainCapText));
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
			? row(HITPOINTS).centredX(metrics[HITPOINTS].stringWidth(DEFEATED_TEXT), HITPOINTS) : BarLayout.NOT_PLACED;

		topBaseline = topRowBaseline;
		bottomBaseline = bottomTop + bottomRowBaseline;
	}

	// Item images have a transparent border around the rune or weapon, so draw them larger to match the skill icon.
	@VisibleForTesting
	static int itemIconSize(FontMetrics metrics)
	{
		return Math.round((metrics.getAscent() + 1) * RUNE_ICON_SCALE);
	}

	void drawRow(Graphics2D graphics, boolean top, ThemeColors colors)
	{
		final int baseline = top ? topBaseline : bottomBaseline;

		if (onRow(NAME, top))
		{
			final FontMetrics textMetrics = metrics[NAME];
			int x = x(NAME);
			if (nameIcon != null)
			{
				x = drawHeaderIcon(graphics, nameIcon, textMetrics, x, baseline);
			}
			useFont(graphics, NAME);
			drawShadowedText(graphics, nameText, x, baseline, colors.getText(), 1f);
			if (levelText != null)
			{
				graphics.setFont(levelFont);
				setTextAntialiasing(graphics, levelItemFont.pixel);
				drawShadowedText(graphics, levelText, x + textMetrics.stringWidth(nameText) + LEVEL_GAP, baseline,
					colors.getLevelText(), 0.9f);
			}
		}

		if (onRow(DAMAGE_NUMBER, top))
		{
			drawDamageNumber(graphics, metrics[DAMAGE_NUMBER], row(DAMAGE_NUMBER), baseline, previewDamage, colors.getText());
		}

		useFont(graphics, HITPOINTS);
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

		if (fightTimeText != null && onRow(FIGHT_TIMER, top))
		{
			useFont(graphics, FIGHT_TIMER);
			final BarLayout row = row(FIGHT_TIMER);
			drawShadowedText(graphics, fightTimeText, damageNumberX(row.spot(FIGHT_TIMER), row.x(FIGHT_TIMER),
				fightTimeSlotWidth, fightTimeWidth), baseline, colors.getLevelText(), 0.9f);
		}

		if (onRow(KILL_COUNT, top))
		{
			useFont(graphics, KILL_COUNT);
			drawShadowedText(graphics, killCountText, x(KILL_COUNT), baseline, colors.getLevelText(), 0.9f);
		}

		final int defenceAscent = metrics[PARTY_DEFENCE].getAscent();
		if (onRow(PARTY_DEFENCE, top))
		{
			useFont(graphics, PARTY_DEFENCE);
			drawDefence(graphics, defence, scaledDefenceIcon, defenceIconWidth, arrowWidth, defenceAscent,
				x(PARTY_DEFENCE), baseline, colors);
		}

		if (onRow(MAGIC_DEFENCE, top))
		{
			useFont(graphics, MAGIC_DEFENCE);
			drawDefence(graphics, magicDefence, scaledMagicIcon, magicIconWidth, arrowWidth, defenceAscent,
				x(MAGIC_DEFENCE), baseline, colors);
		}

		if (onRow(SPECIAL_ATTACKS, top))
		{
			useFont(graphics, SPECIAL_ATTACKS);
			drawSpecialAttacks(graphics, specialAttacks, metrics[SPECIAL_ATTACKS], specialAttackIconSize,
				x(SPECIAL_ATTACKS), baseline, colors);
		}

		if (onRow(WEAKNESS, top))
		{
			int x = x(WEAKNESS);
			if (weaknessIconWidth > 0)
			{
				drawWeaknessIcon(graphics, weaknessIcon, x - weaknessIconPadding,
					baseline - metrics[WEAKNESS].getAscent() / 2 - weaknessIconHeight / 2, weaknessIconWidth, weaknessIconHeight);
				x += weaknessIconAdvance;
			}
			useFont(graphics, WEAKNESS);
			drawShadowedText(graphics, weaknessText, x, baseline, colors.getLevelText(), 0.9f);
		}

		if (onRow(DRAIN_CAP, top))
		{
			int x = x(DRAIN_CAP);
			if (scaledDrainIcon != null)
			{
				graphics.drawImage(scaledDrainIcon, x,
					baseline - metrics[DRAIN_CAP].getAscent() / 2 - scaledDrainIcon.getHeight() / 2, null);
				x += drainIconWidth;
			}
			useFont(graphics, DRAIN_CAP);
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

	private static final class ScaledIcon
	{
		private BufferedImage source;
		private int height;
		private BufferedImage scaled;

		BufferedImage get(BufferedImage icon, int height)
		{
			if (icon == null || icon.getWidth() <= 0 || icon.getHeight() <= 0)
			{
				return null;
			}
			if (icon != source || height != this.height)
			{
				final int width = Math.max(1, Math.round(icon.getWidth() * height / (float) icon.getHeight()));
				scaled = icon.getHeight() == height ? icon : ImageUtil.resizeImage(icon, width, height);
				source = icon;
				this.height = height;
			}
			return scaled;
		}
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
		useFont(graphics, DAMAGE_NUMBER);
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

	// The slot is sized as if every digit were the widest one, so the time doesn't shift as its digits change.
	private void sizeFightTime(String text, FontMetrics metrics)
	{
		if (metrics != sizedFightTimeMetrics)
		{
			widestDigit = widestDigit(metrics);
		}
		else if (text.equals(sizedFightTime))
		{
			return;
		}
		sizedFightTime = text;
		sizedFightTimeMetrics = metrics;
		fightTimeWidth = metrics.stringWidth(text);
		fightTimeSlotWidth = metrics.stringWidth(withDigits(text, widestDigit));
	}

	private static char widestDigit(FontMetrics metrics)
	{
		char widest = '0';
		for (char digit = '1'; digit <= '9'; digit++)
		{
			if (metrics.charWidth(digit) > metrics.charWidth(widest))
			{
				widest = digit;
			}
		}
		return widest;
	}

	@VisibleForTesting
	static String withDigits(String text, char digit)
	{
		final char[] chars = text.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			if (chars[i] >= '0' && chars[i] <= '9')
			{
				chars[i] = digit;
			}
		}
		return new String(chars);
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
