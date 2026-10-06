package com.ghordrin.bosshealthbar;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PartyDefenceTest
{
	private static class DefenceTrackerPlugin extends Plugin
	{
	}

	private static class OtherPlugin extends Plugin
	{
	}

	private static class DefenceInfoBox extends InfoBox
	{
		private final String text;
		private final Color color;

		DefenceInfoBox(Plugin plugin, String tooltip, String text, Color color)
		{
			this(plugin, null, tooltip, text, color);
		}

		DefenceInfoBox(Plugin plugin, BufferedImage image, String tooltip, String text, Color color)
		{
			super(image, plugin);
			setTooltip(tooltip);
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

	private static class VulnerabilityInfoBox extends DefenceInfoBox
	{
		VulnerabilityInfoBox(Plugin plugin, String tooltip)
		{
			super(plugin, tooltip, "V", Color.WHITE);
		}
	}

	private static class BetterPartyDefencePlugin extends Plugin
	{
	}

	private static final Plugin TRACKER = new DefenceTrackerPlugin();
	private static final Plugin BETTER = new BetterPartyDefencePlugin();
	private static final BufferedImage DEFENCE_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	private static final BufferedImage MAGIC_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	private static final Predicate<BufferedImage> IS_MAGIC = image -> image == MAGIC_IMAGE;
	private static final Predicate<BufferedImage> NOTHING_IS_MAGIC = image -> false;

	private static DefenceInfoBox box(String tooltip, String text, Color color)
	{
		return new DefenceInfoBox(TRACKER, tooltip, text, color);
	}

	private static DefenceInfoBox betterBox(BufferedImage image, String text, Color color)
	{
		return new DefenceInfoBox(BETTER, image, "No specs yet", text, color);
	}

	@Test
	public void boxesAreNamedLikeTheirs()
	{
		assertEquals(PartyDefence.BOX_NAME, box("Big Boss", "10", Color.WHITE).getName());
		assertEquals(PartyDefence.BETTER_BOX_NAME, betterBox(DEFENCE_IMAGE, "10", Color.WHITE).getName());
	}

	@Test
	public void betterSkipsTheMagicDefenceBox()
	{
		final PartyDefence.Reading reading = PartyDefence.findBetter(Arrays.asList(
			betterBox(MAGIC_IMAGE, "150", Color.WHITE),
			betterBox(DEFENCE_IMAGE, "120", Color.GREEN)), IS_MAGIC);

		assertEquals(new PartyDefence.Reading("120", Color.GREEN), reading);
	}

	@Test
	public void betterTakesTheFirstBoxWhenNeitherIsMagic()
	{
		final PartyDefence.Reading reading = PartyDefence.findBetter(Arrays.asList(
			box("Big Boss", "5", Color.WHITE),
			betterBox(DEFENCE_IMAGE, "120", Color.GREEN),
			betterBox(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "150", Color.WHITE)), IS_MAGIC);

		assertEquals(new PartyDefence.Reading("120", Color.GREEN), reading);
		assertEquals(reading, PartyDefence.findBetter(Arrays.asList(
			betterBox(DEFENCE_IMAGE, "120", Color.GREEN),
			betterBox(MAGIC_IMAGE, "150", Color.WHITE)), NOTHING_IS_MAGIC));
	}

	@Test
	public void betterIgnoresEmptyText()
	{
		assertNull(PartyDefence.findBetter(Arrays.asList(
			betterBox(DEFENCE_IMAGE, "", Color.WHITE),
			betterBox(MAGIC_IMAGE, "150", Color.WHITE)), IS_MAGIC));
		assertNull(PartyDefence.findBetter(Collections.singletonList(betterBox(DEFENCE_IMAGE, null, Color.WHITE)), IS_MAGIC));
		assertNull(PartyDefence.findBetter(Collections.singletonList(betterBox(MAGIC_IMAGE, "150", Color.WHITE)), IS_MAGIC));
		assertNull(PartyDefence.findBetter(Collections.emptyList(), IS_MAGIC));
	}

	@Test
	public void magicIsTheBoxWithTheMagicImage()
	{
		final List<InfoBox> boxes = Arrays.asList(
			box("Big Boss", "5", Color.WHITE),
			betterBox(DEFENCE_IMAGE, "120", Color.GREEN),
			betterBox(MAGIC_IMAGE, "150", Color.YELLOW));

		assertEquals(new PartyDefence.Reading("150", Color.YELLOW), PartyDefence.findBetterMagic(boxes, IS_MAGIC));
		assertEquals(new PartyDefence.Reading("150", Color.YELLOW), PartyDefence.findBetterMagic(Arrays.asList(
			betterBox(MAGIC_IMAGE, "150", Color.YELLOW),
			betterBox(DEFENCE_IMAGE, "120", Color.GREEN)), IS_MAGIC));
	}

	@Test
	public void unrecognisedImagesGiveDefenceFromTheFirstBoxAndNoMagic()
	{
		final List<InfoBox> boxes = Arrays.asList(
			betterBox(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "150", Color.YELLOW),
			betterBox(DEFENCE_IMAGE, "120", Color.GREEN));

		assertEquals(new PartyDefence.Reading("150", Color.YELLOW), PartyDefence.findBetter(boxes, IS_MAGIC));
		assertNull(PartyDefence.findBetterMagic(boxes, IS_MAGIC));
	}

	@Test
	public void themedMagicIconIsFoundByItsPixelsInEitherOrder()
	{
		final BufferedImage skillMagic = icon(25, 0xFF2040C0);
		final BufferedImage skillDefence = icon(25, 0xFFA0A0A0);
		final MagicIconMatcher matcher = new MagicIconMatcher();
		final Predicate<BufferedImage> isMagic = image -> matcher.isMagic(image, skillMagic, null);
		final InfoBox themedMagic = betterBox(MagicIconMatcher.themed(skillMagic), "150", Color.YELLOW);
		final InfoBox themedDefence = betterBox(MagicIconMatcher.themed(skillDefence), "120", Color.GREEN);

		for (List<InfoBox> boxes : Arrays.asList(Arrays.asList(themedMagic, themedDefence),
			Arrays.asList(themedDefence, themedMagic)))
		{
			assertEquals(new PartyDefence.Reading("120", Color.GREEN), PartyDefence.findBetter(boxes, isMagic));
			assertEquals(new PartyDefence.Reading("150", Color.YELLOW), PartyDefence.findBetterMagic(boxes, isMagic));
		}
	}

	@Test
	public void themedMagicIconFromAnOverrideIsFoundByItsPixels()
	{
		final BufferedImage skillMagic = icon(25, 0xFF2040C0);
		final BufferedImage override = icon(30, 0xFF10E020);
		final MagicIconMatcher matcher = new MagicIconMatcher();

		assertTrue(matcher.isMagic(MagicIconMatcher.themed(override), skillMagic, override));
		assertTrue(matcher.isMagic(MagicIconMatcher.themed(skillMagic), skillMagic, override));
		assertTrue(matcher.isMagic(skillMagic, skillMagic, null));
		assertFalse(matcher.isMagic(MagicIconMatcher.themed(override), skillMagic, null));
		assertFalse(matcher.isMagic(MagicIconMatcher.themed(icon(25, 0xFFA0A0A0)), skillMagic, override));
		assertFalse(matcher.isMagic(null, skillMagic, override));
	}

	private static BufferedImage icon(int size, int argb)
	{
		final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for (int y = 2; y < size - 2; y++)
		{
			for (int x = y % 3; x < size - 2; x += 2)
			{
				image.setRGB(x, y, argb);
			}
		}
		return image;
	}

	@Test
	public void aSingleBoxWithoutTheMagicImageIsDefence()
	{
		final List<InfoBox> boxes = Collections.singletonList(betterBox(DEFENCE_IMAGE, "120", Color.GREEN));

		assertEquals(new PartyDefence.Reading("120", Color.GREEN), PartyDefence.findBetter(boxes, IS_MAGIC));
		assertNull(PartyDefence.findBetterMagic(boxes, IS_MAGIC));
		assertNull(PartyDefence.findBetterMagic(Collections.singletonList(box("Big Boss", "24", Color.RED)), IS_MAGIC));
	}

	@Test
	public void magicIgnoresEmptyText()
	{
		assertNull(PartyDefence.findBetterMagic(Arrays.asList(
			betterBox(DEFENCE_IMAGE, "120", Color.GREEN),
			betterBox(MAGIC_IMAGE, "", Color.WHITE)), IS_MAGIC));
		assertNull(PartyDefence.findBetterMagic(Arrays.asList(
			betterBox(DEFENCE_IMAGE, "120", Color.GREEN),
			betterBox(MAGIC_IMAGE, null, Color.WHITE)), IS_MAGIC));
	}

	@Test
	public void betterIsUsedWhenItsOn()
	{
		final List<InfoBox> boxes = Arrays.asList(
			box("Big Boss", "24", Color.RED),
			betterBox(DEFENCE_IMAGE, "120", Color.GREEN));

		assertEquals(new PartyDefence.Reading("120", Color.GREEN), PartyDefence.select(boxes, "Big Boss", true, IS_MAGIC));
		assertNull(PartyDefence.select(Collections.singletonList(box("Big Boss", "24", Color.RED)), "Big Boss", true, IS_MAGIC));
	}

	@Test
	public void trackerIsUsedWhenBetterIsOff()
	{
		final List<InfoBox> boxes = Arrays.asList(
			betterBox(DEFENCE_IMAGE, "120", Color.GREEN),
			box("Small Boss", "5", Color.WHITE),
			box("Big Boss", "24", Color.RED));

		assertEquals(new PartyDefence.Reading("24", Color.RED), PartyDefence.select(boxes, "Big Boss", false, NOTHING_IS_MAGIC));
		assertNull(PartyDefence.select(boxes, "Other Boss", false, NOTHING_IS_MAGIC));
	}

	@Test
	public void matchesTheTooltipWithoutColorTagsOrCase()
	{
		assertTrue(PartyDefence.matches(box("<col=ffffff>Big Boss</col>", "10", Color.WHITE), "Big Boss"));
		assertTrue(PartyDefence.matches(box("Big Boss", "10", Color.WHITE), "big boss"));
		assertTrue(PartyDefence.matches(box("Big Boss", "10", Color.WHITE), "<col=ffff00>Big Boss</col>"));
		assertFalse(PartyDefence.matches(box("Big Boss", "10", Color.WHITE), "Bigger Boss"));
	}

	@Test
	public void ignoresOtherBoxesAndMissingValues()
	{
		assertFalse(PartyDefence.matches(new VulnerabilityInfoBox(TRACKER, "Big Boss"), "Big Boss"));
		assertFalse(PartyDefence.matches(new DefenceInfoBox(new OtherPlugin(), "Big Boss", "10", Color.WHITE), "Big Boss"));
		assertFalse(PartyDefence.matches(box(null, "10", Color.WHITE), "Big Boss"));
		assertFalse(PartyDefence.matches(box("Big Boss", "10", Color.WHITE), null));
		assertFalse(PartyDefence.matches(null, "Big Boss"));
	}

	@Test
	public void picksTheBoxForTheOpponent()
	{
		final PartyDefence.Reading reading = PartyDefence.find(Arrays.asList(
			new VulnerabilityInfoBox(TRACKER, "Big Boss"),
			box("Small Boss", "5", Color.WHITE),
			box("", "7", Color.WHITE),
			box("<col=ffffff>Big Boss</col>", "", Color.WHITE),
			box("<col=ffffff>Big Boss</col>", "24", Color.RED)), "Big Boss");

		assertEquals(new PartyDefence.Reading("24", Color.RED), reading);
	}

	@Test
	public void findsNothingWithoutAMatch()
	{
		assertNull(PartyDefence.find(Collections.emptyList(), "Big Boss"));
		assertNull(PartyDefence.find(Collections.singletonList(box("Small Boss", "5", Color.WHITE)), "Big Boss"));
		assertNull(PartyDefence.find(Collections.singletonList(box("Big Boss", null, Color.WHITE)), "Big Boss"));
	}

	@Test
	public void keepsTheReadingForOneTickWhenTheBoxIsMissing()
	{
		final PartyDefence.HeldReading defence = new PartyDefence.HeldReading();
		final PartyDefence.Reading reading = new PartyDefence.Reading("24", Color.RED);

		defence.accept("Big Boss", reading);
		assertSame(reading, defence.readingFor("Big Boss"));

		defence.accept("Big Boss", null);
		assertSame(reading, defence.readingFor("Big Boss"));

		defence.accept("Big Boss", null);
		assertNull(defence.readingFor("Big Boss"));
	}

	@Test
	public void aNewReadingResetsTheMissedTick()
	{
		final PartyDefence.HeldReading defence = new PartyDefence.HeldReading();
		final PartyDefence.Reading first = new PartyDefence.Reading("24", Color.RED);
		final PartyDefence.Reading second = new PartyDefence.Reading("20", Color.RED);

		defence.accept("Big Boss", first);
		defence.accept("Big Boss", null);
		defence.accept("Big Boss", second);
		defence.accept("Big Boss", null);
		assertSame(second, defence.readingFor("Big Boss"));
	}

	@Test
	public void readingIsOnlyForTheSameName()
	{
		final PartyDefence.HeldReading defence = new PartyDefence.HeldReading();
		defence.accept("Big Boss", new PartyDefence.Reading("24", Color.RED));

		assertNull(defence.readingFor("Small Boss"));
		assertNull(defence.readingFor(null));

		defence.accept("Small Boss", null);
		assertNull(defence.readingFor("Big Boss"));
		assertNull(defence.readingFor("Small Boss"));
	}
}
