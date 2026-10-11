package com.ghordrin.bosshealthbar;

import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.SpritePixels;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

// Reads the defence the "Better Party Defence" or "Party Defence Tracker" plugin shows in its info box,
// and the magic defence from Better Party Defence.
@Slf4j
@Singleton
class PartyDefence
{
	private static final String PLUGIN_NAME = "Party Defence Tracker";
	private static final String BETTER_PLUGIN_NAME = "Better Party Defence";
	// InfoBox.getName() defaults to the plugin's class name and the info box's class name.
	@VisibleForTesting
	static final String BOX_NAME = "DefenceTrackerPlugin_DefenceInfoBox";
	@VisibleForTesting
	static final String BETTER_BOX_NAME = "BetterPartyDefencePlugin_DefenceInfoBox";

	@Value
	static class Reading
	{
		String text;
		Color color;
	}

	private final Client client;
	private final BossHealthBarConfig config;
	private final InfoBoxManager infoBoxManager;
	private final SkillIconManager skillIconManager;
	private final PluginReader plugin;
	private final PluginReader betterPlugin;
	private final PluginReader.ReadFailure readFailure;

	private boolean available;
	private boolean magicAvailable;
	private final PluginReader.HeldReading<Reading> defence = new PluginReader.HeldReading<>();
	private final PluginReader.HeldReading<Reading> magic = new PluginReader.HeldReading<>();
	private BufferedImage icon;
	private BufferedImage magicIcon;
	private final MagicIconMatcher magicIconMatcher = new MagicIconMatcher();
	private SpritePixels magicOverrideSource;
	private BufferedImage magicOverrideImage;

	@Inject
	PartyDefence(Client client, BossHealthBarConfig config, PluginManager pluginManager, InfoBoxManager infoBoxManager,
		SkillIconManager skillIconManager, DebugLog debugLog)
	{
		this.client = client;
		this.config = config;
		this.infoBoxManager = infoBoxManager;
		this.skillIconManager = skillIconManager;
		this.plugin = new PluginReader(pluginManager, PLUGIN_NAME);
		this.betterPlugin = new PluginReader(pluginManager, BETTER_PLUGIN_NAME);
		this.readFailure = new PluginReader.ReadFailure(debugLog, "Couldn't read party defence info box");
	}

	void invalidatePlugin()
	{
		plugin.invalidate();
		betterPlugin.invalidate();
	}

	void reset()
	{
		invalidatePlugin();
		available = false;
		magicAvailable = false;
		defence.clear();
		magic.clear();
		magicIconMatcher.reset();
		magicOverrideSource = null;
		magicOverrideImage = null;
	}

	// Called each game tick, so the overlay doesn't go through the info boxes every frame.
	void update(Actor opponent)
	{
		final boolean pluginActive = plugin.isActive();
		final boolean betterPluginActive = betterPlugin.isActive();
		available = (pluginActive || betterPluginActive) && config.showPartyDefence();
		magicAvailable = betterPluginActive && config.showMagicDefence();

		final String name = opponent != null ? opponent.getName() : null;
		if (!available || name == null)
		{
			defence.clear();
		}
		if (!magicAvailable || name == null)
		{
			magic.clear();
		}
		if (name == null || !available && !magicAvailable)
		{
			return;
		}

		try
		{
			final List<InfoBox> infoBoxes = infoBoxManager.getInfoBoxes();
			final Predicate<BufferedImage> isMagic = betterPluginActive ? magicTest() : image -> false;
			if (available)
			{
				defence.accept(name, select(infoBoxes, name, betterPluginActive, isMagic));
			}
			if (magicAvailable)
			{
				magic.accept(name, findBetterMagic(infoBoxes, isMagic));
			}
			readFailure.succeeded();
		}
		catch (RuntimeException e)
		{
			readFailure.failed(log, e);
			defence.clear();
			magic.clear();
		}
	}

	private Predicate<BufferedImage> magicTest()
	{
		final BufferedImage skillImage = skillIconManager.getSkillImage(Skill.MAGIC);
		final Map<Integer, SpritePixels> overrides = client.getSpriteOverrides();
		final SpritePixels override = overrides != null ? overrides.get(SpriteID.Staticons.MAGIC) : null;
		if (override != magicOverrideSource)
		{
			magicOverrideSource = override;
			magicOverrideImage = override != null ? override.toBufferedImage() : null;
		}
		final BufferedImage overrideImage = magicOverrideImage;
		return image -> magicIconMatcher.isMagic(image, skillImage, overrideImage);
	}

	boolean isAvailable()
	{
		return available;
	}

	boolean isMagicAvailable()
	{
		return magicAvailable;
	}

	Reading readingFor(Actor opponent)
	{
		return opponent != null ? defence.readingFor(opponent.getName()) : null;
	}

	Reading magicReadingFor(Actor opponent)
	{
		return opponent != null ? magic.readingFor(opponent.getName()) : null;
	}

	BufferedImage icon()
	{
		if (icon == null)
		{
			icon = skillIconManager.getSkillImage(Skill.DEFENCE, true);
		}
		return icon;
	}

	BufferedImage magicIcon()
	{
		if (magicIcon == null)
		{
			magicIcon = skillIconManager.getSkillImage(Skill.MAGIC, true);
		}
		return magicIcon;
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
			final Reading reading = reading(infoBox);
			if (reading != null)
			{
				return reading;
			}
		}
		return null;
	}

	// Better Party Defence removes the other plugin's boxes while it runs, so its box wins when it's on.
	@VisibleForTesting
	static Reading select(List<InfoBox> infoBoxes, String opponentName, boolean betterPluginActive,
		Predicate<BufferedImage> isMagic)
	{
		return betterPluginActive ? findBetter(infoBoxes, isMagic) : find(infoBoxes, opponentName);
	}

	// Its tooltip doesn't name the NPC, so the reading is for whichever NPC that plugin tracks.
	@VisibleForTesting
	static Reading findBetter(List<InfoBox> infoBoxes, Predicate<BufferedImage> isMagic)
	{
		final List<InfoBox> boxes = betterBoxes(infoBoxes);
		final InfoBox magicBox = magicBox(boxes, isMagic);
		for (InfoBox box : boxes)
		{
			if (box != magicBox)
			{
				return reading(box);
			}
		}
		return null;
	}

	@VisibleForTesting
	static Reading findBetterMagic(List<InfoBox> infoBoxes, Predicate<BufferedImage> isMagic)
	{
		final InfoBox magicBox = magicBox(betterBoxes(infoBoxes), isMagic);
		return magicBox != null ? reading(magicBox) : null;
	}

	// The Defence and Magic defence boxes share a name and can be added in either order, so only the image
	// tells them apart. A box that isn't recognised as Magic is taken as Defence.
	private static InfoBox magicBox(List<InfoBox> boxes, Predicate<BufferedImage> isMagic)
	{
		for (InfoBox box : boxes)
		{
			if (isMagic.test(box.getImage()))
			{
				return box;
			}
		}
		return null;
	}

	private static List<InfoBox> betterBoxes(List<InfoBox> infoBoxes)
	{
		final List<InfoBox> boxes = new ArrayList<>(2);
		for (InfoBox infoBox : infoBoxes)
		{
			if (infoBox != null && BETTER_BOX_NAME.equals(infoBox.getName()))
			{
				boxes.add(infoBox);
			}
		}
		return boxes;
	}

	private static Reading reading(InfoBox infoBox)
	{
		final String text = infoBox.getText();
		return text != null && !text.isEmpty() ? new Reading(text, infoBox.getTextColor()) : null;
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
