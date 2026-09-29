package com.ghordrin.bosshealthbar;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

// Turns off the health bar of RuneLite's Opponent Information plugin while this plugin runs, and
// puts the user's own setting back afterwards.
@Singleton
class OpponentInfoOverride
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

	void apply()
	{
		if (!config.hideVanillaOverlay())
		{
			return;
		}

		if (!isHidden())
		{
			final String current = configManager.getConfiguration(GROUP, KEY);
			if (current == null)
			{
				configManager.unsetConfiguration(BossHealthBarConfig.GROUP, SAVED_KEY);
			}
			else
			{
				configManager.setConfiguration(BossHealthBarConfig.GROUP, SAVED_KEY, current);
			}
			configManager.setConfiguration(BossHealthBarConfig.GROUP, HIDDEN_KEY, true);
		}

		configManager.setConfiguration(GROUP, KEY, false);
	}

	void restore()
	{
		if (!isHidden())
		{
			return;
		}

		final String saved = configManager.getConfiguration(BossHealthBarConfig.GROUP, SAVED_KEY);
		if (saved == null)
		{
			configManager.unsetConfiguration(GROUP, KEY);
		}
		else
		{
			configManager.setConfiguration(GROUP, KEY, saved);
		}

		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, SAVED_KEY);
		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, HIDDEN_KEY);
	}

	private boolean isHidden()
	{
		return Boolean.parseBoolean(configManager.getConfiguration(BossHealthBarConfig.GROUP, HIDDEN_KEY));
	}
}
