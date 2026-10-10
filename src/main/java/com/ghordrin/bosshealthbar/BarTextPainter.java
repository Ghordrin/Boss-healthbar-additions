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
import net.runelite.client.util.ImageUtil;

class BarTextPainter
{
	private static final String DEFEATED_TEXT = "Defeated";
	private static final long DAMAGE_NUMBER_FADE_MILLIS = 400;
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
	private final BarFonts fonts;

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

	private final EllipsizedText ellipsizedName = new EllipsizedText();
	private final EllipsizedText partnerName = new EllipsizedText();

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
		this.fonts = new BarFonts(config);
	}

	void updateFonts()
	{
		fonts.updateFonts();
	}

	@VisibleForTesting
	BarFonts fonts()
	{
		return fonts;
	}

	FontMetrics hitpointsMetrics()
	{
		return fonts.metrics(HITPOINTS);
	}

	void drawHitpointsStyleText(Graphics2D graphics, String text, int x, int baseline, Color color)
	{
		fonts.useFont(graphics, HITPOINTS);
		drawShadowedText(graphics, text, x, baseline, color, 1f);
	}

	void applyTextHints(Graphics2D graphics)
	{
		fonts.applyTextHints(graphics);
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
		for (int item = 0; item < ITEM_COUNT; item++)
		{
			rowAbove[item] = fonts.above(item);
			rowBelow[item] = fonts.below(item);
		}
		if (config.showCombatLevel())
		{
			rowAbove[NAME] = Math.max(rowAbove[NAME], fonts.levelAbove());
			rowBelow[NAME] = Math.max(rowBelow[NAME], fonts.levelBelow());
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

	// The partner bar's text row, in the Hitpoints font. Sized from the settings so it doesn't jump.
	int partnerRowHeight()
	{
		return partnerRowHeight(available[NAME], available[HITPOINTS], fonts.above(HITPOINTS), fonts.below(HITPOINTS));
	}

	@VisibleForTesting
	static int partnerRowHeight(boolean showName, boolean showHitpoints, int above, int below)
	{
		return showName || showHitpoints ? above + below : 0;
	}

	// Name on the left, hitpoints or "Defeated" on the right, between the bar's caps.
	void drawPartnerRow(Graphics2D graphics, String name, String hitpointsText, boolean defeated, ThemeColors colors,
		int width, int capWidth, int top)
	{
		final FontMetrics fontMetrics = fonts.metrics(HITPOINTS);
		final int left = capWidth + TEXT_INSET;
		final int right = width - capWidth - TEXT_INSET;
		final int baseline = top + fonts.above(HITPOINTS);
		fonts.useFont(graphics, HITPOINTS);

		final String rightText = defeated ? (available[HITPOINTS] ? DEFEATED_TEXT : null) : hitpointsText;
		int nameRight = right;
		if (rightText != null)
		{
			final int textWidth = fontMetrics.stringWidth(rightText);
			drawShadowedText(graphics, rightText, right - textWidth, baseline,
				defeated ? colors.getDefeatedText() : colors.getHitpointsText(), 1f);
			nameRight -= textWidth + LEVEL_GAP;
		}

		if (available[NAME] && name != null && nameRight > left)
		{
			drawShadowedText(graphics, partnerName.get(name, fontMetrics, nameRight - left), left, baseline,
				colors.getText(), 1f);
		}
	}

	void layoutText(String name, int combatLevel, BufferedImage nameIcon, String hitpointsText,
		boolean defeated, int previewDamage, boolean showFightTime, String fightTimeText, String killCountText,
		PartyDefence.Reading defence,
		BufferedImage defenceIcon, PartyDefence.Reading magicDefence, BufferedImage magicIcon,
		List<SpecialAttackCounts.Reading> specialAttacks, String weaknessText,
		BufferedImage weaknessIcon, String drainCapText, int width, int capWidth, int bottomTop)
	{
		final FontMetrics textMetrics = fonts.metrics(NAME);
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
				nameFixedWidth += LEVEL_GAP + fonts.levelMetrics().stringWidth(levelText);
			}
			row(NAME).addKept(NAME, positions[NAME].getSpot(), nameFixedWidth + ellipsisWidth);
		}

		if (config.showDamageNumber())
		{
			add(DAMAGE_NUMBER, fonts.metrics(DAMAGE_NUMBER).stringWidth(DAMAGE_NUMBER_SIZING));
		}

		// While "Defeated" shows, the hitpoints keep their slot so the other items stay where they were.
		if (hitpointsText != null)
		{
			add(HITPOINTS, fonts.metrics(HITPOINTS).stringWidth(hitpointsText));
		}

		// The slot is kept before the first hit too, so the name doesn't get shorter when the time appears.
		if (showFightTime)
		{
			sizeFightTime(fightTimeText != null ? fightTimeText : EMPTY_FIGHT_TIME, fonts.metrics(FIGHT_TIMER));
			add(FIGHT_TIMER, fightTimeSlotWidth);
		}

		if (killCountText != null)
		{
			add(KILL_COUNT, fonts.metrics(KILL_COUNT).stringWidth(killCountText));
		}

		final int defenceIconSize = fonts.metrics(PARTY_DEFENCE).getAscent() + 1;
		scaledDefenceIcon = defence != null ? defenceIconCache.get(defenceIcon, defenceIconSize) : null;
		defenceIconWidth = scaledDefenceIcon != null ? scaledDefenceIcon.getWidth() + DEFENCE_GAP : 0;
		arrowWidth = Math.max(5, Math.round(fonts.metrics(PARTY_DEFENCE).getAscent() * 0.6f));
		if (defence != null)
		{
			add(PARTY_DEFENCE, defenceIconWidth + arrowWidth + DEFENCE_GAP
				+ fonts.metrics(PARTY_DEFENCE).stringWidth(defence.getText()));
		}

		scaledMagicIcon = magicDefence != null ? magicIconCache.get(magicIcon, defenceIconSize) : null;
		magicIconWidth = scaledMagicIcon != null ? scaledMagicIcon.getWidth() + DEFENCE_GAP : 0;
		if (magicDefence != null)
		{
			add(MAGIC_DEFENCE, magicIconWidth + arrowWidth + DEFENCE_GAP
				+ fonts.metrics(MAGIC_DEFENCE).stringWidth(magicDefence.getText()));
		}

		specialAttackIconSize = itemIconSize(fonts.metrics(SPECIAL_ATTACKS));
		if (!specialAttacks.isEmpty())
		{
			add(SPECIAL_ATTACKS, specialAttacksEnd(specialAttacks, fonts.metrics(SPECIAL_ATTACKS), 0, specialAttackIconSize));
		}

		weaknessIconHeight = weaknessIcon != null && weaknessIcon.getHeight() > 0 ? itemIconSize(fonts.metrics(WEAKNESS)) : 0;
		weaknessIconWidth = weaknessIconHeight > 0
			? Math.round(weaknessIcon.getWidth() * weaknessIconHeight / (float) weaknessIcon.getHeight()) : 0;
		weaknessIconPadding = (weaknessIconWidth - Math.round(weaknessIconWidth / RUNE_ICON_SCALE)) / 2;
		weaknessIconAdvance = weaknessIconWidth > 0 ? weaknessIconWidth - weaknessIconPadding * 2 + DEFENCE_GAP : 0;
		if (weaknessText != null)
		{
			add(WEAKNESS, weaknessIconAdvance + fonts.metrics(WEAKNESS).stringWidth(weaknessText));
		}

		// The drain limit shows the Defence icon too, at its own size.
		scaledDrainIcon = drainCapText != null ? drainIconCache.get(defenceIcon, fonts.metrics(DRAIN_CAP).getAscent() + 1) : null;
		drainIconWidth = scaledDrainIcon != null ? scaledDrainIcon.getWidth() + DEFENCE_GAP : 0;
		if (drainCapText != null)
		{
			add(DRAIN_CAP, drainIconWidth + fonts.metrics(DRAIN_CAP).stringWidth(drainCapText));
		}

		topRow.layout(width, left, right, LEVEL_GAP);
		bottomRow.layout(width, left, right, LEVEL_GAP);

		// The name never drops out. It's laid out at its shortest first, so the items sharing its row keep
		// their space, then gets whatever room is left and is laid out again at the width it ends up with.
		if (showName)
		{
			final BarLayout nameRow = row(NAME);
			nameText = ellipsizedName.get(name, textMetrics, ellipsisWidth + nameRow.room(NAME));
			nameRow.setWidth(NAME, nameFixedWidth + textMetrics.stringWidth(nameText));
			nameRow.layout(width, left, right, LEVEL_GAP);
		}

		defeatedX = defeated
			? row(HITPOINTS).centredX(fonts.metrics(HITPOINTS).stringWidth(DEFEATED_TEXT), HITPOINTS) : BarLayout.NOT_PLACED;

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
			final FontMetrics textMetrics = fonts.metrics(NAME);
			int x = x(NAME);
			if (nameIcon != null)
			{
				x = drawHeaderIcon(graphics, nameIcon, textMetrics, x, baseline);
			}
			fonts.useFont(graphics, NAME);
			drawShadowedText(graphics, nameText, x, baseline, colors.getText(), 1f);
			if (levelText != null)
			{
				fonts.useLevelFont(graphics);
				drawShadowedText(graphics, levelText, x + textMetrics.stringWidth(nameText) + LEVEL_GAP, baseline,
					colors.getLevelText(), 0.9f);
			}
		}

		if (onRow(DAMAGE_NUMBER, top))
		{
			drawDamageNumber(graphics, fonts.metrics(DAMAGE_NUMBER), row(DAMAGE_NUMBER), baseline, previewDamage, colors.getText());
		}

		fonts.useFont(graphics, HITPOINTS);
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
			fonts.useFont(graphics, FIGHT_TIMER);
			final BarLayout row = row(FIGHT_TIMER);
			drawShadowedText(graphics, fightTimeText, damageNumberX(row.spot(FIGHT_TIMER), row.x(FIGHT_TIMER),
				fightTimeSlotWidth, fightTimeWidth), baseline, colors.getLevelText(), 0.9f);
		}

		if (onRow(KILL_COUNT, top))
		{
			fonts.useFont(graphics, KILL_COUNT);
			drawShadowedText(graphics, killCountText, x(KILL_COUNT), baseline, colors.getLevelText(), 0.9f);
		}

		final int defenceAscent = fonts.metrics(PARTY_DEFENCE).getAscent();
		if (onRow(PARTY_DEFENCE, top))
		{
			fonts.useFont(graphics, PARTY_DEFENCE);
			drawDefence(graphics, defence, scaledDefenceIcon, defenceIconWidth, arrowWidth, defenceAscent,
				x(PARTY_DEFENCE), baseline, colors);
		}

		if (onRow(MAGIC_DEFENCE, top))
		{
			fonts.useFont(graphics, MAGIC_DEFENCE);
			drawDefence(graphics, magicDefence, scaledMagicIcon, magicIconWidth, arrowWidth, defenceAscent,
				x(MAGIC_DEFENCE), baseline, colors);
		}

		if (onRow(SPECIAL_ATTACKS, top))
		{
			fonts.useFont(graphics, SPECIAL_ATTACKS);
			drawSpecialAttacks(graphics, specialAttacks, fonts.metrics(SPECIAL_ATTACKS), specialAttackIconSize,
				x(SPECIAL_ATTACKS), baseline, colors);
		}

		if (onRow(WEAKNESS, top))
		{
			int x = x(WEAKNESS);
			if (weaknessIconWidth > 0)
			{
				drawScaled(graphics, weaknessIcon, x - weaknessIconPadding,
					baseline - fonts.metrics(WEAKNESS).getAscent() / 2 - weaknessIconHeight / 2, weaknessIconWidth, weaknessIconHeight);
				x += weaknessIconAdvance;
			}
			fonts.useFont(graphics, WEAKNESS);
			drawShadowedText(graphics, weaknessText, x, baseline, colors.getLevelText(), 0.9f);
		}

		if (onRow(DRAIN_CAP, top))
		{
			int x = x(DRAIN_CAP);
			if (scaledDrainIcon != null)
			{
				graphics.drawImage(scaledDrainIcon, x,
					baseline - fonts.metrics(DRAIN_CAP).getAscent() / 2 - scaledDrainIcon.getHeight() / 2, null);
				x += drainIconWidth;
			}
			fonts.useFont(graphics, DRAIN_CAP);
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
		drawScaled(graphics, icon, left, top, size, size);
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

	private static void drawScaled(Graphics2D graphics, BufferedImage icon, int x, int y, int width, int height)
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

	private static final class EllipsizedText
	{
		private String source;
		private Font font;
		private int maxWidth;
		private String text;

		String get(String name, FontMetrics metrics, int maxWidth)
		{
			if (!name.equals(source) || metrics.getFont() != font || maxWidth != this.maxWidth)
			{
				text = ellipsize(name, metrics, maxWidth);
				source = name;
				font = metrics.getFont();
				this.maxWidth = maxWidth;
			}
			return text;
		}
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
		fonts.useFont(graphics, DAMAGE_NUMBER);
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
}
