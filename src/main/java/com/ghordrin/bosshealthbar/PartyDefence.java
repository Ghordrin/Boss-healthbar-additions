package com.ghordrin.bosshealthbar;

import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

// Reads the defence the "Party Defence Tracker" plugin shows in its info box for the current opponent.
@Slf4j
@Singleton
class PartyDefence
{
	private static final String PLUGIN_NAME = "Party Defence Tracker";
	// InfoBox.getName() defaults to the plugin's class name and the info box's class name.
	@VisibleForTesting
	static final String BOX_NAME = "DefenceTrackerPlugin_DefenceInfoBox";

	@Value
	static class Reading
	{
		String text;
		Color color;
	}

	private final BossHealthBarConfig config;
	private final PluginManager pluginManager;
	private final InfoBoxManager infoBoxManager;
	private final SkillIconManager skillIconManager;

	private Boolean pluginActive;
	private boolean available;
	private String readingName;
	private Reading reading;
	private boolean missedTick;
	private BufferedImage icon;

	@Inject
	PartyDefence(BossHealthBarConfig config, PluginManager pluginManager, InfoBoxManager infoBoxManager,
		SkillIconManager skillIconManager)
	{
		this.config = config;
		this.pluginManager = pluginManager;
		this.infoBoxManager = infoBoxManager;
		this.skillIconManager = skillIconManager;
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
		readingName = null;
		reading = null;
		missedTick = false;
	}

	// Called each game tick, so the overlay doesn't go through the info boxes every frame.
	void update(Actor opponent)
	{
		if (pluginActive == null)
		{
			pluginActive = isPluginActive();
		}
		available = pluginActive && config.showPartyDefence();

		final String name = opponent != null ? opponent.getName() : null;
		if (!available || name == null)
		{
			clear();
			return;
		}

		try
		{
			accept(name, find(infoBoxManager.getInfoBoxes(), name));
		}
		catch (RuntimeException e)
		{
			log.debug("Couldn't read Party Defence Tracker info box", e);
			clear();
		}
	}

	// Their info box can be gone for a tick while it's rebuilt, so the last reading is kept for one tick.
	@VisibleForTesting
	void accept(String name, Reading found)
	{
		if (found != null)
		{
			readingName = name;
			reading = found;
			missedTick = false;
		}
		else if (reading != null && !missedTick && Objects.equals(name, readingName))
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

	Reading readingFor(Actor opponent)
	{
		return opponent != null ? readingFor(opponent.getName()) : null;
	}

	@VisibleForTesting
	Reading readingFor(String name)
	{
		return name != null && name.equals(readingName) ? reading : null;
	}

	BufferedImage icon()
	{
		if (icon == null)
		{
			icon = skillIconManager.getSkillImage(Skill.DEFENCE, true);
		}
		return icon;
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
	static Reading find(List<InfoBox> infoBoxes, String opponentName)
	{
		for (InfoBox infoBox : infoBoxes)
		{
			if (!matches(infoBox, opponentName))
			{
				continue;
			}
			final String text = infoBox.getText();
			if (text != null && !text.isEmpty())
			{
				return new Reading(text, infoBox.getTextColor());
			}
		}
		return null;
	}

	@VisibleForTesting
	static boolean matches(InfoBox infoBox, String opponentName)
	{
		if (infoBox == null || opponentName == null || !BOX_NAME.equals(infoBox.getName()))
		{
			return false;
		}
		final String tooltip = infoBox.getTooltip();
		return tooltip != null && Text.removeTags(tooltip).trim().equalsIgnoreCase(Text.removeTags(opponentName).trim());
	}
}
