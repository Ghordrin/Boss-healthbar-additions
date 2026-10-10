package com.ghordrin.bosshealthbar;

import java.awt.Font;
import net.runelite.client.ui.FontManager;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BarFontsTest
{
	@Test
	public void largeTextKeepsItsBaselineGap()
	{
		assertEquals(18, BarFonts.largeAbove(16));
		assertEquals(6, BarFonts.largeBelow(16));
		assertEquals(20, BarFonts.largeAbove(17));
		assertEquals(6, BarFonts.largeBelow(17));
		assertEquals(36, BarFonts.largeAbove(32));
		assertEquals(12, BarFonts.largeBelow(32));
		assertEquals(0, BarFonts.smallBelow(10, 12));
	}

	@Test
	public void smallTextRowsKeepTheirOldHeight()
	{
		final String small = FontManager.getRunescapeSmallFont().getFamily();
		assertEquals(16, BarFonts.smallRowHeight(16, true, small, Font.SERIF, 17));
		assertEquals(32, BarFonts.smallRowHeight(32, true, small, Font.SERIF, 17));
		// Small Serif 12 went with a Serif 17 name and a 17 high row.
		assertEquals(17, BarFonts.smallRowHeight(12, false, Font.SERIF, Font.SERIF, 17));
		assertEquals(17, BarFonts.smallRowHeight(12, false, Font.SANS_SERIF, Font.SERIF, 17));
		assertEquals(14, BarFonts.smallRowHeight(10, false, Font.SERIF, Font.SERIF, 17));
	}
}
