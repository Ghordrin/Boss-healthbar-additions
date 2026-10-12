package com.ghordrin.bosshealthbar.integrations;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PluginReaderTest
{
	@Test
	public void keepsTheReadingForOneTickWhenTheBoxIsMissing()
	{
		final PluginReader.HeldReading<PartyDefence.Reading> defence = new PluginReader.HeldReading<>();
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
		final PluginReader.HeldReading<PartyDefence.Reading> defence = new PluginReader.HeldReading<>();
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
		final PluginReader.HeldReading<PartyDefence.Reading> defence = new PluginReader.HeldReading<>();
		defence.accept("Big Boss", new PartyDefence.Reading("24", Color.RED));

		assertNull(defence.readingFor("Small Boss"));
		assertNull(defence.readingFor(null));

		defence.accept("Small Boss", null);
		assertNull(defence.readingFor("Big Boss"));
		assertNull(defence.readingFor("Small Boss"));
	}

	@Test
	public void keepsAListReadingForOneTick()
	{
		final PluginReader.HeldReading<List<String>> counts = new PluginReader.HeldReading<>();
		final List<String> found = Arrays.asList("2");

		counts.accept(null, found);
		assertSame(found, counts.reading());

		counts.accept(null, null);
		assertSame(found, counts.reading());

		counts.accept(null, null);
		assertNull(counts.reading());
	}

	@Test
	public void aNewListResetsTheMissedTick()
	{
		final PluginReader.HeldReading<List<String>> counts = new PluginReader.HeldReading<>();
		final List<String> first = Arrays.asList("2");
		final List<String> second = Arrays.asList("3");

		counts.accept(null, first);
		counts.accept(null, null);
		counts.accept(null, second);
		counts.accept(null, null);
		assertSame(second, counts.reading());
	}

	@Test
	public void unchangedReadingsKeepTheSameList()
	{
		final PluginReader.HeldReading<List<String>> counts = new PluginReader.HeldReading<>();
		final List<String> first = Arrays.asList("2");

		counts.accept(null, first);
		counts.accept(null, Arrays.asList("2"));
		assertSame(first, counts.reading());
	}
}
