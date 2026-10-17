package com.ghordrin.bosshealthbar.gamebars;

import com.ghordrin.bosshealthbar.data.BarState;
import com.ghordrin.bosshealthbar.data.NpcUtil;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import com.ghordrin.bosshealthbar.render.BarAnimation;
import com.ghordrin.bosshealthbar.theme.PhaseColors;
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
public class GameBossBar
{
	// Set to 1 on NPCs whose game boss bar only shows a percentage. No gameval constant exists for it.
	private static final int PARAM_HP_PERCENTAGE_ONLY = 2289;

	// The layer holding only the bar, since the game mounts other HUDs, like pillar health bars, in the same interface.
	private static final int GAME_BAR = InterfaceID.HpbarHud.HPDODGER;

	private static final int[] PHASE_MARKER_VARBITS = {
		VarbitID.HPBAR_HUD_LOWER_THRESHOLD,
		VarbitID.HPBAR_HUD_UPPER_THRESHOLD,
		VarbitID.HPBAR_HUD_HP_1,
		VarbitID.HPBAR_HUD_HP_2,
	};

	private final Client client;
	private final DebugLog debugLog;
	private final HiddenWidget gameBar;

	private NPC npc;
	private int searchedId = -1;
	private int loggedTrackedId = -1;
	private int healthSeenId = -1;
	private int loggedZeroId = -1;

	private int percentOnlyNpcId = -1;
	private boolean percentOnly;

	private int loggedFill = -1;
	private int loggedTrail = -1;
	private int loggedTrack = -1;
	private PhaseColors phaseColors;

	private final int[] phaseMarkerValues = new int[PHASE_MARKER_VARBITS.length];
	private int phaseMarkerMaxHealth;
	private float[] phaseMarkers = BarState.NO_PHASE_MARKERS;

	@Inject
	GameBossBar(Client client, DebugLog debugLog)
	{
		this.client = client;
		this.debugLog = debugLog;
		this.gameBar = new HiddenWidget(client, debugLog, GAME_BAR);
	}

	public void onGameTick()
	{
		final int trackedId = trackedNpcId();
		if (trackedId != loggedTrackedId)
		{
			loggedTrackedId = trackedId;
			if (trackedId == -1)
			{
				debugLog.add("Game boss bar tracks nothing");
			}
			else
			{
				debugLog.add("Game boss bar tracks NPC id {} (health {}/{})", trackedId, health(), maxHealth());
			}
		}
		if (trackedId != -1 && trackedId != loggedZeroId && !hasHealth())
		{
			loggedZeroId = trackedId;
			debugLog.add("Game boss bar reads 0 for NPC id {} before its health arrived, ignored", trackedId);
		}
		updatePhaseColors(trackedId);
	}

	// Read once a tick, since the overlay asks every frame. The colors are logged whenever they change, so a
	// boss nobody has fought yet says in the log whether the game gives it a phase color at all.
	private void updatePhaseColors(int trackedId)
	{
		PhaseColors colors = null;
		if (trackedId != -1)
		{
			final Widget fill = client.getWidget(InterfaceID.HpbarHud.HEALTH_BAR_REMAINING);
			final Widget trail = client.getWidget(InterfaceID.HpbarHud.HEALTH_BAR_SLIDING);
			final Widget track = client.getWidget(InterfaceID.HpbarHud.HEALTH_BAR_BACK);
			if (fill != null && trail != null && track != null)
			{
				colors = PhaseColors.of(fill.getTextColor(), trail.getTextColor(), track.getTextColor());
				logColors(trackedId, fill.getTextColor(), trail.getTextColor(), track.getTextColor(), colors != null);
			}
		}
		phaseColors = colors;
	}

	private void logColors(int trackedId, int fill, int trail, int track, boolean phase)
	{
		if (fill == loggedFill && trail == loggedTrail && track == loggedTrack)
		{
			return;
		}
		loggedFill = fill;
		loggedTrail = trail;
		loggedTrack = track;
		debugLog.add("Game boss bar colors for NPC id {}: fill {} trail {} background {}, phase {}", trackedId,
			hex(fill), hex(trail), hex(track), phase);
	}

