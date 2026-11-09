package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BossHealthBarOverlayTest
{
	@Test
	public void theBarCountsAsDrawnThroughAShortGap()
	{
		assertTrue(BossHealthBarOverlay.drawnRecently(1_000_000_000L, 1_000_000_000L));
		assertTrue(BossHealthBarOverlay.drawnRecently(1_400_000_000L, 1_000_000_000L));
		assertFalse(BossHealthBarOverlay.drawnRecently(1_500_000_000L, 1_000_000_000L));
	}

	@Test
	public void flatThemesNeverTurnGold()
	{
		assertFalse(BossHealthBarOverlay.showsGold(true, true));
		assertTrue(BossHealthBarOverlay.showsGold(true, false));
		assertFalse(BossHealthBarOverlay.showsGold(false, false));
	}








}
