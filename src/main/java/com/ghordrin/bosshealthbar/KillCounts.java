package com.ghordrin.bosshealthbar;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.Text;

// Reads the kill counts saved by RuneLite's Chat Commands plugin. It keys them by the boss name in the
// game's kill count message, lowercased, which is usually the NPC name.
@Singleton
class KillCounts
{
	static final String CONFIG_GROUP = "killcount";

	// Opponents whose kill count is saved under a different name, such as one count shared by several NPCs.
	@VisibleForTesting
	static final Map<String, String> KEYS = new ImmutableMap.Builder<String, String>()
		.put("dusk", "grotesque guardians")
		.put("dawn", "grotesque guardians")
		.put("ahrim the blighted", "barrows chests")
		.put("dharok the wretched", "barrows chests")
		.put("guthan the infested", "barrows chests")
		.put("karil the tainted", "barrows chests")
		.put("torag the corrupted", "barrows chests")
		.put("verac the defiled", "barrows chests")
		.put("blood moon", "lunar chest")
		.put("blue moon", "lunar chest")
		.put("eclipse moon", "lunar chest")
		.put("branda the fire queen", "royal titans")
		.put("eldric the ice king", "royal titans")
		.put("crystalline hunllef", "gauntlet")
		.put("corrupted hunllef", "corrupted gauntlet")
		.put("the nightmare", "nightmare")
		.put("the leviathan", "leviathan")
		.put("the whisperer", "whisperer")
		.put("the hueycoatl", "hueycoatl")
		.put("the mimic", "mimic")
		.put("judge of yama", "yama")
		.put("doom of mokhaiotl (shielded)", "doom of mokhaiotl")
		.put("doom of mokhaiotl (burrowed)", "doom of mokhaiotl")
		.build();

	private final ConfigManager configManager;

	private String cachedKey;
	private String cachedText;

	@Inject
	KillCounts(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	static String key(String name)
	{
		if (name == null)
		{
			return null;
		}
		final String lower = Text.removeTags(name).trim().toLowerCase(Locale.ROOT);
		// Other opponents could share a name with an unrelated saved count.
		if (lower.isEmpty() || !KnownBosses.contains(lower))
		{
			return null;
		}
		return KEYS.getOrDefault(lower, lower);
	}

	static String text(Integer count)
	{
		return count != null && count > 0 ? "KC " + QuantityFormatter.formatNumber(count.longValue()) : null;
	}

	void invalidate()
	{
		cachedKey = null;
		cachedText = null;
	}

	// Called every frame, so the result is kept for the last key.
	String textFor(String key)
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
