package com.ghordrin.bosshealthbar;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
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
			super(null, plugin);
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

	private static final Plugin TRACKER = new DefenceTrackerPlugin();

	private static DefenceInfoBox box(String tooltip, String text, Color color)
	{
		return new DefenceInfoBox(TRACKER, tooltip, text, color);
	}

	@Test
	public void boxesAreNamedLikeTheirs()
	{
		assertEquals(PartyDefence.BOX_NAME, box("Big Boss", "10", Color.WHITE).getName());
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
		final PartyDefence defence = new PartyDefence(null, null, null, null);
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
		final PartyDefence defence = new PartyDefence(null, null, null, null);
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
		final PartyDefence defence = new PartyDefence(null, null, null, null);
		defence.accept("Big Boss", new PartyDefence.Reading("24", Color.RED));

		assertNull(defence.readingFor("Small Boss"));
		assertNull(defence.readingFor((String) null));

		defence.accept("Small Boss", null);
		assertNull(defence.readingFor("Big Boss"));
		assertNull(defence.readingFor("Small Boss"));
	}
}
