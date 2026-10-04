package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BarPainter.capRise;
import static com.ghordrin.bosshealthbar.BarPainter.capScale;
import static com.ghordrin.bosshealthbar.BarPainter.capWidth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BarEndsTest
{
	@Test
	public void subtleEndsAtTheReferenceHeight()
	{
		assertEquals(5, capWidth(8, false, BarEnds.SUBTLE));
		assertEquals(4, capRise(8, false, BarEnds.SUBTLE));
	}

	@Test
	public void subtleEndsLeaveTheBarWiderThanClassic()
	{
		for (int height = 4; height <= 24; height++)
		{
			assertTrue(capWidth(height, false, BarEnds.SUBTLE) < capWidth(height, false, BarEnds.CLASSIC));
		}
	}

	@Test
	public void subtleEndsHaveRoomForThePointAndTips()
	{
		for (int height = 4; height <= 24; height++)
		{
			final float s = capScale(height);
			// The point plus half the outline stroke, and the curled tip above the frame line plus half the outline.
			assertTrue(capWidth(height, false, BarEnds.SUBTLE) >= 3.6f * s + 1.2f * s);
			assertTrue(capRise(height, false, BarEnds.SUBTLE) >= 2.4f * s + 1.2f * s - 0.5f);
		}
	}

	@Test
	public void subtleEndsGrowWithBarHeight()
	{
		assertTrue(capWidth(24, false, BarEnds.SUBTLE) > capWidth(8, false, BarEnds.SUBTLE));
		assertTrue(capRise(24, false, BarEnds.SUBTLE) > capRise(8, false, BarEnds.SUBTLE));
	}

	@Test
	public void labelsMatchTheDropdown()
	{
		assertEquals("Classic", BarEnds.CLASSIC.toString());
		assertEquals("Subtle", BarEnds.SUBTLE.toString());
	}
}
