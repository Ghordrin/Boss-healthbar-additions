package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BarAnimationTest
{
	@Test
	public void lostHealthLeavesATrail()
	{
		final BarAnimation animation = new BarAnimation();
		animation.tick(1f, 1f, true, System.currentTimeMillis());
		animation.tick(0.4f, 1f, true, System.currentTimeMillis());

		assertEquals(0.4f, animation.getDisplayedFraction(), 0.001f);
		assertTrue(animation.getTrailFraction() > 0.9f);
	}

	@Test
	public void retargetJumpsToTheNewHealthWithoutATrail()
	{
		final BarAnimation animation = new BarAnimation();
		animation.tick(0.4f, 1f, true, System.currentTimeMillis());
		animation.retarget();
		animation.tick(1f, 1f, true, System.currentTimeMillis());

		assertEquals(1f, animation.getDisplayedFraction(), 0.001f);
		assertEquals(1f, animation.getTrailFraction(), 0.001f);

		animation.retarget();
		animation.tick(0.2f, 1f, true, System.currentTimeMillis());

		assertEquals(0.2f, animation.getDisplayedFraction(), 0.001f);
		assertEquals(0.2f, animation.getTrailFraction(), 0.001f);
	}
}
