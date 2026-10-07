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
	private final FightTimer timer = new FightTimer();

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
	public void aSameNamedNpcCarriesOnAnUndefeatedFight()
	{
		assertTrue(FightTimer.keepsCounting("Boss", false, NO_TICK, "Boss", 100));
		assertFalse(FightTimer.keepsCounting("Boss", true, NO_TICK, "Boss", 100));
		assertFalse(FightTimer.keepsCounting("Boss", false, NO_TICK, "Minion", 100));
		assertFalse(FightTimer.keepsCounting(null, false, NO_TICK, "Boss", 100));
	}

	@Test
	public void aSameNamedNpcOnlyCarriesOnShortlyAfterTheLastOneWentAway()
	{
		assertTrue(FightTimer.keepsCounting("Boss", false, 100, "Boss", 100 + SWAP_TICKS));
		assertFalse(FightTimer.keepsCounting("Boss", false, 100, "Boss", 101 + SWAP_TICKS));
	}

	@Test
	public void showsNothingBeforeTheFirstHit()
	{
		timer.opponentChanged("Boss", false, NO_TICK,10);
		timer.onGameTick(true, false, 11);
		assertNull(timer.getText());
	}

	@Test
	public void countsFromTheFirstHitAndIgnoresLaterOnes()
	{
		timer.opponentChanged("Boss", false, NO_TICK,10);
		timer.onHit(20);
		assertEquals("0:00", timer.getText());
		timer.onHit(30);
		timer.onGameTick(true, false, 120);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void theTextIsOnlyRebuiltWhenTheTimeChanges()
	{
		timer.opponentChanged("Boss", false, NO_TICK,10);
		timer.onHit(10);
		timer.onGameTick(true, false, 12);
		final String text = timer.getText();
		timer.onGameTick(true, false, 12);
		assertSame(text, timer.getText());
	}

	@Test
	public void stopsWhenDefeatedAndKeepsTheTime()
	{
		timer.opponentChanged("Boss", false, NO_TICK,0);
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
		timer.opponentChanged("Boss", false, NO_TICK,0);
		timer.onHit(0);
		timer.opponentDespawned(true, 100);
		timer.onGameTick(false, false, 200);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void carriesOnWhenItComesBackFromZero()
	{
		timer.opponentChanged("Boss", false, NO_TICK,0);
		timer.onHit(0);
		timer.onGameTick(true, true, 50);
		timer.onGameTick(true, false, 100);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void aDifferentOpponentStartsOver()
	{
		timer.opponentChanged("Boss", false, NO_TICK,0);
		timer.onHit(0);
		timer.onGameTick(true, false, 100);
		timer.opponentChanged("Minion", false, NO_TICK, 101);
		assertNull(timer.getText());
		timer.onHit(105);
		assertEquals("0:00", timer.getText());
	}

	@Test
	public void aPhaseSwapToASameNamedNpcKeepsCounting()
	{
		timer.opponentChanged("Boss", false, NO_TICK,0);
		timer.onHit(0);
		timer.opponentChanged("Boss", false, NO_TICK,50);
		timer.onGameTick(true, false, 100);
		assertEquals("1:00", timer.getText());

		timer.opponentDespawned(false, 110);
		timer.opponentChanged("Boss", false, NO_TICK,112);
		timer.onGameTick(true, false, 200);
		assertEquals("2:00", timer.getText());
	}

	@Test
	public void aSameNamedNpcAfterADefeatStartsOver()
	{
		timer.opponentChanged("Boss", false, NO_TICK,0);
		timer.onHit(0);
		timer.opponentChanged("Boss", true, NO_TICK, 50);
		assertNull(timer.getText());

		timer.onHit(60);
		timer.opponentDespawned(true, 100);
		timer.opponentChanged("Boss", false, NO_TICK,101);
		assertNull(timer.getText());
	}

	@Test
	public void aSameNamedNpcLongAfterTheLastOneWentAwayStartsOver()
	{
		timer.opponentChanged("Boss", false, NO_TICK,0);
		timer.onHit(0);
		timer.opponentDespawned(false, 50);
		timer.opponentChanged("Boss", false, NO_TICK,51 + SWAP_TICKS);
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
		timer.opponentChanged("Boss", false, 100, 101);
		timer.onGameTick(true, false, 200);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void anEarlierHitDoesNotRestartAFightThatCarriesOn()
	{
		timer.opponentChanged("Boss", false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentChanged("Boss", false, 50, 50);
		timer.onGameTick(true, false, 100);
		assertEquals("1:00", timer.getText());
	}

	@Test
	public void aSameNamedNpcSoonAfterADefeatedOneDespawnedStartsOver()
	{
		timer.opponentChanged("Boss", false, NO_TICK, 0);
		timer.onHit(0);
		timer.opponentDespawned(true, 100);
		timer.opponentChanged("Boss", false, NO_TICK, 102);
		assertNull(timer.getText());
	}

	@Test
	public void resetClearsTheFight()
	{
		timer.opponentChanged("Boss", false, NO_TICK,0);
		timer.onHit(0);
		timer.reset();
		assertNull(timer.getText());
		timer.opponentChanged("Boss", false, NO_TICK,5);
		assertNull(timer.getText());
	}

	@Test
	public void theSlotSizingSwapsEveryDigit()
	{
		assertEquals("0:00", BarTextPainter.withDigits("1:23", '0'));
		assertEquals("4:44:44", BarTextPainter.withDigits("1:05:09", '4'));
	}
}
