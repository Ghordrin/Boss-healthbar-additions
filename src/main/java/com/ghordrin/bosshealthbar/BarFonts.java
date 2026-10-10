package com.ghordrin.bosshealthbar;

import com.google.common.annotations.VisibleForTesting;
import static com.ghordrin.bosshealthbar.BarTextPainter.DAMAGE_NUMBER;
import static com.ghordrin.bosshealthbar.BarTextPainter.DRAIN_CAP;
import static com.ghordrin.bosshealthbar.BarTextPainter.FIGHT_TIMER;
import static com.ghordrin.bosshealthbar.BarTextPainter.HITPOINTS;
import static com.ghordrin.bosshealthbar.BarTextPainter.ITEM_COUNT;
import static com.ghordrin.bosshealthbar.BarTextPainter.KILL_COUNT;
import static com.ghordrin.bosshealthbar.BarTextPainter.MAGIC_DEFENCE;
import static com.ghordrin.bosshealthbar.BarTextPainter.NAME;
import static com.ghordrin.bosshealthbar.BarTextPainter.PARTY_DEFENCE;
import static com.ghordrin.bosshealthbar.BarTextPainter.SPECIAL_ATTACKS;
import static com.ghordrin.bosshealthbar.BarTextPainter.WEAKNESS;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.runelite.client.config.FontType;
import net.runelite.client.ui.FontManager;

class BarFonts
{
	private static final int REFERENCE_FONT_SIZE = 16;
	private static final int MIN_FONT_SIZE = 8;
	private static final int MAX_FONT_SIZE = 40;
	private static final float SMALL_TEXT_SCALE = 0.72f;
	private static final int HEADER_HEIGHT = 24;
	private static final int HEADER_BASELINE_GAP = 6;
	private static final int FOOTER_HEIGHT = 16;

	private final BossHealthBarConfig config;

	// Each font's metrics, measured with its own smoothing, which changes the glyph widths.
	private final FontMetrics[] metrics = new FontMetrics[ITEM_COUNT];
	private FontMetrics levelMetrics;

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

	BarFonts(BossHealthBarConfig config)
	{
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

	FontMetrics metrics(int item)
	{
		return metrics[item];
	}

	FontMetrics levelMetrics()
	{
		return levelMetrics;
	}

	int above(int item)
	{
		return itemAbove[item];
	}

	int below(int item)
	{
		return itemBelow[item];
	}

	int levelAbove()
	{
		return levelAbove;
	}

	int levelBelow()
	{
		return levelBelow;
	}

	void setTextAntialiasing(Graphics2D graphics, boolean pixel)
	{
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, smoothText && !pixel
			? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
			: RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	}

	void useFont(Graphics2D graphics, int item)
	{
		graphics.setFont(fonts[item]);
		setTextAntialiasing(graphics, pixelFonts[item]);
	}

	void useLevelFont(Graphics2D graphics)
	{
		graphics.setFont(levelFont);
		setTextAntialiasing(graphics, levelItemFont.pixel);
	}

	void applyTextHints(Graphics2D graphics)
	{
		setTextAntialiasing(graphics, pixelFonts[NAME]);
		// Fractional metrics place glyphs between pixels, which makes smoothed text look soft.
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
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
}
