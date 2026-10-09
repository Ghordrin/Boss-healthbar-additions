package com.ghordrin.bosshealthbar;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

// The last health read for the few most recent opponents of the current fight, for a moment where an
// opponent has no reading yet, such as an NPC that comes back after leaving or a boss bar that moved over.
final class LastHealth
{
	static final int CAPACITY = 8;

	private static final class Entry
	{
		final int index;
		final String name;
		final int fight;
		final BarState state;

		Entry(int index, String name, int fight, BarState state)
		{
			this.index = index;
			this.name = name;
			this.fight = fight;
			this.state = state;
		}
	}

	private final Deque<Entry> entries = new ArrayDeque<>();

	// fight identifies the fight the reading belongs to, such as the tick it started. Below 0 means no fight yet.
	void remember(int index, String name, int fight, BarState state)
	{
		if (name == null || fight < 0 || state == null || state.scale <= 0)
		{
			return;
		}
		final Entry newest = entries.peekFirst();
		if (newest != null && newest.index == index && newest.fight == fight && newest.state == state
			&& newest.name.equals(name))
		{
			return;
		}
		removeMatching(index, name);
		entries.addFirst(new Entry(index, name, fight, state));
		while (entries.size() > CAPACITY)
		{
			entries.removeLast();
		}
	}

	// Same NPC first. An NPC that left and came back gets a new index, so a single entry with its name counts too.
	BarState find(int index, String name, int fight)
	{
		if (name == null || fight < 0)
		{
			return null;
		}
		Entry byName = null;
		int sameName = 0;
		for (Entry entry : entries)
		{
			if (entry.fight != fight || !entry.name.equals(name))
			{
				continue;
			}
			if (entry.index == index)
			{
				return entry.state;
			}
			byName = entry;
			sameName++;
		}
		return sameName == 1 ? byName.state : null;
	}

	void clear()
	{
		entries.clear();
	}

	private void removeMatching(int index, String name)
	{
		for (Iterator<Entry> it = entries.iterator(); it.hasNext(); )
		{
			final Entry entry = it.next();
			if (entry.index == index && entry.name.equals(name))
			{
				it.remove();
			}
		}
	}
}
