package com.ghordrin.bosshealthbar;

import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

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

	// The game's boss bar tracks the base NPC ID, not the ID of the current form.
	static int baseId(NPC npc)
	{
		final NPCComposition composition = npc.getComposition();
		return composition != null ? composition.getId() : -1;
	}
}
