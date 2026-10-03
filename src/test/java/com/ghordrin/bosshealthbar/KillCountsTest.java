package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class KillCountsTest
{
	@Test
	public void usesTheLowercasedName()
	{
		assertEquals("zulrah", KillCounts.key("Zulrah"));
		assertEquals("k'ril tsutsaroth", KillCounts.key("K'ril Tsutsaroth"));
		assertEquals("dagannoth rex", KillCounts.key("Dagannoth Rex"));
	}

	@Test
	public void stripsColorTags()
	{
		assertEquals("vorkath", KillCounts.key("<col=ffff00>Vorkath</col>"));
		assertEquals("grotesque guardians", KillCounts.key("<col=00ffff>Dusk</col>"));
	}

	@Test
	public void formsOfOneBossShareAKey()
	{
		assertEquals("grotesque guardians", KillCounts.key("Dawn"));
		assertEquals("barrows chests", KillCounts.key("Dharok the Wretched"));
		assertEquals("lunar chest", KillCounts.key("Eclipse Moon"));
		assertEquals("royal titans", KillCounts.key("Eldric the Ice King"));
		assertEquals("doom of mokhaiotl", KillCounts.key("Doom of Mokhaiotl"));
		assertEquals("doom of mokhaiotl", KillCounts.key("Doom of Mokhaiotl (Burrowed)"));
		assertEquals("yama", KillCounts.key("Judge of Yama"));
	}

	@Test
	public void renamedBossesUseTheirKillCountName()
	{
		assertEquals("gauntlet", KillCounts.key("Crystalline Hunllef"));
		assertEquals("corrupted gauntlet", KillCounts.key("Corrupted Hunllef"));
		assertEquals("nightmare", KillCounts.key("The Nightmare"));
		assertEquals("phosani's nightmare", KillCounts.key("Phosani's Nightmare"));
		assertEquals("leviathan", KillCounts.key("The Leviathan"));
		assertEquals("whisperer", KillCounts.key("The Whisperer"));
		assertEquals("hueycoatl", KillCounts.key("The Hueycoatl"));
	}

	@Test
	public void emptyNamesHaveNoKey()
	{
		assertNull(KillCounts.key(null));
		assertNull(KillCounts.key(""));
		assertNull(KillCounts.key("<col=ff0000></col>"));
	}

	@Test
	public void otherOpponentsHaveNoKey()
	{
		assertNull(KillCounts.key("Mithril dragon"));
		assertNull(KillCounts.key("Wintertodt"));
	}

	@Test
	public void everyRenamedNameIsAKnownBoss()
	{
		for (String name : KillCounts.KEYS.keySet())
		{
			assertTrue(name, KnownBosses.contains(name));
		}
	}

	@Test
	public void formatsTheCount()
	{
		assertEquals("KC 7", KillCounts.text(7));
		assertEquals("KC 1,234", KillCounts.text(1234));
	}

	@Test
	public void missingOrZeroCountsShowNothing()
	{
		assertNull(KillCounts.text(null));
		assertNull(KillCounts.text(0));
	}
}
