package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BarTextPainter.DAMAGE_NUMBER;
import static com.ghordrin.bosshealthbar.BarTextPainter.DRAIN_CAP;
import static com.ghordrin.bosshealthbar.BarTextPainter.HITPOINTS;
import static com.ghordrin.bosshealthbar.BarTextPainter.ITEM_COUNT;
import static com.ghordrin.bosshealthbar.BarTextPainter.KILL_COUNT;
import static com.ghordrin.bosshealthbar.BarTextPainter.MAGIC_DEFENCE;
import static com.ghordrin.bosshealthbar.BarTextPainter.NAME;
import static com.ghordrin.bosshealthbar.BarTextPainter.PARTY_DEFENCE;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;
import net.runelite.client.config.FontType;
import net.runelite.client.ui.FontManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ItemFontsTest
{
	private static final FontType SERIF_17 = new FontType().withFamily(Font.SERIF).withSize(17);
	private static final FontType SERIF_12 = new FontType().withFamily(Font.SERIF).withSize(12);

	private static class FontsConfig implements BossHealthBarConfig
	{
		FontType font = DEFAULT_FONT;
		FontType damageNumberFont = FontType.REGULAR;
		FontType combatLevelFont = FontType.SMALL;
		FontType hitpointsFont = FontType.SMALL;
		FontType killCountFont = FontType.SMALL;
		FontType partyDefenceFont = FontType.SMALL;
		boolean smoothText = true;
		boolean showCombatLevel;
		boolean showKillCount;
		BarPosition killCountPosition = BarPosition.BOTTOM_LEFT;

		@Override
		public FontType font()
		{
			return font;
		}

		@Override
		public FontType damageNumberFont()
		{
			return damageNumberFont;
		}

		@Override
		public FontType combatLevelFont()
		{
			return combatLevelFont;
		}

		@Override
		public FontType hitpointsFont()
		{
			return hitpointsFont;
		}

		@Override
		public FontType killCountFont()
		{
			return killCountFont;
		}

		@Override
		public FontType partyDefenceFont()
		{
			return partyDefenceFont;
		}

		@Override
		public boolean smoothText()
		{
			return smoothText;
		}

		@Override
		public boolean showCombatLevel()
		{
			return showCombatLevel;
		}

		@Override
		public boolean showKillCount()
		{
			return showKillCount;
		}

		@Override
		public BarPosition killCountPosition()
		{
			return killCountPosition;
		}
	}

	private static BarTextPainter painter(FontsConfig config)
	{
		final BarTextPainter painter = new BarTextPainter(null, config);
		painter.updateFonts();
		painter.updateRows(false, false, false, false, false);
		return painter;
	}

	// The old look for a Serif 17 name, as the one-time migration sets it up.
	private static FontsConfig migratedSerif()
	{
		final FontsConfig config = new FontsConfig();
		config.font = SERIF_17;
		config.damageNumberFont = SERIF_17;
		config.combatLevelFont = SERIF_12;
		config.hitpointsFont = SERIF_12;
		config.killCountFont = SERIF_12;
		config.partyDefenceFont = SERIF_12;
		return config;
	}

	@Test
	public void largeTextKeepsItsBaselineGap()
	{
		assertEquals(18, BarTextPainter.largeAbove(16));
		assertEquals(6, BarTextPainter.largeBelow(16));
		assertEquals(20, BarTextPainter.largeAbove(17));
		assertEquals(6, BarTextPainter.largeBelow(17));
		assertEquals(36, BarTextPainter.largeAbove(32));
		assertEquals(12, BarTextPainter.largeBelow(32));
		assertEquals(0, BarTextPainter.smallBelow(10, 12));
	}

	@Test
	public void smallTextRowsKeepTheirOldHeight()
	{
		final String small = FontManager.getRunescapeSmallFont().getFamily();
		assertEquals(16, BarTextPainter.smallRowHeight(16, true, small, Font.SERIF, 17));
		assertEquals(32, BarTextPainter.smallRowHeight(32, true, small, Font.SERIF, 17));
		// Small Serif 12 went with a Serif 17 name and a 17 high row.
		assertEquals(17, BarTextPainter.smallRowHeight(12, false, Font.SERIF, Font.SERIF, 17));
		assertEquals(17, BarTextPainter.smallRowHeight(12, false, Font.SANS_SERIF, Font.SERIF, 17));
		assertEquals(14, BarTextPainter.smallRowHeight(10, false, Font.SERIF, Font.SERIF, 17));
	}

	@Test
	public void migratedSmallTextRowsKeepTheirOldHeightAtEverySize()
	{
		for (int size = 8; size <= 40; size++)
		{
			final FontType name = new FontType().withFamily(Font.SERIF).withSize(size);
			final FontType small = FontDefaultMigration.smallTextFont(name);
			final FontsConfig config = new FontsConfig();
			config.font = name;
			config.damageNumberFont = name;
			config.combatLevelFont = small;
			config.hitpointsFont = small;
			config.killCountFont = small;
			config.partyDefenceFont = small;
			// The old row was as tall as the name font. At the smallest sizes the text reached past that,
			// and the row now grows to fit it.
			final BarTextPainter painter = painter(config);
			assertEquals("size " + size, Math.max(size, painter.bottomRowBaseline()), painter.bottomRowHeight());
			if (size >= 10)
			{
				assertEquals("size " + size, size, painter.bottomRowHeight());
			}
		}
	}

	@Test
	public void theTallestItemSetsTheRowBaselineAndHeight()
	{
		final BarPosition[] positions = new BarPosition[ITEM_COUNT];
		Arrays.fill(positions, BarPosition.TOP_LEFT);
		final boolean[] available = new boolean[ITEM_COUNT];
		available[NAME] = true;
		available[KILL_COUNT] = true;
		final int[] above = new int[ITEM_COUNT];
		final int[] below = new int[ITEM_COUNT];
		above[NAME] = 18;
		below[NAME] = 6;
		above[KILL_COUNT] = 30;
		below[KILL_COUNT] = 2;

		assertEquals(30, BarTextPainter.rowBaseline(positions, available, true, above));
		assertEquals(36, BarTextPainter.rowHeight(positions, available, true, above, below));
		assertEquals(0, BarTextPainter.rowHeight(positions, available, false, above, below));
	}

	@Test
	public void fontsAreReadPerItem()
	{
		final FontsConfig config = new FontsConfig();
		config.partyDefenceFont = SERIF_12;
		final FontType[] fonts = new FontType[ITEM_COUNT];
		BarTextPainter.readItemFonts(config, fonts);
		assertSame(BossHealthBarConfig.DEFAULT_FONT, fonts[NAME]);
		assertSame(FontType.REGULAR, fonts[DAMAGE_NUMBER]);
		assertSame(FontType.SMALL, fonts[HITPOINTS]);
		assertSame(SERIF_12, fonts[PARTY_DEFENCE]);
		assertSame(SERIF_12, fonts[MAGIC_DEFENCE]);
		assertSame(FontType.SMALL, fonts[DRAIN_CAP]);
		assertSame(FontType.SMALL, fonts[BarTextPainter.FIGHT_TIMER]);
	}

	@Test
	public void defaultFontsLookAsBefore()
	{
		final FontsConfig config = new FontsConfig();
		config.showCombatLevel = true;
		final BarTextPainter painter = painter(config);
		final String small = FontManager.getRunescapeSmallFont().getFamily();

		assertEquals(FontManager.getRunescapeFont().getFamily(), painter.font(NAME).getFamily());
		assertEquals(16, painter.font(NAME).getSize());
		assertEquals(16, painter.font(DAMAGE_NUMBER).getSize());
		assertEquals(small, painter.font(HITPOINTS).getFamily());
		assertEquals(16, painter.font(HITPOINTS).getSize());
		assertEquals(small, painter.levelFont().getFamily());
		assertEquals(24, painter.topRowHeight());
		assertEquals(16, painter.bottomRowHeight());
		assertEquals(18, painter.topRowBaseline());
		assertEquals(12, painter.bottomRowBaseline());
	}

	@Test
	public void migratedSerifLooksAsBefore()
	{
		final FontsConfig config = migratedSerif();
		config.showCombatLevel = true;
		final BarTextPainter painter = painter(config);

		// The old code put a row of small text at the small font's ascent + 1, measured with these hints.
		final Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		final int smallAscent = graphics.getFontMetrics(FontManager.getFallbackFont(Font.SERIF, Font.PLAIN, 12)).getAscent();
		graphics.dispose();

		assertEquals(17, painter.font(NAME).getSize());
		assertEquals(12, painter.font(HITPOINTS).getSize());
		assertEquals(12, painter.levelFont().getSize());
		assertEquals(26, painter.topRowHeight());
		assertEquals(17, painter.bottomRowHeight());
		assertEquals(20, painter.topRowBaseline());
		assertEquals(smallAscent + 1, painter.bottomRowBaseline());
	}

	@Test
	public void theNameFontSizeOnlyChangesTheName()
	{
		final FontsConfig config = new FontsConfig();
		config.font = FontType.REGULAR.withSize(24);
		final BarTextPainter painter = painter(config);

		assertEquals(24, painter.font(NAME).getSize());
		assertEquals(16, painter.font(DAMAGE_NUMBER).getSize());
		assertEquals(16, painter.font(HITPOINTS).getSize());
		assertEquals(36, painter.topRowHeight());
		assertEquals(16, painter.bottomRowHeight());
	}

	@Test
	public void rowsGrowToFitLargerFonts()
	{
		final FontsConfig config = new FontsConfig();
		config.hitpointsFont = FontType.SMALL.withSize(32);
		config.showKillCount = true;
		config.killCountPosition = BarPosition.TOP_CENTER;
		config.killCountFont = FontType.SMALL.withSize(40);
		final BarTextPainter painter = painter(config);

		assertEquals(32, painter.font(HITPOINTS).getSize());
		assertEquals(40, painter.font(KILL_COUNT).getSize());
		assertEquals(40, painter.topRowHeight());
		assertEquals(32, painter.bottomRowHeight());
	}

	@Test
	public void iconsFollowTheirItemsFont()
	{
		final Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		final FontMetrics normal = graphics.getFontMetrics(FontManager.getRunescapeSmallFont());
		final FontMetrics large = graphics.getFontMetrics(FontManager.getRunescapeSmallFont().deriveFont(32f));
		graphics.dispose();

		assertTrue(BarTextPainter.itemIconSize(large) > BarTextPainter.itemIconSize(normal));
		assertEquals(Math.round((normal.getAscent() + 1) * 1.3f), BarTextPainter.itemIconSize(normal));
	}

	@Test
	public void onlyChangedFontsAreRebuilt()
	{
		final FontsConfig config = new FontsConfig();
		final BarTextPainter painter = painter(config);
		final Font name = painter.font(NAME);
		final Font hitpoints = painter.font(HITPOINTS);
		final Font killCount = painter.font(KILL_COUNT);

		painter.updateFonts();
		assertSame(name, painter.font(NAME));
		assertSame(hitpoints, painter.font(HITPOINTS));

		config.hitpointsFont = FontType.SMALL.withSize(24);
		painter.updateFonts();
		assertEquals(24, painter.font(HITPOINTS).getSize());
		assertSame(killCount, painter.font(KILL_COUNT));
		assertSame(name, painter.font(NAME));

		// Smooth text only changes how the fonts are measured and drawn.
		config.smoothText = false;
		painter.updateFonts();
		assertSame(killCount, painter.font(KILL_COUNT));

		config.killCountFont = FontType.SMALL.withBold(true);
		painter.updateFonts();
		assertTrue(painter.font(KILL_COUNT).isBold());
		config.killCountFont = FontType.SMALL.withItalic(true);
		painter.updateFonts();
		assertFalse(painter.font(KILL_COUNT).isBold());
		assertTrue(painter.font(KILL_COUNT).isItalic());
		assertSame(name, painter.font(NAME));
	}

	@Test
	public void migrationKeepsTheOldSmallText()
	{
		final FontType small = FontDefaultMigration.smallTextFont(SERIF_17);
		assertEquals(Font.SERIF, small.getFamily());
		assertEquals(12, small.getSize());
		assertFalse(small.isBold());
		assertFalse(small.isItalic());

		final FontType styled = FontDefaultMigration.smallTextFont(SERIF_17.withBold(true).withItalic(true).withSize(20));
		assertEquals(14, styled.getSize());
		assertFalse(styled.isBold());
		assertTrue(styled.isItalic());

		assertEquals(8, FontDefaultMigration.smallTextFont(SERIF_17.withSize(8)).getSize());
	}

	@Test
	public void migrationLeavesRunescapeFontsAlone()
	{
		assertNull(FontDefaultMigration.smallTextFont(FontType.REGULAR));
		assertNull(FontDefaultMigration.smallTextFont(FontType.SMALL));
		assertNull(FontDefaultMigration.smallTextFont(FontType.BOLD));
		assertNull(FontDefaultMigration.smallTextFont(FontType.REGULAR.withSize(20)));
		assertNull(FontDefaultMigration.smallTextFont(null));
	}

	@Test
	public void migrationAlwaysGivesTheDamageNumberTheNameFont()
	{
		final FontType bold = FontType.BOLD.withItalic(true);
		final Map<String, FontType> pixelWrites = FontDefaultMigration.itemFontWrites(bold);
		assertEquals(1, pixelWrites.size());
		assertSame(bold, pixelWrites.get(BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY));
		assertTrue(pixelWrites.get(BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY).isBold());

		final Map<String, FontType> serifWrites = FontDefaultMigration.itemFontWrites(SERIF_17);
		assertEquals(8, serifWrites.size());
		assertSame(SERIF_17, serifWrites.get(BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY));
		assertEquals(12, serifWrites.get(BossHealthBarConfig.HITPOINTS_FONT_KEY).getSize());

		assertTrue(FontDefaultMigration.itemFontWrites(null).isEmpty());
	}

	@Test
	public void migrationCoversEverySmallTextFont()
	{
		assertEquals(7, FontDefaultMigration.SMALL_TEXT_FONT_KEYS.size());
		assertFalse(FontDefaultMigration.SMALL_TEXT_FONT_KEYS.contains(BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY));
	}
}
