package com.ghordrin.bosshealthbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

// A list of NPC IDs and names, separated by commas or new lines, where names may use * as a wildcard.
final class NpcFilter
{
	enum Match
	{
		NEVER,
		ALSO,
		NONE
	}

	static final NpcFilter EMPTY = new NpcFilter(new HashSet<>(), new HashSet<>(), new ArrayList<>());

	private final Set<Integer> ids;
	private final Set<String> names;
	private final List<String> patterns;

	private NpcFilter(Set<Integer> ids, Set<String> names, List<String> patterns)
	{
		this.ids = ids;
		this.names = names;
		this.patterns = patterns;
	}

	static NpcFilter parse(String csv)
	{
		if (csv == null || csv.trim().isEmpty())
		{
			return EMPTY;
		}

		final Set<Integer> ids = new HashSet<>();
		final Set<String> names = new HashSet<>();
		final List<String> patterns = new ArrayList<>();
		for (String entry : Text.fromCSV(csv.replace('\n', ',')))
		{
			if (entry.chars().allMatch(Character::isDigit))
			{
				try
				{
					ids.add(Integer.parseInt(entry));
				}
				catch (NumberFormatException e)
				{
					// Too long to be an NPC ID.
				}
			}
			else if (entry.indexOf('*') >= 0)
			{
				patterns.add(entry);
			}
			else
			{
				names.add(Text.standardize(entry));
			}
		}
		return new NpcFilter(ids, names, patterns);
	}

	boolean isEmpty()
	{
		return ids.isEmpty() && names.isEmpty() && patterns.isEmpty();
	}

	// The name must already be standardized.
	boolean matches(String name, int id, int currentId)
	{
		if (ids.contains(id) || ids.contains(currentId) || names.contains(name))
		{
			return true;
		}
		for (String pattern : patterns)
		{
			if (WildcardMatcher.matches(pattern, name))
			{
				return true;
			}
		}
		return false;
	}

	static Match check(NpcFilter never, NpcFilter also, String name, int id, int currentId)
	{
		if (never.matches(name, id, currentId))
		{
			return Match.NEVER;
		}
		return also.matches(name, id, currentId) ? Match.ALSO : Match.NONE;
	}

	// The lists are checked every frame, so the answer is kept for the last NPC, name and form.
	static final class LastMatch
	{
		private NPC npc;
		private String name;
		private int currentId;
		private Match match = Match.NONE;

		Match get(NPC npc, NpcFilter never, NpcFilter also)
		{
			final String name = npc.getName();
			final int currentId = NpcUtil.currentId(npc);
			if (npc != this.npc || currentId != this.currentId || !name.equals(this.name))
			{
				this.npc = npc;
				this.name = name;
				this.currentId = currentId;
				match = check(never, also, Text.standardize(name), NpcUtil.baseId(npc), currentId);
			}
			return match;
		}

		void clear()
		{
			npc = null;
		}
	}
}