	private static String hex(int color)
	{
		return String.format("%06X", color & 0xFFFFFF);
	}

	// The colors the game is painting a special phase in, or null when it paints its usual ones. Only for the
	// NPC its bar tracks, so a second boss on screen doesn't take another one's phase.
	public PhaseColors phaseColors(Actor actor)
	{
		return isTracking(actor) ? phaseColors : null;
	}

	// A bar held for a boss that has left the scene has no NPC left to match, so it asks by the ID instead.
	public PhaseColors phaseColorsFor(int npcId)
	{
		return npcId != -1 && trackedNpcId() == npcId ? phaseColors : null;
	}

	public int trackedNpcId()
	{
		if (client.getVarbitValue(VarbitID.HPBAR_HUD_BOSS_DISABLED) != 0)
		{
			return -1;
		}
		return client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
	}

	public boolean isTracking(Actor actor)
	{
		if (!(actor instanceof NPC))
		{
			return false;
		}
		final int trackedId = trackedNpcId();
		return trackedId != -1 && NpcUtil.baseId((NPC) actor) == trackedId;
	}

	public NPC findNpc(Actor opponent)
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

	// The NPC the game's bar was last found on, without searching again. Null until one is in the scene.
	public NPC trackedNpc()
	{
		return npc;
	}

	public void onNpcSpawned()
	{
		searchedId = -1;
	}

	public void onNpcDespawned(NPC despawned)
	{
		if (despawned == npc)
		{
			npc = null;
		}
	}

	// The game's bar is replaced for as long as it is up for an NPC the bar covers, and not only while that
	// NPC is the opponent: it stays on the boss through its death and goes back to the boss's idle form after
	// the kill, which our own bar picks up again only once that NPC is in the scene.
	public boolean update(Actor opponent, boolean replaceEnabled, boolean opponentGetsBar, boolean trackedGetsBar)
	{
		final Widget bar = gameBar.widget();
		if (bar == null)
		{
			gameBar.forget();
			return false;
		}

		// An opponent without our bar, such as one on the Never show for list, keeps the game's bar.
		final boolean replace = replaceEnabled && (isTracking(opponent) ? opponentGetsBar : trackedGetsBar);
		if (replace)
		{
			gameBar.hide(bar, "Game boss bar hidden (tracked id {})", trackedNpcId());
		}
		else
		{
			restore();
		}
		return replace;
	}

	private void restore()
	{
		if (!gameBar.isHidden() || trackedNpcId() == -1)
		{
			return;
		}
		release();
	}

	private void release()
	{
		gameBar.release("Game boss bar shown again");
	}

	public void reset()
	{
		// Nothing else unhides the bar, so do it here even when the game no longer tracks an NPC.
		release();
		npc = null;
		searchedId = -1;
		loggedTrackedId = -1;
		healthSeenId = -1;
		loggedZeroId = -1;
		loggedFill = -1;
		loggedTrail = -1;
		loggedTrack = -1;
		phaseColors = null;
	}

	public boolean isHidden()
	{
		return gameBar.isHidden();
	}

	public int maxHealth()
	{
		return client.getVarbitValue(VarbitID.HPBAR_HUD_BASEHP);
	}

	public int health()
	{
		return client.getVarbitValue(VarbitID.HPBAR_HUD_HP);
	}

	// Right after the bar moves to another NPC it can read 0 before that NPC's health arrives, so a 0 only
	// counts once the bar has shown health above 0 for the NPC it tracks.
	public boolean hasHealth()
	{
		final int trackedId = trackedNpcId();
		if (health() > 0)
		{
			healthSeenId = trackedId;
			return true;
		}
		return trackedId != -1 && trackedId == healthSeenId;
	}

	public boolean isPercentOnly()
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

	public float[] phaseMarkers(int maxHealth)
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
