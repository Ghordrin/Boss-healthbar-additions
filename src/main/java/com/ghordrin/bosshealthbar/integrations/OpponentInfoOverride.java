package com.ghordrin.bosshealthbar.integrations;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.config.ConfigWrites;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

// Turns off the health bar of RuneLite's Opponent Information plugin while this plugin runs, and
// puts the user's own setting back afterwards.
@Singleton
public class OpponentInfoOverride
{
	private static final String GROUP = "opponentinfo";
	private static final String KEY = "showOpponentHealthOverlay";
	// Stored in config so the original value can still be restored if the client closes while hidden.
	private static final String SAVED_KEY = "savedOpponentHealthOverlay";
	private static final String HIDDEN_KEY = "opponentHealthOverlayHidden";

	private final ConfigManager configManager;
	private final BossHealthBarConfig config;

	@Inject
	OpponentInfoOverride(ConfigManager configManager, BossHealthBarConfig config)
	{
		this.configManager = configManager;
		this.config = config;
	}

	public void apply()
	{
		if (!config.hideVanillaOverlay())
		{
			return;
		}

		if (!isHidden())
		{
			ConfigWrites.write(configManager, SAVED_KEY, configManager.getConfiguration(GROUP, KEY));
			configManager.setConfiguration(BossHealthBarConfig.GROUP, HIDDEN_KEY, true);
		}

		configManager.setConfiguration(GROUP, KEY, false);
	}

	public void restore()
	{
		if (!isHidden())
		{
			return;
		}

		ConfigWrites.write(configManager, GROUP, KEY, configManager.getConfiguration(BossHealthBarConfig.GROUP, SAVED_KEY));

		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, SAVED_KEY);
		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, HIDDEN_KEY);
	}

	private boolean isHidden()
	{
		return Boolean.parseBoolean(configManager.getConfiguration(BossHealthBarConfig.GROUP, HIDDEN_KEY));
	}
}
