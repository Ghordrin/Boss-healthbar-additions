package com.ghordrin.bosshealthbar.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.tracking.HealthReader.Reading;
import com.ghordrin.bosshealthbar.tracking.HealthReader.Source;
import com.ghordrin.bosshealthbar.data.Health;
import org.junit.Test;

public class HealthReaderTest
{
	private static Reading gameBar(int health, int max)
	{
		return new Reading(Source.GAME_BAR, new Health(health, max, true, max, false));
	}

	private static Reading raidBar(int health, int max)
	{
		return new Reading(Source.RAID_BAR, new Health(health, max, false, 900, false));
	}

	private static Reading overhead(int ratio, int scale)
	{
		return new Reading(Source.OVERHEAD, new Health(ratio, scale, false, 450, false));
	}

	// One tick at a time, as the tracker and the fight group run it.
	private static final class Opponent
	{
		final boolean npc;
		final HealthReader.DownState state = new HealthReader.DownState();
		boolean down;
		boolean defeated;
		boolean revived;
		int tick;

		Opponent(boolean npc)
		{
			this.npc = npc;
		}

		Opponent tick(Reading reading, boolean dead, boolean attackable)
		{
			state.update(reading, dead, npc, attackable, ++tick, null, null, null);
			down = state.down;
			defeated = state.defeated;
			revived = state.takeRevived();
			return this;
		}
	}

	@Test
	public void staysDownUntilBackUp()
	{
		assertTrue(HealthReader.stillDown(false, true, false));
		assertTrue(HealthReader.stillDown(true, false, false));
		assertFalse(HealthReader.stillDown(true, false, true));
		assertTrue(HealthReader.stillDown(true, true, true));
		assertFalse(HealthReader.stillDown(false, false, false));
	}

	@Test
	public void aBossBarAboveZeroWinsOverIsDead()
	{
		assertFalse(HealthReader.isDown(gameBar(200, 450), true, true, true));
		assertFalse(HealthReader.isDown(raidBar(5, 600), true, true, true));
		assertTrue(HealthReader.isDown(overhead(0, 30), true, true, true));
		assertTrue(HealthReader.isDown(null, true, true, true));
	}

	@Test
	public void aBossBarAtZeroIsDown()
	{
		assertTrue(HealthReader.isDown(gameBar(0, 450), false, true, true));
		assertFalse(HealthReader.isDown(gameBar(1, 450), false, true, true));
		assertTrue(HealthReader.isDown(raidBar(0, 600), false, true, true));
		assertFalse(HealthReader.isDown(raidBar(5, 600), false, true, true));
	}

	@Test
	public void anOverheadZeroWithoutIsDeadIsNotDownButADeathFormAtOneIs()
	{
		assertFalse(HealthReader.isDown(overhead(0, 30), false, true, true));
		assertTrue(HealthReader.isDown(overhead(1, 60), false, true, false));
		assertFalse(HealthReader.isDown(overhead(1, 60), false, true, true));
		assertFalse(HealthReader.isDown(overhead(1, 60), false, false, false));
		assertFalse(HealthReader.isDown(null, false, true, false));
	}

	@Test
	public void aBossBarRefillingIsBackUp()
	{
		assertTrue(HealthReader.isBackUp(gameBar(1, 450), false, false));
		assertTrue(HealthReader.isBackUp(raidBar(3, 600), true, false));
		assertFalse(HealthReader.isBackUp(gameBar(0, 450), false, true));
	}

	@Test
	public void theOverheadBarIsBackUpOnceAttackableWithHealthToSpare()
	{
		assertTrue(HealthReader.isBackUp(overhead(2, 30), false, true));
		assertFalse(HealthReader.isBackUp(overhead(1, 30), false, true));
		assertFalse(HealthReader.isBackUp(overhead(20, 30), false, false));
		assertFalse(HealthReader.isBackUp(overhead(20, 30), true, true));
		assertFalse(HealthReader.isBackUp(null, false, true));
	}

