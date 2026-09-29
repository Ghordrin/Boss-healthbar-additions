package com.ghordrin.bosshealthbar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;

@Slf4j
@Singleton
class SuperiorTracker
{
	private static final String SPAWN_MESSAGE = "A superior foe has appeared";
	private static final int MATCH_TICKS = 2;
	private static final int SEARCH_DISTANCE = 15;

	private final Client client;
	private final Map<NPC, Integer> recentSpawnTicks = new HashMap<>();
	private final Set<NPC> superiors = new HashSet<>();
	private int messageTick = -1;

	@Inject
	SuperiorTracker(Client client)
	{
		this.client = client;
	}

	boolean isSuperior(Actor actor)
	{
		return superiors.contains(actor);
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
			log.debug("Superior spawn message on tick {}", messageTick);
		}
	}

	// The game only announces a superior in chat, so pick the attackable NPC that spawned closest to
	// the player around the same tick. The spawn and the message can arrive in either order.
	void onGameTick()
	{
		final int tick = client.getTickCount();
		recentSpawnTicks.values().removeIf(spawnTick -> tick - spawnTick > MATCH_TICKS);

		if (messageTick == -1)
		{
			return;
		}

		final Player player = client.getLocalPlayer();
		NPC nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		if (player != null)
		{
			for (NPC npc : recentSpawnTicks.keySet())
			{
				final int distance = npc.getWorldLocation().distanceTo(player.getWorldLocation());
				if (!npc.isDead() && !superiors.contains(npc) && NpcUtil.isAttackable(npc)
					&& distance <= SEARCH_DISTANCE && distance < nearestDistance)
				{
					nearest = npc;
					nearestDistance = distance;
				}
			}
		}

		if (nearest != null)
		{
			superiors.add(nearest);
			messageTick = -1;
			log.debug("Marked {} as a superior", nearest.getName());
		}
		else if (tick - messageTick >= MATCH_TICKS)
		{
			messageTick = -1;
			log.debug("No spawned NPC found for the superior spawn message");
		}
	}

	void reset()
	{
		recentSpawnTicks.clear();
		superiors.clear();
		messageTick = -1;
	}
}
