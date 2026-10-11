package com.ghordrin.bosshealthbar;

import com.ghordrin.bosshealthbar.HealthReader.Reading;
import com.ghordrin.bosshealthbar.HealthReader.Source;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

	@Test
	public void staysDownUntilBackUp()
	{
		assertTrue(HealthReader.stillDown(false, true, false));
		assertTrue(HealthReader.stillDown(true, false, false));
		assertFalse(HealthReader.stillDown(true, false, true));
		assertTrue(HealthReader.stillDown(true, true, true));
		assertFalse(HealthReader.stillDown(false, false, false));
	}

	// Flips once a 0 needs a confirmed kill: today isDead alone is down, whatever the boss bars say.
	@Test
	public void isDeadIsDownForNow()
	{
		assertTrue(HealthReader.isDown(overhead(0, 30), true, true, true));
		assertTrue(HealthReader.isDown(gameBar(200, 450), true, true, true));
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
	public void anOverheadZeroIsNotDownButADeathFormAtOneIs()
	{
		assertFalse(HealthReader.isDown(overhead(0, 30), false, true, true));
		assertTrue(HealthReader.isDown(overhead(1, 60), false, true, false));
		assertFalse(HealthReader.isDown(overhead(1, 60), false, true, true));
		assertFalse(HealthReader.isDown(overhead(1, 60), false, false, false));
		assertFalse(HealthReader.isDown(null, false, true, false));
	}

	// Flips once a boss bar refilling counts: today only an overhead ratio above 1 is back up.
	@Test
	public void backUpOnlyFromTheOverheadBarForNow()
	{
		assertTrue(HealthReader.isBackUp(2, 30, false, true, true));
		assertFalse(HealthReader.isBackUp(1, 30, false, true, true));
		assertFalse(HealthReader.isBackUp(20, 30, false, true, false));
		assertFalse(HealthReader.isBackUp(20, 30, true, true, true));
		assertFalse(HealthReader.isBackUp(20, 30, false, false, true));
		assertFalse(HealthReader.isBackUp(-1, -1, false, true, true));
	}

	@Test
	public void describesWhereTheReadingCameFrom()
	{
		assertEquals("game boss bar at 300/450", HealthReader.describe(gameBar(300, 450)));
		assertEquals("raid boss bar at 0/600", HealthReader.describe(raidBar(0, 600)));
		assertEquals("overhead bar at 12/30", HealthReader.describe(overhead(12, 30)));
		assertEquals("no reading", HealthReader.describe(null));
	}
}
