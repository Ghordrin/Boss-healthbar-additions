package com.ghordrin.bosshealthbar;

import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

@Singleton
class GameBossBar
{
	// Set to 1 on NPCs whose game boss bar only shows a percentage. No gameval constant exists for it.
	private static final int PARAM_HP_PERCENTAGE_ONLY = 2289;

	private static final int[] PHASE_MARKER_VARBITS = {
		VarbitID.HPBAR_HUD_LOWER_THRESHOLD,
		VarbitID.HPBAR_HUD_UPPER_THRESHOLD,
		VarbitID.HPBAR_HUD_HP_1,
		VarbitID.HPBAR_HUD_HP_2,
	};

	private final Client client;

	private boolean hidden;
	private NPC npc;
	private int searchedId = -1;
	private int replacedId = -1;

	private int percentOnlyNpcId = -1;
	private boolean percentOnly;

	private final int[] phaseMarkerValues = new int[PHASE_MARKER_VARBITS.length];
	private int phaseMarkerMaxHealth;
	private float[] phaseMarkers = BarState.NO_PHASE_MARKERS;

	@Inject
	GameBossBar(Client client)
	{
		this.client = client;
	}

	int trackedNpcId()
	{
		if (client.getVarbitValue(VarbitID.HPBAR_HUD_BOSS_DISABLED) != 0)
		{
			return -1;
		}
		return client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
	}

	boolean isTracking(Actor actor)
	{
		if (!(actor instanceof NPC))
		{
			return false;
		}
		final int trackedId = trackedNpcId();
		return trackedId != -1 && NpcUtil.baseId((NPC) actor) == trackedId;
	}

	NPC findNpc(Actor opponent)
	{
		final int trackedId = trackedNpcId();
		if (trackedId == -1)
		{
			npc = null;
			searchedId = -1;
			return null;
		}

		if (isTracking(opponent))
		{
			npc = (NPC) opponent;
			return npc;
		}

		final Player player = client.getLocalPlayer();
		final Actor target = player != null ? player.getInteracting() : null;
		if (isTracking(target))
		{
			npc = (NPC) target;
			return npc;
		}

		if (npc != null && NpcUtil.baseId(npc) == trackedId)
		{
			return npc;
		}

		if (npc == null && trackedId == searchedId)
		{
			return null;
		}

		npc = null;
		searchedId = trackedId;
		for (NPC candidate : client.getTopLevelWorldView().npcs())
		{
			if (NpcUtil.baseId(candidate) == trackedId)
			{
				npc = candidate;
				break;
			}
		}
		return npc;
	}

	void onNpcSpawned()
	{
		searchedId = -1;
	}

	void onNpcDespawned(NPC despawned)
	{
		if (despawned == npc)
		{
			npc = null;
		}
	}

	void update(Actor opponent, boolean replaceEnabled, boolean opponentGetsBar)
	{
		final Widget bar = client.getWidget(InterfaceID.HpbarHud.UNIVERSE);
		if (bar == null)
		{
			hidden = false;
			return;
		}

		boolean replace = false;
		if (replaceEnabled)
		{
			if (opponentGetsBar && isTracking(opponent))
			{
				replace = true;
				replacedId = trackedNpcId();
			}
			else
			{
				// After the boss despawns, the game's bar can stay up on it while the defeat animation
				// plays, so keep it hidden until it tracks a different NPC.
				replace = replacedId != -1 && trackedNpcId() == replacedId;
			}
		}

		if (replace)
		{
			if (!bar.isSelfHidden())
			{
				bar.setHidden(true);
				hidden = true;
			}
		}
		else
		{
			replacedId = -1;
			restore();
		}
	}

	void restore()
	{
		if (!hidden || trackedNpcId() == -1)
		{
			return;
		}

		final Widget bar = client.getWidget(InterfaceID.HpbarHud.UNIVERSE);
		if (bar != null)
		{
			bar.setHidden(false);
		}
		hidden = false;
	}

	void reset()
	{
		hidden = false;
		npc = null;
		searchedId = -1;
		replacedId = -1;
	}

	int maxHealth()
	{
		return client.getVarbitValue(VarbitID.HPBAR_HUD_BASEHP);
	}

	int health()
	{
		return client.getVarbitValue(VarbitID.HPBAR_HUD_HP);
	}

	boolean isPercentOnly()
	{
		final int npcId = client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
		if (npcId != percentOnlyNpcId)
		{
			final NPCComposition composition = npcId != -1 ? client.getNpcDefinition(npcId) : null;
			percentOnly = composition != null && composition.getIntValue(PARAM_HP_PERCENTAGE_ONLY) == 1;
			percentOnlyNpcId = npcId;
		}
		return percentOnly;
	}

	float[] phaseMarkers(int maxHealth)
	{
		boolean changed = maxHealth != phaseMarkerMaxHealth;
		for (int i = 0; i < PHASE_MARKER_VARBITS.length; i++)
		{
			final int value = client.getVarbitValue(PHASE_MARKER_VARBITS[i]);
			if (value != phaseMarkerValues[i])
			{
				phaseMarkerValues[i] = value;
				changed = true;
			}
		}
		if (!changed)
		{
			return phaseMarkers;
		}
		phaseMarkerMaxHealth = maxHealth;

		float[] markers = null;
		int count = 0;
		for (int value : phaseMarkerValues)
		{
			if (value > 0 && value <= maxHealth + 1)
			{
				if (markers == null)
				{
					markers = new float[PHASE_MARKER_VARBITS.length];
				}
				markers[count++] = markerFraction(value, maxHealth);
			}
		}
		phaseMarkers = markers == null ? BarState.NO_PHASE_MARKERS : Arrays.copyOf(markers, count);
		return phaseMarkers;
	}

	// Same placement as the game's own bar: a marker for value v sits at (v - 1) / max health.
	static float markerFraction(int value, int maxHealth)
	{
		return BarAnimation.clamp01((value - 1) / (float) maxHealth);
	}
}
