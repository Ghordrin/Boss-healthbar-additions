package com.ghordrin.bosshealthbar;

import java.util.HashMap;
import java.util.Map;
import static com.ghordrin.bosshealthbar.BossStats.Element.AIR;
import static com.ghordrin.bosshealthbar.BossStats.Element.EARTH;
import static com.ghordrin.bosshealthbar.BossStats.Element.FIRE;
import static com.ghordrin.bosshealthbar.BossStats.Element.WATER;
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

	@Test
	public void tableIsUnchanged()
	{
		final Map<Integer, BossStats.Weakness> weaknesses = new HashMap<>();
		final Map<Integer, Integer> drainCaps = new HashMap<>();
		for (int id = 0; id <= 0xFFFF; id++)
		{
			final BossStats.Weakness weakness = BossStats.weakness(id);
			if (weakness != null)
			{
				weaknesses.put(id, weakness);
			}
			final Integer cap = BossStats.drainCap(id);
			if (cap != null)
			{
				drainCaps.put(id, cap);
			}
		}
		assertEquals(EXPECTED_WEAKNESSES, weaknesses);
		assertEquals(EXPECTED_DRAIN_CAPS, drainCaps);
	}

	// The table as it was before the ids were switched to gameval constants.
	private static final Map<Integer, BossStats.Weakness> EXPECTED_WEAKNESSES = new HashMap<>();
	private static final Map<Integer, Integer> EXPECTED_DRAIN_CAPS = new HashMap<>();

	static
	{
		weakness(FIRE, 50, 7533);
		weakness(AIR, 50, 1672);
		weakness(EARTH, 50, 8615, 8616, 8617, 8618, 8619, 8620, 8621, 8622);
		weakness(FIRE, 30, 13685, 13686);
		weakness(FIRE, 50, 13668);
		weakness(AIR, 15, 13011);
		weakness(AIR, 15, 13013);
		weakness(WATER, 50, 12596);
		weakness(EARTH, 25, 15626, 15627);
		weakness(FIRE, 50, 8195);
		weakness(FIRE, 30, 6609);
		weakness(WATER, 40, 5862, 5863, 5866);
		weakness(AIR, 50, 2054);
		weakness(EARTH, 10, 319);
		weakness(EARTH, 35, 2266);
		weakness(EARTH, 35, 2267);
		weakness(EARTH, 35, 2265);
		weakness(EARTH, 70, 7852, 7853, 7884, 7885);
		weakness(EARTH, 25, 15628, 15629);
		weakness(AIR, 50, 1673);
		weakness(EARTH, 40, 7851, 7854, 7855, 7882, 7883, 7886, 7887, 7888, 7889);
		weakness(AIR, 15, 13012);
		weakness(FIRE, 50, 14147);
		weakness(EARTH, 50, 11753, 11754, 11755, 11761, 11763);
		weakness(EARTH, 40, 2215);
		weakness(EARTH, 50, 5779);
		weakness(EARTH, 50, 7550, 7551, 7552, 7553, 7554, 7555);
		weakness(AIR, 50, 1674);
		weakness(FIRE, 100, 8583);
		weakness(FIRE, 150, 7584, 7585);
		weakness(WATER, 15, 14180);
		weakness(FIRE, 40, 963, 965, 4303, 4304);
		weakness(AIR, 50, 1675);
		weakness(FIRE, 35, 11721);
		weakness(FIRE, 40, 11719);
		weakness(WATER, 50, 239, 2642);
		weakness(EARTH, 50, 494);
		weakness(AIR, 30, 3162);
		weakness(WATER, 30, 3129);
		weakness(EARTH, 15, 16305, 16307, 16309, 16311);
		weakness(EARTH, 40, 7561, 7562, 7563);
		weakness(FIRE, 15, 8354, 8355, 8356, 8357, 10786, 10787, 10788, 10789,
			10807, 10808, 10809, 10810);
		weakness(EARTH, 20, 7416);
		weakness(AIR, 65, 12077, 12078, 12079, 12080, 12082);
		weakness(FIRE, 40, 8713);
		weakness(FIRE, 35, 6615);
		weakness(AIR, 50, 14860);
		weakness(WATER, 40, 7286);
		weakness(FIRE, 25, 11998);
		weakness(WATER, 20, 7540, 7541, 7542, 7543, 7544, 7545);
		weakness(EARTH, 60, 14009, 14010, 14011, 14012, 14013, 14014, 14015, 14017);
		weakness(EARTH, 60, 12204, 12205, 12206, 12207);
		weakness(AIR, 20, 499);
		weakness(AIR, 50, 1676);
		weakness(EARTH, 50, 11756, 11757, 11758, 11762, 11764);
		weakness(WATER, 40, 7706);
		weakness(WATER, 40, 3127);
		weakness(FIRE, 35, 12223, 12224, 12228, 12425, 12426);
		weakness(FIRE, 40, 6610);
		weakness(AIR, 50, 1677);
		weakness(FIRE, 50, 8374, 8375, 10835, 10836, 10852, 10853);
		weakness(FIRE, 50, 7530, 7531, 7532);
		weakness(FIRE, 40, 8058, 8059, 8060, 8061);
		weakness(AIR, 50, 8340, 10768, 10772);
		weakness(WATER, 50, 14176);
		weakness(FIRE, 50, 2042, 2043, 2044);
		drainCap(10, 11789, 11790, 11791, 11792, 11793, 11794, 11795, 11796);
		drainCap(20, 11778, 11779, 11780);
		drainCap(20, 11719, 11720, 11721);
		drainCap(20, 11730, 11732);
		drainCap(30, 11761, 11762);
		drainCap(30, 9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9460);
		drainCap(30, 9416, 9417, 9418, 9419, 9420, 9421, 9422, 9423, 9424, 11153, 11154, 11155);
		drainCap(100, 8387, 8388, 10867, 10868);
		drainCap(50, 10864, 10865);
		drainCap(80, 14176);
		drainCap(30, 14707, 14708, 14709);
		drainCap(0, 8369, 8370, 8371, 8372, 8373, 8374, 8375, 10830, 10831, 10832, 10833, 10834, 10835, 10836,
			10847, 10848, 10849, 10850, 10851, 10852, 10853);
	}

	private static void weakness(BossStats.Element element, int percent, int... ids)
	{
		for (int id : ids)
		{
			EXPECTED_WEAKNESSES.put(id, new BossStats.Weakness(element, percent));
		}
	}

	private static void drainCap(int cap, int... ids)
	{
		for (int id : ids)
		{
			EXPECTED_DRAIN_CAPS.put(id, cap);
		}
	}
}
