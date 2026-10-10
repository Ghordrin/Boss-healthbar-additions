package com.ghordrin.bosshealthbar;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

final class PluginState
{
	private PluginState()
	{
	}

	static Plugin find(PluginManager pluginManager, String name)
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
