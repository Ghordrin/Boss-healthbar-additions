package com.ghordrin.bosshealthbar;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;

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
		.weakness(Element.FIRE, 50, NpcID.RAIDS_VESPULA_PORTAL)
		.weakness(Element.AIR, 50, NpcID.BARROWS_AHRIM)
		.weakness(Element.EARTH, 50, NpcID.HYDRABOSS, NpcID.HYDRABOSS_P1_TRANSITION, NpcID.HYDRABOSS_P2_TRANSITION,
			NpcID.HYDRABOSS_P3_TRANSITION, NpcID.HYDRABOSS_4, NpcID.HYDRABOSS_3, NpcID.HYDRABOSS_2,
			NpcID.HYDRABOSS_FINALDEATH)
		.weakness(Element.FIRE, 30, NpcID.AMOXLIATL, NpcID.AMOXLIATL_QUEST)
		.weakness(Element.FIRE, 50, NpcID.ARAXXOR)
		.weakness(Element.AIR, 15, NpcID.PMOON_BOSS_BLOOD_MOON_VIS)
		.weakness(Element.AIR, 15, NpcID.PMOON_BOSS_BLUE_MOON_VIS)
		.weakness(Element.WATER, 50, NpcID.RT_FIRE_QUEEN)
		.weakness(Element.EARTH, 25, NpcID.COWBOSS, NpcID.COWBOSS_ROUTEFIND)
		.weakness(Element.FIRE, 50, NpcID.GB_MOSSGIANT)
		.weakness(Element.FIRE, 30, NpcID.CALLISTO)
		.weakness(Element.WATER, 40, NpcID.CERBERUS_ATTACKING, NpcID.CERBERUS_SITTING, NpcID.CERBERUS_RESETTING)
		.weakness(Element.AIR, 50, NpcID.CHAOSELEMENTAL)
		.weakness(Element.EARTH, 10, NpcID.CORP_BEAST)
		.weakness(Element.EARTH, 35, NpcID.DAGCAVE_MAGIC_BOSS)
		.weakness(Element.EARTH, 35, NpcID.DAGCAVE_MELEE_BOSS)
		.weakness(Element.EARTH, 35, NpcID.DAGCAVE_RANGED_BOSS)
		.weakness(Element.EARTH, 70, NpcID.GARGBOSS_DAWN_PHASE1, NpcID.GARGBOSS_DAWN_PHASE1_TRANSITION,
			NpcID.GARGBOSS_DAWN_PHASE3, NpcID.GARGBOSS_DAWN_DEATH)
		.weakness(Element.EARTH, 25, NpcID.COWBOSS_HARDMODE, NpcID.COWBOSS_HARDMODE_GHOST)
		.weakness(Element.AIR, 50, NpcID.BARROWS_DHAROK)
		.weakness(Element.EARTH, 40, NpcID.GARGBOSS_DUSK_PHASE1_DEFENSIVE, NpcID.GARGBOSS_DUSK_PHASE1_TRANSITION,
			NpcID.GARGBOSS_DUSK_PHASE1_FLYTRANSITION, NpcID.GARGBOSS_DUSK_PHASE2_ATTACKING,
			NpcID.GARGBOSS_DUSK_PHASE3_DEFENSIVE, NpcID.GARGBOSS_DUSK_PHASE3_TRANSITION,
			NpcID.GARGBOSS_DUSK_PHASE4_SPAWN, NpcID.GARGBOSS_DUSK_PHASE4, NpcID.GARGBOSS_DUSK_DEATH)
		.weakness(Element.AIR, 15, NpcID.PMOON_BOSS_ECLIPSE_MOON_VIS)
		.weakness(Element.FIRE, 50, NpcID.RT_ICE_KING)
		.weakness(Element.EARTH, 50, NpcID.TOA_WARDEN_ELIDINIS_PHASE2_MAGE, NpcID.TOA_WARDEN_ELIDINIS_PHASE2_RANGE,
			NpcID.TOA_WARDEN_ELIDINIS_PHASE2_EXPOSED, NpcID.TOA_WARDEN_ELIDINIS_PHASE3,
			NpcID.TOA_WARDEN_ELIDINIS_PHASE3_CHARGING)
		.weakness(Element.EARTH, 40, NpcID.GODWARS_BANDOS_AVATAR)
		.weakness(Element.EARTH, 50, NpcID.MOLE_GIANT)
		.weakness(Element.EARTH, 50, NpcID.OLM_HAND_RIGHT_SPAWNING, NpcID.OLM_HEAD_SPAWNING,
			NpcID.OLM_HAND_LEFT_SPAWNING, NpcID.OLM_HAND_RIGHT, NpcID.OLM_HEAD, NpcID.OLM_HAND_LEFT)
		.weakness(Element.AIR, 50, NpcID.BARROWS_GUTHAN)
		.weakness(Element.FIRE, 100, NpcID.HESPORI)
		.weakness(Element.FIRE, 150, NpcID.RAIDS_ICEDEMON_NONCOMBAT, NpcID.RAIDS_ICEDEMON_COMBAT)
		.weakness(Element.WATER, 15, NpcID.YAMA_JUDGE_OF_YAMA)
		.weakness(Element.FIRE, 40, NpcID.KALPHITE_QUEEN, NpcID.KALPHITE_FLYINGQUEEN, NpcID.SWAN_KALPHITE_1,
			NpcID.SWAN_KALPHITE_2)
		.weakness(Element.AIR, 50, NpcID.BARROWS_KARIL)
		.weakness(Element.FIRE, 35, NpcID.TOA_KEPHRI_BOSS_ENRAGE)
		.weakness(Element.FIRE, 40, NpcID.TOA_KEPHRI_BOSS_SHIELDED)
		.weakness(Element.WATER, 50, NpcID.KING_DRAGON, NpcID.TWOCATS_KBD_CUTSCENE)
		.weakness(Element.EARTH, 50, NpcID.SLAYER_KRAKEN_BOSS)
		.weakness(Element.AIR, 30, NpcID.GODWARS_ARMADYL_AVATAR)
		.weakness(Element.WATER, 30, NpcID.GODWARS_ZAMORAK_AVATAR)
		.weakness(Element.EARTH, 15, NpcID.MAD_ANGEL, NpcID.MAD_ANGEL_ANIM, NpcID.MAD_ANGEL_QUEST,
			NpcID.MAD_ANGEL_ANIM_QUEST)
		.weakness(Element.EARTH, 40, NpcID.RAIDS_DOGODILE_SUBMERGED, NpcID.RAIDS_DOGODILE_JUNIOR,
			NpcID.RAIDS_DOGODILE)
		.weakness(Element.FIRE, 15, NpcID.NYLOCAS_BOSS_SPAWNING, NpcID.NYLOCAS_BOSS_MELEE, NpcID.NYLOCAS_BOSS_MAGIC,
			NpcID.NYLOCAS_BOSS_RANGED, NpcID.NYLOCAS_BOSS_SPAWNING_STORY, NpcID.NYLOCAS_BOSS_MELEE_STORY,
			NpcID.NYLOCAS_BOSS_MAGIC_STORY, NpcID.NYLOCAS_BOSS_RANGED_STORY, NpcID.NYLOCAS_BOSS_SPAWNING_HARD,
			NpcID.NYLOCAS_BOSS_MELEE_HARD, NpcID.NYLOCAS_BOSS_MAGIC_HARD, NpcID.NYLOCAS_BOSS_RANGED_HARD)
		.weakness(Element.EARTH, 20, NpcID.HILLGIANT_BOSS)
		.weakness(Element.AIR, 65, NpcID.MUSPAH, NpcID.MUSPAH_MELEE, NpcID.MUSPAH_SOULSPLIT, NpcID.MUSPAH_FINAL,
			NpcID.MUSPAH_TELEPORT)
		.weakness(Element.FIRE, 40, NpcID.SARACHNIS)
		.weakness(Element.FIRE, 35, NpcID.SCORPIA)
		.weakness(Element.AIR, 50, NpcID.GRYPHON_BOSS)
		.weakness(Element.WATER, 40, NpcID.CATA_BOSS)
		.weakness(Element.FIRE, 25, NpcID.VENENATIS_SINGLES)
		.weakness(Element.WATER, 20, NpcID.RAIDS_TEKTON_WAITING, NpcID.RAIDS_TEKTON_WALKING_STANDARD,
			NpcID.RAIDS_TEKTON_FIGHTING_STANDARD, NpcID.RAIDS_TEKTON_WALKING_ENRAGED,
			NpcID.RAIDS_TEKTON_FIGHTING_ENRAGED, NpcID.RAIDS_TEKTON_HAMMERING)
		.weakness(Element.EARTH, 60, NpcID.HUEY_HEAD, NpcID.HUEY_HEAD_RESPAWN_PLACEHOLDER, NpcID.HUEY_HEAD_INVULNERABLE,
			NpcID.HUEY_HEAD_DEFEATED, NpcID.HUEY_HEAD_ENRAGED, NpcID.HUEY_TAIL, NpcID.HUEY_TAIL_BROKEN,
			NpcID.HUEY_BODY_PART)
		.weakness(Element.EARTH, 60, NpcID.WHISPERER, NpcID.WHISPERER_MELEE, NpcID.WHISPERER_QUEST,
			NpcID.WHISPERER_MELEE_QUEST)
		.weakness(Element.AIR, 20, NpcID.SMOKE_DEVIL_BOSS)
		.weakness(Element.AIR, 50, NpcID.BARROWS_TORAG)
		.weakness(Element.EARTH, 50, NpcID.TOA_WARDEN_TUMEKEN_PHASE2_MAGE, NpcID.TOA_WARDEN_TUMEKEN_PHASE2_RANGE,
			NpcID.TOA_WARDEN_TUMEKEN_PHASE2_EXPOSED, NpcID.TOA_WARDEN_TUMEKEN_PHASE3,
			NpcID.TOA_WARDEN_TUMEKEN_PHASE3_CHARGING)
		.weakness(Element.WATER, 40, NpcID.INFERNO_TZKALZUK_PLACEHOLDER)
		.weakness(Element.WATER, 40, NpcID.TZHAAR_FIGHTCAVE_SWARM_BOSS)
		.weakness(Element.FIRE, 35, NpcID.VARDORVIS, NpcID.VARDORVIS_QUEST, NpcID.VARDORVIS_CUTSCENE,
			NpcID.VARDORVIS_BASE_QUEST, NpcID.VARDORVIS_BASE_POSTQUEST)
		.weakness(Element.FIRE, 40, NpcID.VENENATIS)
		.weakness(Element.AIR, 50, NpcID.BARROWS_VERAC)
		.weakness(Element.FIRE, 50, NpcID.VERZIK_PHASE3, NpcID.VERZIK_DEATH_BAT, NpcID.VERZIK_PHASE3_STORY,
			NpcID.VERZIK_DEATH_BAT_STORY, NpcID.VERZIK_PHASE3_HARD, NpcID.VERZIK_DEATH_BAT_HARD)
		.weakness(Element.FIRE, 50, NpcID.RAIDS_VESPULA_FLYING, NpcID.RAIDS_VESPULA_ENRAGED,
			NpcID.RAIDS_VESPULA_WALKING)
		.weakness(Element.FIRE, 40, NpcID.VORKATH_SLEEPING_NOOP, NpcID.VORKATH_SLEEPING, NpcID.VORKATH_QUEST,
			NpcID.VORKATH)
		.weakness(Element.AIR, 50, NpcID.TOB_XARPUS_COMBAT, NpcID.TOB_XARPUS_COMBAT_STORY,
			NpcID.TOB_XARPUS_COMBAT_HARD)
		.weakness(Element.WATER, 50, NpcID.YAMA)
		.weakness(Element.FIRE, 50, NpcID.SNAKEBOSS_BOSS_RANGED, NpcID.SNAKEBOSS_BOSS_MELEE,
			NpcID.SNAKEBOSS_BOSS_MAGIC)
		.drainCap(10, NpcID.AKKHA_SPAWN, NpcID.AKKHA_MELEE, NpcID.AKKHA_RANGE, NpcID.AKKHA_MAGE,
			NpcID.AKKHA_ENRAGE_SPAWN, NpcID.AKKHA_ENRAGE_INITIAL, NpcID.AKKHA_ENRAGE, NpcID.AKKHA_ENRAGE_DUMMY)
		.drainCap(20, NpcID.TOA_BABA, NpcID.TOA_BABA_COFFIN, NpcID.TOA_BABA_DIGGING)
		.drainCap(20, NpcID.TOA_KEPHRI_BOSS_SHIELDED, NpcID.TOA_KEPHRI_BOSS_WEAK, NpcID.TOA_KEPHRI_BOSS_ENRAGE)
		.drainCap(20, NpcID.TOA_ZEBAK, NpcID.TOA_ZEBAK_ENRAGED)
		// The real cap doubles after the enrage, but the NPC ID stays the same.
		.drainCap(30, NpcID.TOA_WARDEN_ELIDINIS_PHASE3, NpcID.TOA_WARDEN_TUMEKEN_PHASE3)
		.drainCap(30, NpcID.NIGHTMARE_PHASE_01, NpcID.NIGHTMARE_PHASE_02, NpcID.NIGHTMARE_PHASE_03,
			NpcID.NIGHTMARE_WEAK_PHASE_01, NpcID.NIGHTMARE_WEAK_PHASE_02, NpcID.NIGHTMARE_WEAK_PHASE_03,
			NpcID.NIGHTMARE_BLAST, NpcID.NIGHTMARE_INITIAL, NpcID.NIGHTMARE_DYING, NpcID.NIGHTMARE_ENTRY_READY)
		.drainCap(30, NpcID.NIGHTMARE_CHALLENGE_PHASE_01, NpcID.NIGHTMARE_CHALLENGE_PHASE_02,
			NpcID.NIGHTMARE_CHALLENGE_PHASE_03, NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_01,
			NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_02, NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_03,
			NpcID.NIGHTMARE_CHALLENGE_BLAST, NpcID.NIGHTMARE_CHALLENGE_INITIAL, NpcID.NIGHTMARE_CHALLENGE_DYING,
			NpcID.NIGHTMARE_CHALLENGE_PHASE_04, NpcID.NIGHTMARE_CHALLENGE_PHASE_05,
			NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_04)
		.drainCap(100, NpcID.TOB_SOTETSEG_NONCOMBAT, NpcID.TOB_SOTETSEG_COMBAT, NpcID.TOB_SOTETSEG_NONCOMBAT_HARD,
			NpcID.TOB_SOTETSEG_COMBAT_HARD)
		.drainCap(50, NpcID.TOB_SOTETSEG_NONCOMBAT_STORY, NpcID.TOB_SOTETSEG_COMBAT_STORY)
		.drainCap(80, NpcID.YAMA)
		.drainCap(30, NpcID.DOM_BOSS, NpcID.DOM_BOSS_SHIELDED, NpcID.DOM_BOSS_BURROWED)
		.drainCap(0, NpcID.VERZIK_INITIAL, NpcID.VERZIK_PHASE1, NpcID.VERZIK_PHASE1_TO2_TRANSITION,
			NpcID.VERZIK_PHASE2, NpcID.VERZIK_PHASE2_TO3_TRANSITION, NpcID.VERZIK_PHASE3, NpcID.VERZIK_DEATH_BAT,
			NpcID.VERZIK_INITIAL_STORY, NpcID.VERZIK_PHASE1_STORY, NpcID.VERZIK_PHASE1_TO2_TRANSITION_STORY,
			NpcID.VERZIK_PHASE2_STORY, NpcID.VERZIK_PHASE2_TO3_TRANSITION_STORY, NpcID.VERZIK_PHASE3_STORY,
			NpcID.VERZIK_DEATH_BAT_STORY, NpcID.VERZIK_INITIAL_HARD, NpcID.VERZIK_PHASE1_HARD,
			NpcID.VERZIK_PHASE1_TO2_TRANSITION_HARD, NpcID.VERZIK_PHASE2_HARD,
			NpcID.VERZIK_PHASE2_TO3_TRANSITION_HARD, NpcID.VERZIK_PHASE3_HARD, NpcID.VERZIK_DEATH_BAT_HARD);

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
