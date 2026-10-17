package com.ghordrin.bosshealthbar.gamebars;

import com.ghordrin.bosshealthbar.data.NpcUtil;
import com.ghordrin.bosshealthbar.debug.DebugLog;
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
public class TobBossBar
{
	// Values of TOB_CLIENT_WAVEPROGRESS_TYPE. Other values show room progress instead of health.
	private static final int PROGRESS_NONE = 0;
	private static final int PROGRESS_BOSS_HEALTH = 1;
	// TOB_CLIENT_PARTYSTATUS: 1 is a party outside, 2 is inside the raid and 3 is spectating it dead.
	private static final int PARTY_INSIDE = 2;
	private static final int SEARCH_DISTANCE = 32;

	private final Client client;
	private final DebugLog debugLog;
	private final HiddenWidget raidBar;

	private NPC boss;
	private NPC loggedBoss;
	private boolean searchNeeded = true;
	private boolean loggedLeftOver;
	private boolean roomBossShown = true;

	@Inject
	TobBossBar(Client client, DebugLog debugLog)
	{
		this.client = client;
		this.debugLog = debugLog;
		this.raidBar = new HiddenWidget(client, debugLog, InterfaceID.TobHud.PROGRESS_CONTAINER);
	}

	// The Theatre of Blood bar doesn't say which NPC it belongs to, so this takes the highest level
	// attackable NPC nearby. Cached until a stronger NPC spawns or the boss changes or despawns.
	public NPC findBoss()
	{
		if (!barIsLive())
		{
			boss = null;
			searchNeeded = true;
			roomBossShown = true;
			if (loggedBoss != null)
			{
				loggedBoss = null;
				debugLog.add("Raid boss bar gone");
			}
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
		if (boss != loggedBoss)
		{
			loggedBoss = boss;
			debugLog.add("Raid boss bar belongs to {}", DebugLog.describe(boss));
		}
		return boss;
	}

	// Leaving the Theatre doesn't clear the progress varbits or unload its HUD, so on their own they keep
	// reading the health the last raid ended on, hours later and anywhere, and hand it to the strongest NPC
	// nearby. The party status is the game's own answer to whether you are in there, so it decides.
	private boolean barIsLive()
	{
		final int status = partyStatus();
		final boolean bossHealth = client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE) == PROGRESS_BOSS_HEALTH;
		final boolean leftOver = bossHealth && status < PARTY_INSIDE;
		if (leftOver != loggedLeftOver)
		{
			loggedLeftOver = leftOver;
			if (leftOver)
			{
				debugLog.add("Raid boss bar values left over from a raid, ignored (party status {})", status);
			}
		}
		return bossHealth && status >= PARTY_INSIDE && raidBar.widget() != null;
	}

	// The last boss found, without searching again.
	public NPC cachedBoss()
	{
		return boss;
	}

	public boolean isTracking(Actor actor)
	{
		return actor != null && actor == findBoss();
	}

	public void onNpcSpawned(NPC npc)
	{
		if (mayBeBoss(npc))
		{
			searchNeeded = true;
		}
	}

	public void onNpcChanged(NPC npc)
	{
		if (npc == boss || mayBeBoss(npc))
		{
			searchNeeded = true;
		}
	}

	public void onNpcDespawned(NPC npc)
	{
		if (npc == boss)
		{
			boss = null;
			searchNeeded = true;
		}
	}

	public void onGameTick()
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

	// The Theatre's bar is replaced for as long as it is up for the room's boss, and not only while that boss
	// is the opponent: it is up from the moment the room starts, before the boss is near enough to be found,
	// and stays up for a moment after the boss dies, when there is no NPC left to match it to.
	public void update(boolean replaceEnabled, boolean bossGetsBar)
	{
		final Widget bar = raidBar.widget();
		if (bar == null)
		{
			raidBar.forget();
			return;
		}

		if (findBoss() != null)
		{
			// Kept for the moments the room's boss can't be found, so a boss on the Never show for list keeps
			// the game's bar for the whole room instead of only while it is in the scene.
			roomBossShown = bossGetsBar;
		}

		// A player who died in the raid spectates the rest of it and never has an opponent, so our own bar
		// can't replace anything for them and the game's bar is left alone.
		if (replaceEnabled && barIsLive() && partyStatus() == PARTY_INSIDE && roomBossShown)
		{
			raidBar.hide(bar, "Raid boss bar hidden");
		}
		else
		{
			restore();
		}
	}

	public void restore()
	{
		if (!raidBar.isHidden())
		{
			return;
		}

		raidBar.release("Raid boss bar restored",
			client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE) != PROGRESS_NONE);
	}

	public void reset()
	{
		raidBar.forget();
		loggedLeftOver = false;
		roomBossShown = true;
		boss = null;
		loggedBoss = null;
		searchNeeded = true;
	}

	// Says whether the player is in the Theatre, which decides whether the progress varbits mean anything.
	public int partyStatus()
	{
		return client.getVarbitValue(VarbitID.TOB_CLIENT_PARTYSTATUS);
	}

	public boolean isHidden()
	{
		return raidBar.isHidden();
	}

	public int maxHealth()
	{
		return client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_MAX);
	}

	public int health(int maxHealth)
	{
		return Math.max(0, Math.min(maxHealth, client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)));
	}
}
