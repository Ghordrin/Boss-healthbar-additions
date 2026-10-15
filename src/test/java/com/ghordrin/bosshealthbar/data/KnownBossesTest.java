package com.ghordrin.bosshealthbar.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.theme.HealthBarTheme;
import com.ghordrin.bosshealthbar.theme.ThemeColors;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;
import org.junit.Test;

public class KnownBossesTest
{
	private static KnownBosses.Icon icon(String name)
	{
		final KnownBosses.Boss boss = KnownBosses.find(name);
		return boss != null ? boss.getIcon() : null;
	}

	private static ThemeColors colors(String name)
	{
		final KnownBosses.Boss boss = KnownBosses.find(name);
		return boss != null ? boss.getColors() : null;
	}

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
		assertEquals(new KnownBosses.Icon(false, SpriteID.IconBoss25x25.ZULRAH), icon("Zulrah"));
		assertEquals(new KnownBosses.Icon(false, SpriteID.IconBoss25x25.GROTESQUE_GUARDIANS), icon("dawn"));
	}

	@Test
	public void raidBossesUseTheirPet()
	{
		assertEquals(new KnownBosses.Icon(true, ItemID.TEKTONPET), icon("Tekton"));
		assertEquals(new KnownBosses.Icon(true, ItemID.VERZIKPET), icon("Verzik Vitur"));
		assertEquals(new KnownBosses.Icon(true, ItemID.TEKTONENRAGEDPET), icon("Tekton (enraged)"));
	}

	@Test
	public void bossesWithoutAnIconAreStillKnown()
	{
		assertTrue(KnownBosses.contains("Ice demon"));
		assertNull(icon("Ice demon"));
		assertNotNull(colors("Ice demon"));
	}

	@Test
	public void ignoresColorTagsInNames()
	{
		assertTrue(KnownBosses.contains("<col=00ffff>Guardian</col>"));
		assertNotNull(colors("<col=00ffff>Guardian</col>"));
	}

	@Test
	public void otherNamesHaveNoIcon()
	{
		assertNull(icon("Mithril dragon"));
		assertNull(icon(null));
	}

	private static String shortName(String name)
	{
		final KnownBosses.Boss boss = KnownBosses.find(name);
		return boss != null ? boss.getShortName() : null;
	}

	@Test
	public void theShortNameKeepsTheFormTheNameSpellsOut()
	{
		assertEquals("Doom", shortName("Doom of Mokhaiotl"));
		assertEquals("Doom (Shielded)", shortName("Doom of Mokhaiotl (Shielded)"));
		assertEquals("Doom (Burrowed)", shortName("Doom of Mokhaiotl (Burrowed)"));
	}

	@Test
	public void bossesWithoutAShortNameHaveNone()
	{
		assertNull(shortName("General Graardor"));
		assertNull(shortName("Tekton (enraged)"));
		assertNull(shortName("Mithril dragon"));
	}

	@Test
	public void theShortNameAndTheKillCountKeyBothSurvive()
	{
		assertEquals("doom of mokhaiotl", KnownBosses.find("Doom of Mokhaiotl (Burrowed)").getKillCountKey());
		assertEquals("Doom (Burrowed)", shortName("Doom of Mokhaiotl (Burrowed)"));
	}

	@Test
	public void everyFormOfABossSharesItsColors()
	{
		assertSame(colors("Tekton"), colors("Tekton (enraged)"));
		assertSame(colors("Doom of Mokhaiotl"), colors("Doom of Mokhaiotl (Burrowed)"));
		assertSame(colors("Great Olm"), colors("Great Olm (Left claw)"));
		assertSame(colors("Dagannoth Rex"), colors("Dagannoth Prime"));
		assertSame(colors("The Nightmare"), colors("Phosani's Nightmare"));
		assertEquals(icon("The Nightmare"), icon("Phosani's Nightmare"));
	}

	@Test
	public void godAlignedBossesUseTheirTheme()
	{
		assertSame(HealthBarTheme.ZAMORAK.getColors(), colors("K'ril Tsutsaroth"));
		assertSame(HealthBarTheme.TUMEKEN.getColors(), colors("Tumeken's Warden"));
	}

	@Test
	public void otherNamesHaveNoColors()
	{
		assertNotNull(colors("vorkath"));
		assertNull(colors("Mithril dragon"));
		assertNull(colors(null));
	}
}
