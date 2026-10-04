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

	@Test
	public void defeatEndsAfterTheFadeOrTheBurn()
	{
		final long hold = BarAnimation.DEFEAT_HOLD.toNanos();
		for (boolean burnAway : new boolean[]{false, true})
		{
			final BarAnimation animation = new BarAnimation();
			animation.startDefeat(1_000L);
			final long end = 1_000L + hold + (burnAway ? BarEffects.BURN_DURATION : BarAnimation.DEFEAT_FADE).toNanos();
			assertEquals(1f, animation.defeatOpacity(1_000L + hold, burnAway), 0.001f);
			assertTrue(animation.defeatOpacity(end - 1_000_000L, burnAway) >= 0f);
			assertEquals(-1f, animation.defeatOpacity(end, burnAway), 0f);
		}
	}

	@Test
	public void burnAwayFadesOnlyTheText()
	{
		final BarAnimation animation = new BarAnimation();
		animation.startDefeat(1_000L);
		final long halfway = 1_000L + BarAnimation.DEFEAT_HOLD.toNanos() + BarAnimation.DEFEAT_FADE.toNanos() / 2;

		assertEquals(0.5f, animation.defeatOpacity(halfway, false), 0.01f);
		assertEquals(1f, animation.defeatTextOpacity(halfway, false), 0f);
		assertEquals(1f, animation.defeatOpacity(halfway, true), 0f);
		assertEquals(0.5f, animation.defeatTextOpacity(halfway, true), 0.01f);
		assertEquals(BarAnimation.DEFEAT_FADE.toMillis() / 2, animation.defeatEffectMillis(halfway));
	}

	@Test
	public void noDefeatMeansNoDefeatEffect()
	{
		final BarAnimation animation = new BarAnimation();
		assertEquals(1f, animation.defeatOpacity(5_000_000_000L, true), 0f);
		assertEquals(1f, animation.defeatTextOpacity(5_000_000_000L, true), 0f);
		assertTrue(animation.defeatEffectMillis(5_000_000_000L) < 0);
	}
}
