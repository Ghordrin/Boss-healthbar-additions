package com.ghordrin.bosshealthbar;

import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

// Reads the counts RuneLite's "Special Attack Counter" plugin shows in its info boxes, one per weapon.
@Slf4j
@Singleton
class SpecialAttackCounts
{
	private static final String PLUGIN_NAME = "Special Attack Counter";
	// InfoBox.getName() defaults to the plugin's class name and the info box's class name.
	@VisibleForTesting
	static final String BOX_NAME = "SpecialCounterPlugin_SpecialCounter";

	@Value
	static class Reading
	{
		BufferedImage image;
		String text;
		Color color;
	}

	private final BossHealthBarConfig config;
	private final PluginManager pluginManager;
	private final InfoBoxManager infoBoxManager;
	private final DebugLog debugLog;
	// A broken info box fails every tick, so the debug log gets it once until a read works again.
	private boolean failureLogged;

	private Boolean pluginActive;
	private boolean available;
	private List<Reading> readings = Collections.emptyList();
	private boolean missedTick;

	@Inject
	SpecialAttackCounts(BossHealthBarConfig config, PluginManager pluginManager, InfoBoxManager infoBoxManager,
		DebugLog debugLog)
	{
		this.config = config;
		this.pluginManager = pluginManager;
		this.infoBoxManager = infoBoxManager;
		this.debugLog = debugLog;
	}

	void invalidatePlugin()
	{
		pluginActive = null;
	}

	void reset()
	{
		pluginActive = null;
		available = false;
		clear();
	}

	private void clear()
	{
		readings = Collections.emptyList();
		missedTick = false;
	}

	// Called each game tick, so the overlay doesn't go through the info boxes every frame.
	void update(Actor opponent)
	{
		if (pluginActive == null)
		{
			pluginActive = isPluginActive();
		}
		available = pluginActive && config.showSpecialAttackCounts();

		if (!available || opponent == null)
		{
			clear();
			return;
		}

		try
		{
			accept(find(infoBoxManager.getInfoBoxes()));
			failureLogged = false;
		}
		catch (RuntimeException e)
		{
			log.debug("Couldn't read Special Attack Counter info boxes", e);
			if (!failureLogged)
			{
				failureLogged = true;
				debugLog.add("Couldn't read Special Attack Counter info boxes: {}", e.toString());
			}
			clear();
		}
	}

	// Their info boxes can be gone for a tick while they're rebuilt, so the last readings are kept for one tick.
	@VisibleForTesting
	void accept(List<Reading> found)
	{
		if (!found.isEmpty())
		{
			if (!found.equals(readings))
			{
				readings = found;
			}
			missedTick = false;
		}
		else if (!readings.isEmpty() && !missedTick)
		{
			missedTick = true;
		}
		else
		{
			clear();
		}
	}

	boolean isAvailable()
	{
		return available;
	}

	List<Reading> readings()
	{
		return readings;
	}

	private boolean isPluginActive()
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (PLUGIN_NAME.equals(plugin.getName()))
			{
				return pluginManager.isPluginActive(plugin);
			}
		}
		return false;
	}

	@VisibleForTesting
	static List<Reading> find(List<InfoBox> infoBoxes)
	{
		List<Reading> found = null;
		for (InfoBox infoBox : infoBoxes)
		{
			if (!matches(infoBox))
			{
				continue;
			}
			final String text = infoBox.getText();
			if (text == null || text.isEmpty())
			{
				continue;
			}
			if (found == null)
			{
				found = new ArrayList<>();
			}
			found.add(new Reading(infoBox.getImage(), text, infoBox.getTextColor()));
		}
		return found != null ? Collections.unmodifiableList(found) : Collections.emptyList();
	}

	@VisibleForTesting
	static boolean matches(InfoBox infoBox)
	{
		return infoBox != null && BOX_NAME.equals(infoBox.getName());
	}
}
