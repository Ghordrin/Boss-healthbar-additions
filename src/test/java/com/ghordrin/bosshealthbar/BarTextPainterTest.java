package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BarTextPainterTest
{
	@Test
	public void thePartnerRowFitsTheHitpointsFontWhileItHasText()
	{
		assertEquals(14, BarTextPainter.partnerRowHeight(true, false, 10, 4));
		assertEquals(14, BarTextPainter.partnerRowHeight(false, true, 10, 4));
		assertEquals(0, BarTextPainter.partnerRowHeight(false, false, 10, 4));
	}

	@Test
	public void theSlotSizingSwapsEveryDigit()
	{
		assertEquals("0:00", BarTextPainter.withDigits("1:23", '0'));
		assertEquals("4:44:44", BarTextPainter.withDigits("1:05:09", '4'));
	}
}
