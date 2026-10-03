package com.ghordrin.bosshealthbar;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.runelite.api.gameval.ItemID;

// Elemental weaknesses and how far each boss's defence can be drained in total, from the OSRS Wiki.
// Keyed by NPC ID, since forms and modes of one boss can differ.
final class BossStats
{
	@Getter
	@RequiredArgsConstructor
	enum Element
	{
		AIR(ItemID.AIRRUNE),
		WATER(ItemID.WATERRUNE),
		EARTH(ItemID.EARTHRUNE),
		FIRE(ItemID.FIRERUNE);

		private final int runeItemId;
	}

	@Value
	static class Weakness
	{
		Element element;
		int percent;
	}

	@Value
	static class Info
	{
		Element weaknessElement;
		String weaknessText;
		String drainCapText;
	}

	private static final Builder TABLE = new Builder()
		.weakness(Element.FIRE, 50, 7533)
		.weakness(Element.AIR, 50, 1672)
		.weakness(Element.EARTH, 50, 8615, 8616, 8617, 8618, 8619, 8620, 8621, 8622)
		.weakness(Element.FIRE, 30, 13685, 13686)
		.weakness(Element.FIRE, 50, 13668)
		.weakness(Element.AIR, 15, 13011)
		.weakness(Element.AIR, 15, 13013)
		.weakness(Element.WATER, 50, 12596)
		.weakness(Element.EARTH, 25, 15626, 15627)
		.weakness(Element.FIRE, 50, 8195)
		.weakness(Element.FIRE, 30, 6609)
		.weakness(Element.WATER, 40, 5862, 5863, 5866)
		.weakness(Element.AIR, 50, 2054)
		.weakness(Element.EARTH, 10, 319)
		.weakness(Element.EARTH, 35, 2266)
		.weakness(Element.EARTH, 35, 2267)
		.weakness(Element.EARTH, 35, 2265)
		.weakness(Element.EARTH, 70, 7852, 7853, 7884, 7885)
		.weakness(Element.EARTH, 25, 15628, 15629)
		.weakness(Element.AIR, 50, 1673)
		.weakness(Element.EARTH, 40, 7851, 7854, 7855, 7882, 7883, 7886, 7887, 7888, 7889)
		.weakness(Element.AIR, 15, 13012)
		.weakness(Element.FIRE, 50, 14147)
		.weakness(Element.EARTH, 50, 11753, 11754, 11755, 11761, 11763)
		.weakness(Element.EARTH, 40, 2215)
		.weakness(Element.EARTH, 50, 5779)
		.weakness(Element.EARTH, 50, 7550, 7551, 7552, 7553, 7554, 7555)
		.weakness(Element.AIR, 50, 1674)
		.weakness(Element.FIRE, 100, 8583)
		.weakness(Element.FIRE, 150, 7584, 7585)
		.weakness(Element.WATER, 15, 14180)
		.weakness(Element.FIRE, 40, 963, 965, 4303, 4304)
		.weakness(Element.AIR, 50, 1675)
		.weakness(Element.FIRE, 35, 11721)
		.weakness(Element.FIRE, 40, 11719)
		.weakness(Element.WATER, 50, 239, 2642)
		.weakness(Element.EARTH, 50, 494)
		.weakness(Element.AIR, 30, 3162)
		.weakness(Element.WATER, 30, 3129)
		.weakness(Element.EARTH, 15, 16305, 16307, 16309, 16311)
		.weakness(Element.EARTH, 40, 7561, 7562, 7563)
		.weakness(Element.FIRE, 15, 8354, 8355, 8356, 8357, 10786, 10787, 10788, 10789,
			10807, 10808, 10809, 10810)
		.weakness(Element.EARTH, 20, 7416)
		.weakness(Element.AIR, 65, 12077, 12078, 12079, 12080, 12082)
		.weakness(Element.FIRE, 40, 8713)
		.weakness(Element.FIRE, 35, 6615)
		.weakness(Element.AIR, 50, 14860)
		.weakness(Element.WATER, 40, 7286)
		.weakness(Element.FIRE, 25, 11998)
		.weakness(Element.WATER, 20, 7540, 7541, 7542, 7543, 7544, 7545)
		.weakness(Element.EARTH, 60, 14009, 14010, 14011, 14012, 14013, 14014, 14015, 14017)
		.weakness(Element.EARTH, 60, 12204, 12205, 12206, 12207)
		.weakness(Element.AIR, 20, 499)
		.weakness(Element.AIR, 50, 1676)
		.weakness(Element.EARTH, 50, 11756, 11757, 11758, 11762, 11764)
		.weakness(Element.WATER, 40, 7706)
		.weakness(Element.WATER, 40, 3127)
		.weakness(Element.FIRE, 35, 12223, 12224, 12228, 12425, 12426)
		.weakness(Element.FIRE, 40, 6610)
		.weakness(Element.AIR, 50, 1677)
		.weakness(Element.FIRE, 50, 8374, 8375, 10835, 10836, 10852, 10853)
		.weakness(Element.FIRE, 50, 7530, 7531, 7532)
		.weakness(Element.FIRE, 40, 8058, 8059, 8060, 8061)
		.weakness(Element.AIR, 50, 8340, 10768, 10772)
		.weakness(Element.WATER, 50, 14176)
		.weakness(Element.FIRE, 50, 2042, 2043, 2044)
		.drainCap(10, 11789, 11790, 11791, 11792, 11793, 11794, 11795, 11796)
		.drainCap(20, 11778, 11779, 11780)
		.drainCap(20, 11719, 11720, 11721)
		.drainCap(20, 11730, 11732)
		// The real cap doubles after the enrage, but the NPC ID stays the same.
		.drainCap(30, 11761, 11762)
		.drainCap(30, 9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9460)
		.drainCap(30, 9416, 9417, 9418, 9419, 9420, 9421, 9422, 9423, 9424, 11153, 11154, 11155)
		.drainCap(100, 8387, 8388, 10867, 10868)
		.drainCap(50, 10864, 10865)
		.drainCap(80, 14176)
		.drainCap(30, 14707, 14708, 14709)
		.drainCap(0, 8369, 8370, 8371, 8372, 8373, 8374, 8375, 10830, 10831, 10832, 10833, 10834, 10835, 10836,
			10847, 10848, 10849, 10850, 10851, 10852, 10853);

