package com.ghordrin.bosshealthbar;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

// Reads the health lines users set up in the "Boss Health Indicators" plugin, which draws them onto the
// game's boss bar. That bar is hidden when this one replaces it, so the lines are drawn here instead.
@Slf4j
@Singleton
class HealthIndicatorMarkers
{
	static final String CONFIG_GROUP = "bosshealthindicators";
	private static final String CONFIG_KEY = "indicators";
	private static final String PLUGIN_NAME = "Boss Health Indicators";

	@Value
	static class Marker
	{
		float fraction;
		Color color;
	}

	@Value
	private static class Entry
	{
		Pattern bossName;
		List<Marker> markers;
	}

	static final Marker[] NONE = new Marker[0];

	private final BossHealthBarConfig config;
	private final ConfigManager configManager;
	private final PluginManager pluginManager;
	private final Gson gson;

	private List<Entry> entries;
	private String cachedName;
	private Marker[] cachedMarkers = NONE;

	@Inject
	HealthIndicatorMarkers(BossHealthBarConfig config, ConfigManager configManager, PluginManager pluginManager, Gson gson)
	{
		this.config = config;
		this.configManager = configManager;
		this.pluginManager = pluginManager;
		this.gson = gson;
	}

	void invalidate()
	{
		entries = null;
		cachedName = null;
	}

	// Called every frame, so the result is kept for the last name.
	Marker[] markersFor(String name)
	{
		if (name == null || !config.showHealthIndicatorMarkers())
		{
			return NONE;
		}

		if (entries == null)
		{
			entries = isPluginActive() ? parse(gson, configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY)) : Collections.emptyList();
			cachedName = null;
		}

		if (!name.equals(cachedName))
		{
			cachedName = name;
			cachedMarkers = match(entries, name);
		}
		return cachedMarkers;
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

	private static Marker[] match(List<Entry> entries, String name)
	{
		final List<Marker> matched = new ArrayList<>();
		for (Entry entry : entries)
		{
			if (entry.getBossName().matcher(name).matches())
			{
				matched.addAll(entry.getMarkers());
			}
		}
		return matched.isEmpty() ? NONE : matched.toArray(NONE);
	}

	@VisibleForTesting
	static Marker[] markersFor(Gson gson, String json, String name)
	{
		return match(parse(gson, json), name);
	}

	// The format belongs to the other plugin and could change, so anything unexpected is skipped.
	private static List<Entry> parse(Gson gson, String json)
	{
		if (json == null || json.isEmpty())
		{
			return Collections.emptyList();
		}

		final JsonElement root;
		try
		{
			root = gson.fromJson(json, JsonElement.class);
		}
		catch (JsonParseException e)
		{
			log.debug("Couldn't read Boss Health Indicators config", e);
			return Collections.emptyList();
		}
		if (root == null || !root.isJsonArray())
		{
			return Collections.emptyList();
		}

		final List<Entry> entries = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray())
		{
			final Entry entry = parseEntry(element);
			if (entry != null)
			{
				entries.add(entry);
			}
		}
		return entries;
	}

	private static Entry parseEntry(JsonElement element)
	{
		if (!element.isJsonObject())
		{
			return null;
		}
		final JsonObject object = element.getAsJsonObject();
		final String bossName = string(object.get("bossName"));
		final JsonElement markerList = object.get("entries");
		if (bossName == null || markerList == null || !markerList.isJsonArray())
		{
			return null;
		}

		final Pattern pattern;
		try
		{
			pattern = Pattern.compile(bossName);
		}
		catch (PatternSyntaxException e)
		{
			return null;
		}

		final List<Marker> markers = new ArrayList<>();
		for (JsonElement markerElement : (JsonArray) markerList)
		{
			final Marker marker = parseMarker(markerElement);
			if (marker != null)
			{
				markers.add(marker);
			}
		}
		return markers.isEmpty() ? null : new Entry(pattern, markers);
	}

	private static Marker parseMarker(JsonElement element)
	{
		if (!element.isJsonObject())
		{
			return null;
		}
		final JsonObject object = element.getAsJsonObject();
		final JsonElement percentage = object.get("percentage");
		final Color color = color(object.get("color"));
		if (percentage == null || !percentage.isJsonPrimitive() || !percentage.getAsJsonPrimitive().isNumber() || color == null)
		{
			return null;
		}
		final float fraction = percentage.getAsFloat();
		return fraction > 0f && fraction < 1f ? new Marker(fraction, color) : null;
	}

	// Saved by Gson without a type adapter, so a color is an object holding its ARGB value. The other
	// plugin draws its lines opaque, so the alpha is dropped.
	private static Color color(JsonElement element)
	{
		if (element == null || !element.isJsonObject())
		{
			return null;
		}
		final JsonElement value = element.getAsJsonObject().get("value");
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
		{
			return null;
		}
		return new Color(value.getAsInt());
	}

	private static String string(JsonElement element)
	{
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
			? element.getAsString() : null;
	}
}
