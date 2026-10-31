package com.ghordrin.bosshealthbar.integrations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import org.junit.Test;

public class KillMomentsTest
{
	private static KillMoments moments(boolean milestones, boolean personalBests)
	{
		return new KillMoments(new BossHealthBarConfig()
		{
			@Override
			public boolean killMilestones()
			{
				return milestones;
			}

			@Override
			public boolean personalBests()
			{
				return personalBests;
			}
		});
	}

	@Test
	public void milestonesAreTheFirstKillAndEveryRoundNumber()
	{
		assertTrue(KillMoments.isMilestone(1, 100));
		assertTrue(KillMoments.isMilestone(100, 100));
		assertTrue(KillMoments.isMilestone(2500, 100));
		assertTrue(KillMoments.isMilestone(50, 25));
		assertFalse(KillMoments.isMilestone(99, 100));
		assertFalse(KillMoments.isMilestone(101, 100));
	}

	@Test
	public void ordinalsReadRight()
	{
		assertEquals("1st", KillMoments.ordinal(1));
		assertEquals("2nd", KillMoments.ordinal(2));
		assertEquals("3rd", KillMoments.ordinal(3));
		assertEquals("11th", KillMoments.ordinal(11));
		assertEquals("12th", KillMoments.ordinal(12));
		assertEquals("13th", KillMoments.ordinal(13));
		assertEquals("100th", KillMoments.ordinal(100));
		assertEquals("101st", KillMoments.ordinal(101));
		assertEquals("1,000th", KillMoments.ordinal(1000));
	}

	@Test
	public void aMilestoneShowsOnItsOwnBossForAWhile()
	{
		final KillMoments moments = moments(true, true);
		moments.killCountSaved("vorkath", "99", "100", 1_000);
		assertEquals("100th kill", moments.labelFor("vorkath", 3_000));
		assertNull(moments.labelFor("zulrah", 3_000));
		assertNull(moments.labelFor("vorkath", 20_000));
	}

	@Test
	public void aMilestoneAndAPersonalBestShowTogether()
	{
		final KillMoments moments = moments(true, true);
		moments.personalBestSaved("vorkath", "95.4", "94.2", 1_000);
		assertEquals("New PB", moments.labelFor("vorkath", 1_500));
		moments.killCountSaved("vorkath", "99", "100", 1_200);
		assertEquals("100th kill, New PB", moments.labelFor("vorkath", 1_500));
	}

	@Test
	public void onlyACountGoingUpToAMilestoneCounts()
	{
		final KillMoments moments = moments(true, true);
		moments.killCountSaved("vorkath", "100", "100", 1_000);
		moments.killCountSaved("vorkath", "150", "101", 1_000);
		moments.killCountSaved("vorkath", "98", "99", 1_000);
		moments.killCountSaved("vorkath", null, "not a number", 1_000);
		assertNull(moments.labelFor("vorkath", 1_500));

		moments.killCountSaved("vorkath", null, "1", 1_000);
		assertEquals("1st kill", moments.labelFor("vorkath", 1_500));
	}

	@Test
	public void anOldMomentDoesNotJoinANewOne()
	{
		final KillMoments moments = moments(true, true);
		moments.personalBestSaved("vorkath", "95.4", "94.2", 1_000);
		moments.killCountSaved("vorkath", "199", "200", 60_000);
		assertEquals("200th kill", moments.labelFor("vorkath", 60_500));
	}

	@Test
	public void onlyAFasterTimeIsANewBest()
	{
		final KillMoments moments = moments(true, true);
		moments.personalBestSaved("vorkath", null, "94.2", 1_000);
		moments.personalBestSaved("vorkath", "90.0", "94.2", 1_000);
		moments.personalBestSaved("vorkath", "94.2", "94.2", 1_000);
		moments.personalBestSaved("vorkath", "94.2", null, 1_000);
		assertNull(moments.labelFor("vorkath", 1_500));
	}

	@Test
	public void switchedOffMomentsStayQuiet()
	{
		final KillMoments moments = moments(false, false);
		moments.killCountSaved("vorkath", "99", "100", 1_000);
		moments.personalBestSaved("vorkath", "95.4", "94.2", 1_000);
		assertNull(moments.labelFor("vorkath", 1_500));
	}
}
