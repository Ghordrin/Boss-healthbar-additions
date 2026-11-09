package com.ghordrin.bosshealthbar.integrations;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.config.ConfigWrites;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import com.google.common.annotations.VisibleForTesting;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;

// Takes the health box of RuneLite's Opponent Information plugin off the screen while this plugin's bar shows, and
// puts it back after. Nothing is written to config, so nothing can be left off if the client closes.
@Singleton
public class OpponentInfoOverride
{
	private static final String PLUGIN_NAME = "Opponent Information";
	private static final String OVERLAY_NAME = "OpponentInfoOverlay";

	private static final String GROUP = "opponentinfo";
	private static final String KEY = "showOpponentHealthOverlay";
	private static final String SAVED_KEY = "savedOpponentHealthOverlay";
	private static final String HIDDEN_KEY = "opponentHealthOverlayHidden";

	private final ConfigManager configManager;
	private final OverlayManager overlayManager;
	private final DebugLog debugLog;

	private boolean hiding;
	private Overlay heldOverlay;

	@Inject
	OpponentInfoOverride(ConfigManager configManager, OverlayManager overlayManager, DebugLog debugLog)
	{
		this.configManager = configManager;
		this.overlayManager = overlayManager;
		this.debugLog = debugLog;
	}

	// Asked every frame, so it only acts when the answer changes.
	public void update(boolean hide)
	{
		if (hide == hiding)
		{
			return;
		}
		hiding = hide;
		if (hide)
		{
			overlayManager.removeIf(this::hold);
			if (heldOverlay != null)
			{
				debugLog.add("Opponent Information box hidden");
			}
		}
		else if (heldOverlay != null)
		{
			overlayManager.add(heldOverlay);
			heldOverlay = null;
			debugLog.add("Opponent Information box shown again");
		}
	}

	private boolean hold(Overlay overlay)
	{
		final Plugin plugin = overlay.getPlugin();
		if (heldOverlay != null || !isOpponentInfoBox(overlay.getName(), plugin != null ? plugin.getName() : null))
		{
			return false;
		}
		heldOverlay = overlay;
		return true;
	}

	@VisibleForTesting
	static boolean isOpponentInfoBox(String overlayName, String pluginName)
	{
		return OVERLAY_NAME.equals(overlayName) && PLUGIN_NAME.equals(pluginName);
	}

	// That plugin adds and removes its overlay itself when it starts and stops, so what is held no longer counts,
	// and the next frame hides the box again if it should be.
	public void onPluginChanged(Plugin plugin)
	{
		if (PLUGIN_NAME.equals(plugin.getName()))
		{
			heldOverlay = null;
			hiding = false;
		}
	}

	// Earlier versions turned that plugin's setting off and kept the user's own value here, so a client closed
	// while it was off puts it back on the next start.
	public void restoreOldSetting()
	{
		if (!Boolean.parseBoolean(configManager.getConfiguration(BossHealthBarConfig.GROUP, HIDDEN_KEY)))
		{
			return;
		}

		ConfigWrites.write(configManager, GROUP, KEY, configManager.getConfiguration(BossHealthBarConfig.GROUP, SAVED_KEY));

		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, SAVED_KEY);
		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, HIDDEN_KEY);
	}
}
