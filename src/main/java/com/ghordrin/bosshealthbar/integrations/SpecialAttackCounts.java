package com.ghordrin.bosshealthbar.integrations;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.debug.DebugLog;
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

// Reads the counts RuneLite's "Special Attack Counter" plugin, or the "Better Party Defence" plugin, shows in its
// info boxes, one per weapon.
@Slf4j
@Singleton
public class SpecialAttackCounts
{
	private static final String PLUGIN_NAME = "Special Attack Counter";
	private static final String BETTER_PLUGIN_NAME = "Better Party Defence";
	// InfoBox.getName() defaults to the plugin's class name and the info box's class name.
	@VisibleForTesting
	static final String BOX_NAME = "SpecialCounterPlugin_SpecialCounter";
	@VisibleForTesting
	static final String BETTER_BOX_NAME = "BetterPartyDefencePlugin_SpecWeaponInfoBox";

	@Value
	public static class Reading
	{
		BufferedImage image;
		String text;
		Color color;
		// Names who landed what, so it is shown when the count is hovered.
		String tooltip;
	}

	private final BossHealthBarConfig config;
	private final InfoBoxManager infoBoxManager;
	private final PluginReader plugin;
	private final PluginReader betterPlugin;
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
		this.betterPlugin = new PluginReader(pluginManager, BETTER_PLUGIN_NAME);
		this.readFailure = new PluginReader.ReadFailure(debugLog, "Couldn't read special attack info boxes");
	}

	public void invalidatePlugin()
	{
		plugin.invalidate();
		betterPlugin.invalidate();
	}

	public void reset()
	{
		invalidatePlugin();
		available = false;
		readings.clear();
	}

	// Called each game tick, so the overlay doesn't go through the info boxes every frame.
	public void update(Actor opponent)
	{
		final boolean betterPluginActive = betterPlugin.isActive();
		available = (plugin.isActive() || betterPluginActive) && config.showSpecialAttackCounts();

		if (!available || opponent == null)
		{
			readings.clear();
			return;
		}

		try
		{
			final List<Reading> found = select(infoBoxManager.getInfoBoxes(), betterPluginActive);
			readings.accept(null, found.isEmpty() ? null : found);
			readFailure.succeeded();
		}
		catch (RuntimeException e)
		{
			readFailure.failed(log, e);
			readings.clear();
		}
	}

	public boolean isAvailable()
	{
		return available;
	}

	public List<Reading> readings()
	{
		final List<Reading> held = readings.reading();
		return held != null ? held : Collections.emptyList();
	}

	// Both plugins can count the same specs, so only one is shown. Better Party Defence's boxes win while it shows
	// them, since it also counts misses and syncs the party; with them turned off, Special Attack Counter's show.
	@VisibleForTesting
	static List<Reading> select(List<InfoBox> infoBoxes, boolean betterPluginActive)
	{
		final List<Reading> better = betterPluginActive ? find(infoBoxes, BETTER_BOX_NAME) : Collections.emptyList();
		return !better.isEmpty() ? better : find(infoBoxes, BOX_NAME);
	}

	@VisibleForTesting
	static List<Reading> find(List<InfoBox> infoBoxes, String boxName)
	{
		List<Reading> found = null;
		for (InfoBox infoBox : infoBoxes)
		{
			if (!matches(infoBox, boxName))
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
			found.add(new Reading(infoBox.getImage(), text, infoBox.getTextColor(), infoBox.getTooltip()));
		}
		return found != null ? Collections.unmodifiableList(found) : Collections.emptyList();
	}

	@VisibleForTesting
	static boolean matches(InfoBox infoBox, String boxName)
	{
		return infoBox != null && boxName.equals(infoBox.getName());
	}
}
