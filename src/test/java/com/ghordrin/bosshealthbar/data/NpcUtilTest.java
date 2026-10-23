package com.ghordrin.bosshealthbar.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NpcUtilTest
{
	@Test
	public void aNameTheClientDoesNotHaveIsNoName()
	{
		assertFalse(NpcUtil.hasName(null));
		assertFalse(NpcUtil.hasName(""));
		// How the client spells it, and what the NPC behind a pair's health bars is called.
		assertFalse(NpcUtil.hasName("null"));
	}

	@Test
	public void aRealNameIsAName()
	{
		assertTrue(NpcUtil.hasName("Bork"));
		assertTrue(NpcUtil.hasName("The Smorks"));
		assertTrue(NpcUtil.hasName("Nulled Warrior"));
	}
}
