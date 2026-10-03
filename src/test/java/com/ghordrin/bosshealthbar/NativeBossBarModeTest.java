package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NativeBossBarModeTest
{
	@Test
	public void oldCheckboxMapsToTheMatchingMode()
	{
		assertEquals(NativeBossBarMode.REPLACE, NativeBossBarMode.fromReplaceSetting("true"));
		assertEquals(NativeBossBarMode.HIDE_OURS, NativeBossBarMode.fromReplaceSetting("false"));
		assertNull(NativeBossBarMode.fromReplaceSetting(null));
	}

	@Test
	public void migrationOverridesTheSavedDefaultButNotAnotherChoice()
	{
		assertEquals(NativeBossBarMode.HIDE_OURS, NativeBossBarMode.migrate("false", "REPLACE"));
		assertEquals(NativeBossBarMode.HIDE_OURS, NativeBossBarMode.migrate("false", null));
		assertEquals(NativeBossBarMode.REPLACE, NativeBossBarMode.migrate("true", "REPLACE"));
		assertNull(NativeBossBarMode.migrate("false", "BOTH"));
		assertNull(NativeBossBarMode.migrate("true", "HIDE_OURS"));
		assertNull(NativeBossBarMode.migrate(null, "REPLACE"));
	}

	@Test
	public void onlyReplaceHidesTheGameBarAndOnlyHideOursHidesOurs()
	{
		assertTrue(NativeBossBarMode.REPLACE.hidesGameBar());
		assertFalse(NativeBossBarMode.REPLACE.hidesOurBar());
		assertFalse(NativeBossBarMode.BOTH.hidesGameBar());
		assertFalse(NativeBossBarMode.BOTH.hidesOurBar());
		assertFalse(NativeBossBarMode.HIDE_OURS.hidesGameBar());
		assertTrue(NativeBossBarMode.HIDE_OURS.hidesOurBar());
	}
}
