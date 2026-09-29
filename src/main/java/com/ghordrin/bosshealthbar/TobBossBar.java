package com.ghordrin.bosshealthbar;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

@Singleton
class TobBossBar
{
	// Values of TOB_CLIENT_WAVEPROGRESS_TYPE. Other values show room progress instead of health.
	private static final int PROGRESS_NONE = 0;
	private static final int PROGRESS_BOSS_HEALTH = 1;
	private static final int SEARCH_DISTANCE = 32;

	private final Client client;

	private boolean hidden;
	private NPC boss;
	private boolean searchNeeded = true;

	@Inject
	TobBossBar(Client client)
	{
		this.client = client;
	}

	// The Theatre of Blood bar doesn't say which NPC it belongs to, so this takes the highest level
	// attackable NPC nearby. Cached until a stronger NPC spawns or the boss changes or despawns.
	NPC findBoss()
	{
		if (client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE) != PROGRESS_BOSS_HEALTH
			|| client.getWidget(InterfaceID.TobHud.PROGRESS_CONTAINER) == null)
		{
			boss = null;
			searchNeeded = true;
			return null;
		}

		if (!searchNeeded)
		{
			return boss;
		}

		searchNeeded = false;
		boss = null;
		final Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc.isDead() || !NpcUtil.isAttackable(npc)
				|| npc.getWorldLocation().distanceTo(player.getWorldLocation()) > SEARCH_DISTANCE)
			{
				continue;
			}

			if (boss == null
				|| npc.getCombatLevel() > boss.getCombatLevel()
				|| (npc.getCombatLevel() == boss.getCombatLevel() && NpcUtil.size(npc) > NpcUtil.size(boss)))
			{
				boss = npc;
			}
		}
		return boss;
	}

	// The last boss found, without searching again.
	NPC cachedBoss()
	{
		return boss;
	}

	boolean isTracking(Actor actor)
	{
		return actor != null && actor == findBoss();
	}

	void onNpcSpawned(NPC npc)
	{
		if (mayBeBoss(npc))
		{
			searchNeeded = true;
		}
	}

	void onNpcChanged(NPC npc)
	{
		if (npc == boss || mayBeBoss(npc))
		{
			searchNeeded = true;
		}
	}

	void onNpcDespawned(NPC npc)
	{
		if (npc == boss)
		{
			boss = null;
			searchNeeded = true;
		}
	}

	void onGameTick()
	{
		if (boss == null)
		{
			// The room's boss may only have come into range as the player walked in.
			searchNeeded = true;
		}
	}

	private boolean mayBeBoss(NPC npc)
	{
		return boss == null || npc.getCombatLevel() >= boss.getCombatLevel();
	}

	void update(Actor opponent, boolean replaceEnabled, boolean opponentGetsBar)
	{
		final Widget bar = client.getWidget(InterfaceID.TobHud.PROGRESS_CONTAINER);
		if (bar == null)
		{
			hidden = false;
			return;
		}

		if (replaceEnabled && isTracking(opponent) && opponentGetsBar)
		{
			if (!bar.isSelfHidden())
			{
				bar.setHidden(true);
				hidden = true;
			}
		}
		else
		{
			restore();
		}
	}

	void restore()
	{
		if (!hidden)
		{
			return;
		}

		final Widget bar = client.getWidget(InterfaceID.TobHud.PROGRESS_CONTAINER);
		if (bar != null && client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE) != PROGRESS_NONE)
		{
			bar.setHidden(false);
		}
		hidden = false;
	}

	void reset()
	{
		hidden = false;
		boss = null;
		searchNeeded = true;
	}

	int maxHealth()
	{
		return client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_MAX);
	}

	int health(int maxHealth)
	{
		return Math.max(0, Math.min(maxHealth, client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)));
	}
}
