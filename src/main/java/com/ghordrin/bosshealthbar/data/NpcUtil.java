package com.ghordrin.bosshealthbar.data;

import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

public final class NpcUtil
{
	private NpcUtil()
	{
	}

	// Only an NPC counts, a player is never attackable here.
	public static boolean isAttackable(Actor actor)
	{
		return actor instanceof NPC && isAttackable((NPC) actor);
	}

	public static boolean isAttackable(NPC npc)
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

	public static int size(NPC npc)
	{
		final NPCComposition composition = npc.getTransformedComposition();
		return composition != null ? composition.getSize() : 0;
	}

	public static int currentId(NPC npc)
	{
		final NPCComposition composition = npc.getTransformedComposition();
		return composition != null ? composition.getId() : npc.getId();
	}

	// The game's boss bar tracks the base NPC ID, not the ID of the current form.
	public static int baseId(NPC npc)
	{
		final NPCComposition composition = npc.getComposition();
		return composition != null ? composition.getId() : -1;
	}
}
