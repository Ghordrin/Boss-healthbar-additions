package com.ghordrin.bosshealthbar;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SpecialAttackCountsTest
{
	private static class SpecialCounterPlugin extends Plugin
	{
	}

	private static class OtherPlugin extends Plugin
	{
	}

	private static class SpecialCounter extends InfoBox
	{
		private final String text;
		private final Color color;

		SpecialCounter(Plugin plugin, BufferedImage image, String text, Color color)
		{
			super(image, plugin);
			this.text = text;
			this.color = color;
		}

		@Override
		public String getText()
		{
			return text;
		}

		@Override
		public Color getTextColor()
		{
			return color;
		}
	}

	private static class OtherCounter extends SpecialCounter
	{
		OtherCounter(Plugin plugin)
		{
			super(plugin, null, "1", Color.WHITE);
		}
	}

	private static final Plugin COUNTER_PLUGIN = new SpecialCounterPlugin();
	private static final BufferedImage MAUL = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	private static final BufferedImage GODSWORD = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	private static SpecialCounter box(BufferedImage image, String text, Color color)
	{
		return new SpecialCounter(COUNTER_PLUGIN, image, text, color);
	}

	private static List<SpecialAttackCounts.Reading> readings(SpecialAttackCounts.Reading... readings)
	{
		return Arrays.asList(readings);
	}

	@Test
	public void boxesAreNamedLikeTheirs()
	{
		assertEquals(SpecialAttackCounts.BOX_NAME, box(MAUL, "2", Color.WHITE).getName());
	}

	@Test
	public void ignoresOtherBoxes()
	{
		assertTrue(SpecialAttackCounts.matches(box(MAUL, "2", Color.WHITE)));
		assertFalse(SpecialAttackCounts.matches(new OtherCounter(COUNTER_PLUGIN)));
		assertFalse(SpecialAttackCounts.matches(new SpecialCounter(new OtherPlugin(), MAUL, "2", Color.WHITE)));
		assertFalse(SpecialAttackCounts.matches(null));
	}

	@Test
	public void findsEveryCountInInfoBoxOrder()
	{
		final List<SpecialAttackCounts.Reading> found = SpecialAttackCounts.find(Arrays.asList(
			box(MAUL, "2", Color.WHITE),
			new OtherCounter(COUNTER_PLUGIN),
			box(null, "", Color.WHITE),
			box(null, null, Color.WHITE),
			box(GODSWORD, "31", Color.GREEN)));

		assertEquals(readings(
			new SpecialAttackCounts.Reading(MAUL, "2", Color.WHITE),
			new SpecialAttackCounts.Reading(GODSWORD, "31", Color.GREEN)), found);
	}

	@Test
	public void findsNothingWithoutBoxes()
	{
		assertTrue(SpecialAttackCounts.find(Collections.emptyList()).isEmpty());
		assertTrue(SpecialAttackCounts.find(Collections.singletonList(new OtherCounter(COUNTER_PLUGIN))).isEmpty());
	}

	@Test
	public void keepsTheReadingsForOneTickWhenTheBoxesAreMissing()
	{
		final SpecialAttackCounts counts = new SpecialAttackCounts(null, null, null);
		final List<SpecialAttackCounts.Reading> found = readings(new SpecialAttackCounts.Reading(MAUL, "2", Color.WHITE));

		counts.accept(found);
		assertSame(found, counts.readings());

		counts.accept(Collections.emptyList());
		assertSame(found, counts.readings());

		counts.accept(Collections.emptyList());
		assertTrue(counts.readings().isEmpty());
	}

	@Test
	public void newReadingsResetTheMissedTick()
	{
		final SpecialAttackCounts counts = new SpecialAttackCounts(null, null, null);
		final List<SpecialAttackCounts.Reading> first = readings(new SpecialAttackCounts.Reading(MAUL, "2", Color.WHITE));
		final List<SpecialAttackCounts.Reading> second = readings(new SpecialAttackCounts.Reading(MAUL, "3", Color.WHITE));

		counts.accept(first);
		counts.accept(Collections.emptyList());
		counts.accept(second);
		counts.accept(Collections.emptyList());
		assertSame(second, counts.readings());
	}

	@Test
	public void groupWidthCoversEveryWeapon()
	{
		final Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		final FontMetrics metrics = graphics.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		graphics.dispose();

		final List<SpecialAttackCounts.Reading> noIcons = readings(
			new SpecialAttackCounts.Reading(null, "2", Color.WHITE),
			new SpecialAttackCounts.Reading(null, "31", Color.WHITE));
		assertEquals(10 + metrics.stringWidth("2") + 6 + metrics.stringWidth("31"),
			BarTextPainter.specialAttacksEnd(noIcons, metrics, 10, 13));

		// A 36x32 item image at height 13 is 15 wide with 1px of padding each side, plus the 3px gap.
		final BufferedImage itemImage = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		final List<SpecialAttackCounts.Reading> withIcon = readings(new SpecialAttackCounts.Reading(itemImage, "2", Color.WHITE));
		assertEquals(10 + 16 + metrics.stringWidth("2"), BarTextPainter.specialAttacksEnd(withIcon, metrics, 10, 13));
	}

	@Test
	public void unchangedReadingsKeepTheSameList()
	{
		final SpecialAttackCounts counts = new SpecialAttackCounts(null, null, null);
		final List<SpecialAttackCounts.Reading> first = readings(new SpecialAttackCounts.Reading(MAUL, "2", Color.WHITE));

		counts.accept(first);
		counts.accept(readings(new SpecialAttackCounts.Reading(MAUL, "2", Color.WHITE)));
		assertSame(first, counts.readings());
	}
}
