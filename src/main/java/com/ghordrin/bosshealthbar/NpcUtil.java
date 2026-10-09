package com.ghordrin.bosshealthbar;

import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.gameval.NpcID;

final class NpcUtil
{
	private NpcUtil()
	{
	}

	static boolean isAttackable(NPC npc)
	{
		final NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			return false;
		}
		for (String action : composition.getActions())
		{
			if ("Attack".equals(action))
			{
				return true;
			}
		}
		return false;
	}

	static int size(NPC npc)
	{
		final NPCComposition composition = npc.getTransformedComposition();
		return composition != null ? composition.getSize() : 0;
	}

	static int currentId(NPC npc)
	{
		final NPCComposition composition = npc.getTransformedComposition();
		return composition != null ? composition.getId() : npc.getId();
	}

	static boolean canBeDefeated(Actor actor)
	{
		return !(actor instanceof NPC) || canBeDefeated(((NPC) actor).getId());
	}

	// Forms that restore health instead of dying, so 0 health there isn't a kill.
	static boolean canBeDefeated(int npcId)
	{
		switch (npcId)
		{
			case NpcID.ABYSSALSIRE_SIRE_STASIS_SLEEPING:
			case NpcID.ABYSSALSIRE_SIRE_STASIS_AWAKE:
			case NpcID.ABYSSALSIRE_SIRE_STASIS_STUNNED:
			case NpcID.ABYSSALSIRE_SIRE_PUPPET:
			case NpcID.ABYSSALSIRE_SIRE_WANDERING:
			case NpcID.ABYSSALSIRE_SIRE_PANICKING:
				return false;
			default:
				return true;
		}
	}

	// The game's boss bar tracks the base NPC ID, not the ID of the current form.
	static int baseId(NPC npc)
	{
		final NPCComposition composition = npc.getComposition();
		return composition != null ? composition.getId() : -1;
	}
}
