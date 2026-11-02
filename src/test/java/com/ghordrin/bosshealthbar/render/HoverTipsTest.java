package com.ghordrin.bosshealthbar.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class HoverTipsTest
{
	@Test
	public void findsTheTipUnderThePoint()
	{
		final HoverTips tips = new HoverTips();
		tips.add(10, 0, 30, 12, "hammer");
		tips.add(36, 0, 60, 12, "godsword");

		assertEquals("hammer", tips.tipAt(10, 0));
		assertEquals("hammer", tips.tipAt(29, 11));
		assertEquals("godsword", tips.tipAt(40, 6));
		assertNull(tips.tipAt(30, 6));
		assertNull(tips.tipAt(20, 12));
		assertNull(tips.tipAt(9, 6));
	}

	@Test
	public void clearingForgetsEveryTip()
	{
		final HoverTips tips = new HoverTips();
		tips.add(0, 0, 10, 10, "hammer");
		tips.clear();

		assertNull(tips.tipAt(5, 5));
	}

	@Test
	public void keepsAnyNumberOfTips()
	{
		final HoverTips tips = new HoverTips();
		for (int i = 0; i < 20; i++)
		{
			tips.add(i * 10, 0, i * 10 + 10, 10, "tip " + i);
		}

		assertEquals("tip 0", tips.tipAt(5, 5));
		assertEquals("tip 19", tips.tipAt(195, 5));
	}

	@Test
	public void skipsEmptyTipsAndAreas()
	{
		final HoverTips tips = new HoverTips();
		tips.add(0, 0, 10, 10, null);
		tips.add(0, 0, 10, 10, "");
		tips.add(0, 0, 0, 10, "flat");

		assertNull(tips.tipAt(0, 5));
	}
}
