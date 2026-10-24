package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.render.BarPainter.capRise;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.render.BarEnds;
import com.ghordrin.bosshealthbar.render.PartnerBar;
import java.awt.Dimension;
import org.junit.Test;

public class PartnerBarOverlayTest
{
	@Test
	public void theWidthFollowsTheDraggedSizeDownToAFloor()
	{
		assertEquals(600, PartnerBarOverlay.barWidth(null, 600));
		assertEquals(300, PartnerBarOverlay.barWidth(new Dimension(300, 40), 600));
		assertEquals(80, PartnerBarOverlay.barWidth(new Dimension(10, 40), 600));
	}

	@Test
	public void withoutADraggedSizeTheThicknessComesFromTheSetting()
	{
		assertEquals(9, PartnerBarOverlay.barThickness(null, 14, true, BarEnds.CLASSIC, 9));
	}

	@Test
	public void theThicknessIsWhatTheDraggedBoxLeavesAfterTheRowAndTheEnds()
	{
		// A flat bar's ends rise 2 above and below it.
		assertEquals(10, PartnerBarOverlay.barThickness(new Dimension(300, 14 + 4 + 10), 14, true,
			BarEnds.CLASSIC, 9));
		assertEquals(30, PartnerBarOverlay.barThickness(new Dimension(300, 4 + 30), 0, true, BarEnds.SUBTLE, 9));
	}

	@Test
	public void aBoxTooSmallToDrawInStillLeavesTheSmallestBar()
	{
		assertEquals(PartnerBar.MIN_HEIGHT, PartnerBarOverlay.barThickness(new Dimension(300, 4), 14, true,
			BarEnds.CLASSIC, 9));
		assertEquals(PartnerBar.MIN_HEIGHT, PartnerBarOverlay.barThickness(new Dimension(300, 0), 14, true,
			BarEnds.CLASSIC, 9));
		assertEquals(PartnerBar.MIN_HEIGHT, PartnerBarOverlay.barThickness(new Dimension(0, -20), 0, false,
			BarEnds.CLASSIC, 9));
	}

	// The ends of a tall bar reach further, so the thickness has to be the largest one that still fits.
	@Test
	public void aTallBoxFitsTheBarAndTheRoomItsEndsNeed()
	{
		for (final BarEnds ends : BarEnds.values())
		{
			for (int boxHeight = 30; boxHeight <= 400; boxHeight += 7)
			{
				final int thickness = PartnerBarOverlay.barThickness(new Dimension(300, boxHeight), 14, false,
					ends, 9);
				assertTrue(boxHeight(thickness, ends) <= boxHeight);
				assertTrue(boxHeight(thickness + 1, ends) > boxHeight);
			}
		}
	}

	private static int boxHeight(int thickness, BarEnds ends)
	{
		return 14 + capRise(thickness, false, ends) * 2 + thickness;
	}
}
