package com.ghordrin.bosshealthbar.integrations;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

public final class PluginState
{
	private PluginState()
	{
	}

	public static Plugin find(PluginManager pluginManager, String name)
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (name.equals(plugin.getName()))
			{
				return plugin;
			}
		}
		return null;
	}

	static boolean isActive(PluginManager pluginManager, String name)
	{
		final Plugin plugin = find(pluginManager, name);
		return plugin != null && pluginManager.isPluginActive(plugin);
	}
}
