package com.ghordrin.bosshealthbar;

import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BossStatsTest
{
	@Test
	public void looksUpWeaknessByNpcId()
	{
		assertEquals(new BossStats.Weakness(BossStats.Element.FIRE, 50), BossStats.weakness(2042));
		assertEquals(new BossStats.Weakness(BossStats.Element.FIRE, 50), BossStats.weakness(2044));
		assertEquals(new BossStats.Weakness(BossStats.Element.WATER, 40), BossStats.weakness(5862));
		assertEquals(new BossStats.Weakness(BossStats.Element.EARTH, 10), BossStats.weakness(319));
		assertEquals(new BossStats.Weakness(BossStats.Element.AIR, 65), BossStats.weakness(12082));
	}

	@Test
	public void formsOfOneBossCanDiffer()
	{
		assertEquals(35, BossStats.weakness(11721).getPercent());
		assertEquals(40, BossStats.weakness(11719).getPercent());
		assertEquals(50, BossStats.drainCap(10864).intValue());
		assertEquals(100, BossStats.drainCap(10867).intValue());
	}

	@Test
	public void looksUpDrainCapByNpcId()
	{
		assertEquals(10, BossStats.drainCap(11789).intValue());
		assertEquals(10, BossStats.drainCap(11796).intValue());
		assertEquals(30, BossStats.drainCap(9460).intValue());
		assertEquals(80, BossStats.drainCap(14176).intValue());
	}

	@Test
	public void bossesThatCantBeDrainedHaveACapOfZero()
	{
		assertEquals(0, BossStats.drainCap(8369).intValue());
		assertEquals(0, BossStats.drainCap(10853).intValue());
		assertEquals("no drain", BossStats.drainCapText(BossStats.drainCap(8370)));
	}

	@Test
	public void unknownIdsHaveNoStats()
	{
		assertNull(BossStats.weakness(1));
		assertNull(BossStats.drainCap(1));
		assertNull(BossStats.weakness(-1));
		assertNull(BossStats.drainCap(-1));
		assertNull(BossStats.drainCap(2042));
	}

	@Test
	public void statesWithoutAWeaknessCanStillHaveACap()
	{
		assertNull(BossStats.weakness(11720));
		assertEquals(20, BossStats.drainCap(11720).intValue());
		assertNull(BossStats.weakness(9423));
		assertEquals(30, BossStats.drainCap(9423).intValue());
	}

	@Test
	public void buildsDisplayInfo()
	{
		assertEquals(new BossStats.Info(BossStats.Element.FIRE, "+40%", "-20"), BossStats.info(11719));
		assertEquals(new BossStats.Info(BossStats.Element.FIRE, "+35%", "-20"), BossStats.info(11721));
		assertEquals(new BossStats.Info(null, null, "-20"), BossStats.info(11720));
		assertEquals(new BossStats.Info(BossStats.Element.FIRE, "+50%", null), BossStats.info(2042));
		assertEquals(new BossStats.Info(BossStats.Element.FIRE, "+50%", "no drain"), BossStats.info(8374));
		assertNull(BossStats.info(1));
		assertNull(BossStats.info(-1));
	}

	@Test
	public void formatsText()
	{
		assertEquals("+40%", BossStats.weaknessText(new BossStats.Weakness(BossStats.Element.FIRE, 40)));
		assertEquals("+150%", BossStats.weaknessText(BossStats.weakness(7584)));
		assertNull(BossStats.weaknessText(null));
		assertEquals("-20", BossStats.drainCapText(20));
		assertNull(BossStats.drainCapText(null));
	}

	@Test
	public void elementsUseTheirRune()
	{
		assertEquals(ItemID.AIRRUNE, BossStats.Element.AIR.getRuneItemId());
		assertEquals(ItemID.WATERRUNE, BossStats.Element.WATER.getRuneItemId());
		assertEquals(ItemID.EARTHRUNE, BossStats.Element.EARTH.getRuneItemId());
		assertEquals(ItemID.FIRERUNE, BossStats.Element.FIRE.getRuneItemId());
	}
}
