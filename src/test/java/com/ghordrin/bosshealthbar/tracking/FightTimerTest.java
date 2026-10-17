package com.ghordrin.bosshealthbar.tracking;

import static com.ghordrin.bosshealthbar.tracking.FightTimer.DEATH_TICKS;
import static com.ghordrin.bosshealthbar.tracking.FightTimer.NO_TICK;
import static com.ghordrin.bosshealthbar.tracking.FightTimer.SWAP_TICKS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import org.junit.Test;

public class FightTimerTest
{
	private final FightTimer timer = new FightTimer(new DebugLog(null));

	@Test
	public void formatsMinutesAndSeconds()
	{
		assertEquals("0:00", FightTimer.format(0, false));
		assertEquals("0:00", FightTimer.format(1, false));
		assertEquals("0:01", FightTimer.format(2, false));
		assertEquals("0:59", FightTimer.format(99, false));
		assertEquals("1:00", FightTimer.format(100, false));
		assertEquals("1:23", FightTimer.format(139, false));
		assertEquals("59:59", FightTimer.format(5999, false));
	}

	@Test
	public void formatsHoursFromAnHour()
	{
		assertEquals("1:00:00", FightTimer.format(6000, false));
		assertEquals("1:05:09", FightTimer.format(6515, false));
		assertEquals("12:34:56", FightTimer.format(75494, false));
	}

	@Test
	public void preciseTimingAddsTheHundredthsTheGameShows()
	{
		assertEquals("0:00.00", FightTimer.format(0, true));
		assertEquals("0:00.60", FightTimer.format(1, true));
		assertEquals("0:01.20", FightTimer.format(2, true));
		assertEquals("0:59.40", FightTimer.format(99, true));
		assertEquals("1:00.00", FightTimer.format(100, true));
		assertEquals("1:00:00.00", FightTimer.format(6000, true));
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
	public void showsNothingUntilTheFightStarts()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onGameTick(true, false, NO_TICK, 11);
		assertNull(timer.getText());
	}

	@Test
	public void readsTheTimeOutOfEveryWordingTheGameUses()
	{
		assertEquals(154, FightTimer.reportedTicks("Fight duration: 1:32.40. Personal best: 0:31.80"));
		assertEquals(106, FightTimer.reportedTicks("Fight duration: 1:03.60 (new personal best)"));
		assertEquals(105, FightTimer.reportedTicks(
			"Wave 'The Maiden of Sugadinti' (Entry Mode) complete!Duration: 1:03.00 Total: 1:03.00"));
		assertEquals(3129, FightTimer.reportedTicks("Challenge duration: <col=ff0000>31:17</col>"));
		assertEquals(250, FightTimer.reportedTicks("Subdued in 2:30"));
		assertEquals(123, FightTimer.reportedTicks("Corrupted challenge duration: 1:14.00"));
		assertEquals(100, FightTimer.reportedTicks("Challenge completion time: 1:00.00"));
		assertEquals(6515, FightTimer.reportedTicks("Fight duration: 1:05:09"));
	}

	@Test
	public void leavesAloneTheTimesThatAreNotThisFight()
	{
		assertEquals(NO_TICK, FightTimer.reportedTicks("Tombs of Amascut total completion time: 31:17"));
		assertEquals(NO_TICK, FightTimer.reportedTicks("Your personal best is 1:09.00"));
		assertEquals(NO_TICK, FightTimer.reportedTicks("You have completed a lap"));
		assertEquals(NO_TICK, FightTimer.reportedTicks(null));
	}

	@Test
	public void theGameSTimeIsTakenOverOnceTheOpponentIsDown()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onEngaged(10);
		timer.onGameTick(true, false, NO_TICK, 110);
		assertEquals("1:00", timer.getText());

		// Still fighting, so a time from elsewhere doesn't touch the clock.
		assertFalse(timer.takeOverTicks(90, false, 110));
		assertEquals("1:00", timer.getText());

