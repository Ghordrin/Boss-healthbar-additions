package com.ghordrin.bosshealthbar;

import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FightGroupTest
{
	private final FightGroup group = new FightGroup(null, null, new DebugLog(null));

	@Test
	public void opponentsOfTheSameFightJoinItAndANewFightStartsEmpty()
	{
		final int first = group.id();
		group.startFight(null, false, 0);
		assertNotEquals(first, group.id());
		group.join(1, "Boss", true, 0);
		group.join(2, "Minion", false, 5);
		group.join(1, "Boss", true, 10);
		assertEquals(2, group.size());

		final int fight = group.id();
		group.startFight(null, false, 20);
		assertNotEquals(fight, group.id());
		assertEquals(0, group.size());
	}

	@Test
	public void aMemberThatLeavesAliveIsKeptAwayWithItsHealth()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		FightGroup.read(member, 0, 0, 12, 30, 450);
		group.despawned(member, false);

		assertTrue(member.isAway());
		assertEquals(1, group.size());
		assertEquals(12, member.ratio);
		assertTrue(group.isAliveMember(7, "Boss", false));
	}

	@Test
	public void aMemberThatDiesIsRemoved()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		group.despawned(member, true);
		assertEquals(0, group.size());
		assertFalse(group.isAliveMember(1, "Boss", false));

		final FightGroup.Member zero = group.join(2, "Boss", true, 0);
		FightGroup.read(zero, 0, 0, 0, 30, 450);
		FightGroup.updateDead(zero, false);
		group.despawned(zero, false);
		assertEquals(0, group.size());
	}

	@Test
	public void aDeathReadingIsZeroOrALastBitOfHealthItCanNoLongerBeAttackedAt()
	{
		final FightGroup.Member member = new FightGroup.Member();
		FightGroup.read(member, 0, 0, 0, 30, 450);
		assertTrue(FightGroup.diedReading(member, true));

		FightGroup.read(member, 0, 0, 1, 60, 450);
		assertTrue(FightGroup.diedReading(member, false));
		assertFalse(FightGroup.diedReading(member, true));

		FightGroup.read(member, 1, 450, 0, 0, 450);
		assertFalse(FightGroup.diedReading(member, false));

		assertFalse(FightGroup.diedReading(new FightGroup.Member(), false));
	}

	@Test
	public void aDeadNpcIsNotAnAliveMember()
	{
		group.join(1, "Boss", true, 0);
		assertTrue(group.isAliveMember(1, "Boss", false));
		assertFalse(group.isAliveMember(1, "Boss", true));
		assertFalse(group.isAliveMember(2, "Other", false));
	}

	@Test
	public void aReturningBossTakesBackTheOnlyAwayMemberWithItsName()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		FightGroup.read(member, 0, 0, 12, 30, 450);
		group.despawned(member, false);

		assertSame(member, group.join(9, "Boss", true, 50));
		assertFalse(member.isAway());
		assertEquals(12, member.ratio);
		assertEquals(1, group.size());
		assertSame(member, group.find(9));
	}

	@Test
	public void twoAwayMembersWithTheSameNameAreNotGuessed()
	{
		final FightGroup.Member first = group.join(1, "Boss", true, 0);
		final FightGroup.Member second = group.join(2, "Boss", true, 0);
		group.despawned(first, false);
		group.despawned(second, false);

		assertNull(group.awayByName("Boss"));
		assertFalse(group.isAliveMember(9, "Boss", false));
		assertNotSame(first, group.join(9, "Boss", true, 10));
		assertEquals(3, group.size());
	}

	@Test
	public void onlyABossIsTakenBackByName()
	{
		final FightGroup.Member minion = group.join(1, "Minion", false, 0);
		group.despawned(minion, false);
		assertNull(group.awayByName("Minion"));
	}

	@Test
	public void thePartnerIsTheMostRecentlyFoughtOtherBossWithHealth()
	{
		final FightGroup.Member own = group.join(1, "First", true, 30);
		final FightGroup.Member older = group.join(2, "Second", true, 10);
		final FightGroup.Member newer = group.join(3, "Third", true, 20);
		final FightGroup.Member minion = group.join(4, "Minion", false, 25);
		final FightGroup.Member noHealth = group.join(5, "Fifth", true, 28);
		for (FightGroup.Member member : new FightGroup.Member[]{own, older, newer, minion})
		{
			FightGroup.read(member, 0, 0, 20, 30, 450);
		}

		assertSame(newer, group.choosePartner(own));
		newer.dead = true;
		assertSame(older, group.choosePartner(own));
		assertNull(noHealth.state);
	}

	@Test
	public void anAwayBossCanBeThePartner()
	{
		final FightGroup.Member own = group.join(1, "First", true, 30);
		final FightGroup.Member away = group.join(2, "Second", true, 20);
		FightGroup.read(away, 0, 0, 20, 30, 450);
		group.despawned(away, false);

		group.updatePartner(own, true);
		assertSame(away, group.partner());
		assertEquals("Second", group.partner().state.name);
	}

	@Test
	public void anOpponentThatIsNotABossHasNoPartner()
	{
		final FightGroup.Member own = group.join(1, "Minion", false, 30);
		final FightGroup.Member boss = group.join(2, "Boss", true, 20);
		FightGroup.read(boss, 0, 0, 20, 30, 450);
		assertNull(group.choosePartner(own));
		assertNull(group.choosePartner(null));
	}

	@Test
	public void thePartnerStaysWhileThereIsNoOpponent()
	{
		final FightGroup.Member own = group.join(1, "First", true, 30);
		final FightGroup.Member other = group.join(2, "Second", true, 20);
		FightGroup.read(other, 0, 0, 20, 30, 450);
		group.updatePartner(own, true);
		group.updatePartner(null, false);
		assertSame(other, group.partner());

		group.despawned(other, true);
		assertNull(group.partner());
	}

	@Test
	public void theOldestIsDroppedWhenFull()
	{
		for (int i = 0; i < FightGroup.MAX_MEMBERS; i++)
		{
			group.join(i, "Npc " + i, false, 100 - i);
		}
		group.join(50, "New", false, 200);
		assertEquals(FightGroup.MAX_MEMBERS, group.size());
		assertNull(group.find(FightGroup.MAX_MEMBERS - 1));
		assertEquals("New", group.find(50).name);
	}

	@Test
	public void aGroupFightLastsUntilTheNextFight()
	{
		final FightGroup.Member first = group.join(1, "First", true, 0);
		group.join(2, "Minion", false, 0);
		assertFalse(group.isGroupFight());

		group.join(3, "Second", true, 5);
		assertTrue(group.isGroupFight());
		group.despawned(first, true);
		assertTrue(group.isGroupFight());

		group.startFight(null, false, 50);
		assertFalse(group.isGroupFight());
	}

	@Test
	public void aMinionThatTurnsOutToBeABossCountsOnce()
	{
		group.join(1, "First", true, 0);
		group.join(2, "Second", false, 0);
		assertFalse(group.isGroupFight());
		group.join(2, "Second", true, 1);
		group.join(2, "Second", true, 2);
		assertTrue(group.isGroupFight());
	}

	@Test
	public void restoringAnotherFightStartsFromItsMember()
	{
		group.join(1, "Boss", true, 0);
		final int fight = group.id();
		group.startFight(null, false, 10);
		group.join(2, "Other", true, 10);

		group.restore(fight, null, true, 20);
		assertEquals(fight, group.id());
		assertEquals(0, group.size());

		group.join(3, "Third", true, 20);
		group.restore(fight, null, true, 30);
		assertEquals(1, group.size());
	}

	@Test
	public void awayMembersAreForgottenAfterAWhile()
	{
		final FightGroup.Member away = group.join(1, "Boss", true, 0);
		final FightGroup.Member present = group.join(2, "Other", true, 0);
		group.despawned(away, false);

		group.expireAway(BossMemory.FORGET_TICKS);
		assertEquals(2, group.size());
		group.expireAway(BossMemory.FORGET_TICKS + 1);
		assertEquals(1, group.size());
		assertSame(present, group.find(2));
	}

	@Test
	public void aZeroReadingIsDeadAndAFreshOneIsAliveAgain()
	{
		final FightGroup.Member member = new FightGroup.Member();
		FightGroup.updateDead(member, false);
		assertFalse(member.dead);

		FightGroup.read(member, 0, 0, 0, 30, 450);
		FightGroup.updateDead(member, false);
		assertTrue(member.dead);

		FightGroup.read(member, 0, 0, 5, 30, 450);
		FightGroup.updateDead(member, false);
		assertFalse(member.dead);

		FightGroup.updateDead(member, true);
		assertTrue(member.dead);
	}

	@Test
	public void theGameBarWinsOverTheOverheadBarWhichWinsOverTheLastReading()
	{
		final FightGroup.Member member = new FightGroup.Member();

		assertTrue(FightGroup.read(member, 300, 450, 20, 30, 450));
		assertReading(member, 300, 450, true, 450);

		assertTrue(FightGroup.read(member, 0, 0, 15, 30, 450));
		assertReading(member, 15, 30, false, 450);

		// The overhead bar went away, so the last reading stays.
		assertFalse(FightGroup.read(member, 0, 0, -1, -1, 450));
		assertReading(member, 15, 30, false, 450);
	}

	@Test
	public void theStateIsOnlyRebuiltWhenTheReadingChanges()
	{
		final FightGroup.Member member = new FightGroup.Member();
		FightGroup.read(member, 0, 0, 15, 30, 450);
		member.state = new BarState("Test", 0, 450, 15, 30, false, false, BarState.NO_PHASE_MARKERS,
			HealthIndicatorMarkers.NONE, null, null, Collections.emptyList(), null);
		final BarState state = member.state;

		assertFalse(FightGroup.read(member, 0, 0, 15, 30, 450));
		assertSame(state, member.state);

		assertTrue(FightGroup.read(member, 0, 0, 14, 30, 450));
		assertNull(member.state);
	}

	@Test
	public void thePartnerRowFitsTheHitpointsFontWhileItHasText()
	{
		assertEquals(14, BarTextPainter.partnerRowHeight(true, false, 10, 4));
		assertEquals(14, BarTextPainter.partnerRowHeight(false, true, 10, 4));
		assertEquals(0, BarTextPainter.partnerRowHeight(false, false, 10, 4));
	}

	private static void assertReading(FightGroup.Member member, int ratio, int scale, boolean exact, Integer maxHealth)
	{
		assertEquals(ratio, member.ratio);
		assertEquals(scale, member.scale);
		assertEquals(exact, member.exact);
		assertEquals(maxHealth, member.maxHealth);
	}
}
