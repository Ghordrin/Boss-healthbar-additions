package com.ghordrin.bosshealthbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;

@Singleton
class SuperiorTracker
{
	private static final String SPAWN_MESSAGE = "A superior foe has appeared";
	private static final int MATCH_TICKS = 2;
	private static final int SEARCH_DISTANCE = 15;

	@Value
	static class Spawn<T>
	{
		T npc;
		boolean knownSuperior;
		boolean eligible;
	}

	@Value
	static class Match<T>
	{
		boolean decided;
		T superior;
	}

	private final Client client;
	private final DebugLog debugLog;
	private final Map<NPC, Integer> recentSpawnTicks = new HashMap<>();
	private final Set<NPC> superiors = new HashSet<>();
	private int messageTick = -1;

	@Inject
	SuperiorTracker(Client client, DebugLog debugLog)
	{
		this.client = client;
		this.debugLog = debugLog;
	}

	boolean isSuperior(Actor actor)
	{
		return actor instanceof NPC
			&& (superiors.contains(actor) || SuperiorIds.contains(NpcUtil.currentId((NPC) actor)));
	}

	void onNpcSpawned(NPC npc)
	{
		recentSpawnTicks.put(npc, client.getTickCount());
	}

	void onNpcDespawned(NPC npc)
	{
		recentSpawnTicks.remove(npc);
		superiors.remove(npc);
	}

	void onChatMessage(ChatMessage event)
	{
		if ((event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
			&& event.getMessage().contains(SPAWN_MESSAGE))
		{
			messageTick = client.getTickCount();
			debugLog.add("Superior spawn message on tick {}", messageTick);
		}
	}

	// Superiors in SuperiorIds are known by their ID. For any other, the chat message is matched to the
	// one attackable NPC that spawned near the player around the same tick, in either order.
	void onGameTick()
	{
		final int tick = client.getTickCount();
		recentSpawnTicks.values().removeIf(spawnTick -> tick - spawnTick > MATCH_TICKS);

		if (messageTick == -1)
		{
			return;
		}

		final Player player = client.getLocalPlayer();
		final List<Spawn<NPC>> spawns = new ArrayList<>();
		int eligible = 0;
		for (NPC npc : recentSpawnTicks.keySet())
		{
			final boolean known = SuperiorIds.contains(NpcUtil.currentId(npc));
			final Spawn<NPC> spawn = new Spawn<>(npc, known, !known && player != null && isEligible(npc, player));
			eligible += spawn.isEligible() ? 1 : 0;
			spawns.add(spawn);
		}

		final Match<NPC> match = match(spawns);
		if (match.isDecided())
		{
			messageTick = -1;
			if (match.getSuperior() != null)
			{
				superiors.add(match.getSuperior());
				debugLog.add("Marked {} as a superior", DebugLog.describe(match.getSuperior()));
			}
			else if (eligible > 1 && spawns.stream().noneMatch(Spawn::isKnownSuperior))
			{
				debugLog.add("{} NPCs spawned with the superior spawn message, marking none", eligible);
			}
		}
		else if (tick - messageTick >= MATCH_TICKS)
		{
			messageTick = -1;
			debugLog.add("No spawned NPC found for the superior spawn message");
		}
	}

	private boolean isEligible(NPC npc, Player player)
	{
		return !npc.isDead() && !superiors.contains(npc) && NpcUtil.isAttackable(npc)
			&& npc.getWorldLocation().distanceTo(player.getWorldLocation()) <= SEARCH_DISTANCE;
	}

	// A known superior takes the message. Otherwise a single eligible spawn is marked; with several it
	// would be a guess, so none is.
	static <T> Match<T> match(List<Spawn<T>> spawns)
	{
		T only = null;
		int eligible = 0;
		for (Spawn<T> spawn : spawns)
		{
			if (spawn.isKnownSuperior())
			{
				return new Match<>(true, null);
			}
			if (spawn.isEligible())
			{
				only = spawn.getNpc();
				eligible++;
			}
		}
		if (eligible > 1)
		{
			return new Match<>(true, null);
		}
		return new Match<>(eligible == 1, only);
	}

	void reset()
	{
		recentSpawnTicks.clear();
		superiors.clear();
		messageTick = -1;
	}
}
