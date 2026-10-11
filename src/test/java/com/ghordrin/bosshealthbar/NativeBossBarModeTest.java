package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NativeBossBarModeTest
{
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
