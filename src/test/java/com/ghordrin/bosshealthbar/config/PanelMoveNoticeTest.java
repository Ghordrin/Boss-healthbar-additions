package com.ghordrin.bosshealthbar.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PanelMoveNoticeTest
{
	// A changed name would show the notice again to everyone who has already seen it.
	@Test
	public void keyIsUnchanged()
	{
		assertEquals("panelMoveTold", PanelMoveNotice.TOLD_KEY);
	}

	@Test
	public void tellsWhenNothingIsSaved()
	{
		assertTrue(PanelMoveNotice.shouldTell(null, false));
	}

	@Test
	public void doesNotTellTwiceEver()
	{
		assertFalse(PanelMoveNotice.shouldTell("true", false));
	}

	@Test
	public void doesNotTellTwiceInASession()
	{
		assertFalse(PanelMoveNotice.shouldTell(null, true));
	}
}
