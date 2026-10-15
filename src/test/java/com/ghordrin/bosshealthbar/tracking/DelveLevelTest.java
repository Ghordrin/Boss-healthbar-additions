package com.ghordrin.bosshealthbar.tracking;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DelveLevelTest
{
	// The varp counts finished delves, so it reads one behind the level being fought.
	@Test
	public void theLevelIsOnePastTheDelvesFinished()
	{
		assertEquals(1, DelveLevel.levelFor(0));
		assertEquals(2, DelveLevel.levelFor(1));
		assertEquals(9, DelveLevel.levelFor(8));
	}

	@Test
	public void anUnsetVarpStillReadsAsTheFirstDelve()
	{
		assertEquals(1, DelveLevel.levelFor(-1));
	}

	@Test
	public void theLevelReadsAsTheGameWordsIt()
	{
		assertEquals("Delve 1", DelveLevel.text(1));
		assertEquals("Delve 8", DelveLevel.text(8));
		assertEquals("Delve 14", DelveLevel.text(14));
	}
}
