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
	private final InfoBoxManager infoBoxManager;
	private final PluginReader plugin;
	private final PluginReader.ReadFailure readFailure;

	private boolean available;
	private final PluginReader.HeldReading<List<Reading>> readings = new PluginReader.HeldReading<>();

	@Inject
	SpecialAttackCounts(BossHealthBarConfig config, PluginManager pluginManager, InfoBoxManager infoBoxManager,
		DebugLog debugLog)
	{
		this.config = config;
		this.infoBoxManager = infoBoxManager;
		this.plugin = new PluginReader(pluginManager, PLUGIN_NAME);
		this.readFailure = new PluginReader.ReadFailure(debugLog, "Couldn't read Special Attack Counter info boxes");
	}

	void invalidatePlugin()
	{
		plugin.invalidate();
	}

	void reset()
	{
		plugin.invalidate();
		available = false;
		readings.clear();
	}

	// Called each game tick, so the overlay doesn't go through the info boxes every frame.
	void update(Actor opponent)
	{
		available = plugin.isActive() && config.showSpecialAttackCounts();

		if (!available || opponent == null)
		{
			readings.clear();
			return;
		}

		try
		{
			final List<Reading> found = find(infoBoxManager.getInfoBoxes());
			readings.accept(null, found.isEmpty() ? null : found);
			readFailure.succeeded();
		}
		catch (RuntimeException e)
		{
			readFailure.failed(log, e);
			readings.clear();
		}
	}

	boolean isAvailable()
	{
		return available;
	}

	List<Reading> readings()
	{
		final List<Reading> held = readings.reading();
		return held != null ? held : Collections.emptyList();
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
