package com.ghordrin.bosshealthbar.render;

import static com.ghordrin.bosshealthbar.render.BarPainter.capRise;
import static com.ghordrin.bosshealthbar.render.BarPainter.capScale;
import static com.ghordrin.bosshealthbar.render.BarPainter.capWidth;
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

	// The reach of the shape drawn in BarPainter.subtleCap, scaled as it scales there: the point, the curled
	// tip above the frame line, and half the outline stroke around both.
	@Test
	public void subtleEndsHaveRoomForThePointAndTips()
	{
		for (int height = 4; height <= 24; height++)
		{
			// Kept apart from BarPainter.capScale so a wrong scale there can't move both sides together.
			final float s = 1f + Math.max(0, height - 8) * 0.09f;
			assertEquals(s, capScale(height), 0.0001f);
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
}
