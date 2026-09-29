package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class KnownBossesTest
{
	@Test
	public void matchesListedBossesIgnoringCase()
	{
		assertTrue(KnownBosses.contains("Zulrah"));
		assertTrue(KnownBosses.contains("zulrah"));
		assertTrue(KnownBosses.contains("K'RIL TSUTSAROTH"));
	}

	@Test
	public void rejectsOtherNamesAndNull()
	{
		assertFalse(KnownBosses.contains("Mithril dragon"));
		assertFalse(KnownBosses.contains(""));
		assertFalse(KnownBosses.contains(null));
	}
}