	private static final Map<Integer, Weakness> WEAKNESSES = TABLE.weaknesses.build();
	private static final Map<Integer, Integer> DRAIN_CAPS = TABLE.drainCaps.build();

	private BossStats()
	{
	}

	static Weakness weakness(int npcId)
	{
		return WEAKNESSES.get(npcId);
	}

	static Integer drainCap(int npcId)
	{
		return DRAIN_CAPS.get(npcId);
	}

	static Info info(int npcId)
	{
		final Weakness weakness = weakness(npcId);
		final Integer drainCap = drainCap(npcId);
		if (weakness == null && drainCap == null)
		{
			return null;
		}
		return new Info(weakness != null ? weakness.getElement() : null, weaknessText(weakness), drainCapText(drainCap));
	}

	static String weaknessText(Weakness weakness)
	{
		return weakness != null ? "+" + weakness.getPercent() + "%" : null;
	}

	static String drainCapText(Integer cap)
	{
		if (cap == null)
		{
			return null;
		}
		return cap > 0 ? "-" + cap : "no drain";
	}

	private static final class Builder
	{
		private final ImmutableMap.Builder<Integer, Weakness> weaknesses = ImmutableMap.builder();
		private final ImmutableMap.Builder<Integer, Integer> drainCaps = ImmutableMap.builder();

		Builder weakness(Element element, int percent, int... npcIds)
		{
			final Weakness weakness = new Weakness(element, percent);
			for (int npcId : npcIds)
			{
				weaknesses.put(npcId, weakness);
			}
			return this;
		}

		Builder drainCap(int cap, int... npcIds)
		{
			for (int npcId : npcIds)
			{
				drainCaps.put(npcId, cap);
			}
			return this;
		}
	}
}