	@Test
	public void unfightableSinceCountsFromTheFirstTickDownAndUnattackable()
	{
		assertEquals(FightTimer.NO_TICK, HealthReader.unfightableSince(false, false, FightTimer.NO_TICK, 5));
		assertEquals(FightTimer.NO_TICK, HealthReader.unfightableSince(true, true, 4, 5));
		assertEquals(5, HealthReader.unfightableSince(true, false, FightTimer.NO_TICK, 5));
		assertEquals(4, HealthReader.unfightableSince(true, false, 4, 5));
		assertFalse(HealthReader.isDefeated(true, true, 5, 5));
		assertTrue(HealthReader.isDefeated(true, true, 5, 6));
		assertFalse(HealthReader.isDefeated(false, true, 5, 6));
	}

	@Test
	public void healingFromZeroIsDownButNeverDefeated()
	{
		final Opponent boss = new Opponent(true);
		boss.tick(overhead(10, 30), false, true);
		assertFalse(boss.down);
		boss.tick(overhead(0, 30), true, true);
		assertTrue(boss.down);
		for (int i = 0; i < 9; i++)
		{
			boss.tick(overhead(0, 30), true, true);
			assertFalse(boss.defeated);
		}
		boss.tick(overhead(12, 30), false, true);
		assertFalse(boss.down);
		assertFalse(boss.defeated);
	}

	@Test
	public void aDeathFormIsDefeatedAfterTwoUnattackableTicks()
	{
		final Opponent boss = new Opponent(true);
		boss.tick(overhead(3, 60), false, true);
		boss.tick(overhead(1, 60), false, false);
		assertTrue(boss.down);
		assertFalse(boss.defeated);
		boss.tick(overhead(1, 60), false, false);
		assertTrue(boss.defeated);
	}

	@Test
	public void aBossBarAtZeroThatRefillsWhileUnattackableIsBackUp()
	{
		final Opponent boss = new Opponent(true);
		boss.tick(gameBar(40, 450), false, true);
		boss.tick(gameBar(0, 450), false, false);
		assertTrue(boss.down);
		assertFalse(boss.defeated);
		boss.tick(gameBar(450, 450), false, false);
		assertFalse(boss.down);
		assertFalse(boss.defeated);
		// The kill was never confirmed, so this is the same fight carrying on.
		assertFalse(boss.revived);
	}

	@Test
	public void aConfirmedDefeatIsFinalAndHealthComingBackStartsTheNextLife()
	{
		final Opponent boss = new Opponent(true);
		boss.tick(overhead(3, 60), false, true);
		boss.tick(overhead(1, 60), false, false);
		boss.tick(overhead(1, 60), false, false);
		assertTrue(boss.defeated);
		assertFalse(boss.revived);

		// Down with nothing left to read, as when it turns into a form without a bar: the kill still counts.
		for (int i = 0; i < FightTimer.SWAP_TICKS; i++)
		{
			boss.tick(null, false, false);
			assertTrue(boss.down);
			assertTrue(boss.defeated);
			assertFalse(boss.revived);
		}

		boss.tick(overhead(60, 60), false, true);
		assertFalse(boss.down);
		assertFalse(boss.defeated);
		assertTrue(boss.revived);

		// Reported once, and the new life starts undefeated.
		boss.tick(overhead(55, 60), false, true);
		assertFalse(boss.defeated);
		assertFalse(boss.revived);
	}

	@Test
	public void beingAttackableAgainWhileStillAtZeroWasNeverAKill()
	{
		final Opponent boss = new Opponent(true);
		boss.tick(overhead(3, 60), false, true);
		boss.tick(overhead(1, 60), false, false);
		boss.tick(overhead(1, 60), false, false);
		assertTrue(boss.defeated);

		// Still down, but it can be fought again, so it never died and the bar comes back.
		boss.tick(overhead(1, 60), false, true);
		assertTrue(boss.down);
		assertFalse(boss.defeated);
		assertFalse(boss.revived);

		boss.tick(overhead(40, 60), false, true);
		assertFalse(boss.down);
		assertFalse(boss.revived);
	}

	@Test
	public void aShortDownIsAnotherPhaseAndNotTheNextOne()
	{
		assertFalse(HealthReader.isRevived(FightTimer.NO_TICK, 100));
		assertFalse(HealthReader.isRevived(100 - FightTimer.SWAP_TICKS, 100));
		assertTrue(HealthReader.isRevived(99 - FightTimer.SWAP_TICKS, 100));

		final Opponent phased = new Opponent(true);
		phased.tick(gameBar(40, 450), false, true);
		phased.tick(gameBar(0, 450), false, false);
		phased.tick(gameBar(450, 450), false, false);
		assertFalse(phased.revived);

		// Long enough to confirm a kill, but back for its next phase within the gap a phase change takes.
		final Opponent confirmed = new Opponent(true);
		confirmed.tick(gameBar(40, 450), false, true);
		for (int i = 0; i < FightTimer.SWAP_TICKS; i++)
		{
			confirmed.tick(gameBar(0, 450), false, false);
		}
		assertTrue(confirmed.defeated);
		confirmed.tick(gameBar(450, 450), false, false);
		assertFalse(confirmed.down);
		assertFalse(confirmed.defeated);
		assertFalse(confirmed.revived);
	}

