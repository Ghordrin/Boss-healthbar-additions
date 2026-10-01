package com.ghordrin.bosshealthbar;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class KnownBossesTest
{
	@Test
	public void matchesListedBossesIgnoringCase()
	{
		assertTrue(KnownBosses.contains("Zulrah"));
		assertTrue(KnownBosses.contains("zulrah"));
		assertTrue(KnownBosses.contains("K'RIL TSUTSAROTH"));
	}

	@Test
	public void rejectsOtherNamesAndNull()
	{
		assertFalse(KnownBosses.contains("Mithril dragon"));
		assertFalse(KnownBosses.contains(""));
		assertFalse(KnownBosses.contains(null));
	}

	@Test
	public void bossesUseTheirHiscoreIcon()
	{
		assertEquals(new KnownBosses.Icon(false, SpriteID.IconBoss25x25.ZULRAH), KnownBosses.icon("Zulrah"));
		assertEquals(new KnownBosses.Icon(false, SpriteID.IconBoss25x25.GROTESQUE_GUARDIANS), KnownBosses.icon("dawn"));
	}

	@Test
	public void raidBossesUseTheirPet()
	{
		assertEquals(new KnownBosses.Icon(true, ItemID.TEKTONPET), KnownBosses.icon("Tekton"));
		assertEquals(new KnownBosses.Icon(true, ItemID.VERZIKPET), KnownBosses.icon("Verzik Vitur"));
		assertEquals(new KnownBosses.Icon(true, ItemID.TEKTONENRAGEDPET), KnownBosses.icon("Tekton (enraged)"));
	}

	@Test
	public void bossesWithoutAnIconAreStillKnown()
	{
		assertTrue(KnownBosses.contains("Ice demon"));
		assertNull(KnownBosses.icon("Ice demon"));
		assertNotNull(KnownBosses.colors("Ice demon"));
	}

	@Test
	public void ignoresColorTagsInNames()
	{
		assertTrue(KnownBosses.contains("<col=00ffff>Guardian</col>"));
		assertNotNull(KnownBosses.colors("<col=00ffff>Guardian</col>"));
	}

	@Test
	public void otherNamesHaveNoIcon()
	{
		assertNull(KnownBosses.icon("Mithril dragon"));
		assertNull(KnownBosses.icon(null));
	}

	@Test
	public void everyFormOfABossSharesItsColors()
	{
		assertSame(KnownBosses.colors("Tekton"), KnownBosses.colors("Tekton (enraged)"));
		assertSame(KnownBosses.colors("Doom of Mokhaiotl"), KnownBosses.colors("Doom of Mokhaiotl (Burrowed)"));
		assertSame(KnownBosses.colors("Great Olm"), KnownBosses.colors("Great Olm (Left claw)"));
		assertSame(KnownBosses.colors("Dagannoth Rex"), KnownBosses.colors("Dagannoth Prime"));
	}

	@Test
	public void godAlignedBossesUseTheirTheme()
	{
		assertSame(HealthBarTheme.ZAMORAK.getColors(), KnownBosses.colors("K'ril Tsutsaroth"));
		assertSame(HealthBarTheme.TUMEKEN.getColors(), KnownBosses.colors("Tumeken's Warden"));
	}

	@Test
	public void otherNamesHaveNoColors()
	{
		assertNotNull(KnownBosses.colors("vorkath"));
		assertNull(KnownBosses.colors("Mithril dragon"));
		assertNull(KnownBosses.colors(null));
	}
}
