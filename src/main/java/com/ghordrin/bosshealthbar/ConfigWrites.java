package com.ghordrin.bosshealthbar;

import lombok.Value;
import net.runelite.client.config.ConfigManager;

final class ConfigWrites
{
	// A null value means unset the key.
	@Value
	static class Write
	{
		String key;
		Object value;
	}

	private ConfigWrites()
	{
	}

	static void write(ConfigManager configManager, String key, Object value)
	{
		write(configManager, BossHealthBarConfig.GROUP, key, value);
	}

	static void write(ConfigManager configManager, String group, String key, Object value)
	{
		if (value == null)
		{
			configManager.unsetConfiguration(group, key);
		}
		else
		{
			configManager.setConfiguration(group, key, value);
		}
	}

	static Boolean savedBoolean(ConfigManager configManager, String key)
	{
		final String saved = configManager.getConfiguration(BossHealthBarConfig.GROUP, key);
		return saved != null ? Boolean.valueOf(saved) : null;
	}
}
