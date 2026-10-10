package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.FightTimer.NO_TICK;
import static com.ghordrin.bosshealthbar.FightTimer.SWAP_TICKS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FightTimerTest
{
	private final FightTimer timer = new FightTimer(new DebugLog(null));

	@Test
	public void formatsMinutesAndSeconds()
	{
		assertEquals("0:00", FightTimer.format(0));
		assertEquals("0:00", FightTimer.format(1));
		assertEquals("0:01", FightTimer.format(2));
		assertEquals("0:59", FightTimer.format(99));
		assertEquals("1:00", FightTimer.format(100));
		assertEquals("1:23", FightTimer.format(139));
		assertEquals("59:59", FightTimer.format(5999));
	}

	@Test
	public void formatsHoursFromAnHour()
	{
		assertEquals("1:00:00", FightTimer.format(6000));
		assertEquals("1:05:09", FightTimer.format(6515));
		assertEquals("12:34:56", FightTimer.format(75494));
	}

	@Test
	public void elapsedStopsAtTheEndTick()
	{
		assertEquals(NO_TICK, FightTimer.elapsedTicks(NO_TICK, NO_TICK, 50));
		assertEquals(40, FightTimer.elapsedTicks(10, NO_TICK, 50));
		assertEquals(20, FightTimer.elapsedTicks(10, 30, 50));
	}

	@Test
	public void anyNpcCarriesOnAnUndefeatedFight()
	{
		assertTrue(FightTimer.keepsCounting(true, false, false, NO_TICK, 100));
		assertFalse(FightTimer.keepsCounting(true, false, true, NO_TICK, 100));
		assertFalse(FightTimer.keepsCounting(false, false, false, NO_TICK, 100));
	}

	@Test
	public void anotherNpcOnlyCarriesOnShortlyAfterTheLastOneWentAway()
	{
		assertTrue(FightTimer.keepsCounting(true, false, false, 100, 100 + SWAP_TICKS));
		assertFalse(FightTimer.keepsCounting(true, false, false, 100, 101 + SWAP_TICKS));
	}

	@Test
	public void anAliveMemberOfTheFightCarriesItOnWhateverHappenedBefore()
	{
		assertTrue(FightTimer.keepsCounting(true, true, true, NO_TICK, 100));
		assertTrue(FightTimer.keepsCounting(true, true, false, 100, 500 + SWAP_TICKS));
		assertTrue(FightTimer.keepsCounting(true, true, true, 100, 500 + SWAP_TICKS));
		assertFalse(FightTimer.keepsCounting(false, true, false, NO_TICK, 100));
	}

	@Test
	public void showsNothingBeforeTheFirstHit()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onGameTick(true, false, 11);
		assertNull(timer.getText());
	}

	@Test
	public void countsFromTheFirstHitAndIgnoresLaterOnes()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onHit(20);
		assertEquals("0:00", timer.getText());
		timer.onHit(30);
		timer.onGameTick(true, false, 120);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void theTextIsOnlyRebuiltWhenTheTimeChanges()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onHit(10);
		timer.onGameTick(true, false, 12);
		final String text = timer.getText();
		timer.onGameTick(true, false, 12);
		assertSame(text, timer.getText());
	}

	@Test
	public void stopsWhenDefeatedAndKeepsTheTime()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, 100);
		timer.onGameTick(true, true, 150);
		assertEquals("1:00", timer.getText());

		timer.opponentDespawned(true, 160);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void despawningAsItDiesStopsTheTimer()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(true, 100);
		timer.onGameTick(false, false, 200);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void carriesOnWhenItComesBackFromZero()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, 50);
		timer.onGameTick(true, false, 100);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void anotherOpponentWhileTheLastOneIsUpCarriesOn()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, false, 100);
		assertTrue(timer.opponentChanged(false, false, NO_TICK, 101));
		timer.onGameTick(true, false, 200);
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void anotherOpponentAfterADefeatStartsOver()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, 100);
		assertFalse(timer.opponentChanged(false, false, NO_TICK, 101));
		assertNull(timer.getText());
		timer.onHit(105);
		assertEquals("0:00", timer.getText());
	}

	@Test
	public void aPhaseSwapToASameNamedNpcKeepsCounting()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentChanged(false, false, NO_TICK, 50);
		timer.onGameTick(true, false, 100);
		assertEquals("1:00", timer.getText());

		timer.opponentDespawned(false, 110);
		timer.opponentChanged(false, false, NO_TICK, 112);
		timer.onGameTick(true, false, 200);
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void aSameNamedNpcAfterADefeatStartsOver()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentChanged(false, true, NO_TICK, 50);
		assertNull(timer.getText());

		timer.onHit(60);
		timer.opponentDespawned(true, 100);
		timer.opponentChanged(false, false, NO_TICK, 101);
		assertNull(timer.getText());
	}

	@Test
	public void aSameNamedNpcLongAfterTheLastOneWentAwayStartsOver()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(false, 50);
		timer.opponentChanged(false, false, NO_TICK, 51 + SWAP_TICKS);
		assertNull(timer.getText());
	}

	@Test
	public void onlyAHitFromThisOrTheLastTickCountsFromBeforeTheOpponentWasPicked()
	{
		assertEquals(100, FightTimer.recentHitTick(100, 100));
		assertEquals(100, FightTimer.recentHitTick(100, 101));
		assertEquals(NO_TICK, FightTimer.recentHitTick(100, 102));
		assertEquals(NO_TICK, FightTimer.recentHitTick(NO_TICK, 100));
	}

	@Test
	public void theHitThatBroughtUpTheOpponentStartsTheTimer()
	{
		timer.opponentChanged(false, false, 100, 101);
		timer.onGameTick(true, false, 200);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void anEarlierHitDoesNotRestartAFightThatCarriesOn()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentChanged(false, false, 50, 50);
		timer.onGameTick(true, false, 100);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void aSameNamedNpcSoonAfterADefeatedOneDespawnedStartsOver()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(true, 100);
		timer.opponentChanged(false, false, NO_TICK, 102);
		assertNull(timer.getText());
	}

	@Test
	public void anAliveMemberAfterADefeatCarriesOnAndRunsAgain()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, 100);
		timer.opponentDespawned(true, 105);
		timer.onGameTick(false, false, 150);
		assertEquals("1:00", timer.getText());

		assertTrue(timer.opponentChanged(true, false, NO_TICK, 150));
		timer.onGameTick(true, false, 200);
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void anAliveMemberCarriesOnWhateverTheGap()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(false, 60);
		assertTrue(timer.opponentChanged(true, false, NO_TICK, 60 + SWAP_TICKS * 3));
		timer.onGameTick(true, false, 200);
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void aNewNpcAfterTheLastOneWasDefeatedStartsOver()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		assertFalse(timer.opponentChanged(false, true, NO_TICK, 50));
		assertNull(timer.getText());
	}

	@Test
	public void aNewNpcLongAfterTheLastOneLeftStartsOver()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(false, 100);
		// The time shown stays until the next opponent.
		timer.onGameTick(false, false, 150);
		assertEquals("1:30", timer.getText());
		assertFalse(timer.opponentChanged(false, false, NO_TICK, 101 + SWAP_TICKS));
		assertNull(timer.getText());
	}

	@Test
	public void aKillFoundAtTheDespawnStopsWhereItWasFirstSeen()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(true, 100, 130);
		timer.onGameTick(false, false, 200);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void resetClearsTheFight()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.reset();
		assertNull(timer.getText());
		timer.opponentChanged(false, false, NO_TICK, 5);
		assertNull(timer.getText());
	}

	@Test
	public void anOpponentBackFromOutOfSightKeepsItsStart()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(false, 50);
		final FightTimer.Fight fight = timer.save(1);
		timer.opponentChanged(false, false, NO_TICK, 60);
		timer.opponentReturned(fight, NO_TICK, 70 + SWAP_TICKS);
		timer.onGameTick(true, false, 100);
		assertEquals("1:00", timer.getText());
		timer.onGameTick(true, false, 200);
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void aStoppedFightStaysStoppedWhenTheOpponentComesBack()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, 100);
		timer.opponentDespawned(false, 120);
		final FightTimer.Fight fight = timer.save(1);
		timer.reset();
		timer.opponentReturned(fight, NO_TICK, 200);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void anOpponentBackBeforeAnyHitStartsOnTheNextHit()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.opponentDespawned(false, 50);
		final FightTimer.Fight fight = timer.save(1);
		timer.opponentReturned(fight, NO_TICK, 100);
		assertNull(timer.getText());
		timer.onHit(110);
		timer.onGameTick(true, false, 210);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void startingOverCountsFromThatTick()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, 50);
		timer.startOver(100);
		timer.onGameTick(true, false, 200);
		assertEquals("1:00", timer.getText());
	}
}
