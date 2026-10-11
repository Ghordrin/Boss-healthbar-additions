package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BarPainter.capScale;
import static com.ghordrin.bosshealthbar.BarPainter.scaledCapWidth;
import static com.ghordrin.bosshealthbar.BarPainter.scaledFrameStroke;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BarPainterTest
{
	@Test
	public void capsOnlyGrowOnceTheBarIsTallerThanTheReferenceHeight()
	{
		assertEquals(1f, capScale(4), 0.0001f);
		assertEquals(1f, capScale(8), 0.0001f);
		assertTrue(capScale(24) > capScale(9));
		assertTrue(capScale(9) > capScale(8));
	}

	@Test
	public void scaledCapWidthGrowsWithBarHeight()
	{
		assertEquals(scaledCapWidth(4), scaledCapWidth(8));
		assertTrue(scaledCapWidth(24) > scaledCapWidth(8));
	}

	@Test
	public void scaledFrameStrokeNeverGoesBelowAHairline()
	{
		assertEquals(1f, scaledFrameStroke(4), 0.0001f);
		assertEquals(1f, scaledFrameStroke(8), 0.0001f);
		assertTrue(scaledFrameStroke(24) > 1f);
	}
}
