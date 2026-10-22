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
	public void theBarIsHalfTheMainOnesHeightDownToAFloor()
	{
		assertEquals(20, PartnerBar.barThickness(40));
		assertEquals(8, PartnerBar.barThickness(15));
		assertEquals(4, PartnerBar.barThickness(6));
		assertEquals(4, PartnerBar.barThickness(1));
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
