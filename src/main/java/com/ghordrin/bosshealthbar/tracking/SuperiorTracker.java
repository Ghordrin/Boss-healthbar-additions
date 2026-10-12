package com.ghordrin.bosshealthbar.tracking;

import com.ghordrin.bosshealthbar.data.NpcUtil;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import com.google.common.collect.ImmutableSet;
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
import net.runelite.api.gameval.NpcID;

@Singleton
public class SuperiorTracker
{
	private static final String SPAWN_MESSAGE = "A superior foe has appeared";
	private static final int MATCH_TICKS = 2;
	private static final int SEARCH_DISTANCE = 15;

	// Superior slayer monsters, including their death forms. The weekly data check lists new ones.
	private static final Set<Integer> KNOWN_IDS = ImmutableSet.of(
		NpcID.SUPERIOR_CRAWLING_HAND, NpcID.SUPERIOR_CAVE_CRAWLER, NpcID.SUPERIOR_BANSHEE,
		NpcID.SUPERIOR_KOUREND_BANSHEE, NpcID.SUPERIOR_ROCKSLUG, NpcID.SUPERIOR_COCKATRICE, NpcID.SUPERIOR_PYREFIEND,
		NpcID.SUPERIOR_BASILISK, NpcID.SUPERIOR_INFERNAL_MAGE, NpcID.SUPERIOR_BLOODVELD,
		NpcID.SUPERIOR_KOUREND_BLOODVELD, NpcID.SUPERIOR_JELLY, NpcID.SUPERIOR_KOUREND_JELLY,
		NpcID.SUPERIOR_CAVE_HORROR, NpcID.SUPERIOR_ABBERANT_SPECTRE, NpcID.SUPERIOR_KOUREND_SPECTRE,
		NpcID.SUPERIOR_DUSTDEVIL, NpcID.SUPERIOR_KURASK, NpcID.SUPERIOR_SMOKE_DEVIL, NpcID.SUPERIOR_GARGOYLE,
		NpcID.SUPERIOR_GARGOYLE_DEAD, NpcID.SUPERIOR_DARK_BEAST, NpcID.SUPERIOR_ABYSSAL_DEMON,
		NpcID.SUPERIOR_NECHRYAEL, NpcID.SUPERIOR_BASILISK_KNIGHT, NpcID.SUPERIOR_PYRELORD, NpcID.SUPERIOR_TUROTH,
		NpcID.SUPERIOR_WYRM_DARK, NpcID.SUPERIOR_WYRM_LIGHT, NpcID.SUPERIOR_DRAKE, NpcID.SUPERIOR_DRAKE_DEATH,
		NpcID.SUPERIOR_HYDRA, NpcID.SUPERIOR_WARPED_TERRORBIRD, NpcID.SUPERIOR_WARPED_TORTOISE,
		NpcID.SUPERIOR_ARAXYTE, NpcID.SUPERIOR_CAVE_CRAWLER_ICE, NpcID.SUPERIOR_CUSTODIAN, NpcID.SUPERIOR_GRYPHON,
		NpcID.SUPERIOR_CHILLED_JELLY, NpcID.SUPERIOR_AQUANITE, NpcID.SUPERIOR_AQUANITE_NOLURE,
		NpcID.SUPERIOR_LAVA_STRYKEWYRM, NpcID.SUPERIOR_VENATOR,
		NpcID.DEADMAN_BREACH_SUPERIOR_ABYSSAL_DEMON, NpcID.DEADMAN_BREACH_SUPERIOR_PYREFIEND,
		NpcID.DEADMAN_BREACH_SUPERIOR_CAVE_HORROR, NpcID.DEADMAN_BREACH_SUPERIOR_INFERNAL_MAGE,
		NpcID.DEADMAN_BREACH_SUPERIOR_KOUREND_JELLY, NpcID.DEADMAN_BREACH_SUPERIOR_DARK_BEAST,
		NpcID.LEAGUE_SUPERIOR_CRAWLING_HAND, NpcID.LEAGUE_SUPERIOR_CAVE_CRAWLER, NpcID.LEAGUE_SUPERIOR_BANSHEE,
		NpcID.LEAGUE_SUPERIOR_KOUREND_BANSHEE, NpcID.LEAGUE_SUPERIOR_ROCKSLUG, NpcID.LEAGUE_SUPERIOR_COCKATRICE,
		NpcID.LEAGUE_SUPERIOR_PYREFIEND, NpcID.LEAGUE_SUPERIOR_PYRELORD, NpcID.LEAGUE_SUPERIOR_BASILISK,
		NpcID.LEAGUE_SUPERIOR_BASILISK_KNIGHT, NpcID.LEAGUE_SUPERIOR_INFERNAL_MAGE, NpcID.LEAGUE_SUPERIOR_BLOODVELD,
		NpcID.LEAGUE_SUPERIOR_KOUREND_BLOODVELD, NpcID.LEAGUE_SUPERIOR_JELLY, NpcID.LEAGUE_SUPERIOR_KOUREND_JELLY,
		NpcID.LEAGUE_SUPERIOR_CAVE_HORROR, NpcID.LEAGUE_SUPERIOR_ABBERANT_SPECTRE,
		NpcID.LEAGUE_SUPERIOR_KOUREND_SPECTRE, NpcID.LEAGUE_SUPERIOR_DUSTDEVIL, NpcID.LEAGUE_SUPERIOR_KURASK,
		NpcID.LEAGUE_SUPERIOR_SMOKE_DEVIL, NpcID.LEAGUE_SUPERIOR_GARGOYLE, NpcID.LEAGUE_SUPERIOR_GARGOYLE_DEAD,
		NpcID.LEAGUE_SUPERIOR_DARK_BEAST, NpcID.LEAGUE_SUPERIOR_ABYSSAL_DEMON, NpcID.LEAGUE_SUPERIOR_NECHRYAEL,
		NpcID.LEAGUE_SUPERIOR_TUROTH, NpcID.LEAGUE_SUPERIOR_WYRM_DARK, NpcID.LEAGUE_SUPERIOR_WYRM_LIGHT,
		NpcID.LEAGUE_SUPERIOR_DRAKE, NpcID.LEAGUE_SUPERIOR_DRAKE_DEATH, NpcID.LEAGUE_SUPERIOR_HYDRA);

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
			&& (superiors.contains(actor) || isKnownSuperior(NpcUtil.currentId((NPC) actor)));
	}

	static boolean isKnownSuperior(int npcId)
	{
		return KNOWN_IDS.contains(npcId);
	}

	public void onNpcSpawned(NPC npc)
	{
		recentSpawnTicks.put(npc, client.getTickCount());
	}

	public void onNpcDespawned(NPC npc)
	{
		recentSpawnTicks.remove(npc);
		superiors.remove(npc);
	}

	public void onChatMessage(ChatMessage event)
	{
		if ((event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
			&& event.getMessage().contains(SPAWN_MESSAGE))
		{
			messageTick = client.getTickCount();
			debugLog.add("Superior spawn message on tick {}", messageTick);
		}
	}

	// Superiors in KNOWN_IDS are known by their ID. For any other, the chat message is matched to the
	// one attackable NPC that spawned near the player around the same tick, in either order.
	public void onGameTick()
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
			final boolean known = isKnownSuperior(NpcUtil.currentId(npc));
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

	public void reset()
	{
		recentSpawnTicks.clear();
		superiors.clear();
		messageTick = -1;
	}
}
