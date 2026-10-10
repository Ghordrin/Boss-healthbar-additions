package com.ghordrin.bosshealthbar;

import net.runelite.client.config.ConfigManager;

final class ConfigWrites
{
	private ConfigWrites()
	{
	}

	// A null value means unset the key.
	static void write(ConfigManager configManager, String key, Object value)
	{
		if (value == null)
		{
			configManager.unsetConfiguration(BossHealthBarConfig.GROUP, key);
		}
		else
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, key, value);
		}
	}

	static Boolean savedBoolean(ConfigManager configManager, String key)
	{
		final String saved = configManager.getConfiguration(BossHealthBarConfig.GROUP, key);
		return saved != null ? Boolean.valueOf(saved) : null;
	}
}
