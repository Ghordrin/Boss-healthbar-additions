package com.ghordrin.bosshealthbar.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class OpponentInfoTest
{
	@Test
	public void theNameGivenForTheHealthBarComesFirst()
	{
		assertEquals("Boss", OpponentInfo.barName("Boss", "Something else"));
		assertEquals("Boss", OpponentInfo.barName("<col=ff0000>Boss</col>", null));
	}

	@Test
	public void withoutOneTheGameBarsTitleIsUsed()
	{
		assertEquals("Boss", OpponentInfo.barName(null, "Boss"));
		assertEquals("Boss", OpponentInfo.barName("", "Boss"));
	}

	@Test
	public void withNeitherThereIsNoName()
	{
		assertNull(OpponentInfo.barName(null, null));
		assertNull(OpponentInfo.barName("", null));
	}
}
