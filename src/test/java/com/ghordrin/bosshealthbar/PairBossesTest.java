package com.ghordrin.bosshealthbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.NpcID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PairBossesTest
{
	private static final int[] FIRST = {NpcID.GARGBOSS_DAWN_SPAWN, NpcID.GARGBOSS_DAWN_PHASE1,
		NpcID.GARGBOSS_DAWN_PHASE1_TRANSITION, NpcID.GARGBOSS_DAWN_PHASE3, NpcID.GARGBOSS_DAWN_DEATH};
	private static final int[] SECOND = {NpcID.GARGBOSS_DUSK_SPAWN, NpcID.GARGBOSS_DUSK_PHASE1_DEFENSIVE,
		NpcID.GARGBOSS_DUSK_PHASE1_TRANSITION, NpcID.GARGBOSS_DUSK_PHASE1_FLYTRANSITION,
		NpcID.GARGBOSS_DUSK_PHASE2_ATTACKING, NpcID.GARGBOSS_DUSK_PHASE3_DEFENSIVE,
		NpcID.GARGBOSS_DUSK_PHASE3_TRANSITION, NpcID.GARGBOSS_DUSK_PHASE4_SPAWN, NpcID.GARGBOSS_DUSK_PHASE4,
		NpcID.GARGBOSS_DUSK_DEATH};

	private final List<PairBosses.Slot> slots = new ArrayList<>();

	@Test
	public void theTableCoversExactlyEachFormOfBothMembers()
	{
		final Set<Integer> ids = new HashSet<>();
		for (int id : FIRST)
		{
			ids.add(id);
		}
		for (int id : SECOND)
		{
			ids.add(id);
		}
		assertEquals(15, ids.size());
		assertEquals(ids, PairBosses.members().keySet());

		final PairBosses.Member first = PairBosses.member(FIRST[0]);
		final PairBosses.Member second = PairBosses.member(SECOND[0]);
		assertNotSame(first, second);
		assertEquals(PairBosses.GUARDIANS, first.pair);
		assertEquals(PairBosses.GUARDIANS, second.pair);
		Arrays.stream(FIRST).forEach(id -> assertSame(first, PairBosses.member(id)));
		Arrays.stream(SECOND).forEach(id -> assertSame(second, PairBosses.member(id)));
		assertNull(PairBosses.member(NpcID.GARGBOSS_DUSK_SPAWN - 1));
	}

	@Test
	public void onlyThePairNameIsAPairKey()
	{
		assertTrue(PairBosses.isPairKey(PairBosses.GUARDIANS));
		assertFalse(PairBosses.isPairKey("Dusk"));
		assertFalse(PairBosses.isPairKey(null));
	}

	@Test
	public void aMemberThatDiesIsForgottenAndOneThatLeavesIsKept()
	{
		final PairBosses.Slot dead = PairBosses.slot(slots, PairBosses.member(FIRST[0]), true);
		final PairBosses.Slot away = PairBosses.slot(slots, PairBosses.member(SECOND[0]), true);
		dead.dead = true;
		PairBosses.despawned(slots, dead, false);
		PairBosses.despawned(slots, away, false);
		assertNull(PairBosses.slot(slots, PairBosses.member(FIRST[0]), false));
		assertSame(away, PairBosses.slot(slots, PairBosses.member(SECOND[0]), false));
		assertNull(away.npc);
		assertTrue(PairBosses.isAlive(away));

		PairBosses.despawned(slots, away, true);
		assertTrue(slots.isEmpty());
	}

	@Test
	public void aPairIsForgottenOnceNoMemberHasBeenAroundForAWhile()
	{
		final PairBosses.Slot first = PairBosses.slot(slots, PairBosses.member(FIRST[0]), true);
		final PairBosses.Slot second = PairBosses.slot(slots, PairBosses.member(SECOND[0]), true);
		first.lastPresentTick = 100;
		second.lastPresentTick = 90;

		assertTrue(PairBosses.clearGone(slots, 100 + PairBosses.CLEAR_TICKS).isEmpty());
		assertEquals(2, slots.size());
		assertTrue(PairBosses.hasPair(slots, PairBosses.GUARDIANS));

		assertEquals(Collections.singleton(PairBosses.GUARDIANS), PairBosses.clearGone(slots, 101 + PairBosses.CLEAR_TICKS));
		assertTrue(slots.isEmpty());
		assertFalse(PairBosses.hasPair(slots, PairBosses.GUARDIANS));
	}

	@Test
	public void aPairThatComesBackAfterBeingForgottenIsHeldAgain()
	{
		final PairBosses.Slot away = PairBosses.slot(slots, PairBosses.member(FIRST[0]), true);
		away.lastPresentTick = 0;
		assertEquals(Collections.singleton(PairBosses.GUARDIANS), PairBosses.clearGone(slots, 500));

		// The pair comes back: the new slots are a new pair, held until it's gone again.
		final PairBosses.Slot back = PairBosses.slot(slots, PairBosses.member(SECOND[0]), true);
		back.lastPresentTick = 500;
		assertTrue(PairBosses.hasPair(slots, PairBosses.GUARDIANS));
		assertTrue(PairBosses.clearGone(slots, 500 + PairBosses.CLEAR_TICKS).isEmpty());
	}

	@Test
	public void onlyTheDeathFormsCountAsDying()
	{
		assertTrue(PairBosses.isDeathForm(NpcID.GARGBOSS_DAWN_DEATH));
		assertTrue(PairBosses.isDeathForm(NpcID.GARGBOSS_DUSK_DEATH));
		Arrays.stream(FIRST).filter(id -> id != NpcID.GARGBOSS_DAWN_DEATH)
			.forEach(id -> assertFalse(PairBosses.isDeathForm(id)));
		Arrays.stream(SECOND).filter(id -> id != NpcID.GARGBOSS_DUSK_DEATH)
			.forEach(id -> assertFalse(PairBosses.isDeathForm(id)));
	}

	@Test
	public void aMemberInItsDeathFormIsDeadWhateverItsHealth()
	{
		final PairBosses.Slot slot = new PairBosses.Slot(PairBosses.member(FIRST[0]));
		PairBosses.read(slot, 0, 0, 20, 30, 450);
		PairBosses.updateDead(slot, false);
		assertTrue(PairBosses.isAlive(slot));

		PairBosses.updateDead(slot, true);
		assertFalse(PairBosses.isAlive(slot));

		// A dead member is forgotten when it despawns, so the timer can stop at the last kill.
		slots.add(slot);
		PairBosses.despawned(slots, slot, false);
		assertTrue(slots.isEmpty());
	}

	@Test
	public void aZeroReadingIsDeadAndAFreshOneIsAliveAgain()
	{
		final PairBosses.Slot slot = new PairBosses.Slot(PairBosses.member(SECOND[0]));
		PairBosses.updateDead(slot, false);
		assertTrue(PairBosses.isAlive(slot));

		PairBosses.read(slot, 0, 0, 0, 30, 450);
		PairBosses.updateDead(slot, false);
		assertFalse(PairBosses.isAlive(slot));

		PairBosses.read(slot, 0, 0, 5, 30, 450);
		PairBosses.updateDead(slot, false);
		assertTrue(PairBosses.isAlive(slot));
	}

	@Test
	public void onlyTheFirstFormOfEachMemberIsAStartForm()
	{
		assertTrue(PairBosses.isStartForm(NpcID.GARGBOSS_DAWN_SPAWN));
		assertTrue(PairBosses.isStartForm(NpcID.GARGBOSS_DUSK_SPAWN));
		assertFalse(PairBosses.isStartForm(NpcID.GARGBOSS_DUSK_PHASE4_SPAWN));
		assertFalse(PairBosses.isStartForm(NpcID.GARGBOSS_DAWN_PHASE1));
	}

	@Test
	public void aStartFormIsSeededAtFullHealth()
	{
		final PairBosses.Slot slot = new PairBosses.Slot(PairBosses.member(FIRST[0]));
		PairBosses.seedFull(slot, 450);
		assertReading(slot, 450, 450, true, 450);

		final PairBosses.Slot unknown = new PairBosses.Slot(PairBosses.member(FIRST[0]));
		PairBosses.seedFull(unknown, null);
		assertReading(unknown, 1, 1, false, null);
	}

	@Test
	public void theGameBarWinsOverTheOverheadBarWhichWinsOverTheLastReading()
	{
		final PairBosses.Slot slot = new PairBosses.Slot(PairBosses.member(SECOND[0]));
		PairBosses.seedFull(slot, 450);

		assertTrue(PairBosses.read(slot, 300, 450, 20, 30, 450));
		assertReading(slot, 300, 450, true, 450);

		assertTrue(PairBosses.read(slot, 0, 0, 15, 30, 450));
		assertReading(slot, 15, 30, false, 450);

		// The overhead bar went away, so the last reading stays.
		assertFalse(PairBosses.read(slot, 0, 0, -1, -1, 450));
		assertReading(slot, 15, 30, false, 450);
	}

	@Test
	public void theStateIsOnlyRebuiltWhenTheReadingChanges()
	{
		final PairBosses.Slot slot = new PairBosses.Slot(PairBosses.member(SECOND[0]));
		PairBosses.read(slot, 0, 0, 15, 30, 450);
		slot.state = new BarState("Test", 0, 450, 15, 30, false, false, BarState.NO_PHASE_MARKERS,
			HealthIndicatorMarkers.NONE, null, null, Collections.emptyList(), null);
		final BarState state = slot.state;

		assertFalse(PairBosses.read(slot, 0, 0, 15, 30, 450));
		assertSame(state, slot.state);

		assertTrue(PairBosses.read(slot, 0, 0, 14, 30, 450));
		assertNull(slot.state);
	}

	@Test
	public void thePartnerIsTheOtherMemberOfThePair()
	{
		final PairBosses.Member first = PairBosses.member(FIRST[0]);
		final PairBosses.Member second = PairBosses.member(SECOND[0]);
		assertNull(PairBosses.partner(slots, first));

		final PairBosses.Slot firstSlot = PairBosses.slot(slots, first, true);
		assertNull(PairBosses.partner(slots, first));
		assertSame(firstSlot, PairBosses.partner(slots, second));

		final PairBosses.Slot secondSlot = PairBosses.slot(slots, second, true);
		assertSame(secondSlot, PairBosses.partner(slots, first));
	}

	@Test
	public void thePartnerRowFitsTheHitpointsFontWhileItHasText()
	{
		assertEquals(14, BarTextPainter.partnerRowHeight(true, false, 10, 4));
		assertEquals(14, BarTextPainter.partnerRowHeight(false, true, 10, 4));
		assertEquals(0, BarTextPainter.partnerRowHeight(false, false, 10, 4));
	}

	@Test
	public void theSlotIsFoundByMember()
	{
		final PairBosses.Slot slot = PairBosses.slot(slots, PairBosses.member(FIRST[1]), true);
		assertSame(slot, PairBosses.slot(slots, PairBosses.member(FIRST[3]), true));
		assertNotNull(slot);
		assertEquals(1, slots.size());
	}

	private static void assertReading(PairBosses.Slot slot, int ratio, int scale, boolean exact, Integer maxHealth)
	{
		assertEquals(ratio, slot.ratio);
		assertEquals(scale, slot.scale);
		assertEquals(exact, slot.exact);
		assertEquals(maxHealth, slot.maxHealth);
	}
}
