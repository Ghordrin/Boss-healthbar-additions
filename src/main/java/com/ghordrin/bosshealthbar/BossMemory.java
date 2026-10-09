package com.ghordrin.bosshealthbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Singleton;

// Opponents that went out of sight, kept by NPC index so the bar can carry on when they come back.
// The game only reuses an NPC's index once that NPC is gone, so the name is checked as well.
@Singleton
class BossMemory
{
	static final int MAX_ENTRIES = 8;
	// 10 minutes and 60 seconds in game ticks.
	static final int FORGET_TICKS = 1000;
	static final int HOLD_TICKS = 100;
	// The game stops showing an NPC once its south-west tile is about 15 tiles away. Kills happen well inside that.
	static final int OUT_OF_SIGHT_DISTANCE = 15;
	// Health this much higher on return means the opponent healed or respawned, so it's a new fight.
	static final float HEALED_FRACTION = 0.2f;

	static final class Entry
	{
		final int index;
		final String name;
		final int tick;
		final FightTimer.Fight fight;
		BarState state;

		Entry(int index, String name, int tick, FightTimer.Fight fight)
		{
			this.index = index;
			this.name = name;
			this.tick = tick;
			this.fight = fight;
		}
	}

	private final List<Entry> entries = new ArrayList<>(MAX_ENTRIES);

	Entry remember(int index, String name, int tick, FightTimer.Fight fight)
	{
		forget(index);
		if (entries.size() >= MAX_ENTRIES)
		{
			entries.remove(0);
		}
		final Entry entry = new Entry(index, name, tick, fight);
		entries.add(entry);
		return entry;
	}

	// Drops an entry whose index now belongs to a different NPC, and any that have expired.
	Entry find(int index, String name, int tick)
	{
		for (int i = entries.size() - 1; i >= 0; i--)
		{
			final Entry entry = entries.get(i);
			if (isExpired(entry.tick, tick))
			{
				entries.remove(i);
			}
			else if (entry.index == index)
			{
				if (Objects.equals(entry.name, name))
				{
					return entry;
				}
				entries.remove(i);
			}
		}
		return null;
	}

	Entry take(int index, String name, int tick)
	{
		final Entry entry = find(index, name, tick);
		if (entry != null)
		{
			entries.remove(entry);
		}
		return entry;
	}

	boolean contains(int index)
	{
		for (Entry entry : entries)
		{
			if (entry.index == index)
			{
				return true;
			}
		}
		return false;
	}

	void forget(int index)
	{
		for (int i = entries.size() - 1; i >= 0; i--)
		{
			if (entries.get(i).index == index)
			{
				entries.remove(i);
			}
		}
	}

	void forgetFight(String fightName)
	{
		entries.removeIf(entry -> entry.fight != null && Objects.equals(entry.fight.name, fightName));
	}

	void clear()
	{
		entries.clear();
	}

	int size()
	{
		return entries.size();
	}

	static boolean isOutOfSight(boolean defeated, boolean playerDied, int distance)
	{
		return !defeated && !playerDied && distance >= OUT_OF_SIGHT_DISTANCE;
	}

	static int tileDistance(int npcX, int npcY, int x, int y)
	{
		return Math.max(Math.abs(x - npcX), Math.abs(y - npcY));
	}

	static boolean healedSince(int rememberedRatio, int rememberedScale, int ratio, int scale)
	{
		return rememberedScale > 0 && scale > 0
			&& ratio / (float) scale - rememberedRatio / (float) rememberedScale > HEALED_FRACTION;
	}

	static boolean isExpired(int rememberedTick, int tick)
	{
		return tick - rememberedTick > FORGET_TICKS;
	}

	static boolean isHeld(int rememberedTick, int tick)
	{
		return tick - rememberedTick <= HOLD_TICKS;
	}
}
