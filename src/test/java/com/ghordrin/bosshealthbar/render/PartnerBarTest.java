package com.ghordrin.bosshealthbar.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PartnerBarTest
{
	@Test
	public void itIsLiveInAPreviewOrAGroupFightWhileTheSettingIsOn()
	{
		assertTrue(PartnerBar.isLive(true, true, false));
		assertTrue(PartnerBar.isLive(true, false, true));
		assertFalse(PartnerBar.isLive(true, false, false));
		assertFalse(PartnerBar.isLive(false, true, true));
	}

	@Test
	public void theBarIsAShareOfTheMainOnesHeightDownToAFloor()
	{
		assertEquals(20, PartnerBar.barThickness(40, 50));
		assertEquals(8, PartnerBar.barThickness(15, 50));
		assertEquals(4, PartnerBar.barThickness(6, 50));
		assertEquals(4, PartnerBar.barThickness(1, 50));
		assertEquals(40, PartnerBar.barThickness(40, 100));
		assertEquals(8, PartnerBar.barThickness(40, 20));
		assertEquals(10, PartnerBar.barThickness(16, 65));
	}

	// Half the height is what the bar had before the scale was a setting, so that share has to land on the same
	// pixel for every height.
	@Test
	public void halfTheHeightIsWhatTheBarUsedToBe()
	{
		for (int barHeight = 1; barHeight <= 64; barHeight++)
		{
			assertEquals(Math.max(4, Math.round(barHeight * 0.5f)), PartnerBar.barThickness(barHeight, 50));
		}
	}

	@Test
	public void theFloorHoldsAtSmallHeightsAndLowShares()
	{
		assertEquals(4, PartnerBar.barThickness(4, 20));
		assertEquals(4, PartnerBar.barThickness(16, 20));
		assertEquals(4, PartnerBar.barThickness(1, 100));
	}

	@Test
	public void theHeightIsTheRowTheBarAndTheRoomItsEndsReachInto()
	{
		// A flat bar's ends rise 2 above and below it.
		assertEquals(10 + 6 + 4, PartnerBar.height(10, 6, true, BarEnds.CLASSIC));
		assertEquals(10 + 6 + 4, PartnerBar.height(10, 6, true, BarEnds.SUBTLE));
		assertTrue(PartnerBar.height(10, 6, false, BarEnds.CLASSIC) > 10 + 6);
		assertTrue(PartnerBar.height(10, 6, false, BarEnds.SUBTLE) > 10 + 6);
	}
}