		assertTrue(timer.takeOverTicks(90, true, 110));
		assertEquals("0:54", timer.getText());
		assertEquals(90, timer.getElapsedTicks(200));
	}

	@Test
	public void aWholeSecondShowsAsTheGamePrintedIt()
	{
		// The game rounds down to the second it prints, so the ticks have to land inside that second.
		assertEquals("31:17", FightTimer.format(FightTimer.reportedTicks("Challenge duration: 31:17"), false));
		assertEquals("0:31", FightTimer.format(FightTimer.reportedTicks("Subdued in 0:31"), false));
		assertEquals("1:03.00", FightTimer.format(FightTimer.reportedTicks("Fight duration: 1:03.00"), true));
	}

	@Test
	public void aTimeReportedBeforeTheKillIsConfirmedIsUsedOnTheNextTick()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onEngaged(10);
		timer.onGameTick(true, false, NO_TICK, 110);

		// The message comes in on the tick the boss goes down, before the kill is confirmed.
		timer.onReportedTime(90, false, 110);
		assertEquals("1:00", timer.getText());

		timer.onGameTick(true, true, 110, 111);
		assertEquals("0:54", timer.getText());
	}

	@Test
	public void aTimeThatIsNeverConfirmedIsDropped()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onEngaged(10);
		timer.onReportedTime(90, false, 110);

		for (int tick = 111; tick <= 110 + DEATH_TICKS; tick++)
		{
			timer.onGameTick(true, false, NO_TICK, tick);
		}
		timer.onGameTick(true, true, 200, 200);
		assertEquals(190, timer.getElapsedTicks(200));
	}

	@Test
	public void aTimeFarFromOursIsAboutSomethingElse()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onEngaged(10);
		timer.onGameTick(true, false, NO_TICK, 110);

		assertFalse(timer.takeOverTicks(100 + FightTimer.REPORT_GAP_TICKS + 1, true, 110));
		assertEquals("1:00", timer.getText());
		assertTrue(timer.takeOverTicks(100 + FightTimer.REPORT_GAP_TICKS, true, 110));
	}

	@Test
	public void switchingPreciseTimingRedrawsTheSameTimeWithHundredths()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onEngaged(10);
		timer.onGameTick(true, false, NO_TICK, 110);
		assertEquals("1:00", timer.getText());

		timer.setPreciseTiming(true);
		timer.onGameTick(true, false, NO_TICK, 110);
		assertEquals("1:00.00", timer.getText());
	}

	@Test
	public void theFightStartsWhenTheOpponentCanBeFought()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onEngaged(10);
		timer.onGameTick(true, false, NO_TICK, 110);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void engagingDoesNotRestartAFightThatIsRunning()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onHit(10);
		timer.onEngaged(50);
		assertEquals(10, timer.getStartTick());
	}

	// The clock carries on with no further engagement, so interaction dropping out mid-fight doesn't touch it.
	@Test
	public void aRunningFightKeepsCountingWithoutEngagement()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onEngaged(10);
		timer.onGameTick(true, false, NO_TICK, 110);
		assertEquals("1:00", timer.getText());

		timer.onGameTick(true, false, NO_TICK, 210);
		assertEquals("2:00", timer.getText());
		assertEquals(10, timer.getStartTick());
	}

	@Test
	public void engagingDoesNotRestartAStoppedFight()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 100);
		timer.onGameTick(true, true, NO_TICK, 100 + DEATH_TICKS);
		timer.onEngaged(120);
		assertEquals("1:06", timer.getText());

		timer.onGameTick(true, false, NO_TICK, 150);
		assertEquals("1:30", timer.getText());
	}

	@Test
	public void engagingAfterStartingOverKeepsTheNewStart()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.startOver(100);
		timer.onEngaged(130);
		timer.onGameTick(true, false, NO_TICK, 200);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void aDefeatBeforeTheFightStartedShowsNothing()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onGameTick(true, true, NO_TICK, 11);
		timer.onGameTick(true, true, NO_TICK, 60);
		assertNull(timer.getText());
	}

	@Test
	public void engagingWithoutAnOpponentStartsNothing()
	{
		timer.onEngaged(10);
		assertNull(timer.getText());
	}

	@Test
	public void aFightStartedWhenEngagedStopsOnDefeat()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onEngaged(0);
		timer.onGameTick(true, true, NO_TICK, 100);
		timer.onGameTick(true, true, NO_TICK, 150);
		assertEquals("1:06", timer.getText());
	}

	@Test
	public void countsFromTheFirstHitAndIgnoresLaterOnes()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onHit(20);
		assertEquals("0:00", timer.getText());
		timer.onHit(30);
		timer.onGameTick(true, false, NO_TICK, 120);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void theTextIsOnlyRebuiltWhenTheTimeChanges()
	{
		timer.opponentChanged(false, false, NO_TICK, 10);
		timer.onHit(10);
		timer.onGameTick(true, false, NO_TICK, 12);
		final String text = timer.getText();
		timer.onGameTick(true, false, NO_TICK, 12);
		assertSame(text, timer.getText());
	}

	@Test
	public void keepsCountingAfterADefeatAndStopsWhenItIsGone()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 100);
		timer.onGameTick(true, true, NO_TICK, 104);
		assertEquals("1:02", timer.getText());

		timer.opponentDespawned(true, 106);
		assertEquals("1:03", timer.getText());
		timer.onGameTick(false, false, NO_TICK, 200);
		assertEquals("1:03", timer.getText());
	}

	@Test
	public void stopsAtTheDeathWindowWhenTheOpponentNeverGoesAway()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 100);
		timer.onGameTick(true, true, NO_TICK, 100 + DEATH_TICKS);
		assertEquals("1:06", timer.getText());

		timer.onGameTick(true, true, NO_TICK, 300);
		assertEquals("1:06", timer.getText());
	}

	// The kill is only confirmed a few ticks into the death sequence, which started at the first 0.
	@Test
	public void theDeathWindowIsCountedFromTheFirstZero()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		for (int tick = 100; tick <= 300; tick++)
		{
			timer.onGameTick(true, tick >= 104, 100, tick);
		}
		assertEquals(100 + DEATH_TICKS, timer.getElapsedTicks(300));
		assertEquals("1:06", timer.getText());
	}

	@Test
	public void aLateDespawnDoesNotCountPastTheDeathWindow()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 100);
		timer.opponentDespawned(true, 200);
		assertEquals("1:06", timer.getText());
	}

	@Test
	public void anUnconfirmedZeroNeverStopsTheClock()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		for (int tick = 100; tick <= 200; tick++)
		{
			timer.onGameTick(true, false, NO_TICK, tick);
		}
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void anAliveMemberAfterTheDeathWindowRunsTheClockAgain()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 100);
		timer.onGameTick(true, true, NO_TICK, 100 + DEATH_TICKS);
		assertEquals("1:06", timer.getText());

		assertTrue(timer.opponentChanged(true, false, NO_TICK, 115));
		timer.onGameTick(true, false, NO_TICK, 200);
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void theFightEndsWhenTheOpponentIsGone()
	{
		assertEquals(130, FightTimer.endOfFight(NO_TICK, 130));
		assertEquals(106, FightTimer.endOfFight(100, 106));
		assertEquals(100 + DEATH_TICKS, FightTimer.endOfFight(100, 130));
		assertEquals(100 + DEATH_TICKS, FightTimer.endOfFight(100, 100 + DEATH_TICKS));
	}

	@Test
	public void despawningAsItDiesStopsTheTimer()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(true, 100);
		timer.onGameTick(false, false, NO_TICK, 200);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void aDefeatInsideTheDeathWindowDoesNotStopTheClock()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 50);
		timer.onGameTick(true, false, NO_TICK, 100);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void anOpponentBackUpAfterTheDeathWindowRunsTheClockAgain()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 50);
		timer.onGameTick(true, true, NO_TICK, 50 + DEATH_TICKS);
		assertEquals("0:36", timer.getText());

		timer.onGameTick(true, false, NO_TICK, 100);
		assertEquals("1:00", timer.getText());
		timer.onGameTick(true, false, NO_TICK, 200);
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void anotherOpponentWhileTheLastOneIsUpCarriesOn()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, false, NO_TICK, 100);
		assertTrue(timer.opponentChanged(false, false, NO_TICK, 101));
		timer.onGameTick(true, false, NO_TICK, 200);
		assertEquals("2:00", timer.getText());
	}

	// The clock only stops at the end of the death window, so the next opponent is picked after it.
	@Test
	public void anotherOpponentAfterADefeatStartsOver()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 100);
		timer.onGameTick(true, true, NO_TICK, 100 + DEATH_TICKS);
		assertFalse(timer.opponentChanged(false, false, NO_TICK, 111));
		assertNull(timer.getText());
		timer.onHit(115);
		assertEquals("0:00", timer.getText());
	}

	@Test
	public void aPhaseSwapToASameNamedNpcKeepsCounting()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentChanged(false, false, NO_TICK, 50);
		timer.onGameTick(true, false, NO_TICK, 100);
		assertEquals("1:00", timer.getText());

		timer.opponentDespawned(false, 110);
		timer.opponentChanged(false, false, NO_TICK, 112);
		timer.onGameTick(true, false, NO_TICK, 200);
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
		timer.onGameTick(true, false, NO_TICK, 200);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void anEarlierHitDoesNotRestartAFightThatCarriesOn()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentChanged(false, false, 50, 50);
		timer.onGameTick(true, false, NO_TICK, 100);
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
		timer.onGameTick(true, true, NO_TICK, 100);
		timer.opponentDespawned(true, 105);
		timer.onGameTick(false, false, NO_TICK, 150);
		assertEquals("1:03", timer.getText());

		assertTrue(timer.opponentChanged(true, false, NO_TICK, 150));
		timer.onGameTick(true, false, NO_TICK, 200);
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void anAliveMemberCarriesOnWhateverTheGap()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(false, 60);
		assertTrue(timer.opponentChanged(true, false, NO_TICK, 60 + SWAP_TICKS * 3));
		timer.onGameTick(true, false, NO_TICK, 200);
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
		timer.onGameTick(false, false, NO_TICK, 150);
		assertEquals("1:30", timer.getText());
		assertFalse(timer.opponentChanged(false, false, NO_TICK, 101 + SWAP_TICKS));
		assertNull(timer.getText());
	}

	@Test
	public void aKillFoundAtTheDespawnCountsAtMostTheDeathWindow()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(true, 100, 130);
		timer.onGameTick(false, false, NO_TICK, 200);
		assertEquals("1:06", timer.getText());
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
		timer.onGameTick(true, false, NO_TICK, 100);
		assertEquals("1:00", timer.getText());
		timer.onGameTick(true, false, NO_TICK, 200);
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void aReturnedOpponentsSnapshotResumesItsStartAndEnd()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 100);
		timer.onGameTick(true, true, NO_TICK, 100 + DEATH_TICKS);
		timer.opponentDespawned(false, 120);
		final FightTimer.Fight saved = timer.save(7);
		assertEquals(7, saved.group);

		// Another fight in between leaves the snapshot alone.
		timer.opponentChanged(false, false, NO_TICK, 200);
		timer.onHit(200);
		timer.onGameTick(true, false, NO_TICK, 260);
		assertEquals("0:36", timer.getText());

		timer.opponentReturned(saved, NO_TICK, 300);
		assertEquals("1:06", timer.getText());
	}

	@Test
	public void aStoppedFightStaysStoppedWhenTheOpponentComesBack()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 100);
		timer.onGameTick(true, true, NO_TICK, 100 + DEATH_TICKS);
		timer.opponentDespawned(false, 120);
		final FightTimer.Fight fight = timer.save(1);
		timer.reset();
		timer.opponentReturned(fight, NO_TICK, 200);
		assertEquals("1:06", timer.getText());
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
		timer.onGameTick(true, false, NO_TICK, 210);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void startingOverCountsFromThatTick()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, true, NO_TICK, 50);
		timer.startOver(100);
		timer.onGameTick(true, false, NO_TICK, 200);
		assertEquals("1:00", timer.getText());
	}

	// Nothing of a previous fight survives a reset, so the next opponent starts over.
	@Test
	public void aResetFightDoesNotCarryIntoTheNextOpponent()
	{
		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(0);
		timer.onGameTick(true, false, NO_TICK, 100);
		timer.reset();

		assertFalse(timer.opponentChanged(false, false, NO_TICK, 110));
		assertEquals(NO_TICK, timer.getStartTick());
	}

	@Test
	public void matchesAnyWordingOfADurationMessage()
	{
		assertTrue(FightTimer.mentionsDuration("Fight duration: 1:52. Personal best: 1:44"));
		assertTrue(FightTimer.mentionsDuration("Challenge duration: 2:30"));
		assertTrue(FightTimer.mentionsDuration("Duration: 23:10"));
		assertFalse(FightTimer.mentionsDuration("You have defeated your opponent."));
		assertFalse(FightTimer.mentionsDuration(null));
	}

	@Test
	public void reportsTheElapsedTicksOfTheRunningFight()
	{
		assertEquals(NO_TICK, timer.getElapsedTicks(10));

		timer.opponentChanged(false, false, NO_TICK, 0);
		timer.onHit(10);
		assertEquals(40, timer.getElapsedTicks(50));

		timer.onGameTick(true, true, NO_TICK, 60);
		timer.onGameTick(true, true, NO_TICK, 60 + DEATH_TICKS);
		assertEquals(60, timer.getElapsedTicks(200));
	}
}
