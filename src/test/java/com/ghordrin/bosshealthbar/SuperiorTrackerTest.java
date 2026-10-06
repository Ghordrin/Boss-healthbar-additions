package com.ghordrin.bosshealthbar;

import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.gameval.NpcID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SuperiorTrackerTest
{
	private static SuperiorTracker.Spawn<String> known(String npc)
	{
		return new SuperiorTracker.Spawn<>(npc, true, false);
	}

	private static SuperiorTracker.Spawn<String> eligible(String npc)
	{
		return new SuperiorTracker.Spawn<>(npc, false, true);
	}

	private static SuperiorTracker.Spawn<String> ineligible(String npc)
	{
		return new SuperiorTracker.Spawn<>(npc, false, false);
	}

	@Test
	public void knownSuperiorsAreRecognisedById()
	{
		assertTrue(SuperiorIds.contains(NpcID.SUPERIOR_ABYSSAL_DEMON));
		assertTrue(SuperiorIds.contains(NpcID.SUPERIOR_GARGOYLE_DEAD));
		assertTrue(SuperiorIds.contains(NpcID.LEAGUE_SUPERIOR_HYDRA));
		assertTrue(SuperiorIds.contains(NpcID.DEADMAN_BREACH_SUPERIOR_DARK_BEAST));
		assertTrue(SuperiorIds.contains(NpcID.SUPERIOR_VENATOR));
	}

	@Test
	public void thrallsDeathSpawnsAndQuestMonstersAreNotSuperiors()
	{
		assertFalse(SuperiorIds.contains(NpcID.ARCEUUS_THRALL_GHOST_SUPERIOR));
		assertFalse(SuperiorIds.contains(NpcID.ARCEUUS_THRALL_SKELETON_SUPERIOR));
		assertFalse(SuperiorIds.contains(NpcID.ARCEUUS_THRALL_ZOMBIE_SUPERIOR));
		assertFalse(SuperiorIds.contains(NpcID.THRALL_IMP_MAGIC_SUPERIOR));
		assertFalse(SuperiorIds.contains(NpcID.THRALL_IMP_RANGED_SUPERIOR));
		assertFalse(SuperiorIds.contains(NpcID.THRALL_IMP_MELEE_SUPERIOR));
		assertFalse(SuperiorIds.contains(NpcID.SUPERIOR_NECHRYAEL_MELEE_SPAWN));
		assertFalse(SuperiorIds.contains(NpcID.SUPERIOR_NECHRYAEL_MAGIC_SPAWN));
		assertFalse(SuperiorIds.contains(NpcID.VIKINGEXILE_BASILISK_SUPERIOR));
		assertFalse(SuperiorIds.contains(NpcID.SLAYER_ABYSSAL));
	}

	@Test
	public void aKnownSuperiorTakesTheMessageOverACloserSpawn()
	{
		final SuperiorTracker.Match<String> match = SuperiorTracker.match(Arrays.asList(eligible("closer"), known("superior")));
		assertTrue(match.isDecided());
		assertNull(match.getSuperior());
	}

	@Test
	public void oneUnknownCandidateIsMarked()
	{
		final SuperiorTracker.Match<String> match = SuperiorTracker.match(Arrays.asList(ineligible("far"), eligible("new")));
		assertTrue(match.isDecided());
		assertEquals("new", match.getSuperior());
	}

	@Test
	public void twoCandidatesMarkNone()
	{
		final SuperiorTracker.Match<String> match = SuperiorTracker.match(Arrays.asList(eligible("a"), eligible("b")));
		assertTrue(match.isDecided());
		assertNull(match.getSuperior());
	}

	@Test
	public void withoutCandidatesTheMessageKeepsWaiting()
	{
		assertFalse(SuperiorTracker.match(Collections.<SuperiorTracker.Spawn<String>>emptyList()).isDecided());
		assertFalse(SuperiorTracker.match(Collections.singletonList(ineligible("dead"))).isDecided());
	}
}