	@Test
	public void clearingForgetsAConfirmedDefeat()
	{
		final Opponent boss = new Opponent(true);
		boss.tick(overhead(0, 30), true, false);
		boss.tick(overhead(0, 30), true, false);
		assertTrue(boss.defeated);

		boss.state.clear();
		assertFalse(boss.state.down);
		assertFalse(boss.state.defeated);
		assertFalse(boss.state.takeRevived());

		boss.tick(overhead(30, 30), false, true);
		assertFalse(boss.revived);
	}

	@Test
	public void aBossBarAtZeroThenAnUnattackableFormIsDefeated()
	{
		final Opponent boss = new Opponent(true);
		boss.tick(gameBar(40, 450), false, true);
		boss.tick(gameBar(0, 450), false, true);
		assertTrue(boss.down);
		assertFalse(boss.defeated);
		boss.tick(gameBar(0, 450), false, false);
		assertFalse(boss.defeated);
		boss.tick(gameBar(0, 450), false, false);
		assertTrue(boss.defeated);
	}

	@Test
	public void anOverheadZeroWithoutIsDeadIsNotDown()
	{
		final Opponent boss = new Opponent(true);
		boss.tick(overhead(0, 30), false, false);
		boss.tick(overhead(0, 30), false, false);
		assertFalse(boss.down);
		assertFalse(boss.defeated);
	}

	@Test
	public void aPlayerAtZeroIsDefeatedAtOnce()
	{
		final Opponent player = new Opponent(false);
		player.tick(overhead(0, 30), true, false);
		assertTrue(player.defeated);
	}

	@Test
	public void theWatchOnlyFiresWhenSomethingThatMattersChanges()
	{
		final HealthReader.Watch watch = new HealthReader.Watch();
		assertTrue(watch.changed(overhead(20, 30), false, false, true, 7));
		assertFalse(watch.changed(overhead(15, 30), false, false, true, 7));
		assertTrue(watch.changed(overhead(1, 30), false, false, true, 7));
		assertTrue(watch.changed(overhead(0, 30), false, false, true, 7));
		assertTrue(watch.changed(overhead(0, 30), false, true, true, 7));
		assertTrue(watch.changed(overhead(0, 30), false, true, false, 7));
		assertTrue(watch.changed(overhead(0, 30), false, true, false, 8));
		assertTrue(watch.changed(gameBar(0, 450), false, true, false, 8));
		assertTrue(watch.changed(null, false, true, false, 8));
		assertFalse(watch.changed(null, false, true, false, 8));
		watch.clear();
		assertTrue(watch.changed(null, false, true, false, 8));
	}

	@Test
	public void anOverheadBarWithNoValueIsNoReadingAndGetsItsOwnLogLine()
	{
		assertTrue(HealthReader.hasNoValue(-1, 60));
		assertFalse(HealthReader.hasNoValue(0, 60));
		assertFalse(HealthReader.hasNoValue(-1, -1));

		final HealthReader.Watch watch = new HealthReader.Watch();
		assertTrue(watch.changed(overhead(20, 60), false, false, true, 7));
		assertTrue(watch.changed(null, true, false, true, 7));
		assertFalse(watch.changed(null, true, false, true, 7));
		assertTrue(watch.changed(null, false, false, true, 7));
	}

	@Test
	public void describesWhereTheReadingCameFrom()
	{
		assertEquals("game boss bar at 300/450", HealthReader.describe(gameBar(300, 450)));
		assertEquals("raid boss bar at 0/600, max 900", HealthReader.describe(raidBar(0, 600)));
		assertEquals("overhead bar at 12/30, max 450", HealthReader.describe(overhead(12, 30)));
		assertEquals("no reading", HealthReader.describe(null));
	}
}
