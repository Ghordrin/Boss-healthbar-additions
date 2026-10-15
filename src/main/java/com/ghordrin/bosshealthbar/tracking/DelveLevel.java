package com.ghordrin.bosshealthbar.tracking;

import com.ghordrin.bosshealthbar.data.BarState;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import com.google.common.annotations.VisibleForTesting;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;

// The delve level the game shows on its own bar. Read per update rather than once per opponent, since the level
// changes while the opponent stays the same.
@Singleton
public class DelveLevel
{
	private static final String DELVE_BOSS = "Doom of Mokhaiotl";

	private final Client client;
	private final DebugLog debugLog;

	// Starts below any reading, so a first reading of no delves done still logs.
	private int loggedDelvesDone = -1;

	@Inject
	DelveLevel(Client client, DebugLog debugLog)
	{
		this.client = client;
		this.debugLog = debugLog;
	}

	// The level to show for this opponent, or NO_DELVE_LEVEL when it has none. Named rather than matched by ID,
	// like the rest of the boss lookups, since each form has its own ID.
	public int forOpponent(String name)
	{
		if (name == null || !name.contains(DELVE_BOSS))
		{
			return BarState.NO_DELVE_LEVEL;
		}

		final int delvesDone = client.getVarpValue(VarPlayerID.DOM_CURRENT_LEVEL_TEMP);
		if (delvesDone != loggedDelvesDone)
		{
			loggedDelvesDone = delvesDone;
			debugLog.add("Delve level {}", levelFor(delvesDone));
		}
		return levelFor(delvesDone);
	}

	// The varp counts the delves finished this trip, so the level being fought is one past it: it reads 0 on
	// delve 1 and 1 on delve 2.
	@VisibleForTesting
	static int levelFor(int delvesDone)
	{
		return Math.max(delvesDone, 0) + 1;
	}

	public static String text(int level)
	{
		return "Delve " + level;
	}
}
