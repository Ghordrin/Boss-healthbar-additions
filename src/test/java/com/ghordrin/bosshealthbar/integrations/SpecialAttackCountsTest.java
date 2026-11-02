package com.ghordrin.bosshealthbar.integrations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.render.BarTextPainter;
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
import org.junit.Test;

public class SpecialAttackCountsTest
{
	private static class SpecialCounterPlugin extends Plugin
	{
	}

	private static class OtherPlugin extends Plugin
	{
	}

	private static class BetterPartyDefencePlugin extends Plugin
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

	private static class SpecWeaponInfoBox extends SpecialCounter
	{
		SpecWeaponInfoBox(BufferedImage image, String text)
		{
			super(new BetterPartyDefencePlugin(), image, text, Color.WHITE);
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
	public void ignoresOtherBoxes()
	{
		assertTrue(SpecialAttackCounts.matches(box(MAUL, "2", Color.WHITE), SpecialAttackCounts.BOX_NAME));
		assertFalse(SpecialAttackCounts.matches(new OtherCounter(COUNTER_PLUGIN), SpecialAttackCounts.BOX_NAME));
		assertFalse(SpecialAttackCounts.matches(new SpecialCounter(new OtherPlugin(), MAUL, "2", Color.WHITE),
			SpecialAttackCounts.BOX_NAME));
		assertFalse(SpecialAttackCounts.matches(null, SpecialAttackCounts.BOX_NAME));
	}

	@Test
	public void findsEveryCountInInfoBoxOrder()
	{
		final List<SpecialAttackCounts.Reading> found = SpecialAttackCounts.find(Arrays.asList(
			box(MAUL, "2", Color.WHITE),
			new OtherCounter(COUNTER_PLUGIN),
			box(null, "", Color.WHITE),
			box(null, null, Color.WHITE),
			box(GODSWORD, "31", Color.GREEN)), SpecialAttackCounts.BOX_NAME);

		assertEquals(readings(
			new SpecialAttackCounts.Reading(MAUL, "2", Color.WHITE, null),
			new SpecialAttackCounts.Reading(GODSWORD, "31", Color.GREEN, null)), found);
	}

	// In a party the tooltip names who landed what, which the bar shows when the count is hovered.
	@Test
	public void keepsTheTooltip()
	{
		final SpecialCounter box = box(MAUL, "2", Color.WHITE);
		box.setTooltip("Dragon warhammer special has hit 2 times.</br>You: Dragon warhammer special has hit 2 times.");

		assertEquals(readings(new SpecialAttackCounts.Reading(MAUL, "2", Color.WHITE,
				"Dragon warhammer special has hit 2 times.</br>You: Dragon warhammer special has hit 2 times.")),
			SpecialAttackCounts.find(Collections.singletonList(box), SpecialAttackCounts.BOX_NAME));
	}

	// Both plugins can count the same specs, so Better Party Defence's boxes take over while it shows any.
	@Test
	public void betterPartyDefenceWinsWhileItShowsBoxes()
	{
		final List<InfoBox> both = Arrays.asList(box(MAUL, "2", Color.WHITE), new SpecWeaponInfoBox(GODSWORD, "3"));
		final List<SpecialAttackCounts.Reading> better = readings(
			new SpecialAttackCounts.Reading(GODSWORD, "3", Color.WHITE, null));
		final List<SpecialAttackCounts.Reading> counter = readings(
			new SpecialAttackCounts.Reading(MAUL, "2", Color.WHITE, null));

		assertTrue(SpecialAttackCounts.matches(new SpecWeaponInfoBox(GODSWORD, "3"), SpecialAttackCounts.BETTER_BOX_NAME));
		assertEquals(better, SpecialAttackCounts.select(both, true));
		assertEquals(counter, SpecialAttackCounts.select(both, false));
		assertEquals(counter, SpecialAttackCounts.select(Collections.singletonList(box(MAUL, "2", Color.WHITE)), true));
		assertEquals(better, SpecialAttackCounts.select(
			Collections.singletonList(new SpecWeaponInfoBox(GODSWORD, "3")), true));
	}

	@Test
	public void findsNothingWithoutBoxes()
	{
		assertTrue(SpecialAttackCounts.find(Collections.emptyList(), SpecialAttackCounts.BOX_NAME).isEmpty());
		assertTrue(SpecialAttackCounts.find(Collections.singletonList(new OtherCounter(COUNTER_PLUGIN)),
			SpecialAttackCounts.BOX_NAME).isEmpty());
	}

	@Test
	public void groupWidthCoversEveryWeapon()
	{
		final Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		final FontMetrics metrics = graphics.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		graphics.dispose();

		final List<SpecialAttackCounts.Reading> noIcons = readings(
			new SpecialAttackCounts.Reading(null, "2", Color.WHITE, null),
			new SpecialAttackCounts.Reading(null, "31", Color.WHITE, null));
		assertEquals(10 + metrics.stringWidth("2") + 6 + metrics.stringWidth("31"),
			BarTextPainter.specialAttacksEnd(noIcons, metrics, 10, 13));

		// A 36x32 item image at height 13 is 15 wide with 1px of padding each side, plus the 3px gap.
		final BufferedImage itemImage = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		final List<SpecialAttackCounts.Reading> withIcon = readings(new SpecialAttackCounts.Reading(itemImage, "2", Color.WHITE, null));
		assertEquals(10 + 16 + metrics.stringWidth("2"), BarTextPainter.specialAttacksEnd(withIcon, metrics, 10, 13));
	}
}
