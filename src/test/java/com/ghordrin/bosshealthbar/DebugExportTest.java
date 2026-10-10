package com.ghordrin.bosshealthbar;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DebugExportTest
{
	@Test
	public void namesFilesByTime()
	{
		assertEquals("boss-health-bar-debug-20260102-130405.txt",
			DebugExport.fileName(LocalDateTime.of(2026, 1, 2, 13, 4, 5)));
	}

	@Test
	public void keepsTheNewestOwnFiles()
	{
		final List<String> names = new ArrayList<>();
		for (int i = 12; i >= 1; i--)
		{
			names.add(String.format("boss-health-bar-debug-202601%02d-120000.txt", i));
		}
		names.add("notes.txt");
		names.add("boss-health-bar-debug-old.txt");
		assertEquals(Arrays.asList("boss-health-bar-debug-20260101-120000.txt", "boss-health-bar-debug-20260102-120000.txt"),
			DebugExport.namesToDelete(names, 10));
		assertTrue(DebugExport.namesToDelete(names.subList(0, 10), 10).isEmpty());
		assertTrue(DebugExport.namesToDelete(Collections.singletonList("notes.txt"), 0).isEmpty());
	}
}
