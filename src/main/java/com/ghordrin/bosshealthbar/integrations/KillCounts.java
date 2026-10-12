package com.ghordrin.bosshealthbar.integrations;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.QuantityFormatter;

// Reads the kill counts saved by RuneLite's Chat Commands plugin. It keys them by the boss name in the
// game's kill count message, lowercased, which is usually the NPC name.
@Singleton
public class KillCounts
{
	public static final String CONFIG_GROUP = "killcount";

	private final ConfigManager configManager;

	private String cachedKey;
	private String cachedText;

	@Inject
	KillCounts(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	public static String text(Integer count)
	{
		return count != null && count > 0 ? "KC " + QuantityFormatter.formatNumber(count.longValue()) : null;
	}

	public void invalidate()
	{
		cachedKey = null;
		cachedText = null;
	}

	// Called every frame, so the result is kept for the last key.
	public String textFor(String key)
	{
		if (key == null)
		{
			return null;
		}
		if (!key.equals(cachedKey))
		{
			cachedKey = key;
			cachedText = text(configManager.getRSProfileConfiguration(CONFIG_GROUP, key, int.class));
		}
		return cachedText;
	}
}
