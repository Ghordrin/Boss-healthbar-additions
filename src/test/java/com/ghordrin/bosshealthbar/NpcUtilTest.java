package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;

public class NpcUtilTest
{
	@Test
	public void formsThatRestoreHealthCannotBeDefeated()
	{
		assertFalse(NpcUtil.canBeDefeated(NpcID.ABYSSALSIRE_SIRE_STASIS_SLEEPING));
		assertFalse(NpcUtil.canBeDefeated(NpcID.ABYSSALSIRE_SIRE_STASIS_AWAKE));
		assertFalse(NpcUtil.canBeDefeated(NpcID.ABYSSALSIRE_SIRE_STASIS_STUNNED));
		assertFalse(NpcUtil.canBeDefeated(NpcID.ABYSSALSIRE_SIRE_PUPPET));
		assertFalse(NpcUtil.canBeDefeated(NpcID.ABYSSALSIRE_SIRE_WANDERING));
		assertFalse(NpcUtil.canBeDefeated(NpcID.ABYSSALSIRE_SIRE_PANICKING));
	}

	@Test
	public void otherNpcsCanBeDefeated()
	{
		assertTrue(NpcUtil.canBeDefeated(NpcID.ABYSSALSIRE_SIRE_APOCALYPSE));
		assertTrue(NpcUtil.canBeDefeated(NpcID.CORP_BEAST));
		assertTrue(NpcUtil.canBeDefeated(NpcID.GOBLIN));
	}
}
