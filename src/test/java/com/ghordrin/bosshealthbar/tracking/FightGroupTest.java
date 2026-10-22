package com.ghordrin.bosshealthbar.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.data.BarState;
import com.ghordrin.bosshealthbar.data.Health;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class FightGroupTest
{
	private final FightGroup group = new FightGroup(null, new DebugLog(null));

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
		FightGroup.read(member, overhead(12, 30));
		group.despawned(member, false);

		assertTrue(member.isAway());
		assertEquals(1, group.size());
		assertEquals(12, member.ratio);
		assertTrue(group.isAliveMember(7, "Boss"));
	}

	@Test
	public void aMemberThatDiesIsRemoved()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		group.despawned(member, true);
		assertEquals(0, group.size());
		assertFalse(group.isAliveMember(1, "Boss"));

		final FightGroup.Member confirmed = group.join(2, "Boss", true, 0);
		confirmed.downState.update(overheadReading(1, 60), false, true, false, 1, null, null, null);
		confirmed.downState.update(overheadReading(1, 60), false, true, false, 2, null, null, null);
		group.despawned(confirmed, false);
		assertEquals(0, group.size());
	}

	@Test
	public void aDeathReadingIsZeroOrALastBitOfHealthItCanNoLongerBeAttackedAt()
	{
		final FightGroup.Member member = new FightGroup.Member();
		FightGroup.read(member, overhead(0, 30));
		assertTrue(FightGroup.diedReading(member, true));

		FightGroup.read(member, overhead(1, 60));
		assertTrue(FightGroup.diedReading(member, false));
		assertFalse(FightGroup.diedReading(member, true));

		FightGroup.read(member, gameBar(1, 450));
		assertFalse(FightGroup.diedReading(member, false));

		assertFalse(FightGroup.diedReading(new FightGroup.Member(), false));
	}

	@Test
	public void onlyAConfirmedKillIsNotAnAliveMember()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		member.downState.update(overheadReading(0, 30), true, true, true, 1, null, null, null);
		assertTrue(group.isAliveMember(1, "Boss"));
		member.downState.defeated = true;
		assertFalse(group.isAliveMember(1, "Boss"));
		assertFalse(group.isAliveMember(2, "Other"));
	}

	@Test
	public void aReturningBossTakesBackTheOnlyAwayMemberWithItsName()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		FightGroup.read(member, overhead(12, 30));
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
		assertFalse(group.isAliveMember(9, "Boss"));
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
			FightGroup.read(member, overhead(20, 30));
		}

		assertEquals(Collections.singletonList(newer), group.choosePartner(own));
		newer.downState.defeated = true;
		assertEquals(Collections.singletonList(older), group.choosePartner(own));
		assertNull(noHealth.state);
	}

	// Only as many as there are bars for them, most recently fought first.
	@Test
	public void theOtherBossesComeInTheOrderTheyWereFought()
	{
		final FightGroup.Member own = group.join(1, "First", true, 30);
		final FightGroup.Member older = group.join(2, "Second", true, 10);
		final FightGroup.Member newer = group.join(3, "Third", true, 20);
		FightGroup.read(older, overhead(20, 30));
		FightGroup.read(newer, overhead(20, 30));

		final List<FightGroup.Member> chosen = group.choosePartner(own);
		assertEquals(FightGroup.MAX_EXTRA_BARS, chosen.size());
		assertSame(newer, chosen.get(0));
		for (int extra = 1; extra < chosen.size(); extra++)
		{
			assertTrue(chosen.get(extra - 1).lastEngagedTick >= chosen.get(extra).lastEngagedTick);
		}

		group.updatePartner(own, true);
		assertSame(newer, group.partner());
		assertSame(group.partner(), group.partner(0));
		assertNull(group.partner(FightGroup.MAX_EXTRA_BARS));
	}

	@Test
	public void anAwayBossCanBeThePartner()
	{
		final FightGroup.Member own = group.join(1, "First", true, 30);
		final FightGroup.Member away = group.join(2, "Second", true, 20);
		FightGroup.read(away, overhead(20, 30));
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
		FightGroup.read(boss, overhead(20, 30));
		assertTrue(group.choosePartner(own).isEmpty());
		assertTrue(group.choosePartner(null).isEmpty());
	}

	@Test
	public void thePartnerStaysWhileThereIsNoOpponent()
	{
		final FightGroup.Member own = group.join(1, "First", true, 30);
		final FightGroup.Member other = group.join(2, "Second", true, 20);
		FightGroup.read(other, overhead(20, 30));
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
	public void aMemberAtZeroShowsTheZeroAndIsOnlyDeadOnceConfirmed()
	{
		final FightGroup.Member member = new FightGroup.Member();
		FightGroup.read(member, overhead(20, 30));

		member.downState.update(overheadReading(0, 30), true, true, true, 1, null, null, null);
		assertTrue(FightGroup.read(member, overhead(0, 30)));
		assertReading(member, 0, 30, false, 450);
		assertTrue(member.downState.down);
		assertFalse(member.isDead());

		// Healing back from 0 carries on from there.
		member.downState.update(overheadReading(12, 30), false, true, true, 2, null, null, null);
		assertTrue(FightGroup.read(member, overhead(12, 30)));
		assertReading(member, 12, 30, false, 450);
		assertFalse(member.downState.down);
		assertFalse(member.isDead());
	}

	@Test
	public void aMemberIsDeadOnceItCanNoLongerBeAttackedForTwoTicks()
	{
		final FightGroup.Member member = new FightGroup.Member();
		member.downState.update(overheadReading(0, 30), true, true, false, 1, null, null, null);
		assertFalse(member.isDead());
		member.downState.update(overheadReading(0, 30), true, true, false, 2, null, null, null);
		assertTrue(member.isDead());
	}

	@Test
	public void aMemberBackUpAfterAConfirmedKillStartsANewLife()
	{
		final FightGroup.Member own = group.join(1, "First", true, 0);
		FightGroup.read(own, overhead(10, 30));
		final FightGroup.Member member = group.join(2, "Second", true, 0);
		FightGroup.read(member, overhead(20, 30));
		member.downState.update(overheadReading(0, 30), true, true, false, 1, null, null, null);
		member.downState.update(overheadReading(0, 30), true, true, false, 2, null, null, null);
		member.downState.update(null, false, true, false, 3, null, null, null);
		assertTrue(member.isDead());
		assertFalse(group.isAliveMember(2, "Second"));
		assertTrue(group.choosePartner(own).isEmpty());
		assertFalse(FightGroup.startNewLife(member));

		// Back with full health well after the kill, so this is the next one and not another phase.
		member.downState.update(overheadReading(30, 30), false, true, true, 20, null, null, null);
		assertTrue(FightGroup.startNewLife(member));
		assertFalse(member.isDead());
		assertTrue(group.isAliveMember(2, "Second"));
		assertEquals(-1, member.ratio);
		assertEquals(-1, member.scale);
		assertNull(member.maxHealth);
		assertNull(member.state);

		FightGroup.read(member, overhead(30, 30));
		assertEquals(Collections.singletonList(member), group.choosePartner(own));
		group.despawned(member, false);
		assertSame(member, group.awayByName("Second"));
	}

	@Test
	public void eachReadingIsKeptAndNoReadingKeepsTheLastOne()
	{
		final FightGroup.Member member = new FightGroup.Member();

		assertTrue(FightGroup.read(member, gameBar(300, 450)));
		assertReading(member, 300, 450, true, 450);

		assertTrue(FightGroup.read(member, overhead(15, 30)));
		assertReading(member, 15, 30, false, 450);

		// The overhead bar went away, so the last reading stays.
		assertFalse(FightGroup.read(member, null));
		assertReading(member, 15, 30, false, 450);

		// An overhead bar with no value isn't a reading either.
		assertFalse(FightGroup.read(member, overhead(-1, 60)));
		assertReading(member, 15, 30, false, 450);
	}

	@Test
	public void theStateIsOnlyRebuiltWhenTheReadingChanges()
	{
		final FightGroup.Member member = new FightGroup.Member();
		FightGroup.read(member, overhead(15, 30));
		member.state = BarState.healthOnly("Test", new Health(15, 30, false, 450, false));
		final BarState state = member.state;

		assertFalse(FightGroup.read(member, overhead(15, 30)));
		assertSame(state, member.state);

		assertTrue(FightGroup.read(member, overhead(14, 30)));
		assertNull(member.state);
	}

	@Test
	public void theRememberedHealthIsFoundByIndexAndName()
	{
		final Health first = gameBar(300, 500);
		final Health second = gameBar(200, 500);
		FightGroup.read(group.join(1, "Boss", true, 0), first);
		FightGroup.read(group.join(2, "Boss", true, 0), second);

		assertSame(first, group.rememberedHealth(1, "Boss"));
		assertSame(second, group.rememberedHealth(2, "Boss"));
		assertNull(group.rememberedHealth(1, "Other"));
		assertNull(group.rememberedHealth(1, null));
	}

	// An NPC that leaves and comes back gets a new index, and isn't always linked back to the member it left as.
	@Test
	public void aSingleMemberWithTheNameCoversAMemberWithoutAReading()
	{
		final Health health = gameBar(300, 500);
		final FightGroup.Member left = group.join(1, "Npc", false, 0);
		FightGroup.read(left, health);
		FightGroup.read(group.join(2, "Minion", false, 0), gameBar(50, 500));
		group.despawned(left, false);

		final FightGroup.Member back = group.join(9, "Npc", false, 10);
		assertNull(back.lastAboveZero);
		assertSame(health, group.rememberedHealth(9, "Npc"));
	}

	@Test
	public void anOpponentBackFromMemoryBringsItsRememberedHealthToANewGroup()
	{
		FightGroup.read(group.join(1, "Boss", true, 0), gameBar(300, 500));
		final int fight = group.id();
		group.startFight(null, false, 50);
		group.join(2, "Other", true, 50);

		// The fight it belonged to was cleared meanwhile, so it rejoins without its reading.
		group.restore(fight, null, true, 60);
		group.join(9, "Boss", true, 60);
		assertNull(group.rememberedHealth(9, "Boss"));

		final Health remembered = gameBar(300, 500);
		group.restoreHealth(9, remembered);
		assertSame(remembered, group.rememberedHealth(9, "Boss"));

		// A member that read its own health keeps it.
		group.restoreHealth(9, gameBar(10, 500));
		assertSame(remembered, group.rememberedHealth(9, "Boss"));
	}

	@Test
	public void severalMembersWithTheNameAreNotGuessed()
	{
		FightGroup.read(group.join(1, "Boss", true, 0), gameBar(300, 500));
		FightGroup.read(group.join(2, "Boss", true, 0), gameBar(200, 500));
		assertNull(group.rememberedHealth(9, "Boss"));
	}

	@Test
	public void aNewReadingReplacesTheRememberedOne()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		FightGroup.read(member, gameBar(300, 500));
		final Health newer = gameBar(250, 500);
		FightGroup.read(member, newer);
		assertSame(newer, group.rememberedHealth(1, "Boss"));
		assertSame(newer, group.rememberedHealth(5, "Boss"));
	}

	@Test
	public void theRememberedHealthGoesWithTheMemberThatIsDropped()
	{
		for (int i = 0; i < FightGroup.MAX_MEMBERS; i++)
		{
			FightGroup.read(group.join(i, "Npc " + i, false, 100 - i), gameBar(100 + i, 500));
		}
		FightGroup.read(group.join(50, "New", false, 200), gameBar(400, 500));

		assertNull(group.rememberedHealth(FightGroup.MAX_MEMBERS - 1, "Npc " + (FightGroup.MAX_MEMBERS - 1)));
		assertNotNull(group.rememberedHealth(50, "New"));
		assertNotNull(group.rememberedHealth(1, "Npc 1"));
	}

	@Test
	public void anotherFightDoesntSeeTheRememberedHealth()
	{
		FightGroup.read(group.join(1, "Boss", true, 0), gameBar(300, 500));
		group.startFight(null, false, 10);
		assertNull(group.rememberedHealth(1, "Boss"));
		assertNull(group.rememberedHealth(2, "Boss"));
	}

	// A 0 isn't remembered, so the last health above it stays for an opponent that can still come back from it.
	@Test
	public void aZeroOrNoValueKeepsTheLastHealthAboveIt()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		final Health above = overhead(40, 60);
		FightGroup.read(member, above);
		FightGroup.read(member, overhead(0, 60));
		assertSame(above, group.rememberedHealth(1, "Boss"));

		FightGroup.read(group.join(2, "Other", true, 0), overhead(0, 60));
		assertNull(group.rememberedHealth(2, "Other"));

		FightGroup.read(member, new Health(-1, 60, false, 450, false));
		assertSame(above, group.rememberedHealth(1, "Boss"));
	}

	@Test
	public void aReadingWithoutHealthIsNotRemembered()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		FightGroup.read(member, new Health(-1, -1, false, null, false));
		FightGroup.read(member, null);
		assertNull(group.rememberedHealth(1, "Boss"));
	}

	@Test
	public void aNewLifeForgetsTheRememberedHealth()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		FightGroup.read(member, overhead(20, 30));
		member.downState.update(overheadReading(0, 30), true, true, false, 1, null, null, null);
		member.downState.update(overheadReading(0, 30), true, true, false, 2, null, null, null);
		member.downState.update(overheadReading(30, 30), false, true, true, 20, null, null, null);

		assertTrue(FightGroup.startNewLife(member));
		assertNull(group.rememberedHealth(1, "Boss"));
	}

	// A cleared group keeps no member by name, so a same-named opponent can't carry the old fight.
	@Test
	public void aClearedGroupKeepsNoAwayMember()
	{
		final FightGroup.Member member = group.join(1, "Boss", true, 0);
		group.despawned(member, false);
		assertNotNull(group.awayByName("Boss"));

		group.clear();
		assertNull(group.awayByName("Boss"));
		assertFalse(group.isAliveMember(1, "Boss"));
	}

	private static Health overhead(int ratio, int scale)
	{
		return new Health(ratio, scale, false, 450, false);
	}

	private static HealthReader.Reading overheadReading(int ratio, int scale)
	{
		return new HealthReader.Reading(HealthReader.Source.OVERHEAD, overhead(ratio, scale));
	}

	private static Health gameBar(int health, int max)
	{
		return new Health(health, max, true, max, false);
	}

	private static void assertReading(FightGroup.Member member, int ratio, int scale, boolean exact, Integer maxHealth)
	{
		assertEquals(ratio, member.ratio);
		assertEquals(scale, member.scale);
		assertEquals(exact, member.exact);
		assertEquals(maxHealth, member.maxHealth);
	}
}
