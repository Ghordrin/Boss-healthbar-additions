package com.ghordrin.bosshealthbar.integrations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class OpponentInfoOverrideTest
{
	@Test
	public void onlyThatPluginsOwnOverlayCounts()
	{
		assertTrue(OpponentInfoOverride.isOpponentInfoBox("OpponentInfoOverlay", "Opponent Information"));
		assertFalse(OpponentInfoOverride.isOpponentInfoBox("OpponentInfoOverlay", "Some Other Plugin"));
		assertFalse(OpponentInfoOverride.isOpponentInfoBox("PlayerComparisonOverlay", "Opponent Information"));
		assertFalse(OpponentInfoOverride.isOpponentInfoBox("OpponentInfoOverlay", null));
	}
}
