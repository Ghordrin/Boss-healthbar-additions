package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.DamageTracker.NO_TICK;
import static com.ghordrin.bosshealthbar.DamageTracker.isSameAttack;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DamageTrackerTest
{
	@Test
	public void noStartTickIsNeverTheSameAttack()
	{
		assertFalse(isSameAttack(NO_TICK, 100));
		assertFalse(isSameAttack(NO_TICK, 0));
	}

	@Test
	public void theSameTickAndTheNextTickAreTheSameAttack()
	{
		assertTrue(isSameAttack(100, 100));
		assertTrue(isSameAttack(100, 101));
	}

	@Test
	public void twoTicksLaterIsANewAttack()
	{
		assertFalse(isSameAttack(100, 102));
	}

	@Test
	public void aTickFromBeforeTheStartIsANewAttack()
	{
		assertFalse(isSameAttack(100, 5));
	}

	@Test
	public void hitsOnTheSameTickAddUp()
	{
		final DamageTracker tracker = new DamageTracker();
		hit(tracker, 20, 100);
		hit(tracker, 15, 100);

		assertEquals(35, tracker.getComboDamage());
	}

	@Test
	public void hitsTwoTicksApartStartFresh()
	{
		final DamageTracker tracker = new DamageTracker();
		hit(tracker, 20, 100);
		hit(tracker, 15, 102);

		assertEquals(15, tracker.getComboDamage());
	}

	@Test
	public void aFourTickCadenceNeverAccumulates()
	{
		final DamageTracker tracker = new DamageTracker();
		for (int tick = 100; tick < 140; tick += 4)
		{
			hit(tracker, 25, tick);
			assertEquals(25, tracker.getComboDamage());
		}
	}

	@Test
	public void multiHitAttackAddsUpThenTheNextAttackStartsFresh()
	{
		final DamageTracker tracker = new DamageTracker();
		hit(tracker, 20, 100);
		hit(tracker, 10, 100);
		hit(tracker, 5, 101);
		hit(tracker, 5, 101);
		assertEquals(40, tracker.getComboDamage());

		hit(tracker, 12, 105);
		assertEquals(12, tracker.getComboDamage());
	}

	@Test
	public void hitsOnEveryTickDoNotChainIntoOneTotal()
	{
		final DamageTracker tracker = new DamageTracker();
		hit(tracker, 10, 100);
		hit(tracker, 10, 101);
		hit(tracker, 10, 102);
		assertEquals(10, tracker.getComboDamage());

		hit(tracker, 10, 103);
		assertEquals(20, tracker.getComboDamage());
	}

	@Test
	public void resetComboStartsTheNextHitFresh()
	{
		final DamageTracker tracker = new DamageTracker();
		hit(tracker, 20, 100);
		tracker.resetCombo();
		hit(tracker, 15, 100);

		assertEquals(15, tracker.getComboDamage());
	}

	@Test
	public void partyHitsKeepAddingWhileTheyKeepComing()
	{
		final DamageTracker tracker = new DamageTracker();
		tracker.addToCombo(20, 1000, 100, true);
		tracker.addToCombo(15, 3400, 104, true);
		tracker.addToCombo(10, 5800, 108, true);

		assertEquals(45, tracker.getComboDamage());
	}

	@Test
	public void partyTotalStartsFreshAfterAPause()
	{
		final DamageTracker tracker = new DamageTracker();
		tracker.addToCombo(20, 1000, 100, true);
		tracker.addToCombo(15, 3501, 105, true);

		assertEquals(15, tracker.getComboDamage());
	}

	@Test
	public void zeroPartyHitsAreIgnored()
	{
		final DamageTracker tracker = new DamageTracker();
		tracker.recordPartyHit(0, 100);

		assertEquals(0, tracker.getComboDamage());
	}

	private static void hit(DamageTracker tracker, int amount, int tick)
	{
		tracker.addToCombo(amount, tick * 600L, tick, false);
	}
}
