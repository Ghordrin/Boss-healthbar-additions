package com.ghordrin.bosshealthbar;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DebugLogTest
{
	private final DebugLog debugLog = new DebugLog(null);

	@Test
	public void formatsArgumentsIncludingNulls()
	{
		debugLog.add("Opponent {} at {}/{}", null, 5, null);
		final List<DebugLog.Entry> entries = debugLog.snapshot();
		assertEquals(1, entries.size());
		assertEquals("Opponent null at 5/null", entries.get(0).message);
		assertEquals(DebugLog.NO_TICK, entries.get(0).tick);
	}

	@Test
	public void dropsTheOldestPastCapacity()
	{
		for (int i = 0; i < DebugLog.CAPACITY + 5; i++)
		{
			debugLog.add("Event {}", i);
		}
		final List<DebugLog.Entry> entries = debugLog.snapshot();
		assertEquals(DebugLog.CAPACITY, entries.size());
		assertEquals("Event 5", entries.get(0).message);
		assertEquals("Event " + (DebugLog.CAPACITY + 4), entries.get(entries.size() - 1).message);
	}

	@Test
	public void collapsesIdenticalConsecutiveMessages()
	{
		debugLog.add(1000, 7, "Same");
		debugLog.add(2000, 9, "Same");
		debugLog.add(3000, 10, "Other");
		debugLog.add(4000, 11, "Same");
		final List<DebugLog.Entry> entries = debugLog.snapshot();
		assertEquals(3, entries.size());
		assertEquals(2, entries.get(0).repeats);
		assertEquals(2000, entries.get(0).millis);
		assertEquals(9, entries.get(0).tick);
		assertEquals(1, entries.get(2).repeats);
	}

	@Test
	public void rendersHeaderThenEventsInOrder()
	{
		final long millis = LocalDateTime.of(2026, 1, 2, 13, 4, 5, 6_000_000).toInstant(ZoneOffset.UTC).toEpochMilli();
		final List<DebugLog.Entry> entries = Arrays.asList(
			new DebugLog.Entry(millis, DebugLog.NO_TICK, "First", 1),
			new DebugLog.Entry(millis + 600, 42, "Second", 3));
		final String text = DebugLog.render(Arrays.asList("Title", "Line"), entries, ZoneOffset.UTC);
		assertEquals("Title\nLine\n\nRecent events (oldest first)\n"
			+ "13:04:05.006  tick -  First\n"
			+ "13:04:05.606  tick 42  Second (x 3)\n", text);
	}

	@Test
	public void describesNpcsAndNothing()
	{
		assertEquals("Boss (id 1, form 2, index 3)", DebugLog.describeNpc("Boss", 1, 2, 3));
		assertEquals("none", DebugLog.describe(null));
	}

	@Test
	public void cutsLongValues()
	{
		assertEquals("abc", DebugLog.cut("abc", 3));
		assertEquals("ab...", DebugLog.cut("abc", 2));
		assertEquals("null", DebugLog.cut(null, 2));
	}
}
