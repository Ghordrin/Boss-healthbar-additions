package com.ghordrin.bosshealthbar.integrations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.ghordrin.bosshealthbar.data.KnownBosses;
import org.junit.Test;

public class KillCountsTest
{
	private static String key(String name)
	{
		final KnownBosses.Boss boss = KnownBosses.find(name);
		return boss != null ? boss.getKillCountKey() : null;
	}

	@Test
	public void usesTheLowercasedName()
	{
		assertEquals("zulrah", key("Zulrah"));
		assertEquals("k'ril tsutsaroth", key("K'ril Tsutsaroth"));
		assertEquals("dagannoth rex", key("Dagannoth Rex"));
	}

	@Test
	public void stripsColorTags()
	{
		assertEquals("vorkath", key("<col=ffff00>Vorkath</col>"));
		assertEquals("grotesque guardians", key("<col=00ffff>Dusk</col>"));
	}

	// Every form of a boss has to land on the one key the game counts it under.
	@Test
	public void formsOfOneBossShareAKey()
	{
		assertEquals("grotesque guardians", key("Dawn"));
		assertEquals("grotesque guardians", key("Dusk"));
		assertEquals("barrows chests", key("Dharok the Wretched"));
		assertEquals("barrows chests", key("Ahrim the Blighted"));
		assertEquals("barrows chests", key("Guthan the Infested"));
		assertEquals("barrows chests", key("Karil the Tainted"));
		assertEquals("barrows chests", key("Torag the Corrupted"));
		assertEquals("barrows chests", key("Verac the Defiled"));
		assertEquals("lunar chest", key("Eclipse Moon"));
		assertEquals("lunar chest", key("Blood Moon"));
		assertEquals("lunar chest", key("Blue Moon"));
		assertEquals("royal titans", key("Eldric the Ice King"));
		assertEquals("royal titans", key("Branda the Fire Queen"));
		assertEquals("doom of mokhaiotl", key("Doom of Mokhaiotl"));
		assertEquals("doom of mokhaiotl", key("Doom of Mokhaiotl (Burrowed)"));
		assertEquals("doom of mokhaiotl", key("Doom of Mokhaiotl (Shielded)"));
		assertEquals("mimic", key("The Mimic"));
		assertEquals("yama", key("Judge of Yama"));
		assertEquals("yama", key("Yama"));
	}

	@Test
	public void renamedBossesUseTheirKillCountName()
	{
		assertEquals("gauntlet", key("Crystalline Hunllef"));
		assertEquals("corrupted gauntlet", key("Corrupted Hunllef"));
		assertEquals("nightmare", key("The Nightmare"));
		assertEquals("phosani's nightmare", key("Phosani's Nightmare"));
		assertEquals("leviathan", key("The Leviathan"));
		assertEquals("whisperer", key("The Whisperer"));
		assertEquals("hueycoatl", key("The Hueycoatl"));
	}

	@Test
	public void emptyNamesHaveNoKey()
	{
		assertNull(key(null));
		assertNull(key(""));
		assertNull(key("<col=ff0000></col>"));
	}

	@Test
	public void otherOpponentsHaveNoKey()
	{
		assertNull(key("Mithril dragon"));
		assertNull(key("Wintertodt"));
	}

	@Test
	public void trimsTheName()
	{
		assertEquals("zulrah", key(" Zulrah "));
		assertEquals("grotesque guardians", key("<col=00ffff> Dawn</col>"));
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
