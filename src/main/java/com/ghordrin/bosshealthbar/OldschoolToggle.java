package com.ghordrin.bosshealthbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Value;
import net.runelite.client.config.ConfigManager;

// The Oldschool checkbox turns off Match boss colors and Rare gold bars and brings them back when it's
// unticked. Ticking either of those while Oldschool is on unticks Oldschool instead.
final class OldschoolToggle
{
	// What Match boss colors and Rare gold bars were set to before Oldschool turned them off.
	static final String SAVED_MATCH_BOSS_COLORS_KEY = "oldschoolSavedMatchBossColors";
	static final String SAVED_RARE_GOLD_BARS_KEY = "oldschoolSavedRareGoldBars";

	@Value
	static class Settings
	{
		boolean oldschool;
		boolean matchBossColors;
		boolean rareGoldBars;
		// Null when nothing is remembered.
		Boolean savedMatchBossColors;
		Boolean savedRareGoldBars;
	}

	// A null value means unset the key.
	@Value
	static class Write
	{
		String key;
		Boolean value;
	}

	private OldschoolToggle()
	{
	}

	static boolean handles(String key)
	{
		return BossHealthBarConfig.OLDSCHOOL_THEME_KEY.equals(key)
			|| BossHealthBarConfig.MATCH_BOSS_COLORS_KEY.equals(key)
			|| BossHealthBarConfig.RARE_GOLD_BARS_KEY.equals(key);
	}

	// The defaults of the three boxes. Memory goes first so unticking Oldschool restores nothing.
	static List<Write> resetWrites()
	{
		return Arrays.asList(
			new Write(SAVED_MATCH_BOSS_COLORS_KEY, null),
			new Write(SAVED_RARE_GOLD_BARS_KEY, null),
			new Write(BossHealthBarConfig.OLDSCHOOL_THEME_KEY, false),
			new Write(BossHealthBarConfig.MATCH_BOSS_COLORS_KEY, false),
			new Write(BossHealthBarConfig.RARE_GOLD_BARS_KEY, true));
	}

	// The remembered keys are cleared before anything is ticked back on, so the change events these
	// writes cause find nothing left to do.
	static List<Write> writes(String key, boolean oldValue, boolean newValue, Settings current)
	{
		if (oldValue == newValue)
		{
			return Collections.emptyList();
		}

		final List<Write> writes = new ArrayList<>();
		if (BossHealthBarConfig.OLDSCHOOL_THEME_KEY.equals(key))
		{
			if (newValue)
			{
				// Switching RuneLite profiles also reports changed keys. Values the new profile already
				// remembers are kept rather than overwritten with the boxes it has turned off.
				if (current.getSavedMatchBossColors() == null)
				{
					writes.add(new Write(SAVED_MATCH_BOSS_COLORS_KEY, current.isMatchBossColors()));
				}
				if (current.getSavedRareGoldBars() == null)
				{
					writes.add(new Write(SAVED_RARE_GOLD_BARS_KEY, current.isRareGoldBars()));
				}
				writes.add(new Write(BossHealthBarConfig.MATCH_BOSS_COLORS_KEY, false));
				writes.add(new Write(BossHealthBarConfig.RARE_GOLD_BARS_KEY, false));
			}
			else
			{
				restore(writes, null, current);
			}
		}
		else if (newValue && current.isOldschool()
			&& (BossHealthBarConfig.MATCH_BOSS_COLORS_KEY.equals(key) || BossHealthBarConfig.RARE_GOLD_BARS_KEY.equals(key)))
		{
			restore(writes, key, current);
			writes.add(new Write(BossHealthBarConfig.OLDSCHOOL_THEME_KEY, false));
		}
		return writes;
	}

	private static void restore(List<Write> writes, String skippedKey, Settings current)
	{
		if (current.getSavedMatchBossColors() != null)
		{
			writes.add(new Write(SAVED_MATCH_BOSS_COLORS_KEY, null));
		}
		if (current.getSavedRareGoldBars() != null)
		{
			writes.add(new Write(SAVED_RARE_GOLD_BARS_KEY, null));
		}
		if (current.getSavedMatchBossColors() != null && !BossHealthBarConfig.MATCH_BOSS_COLORS_KEY.equals(skippedKey))
		{
			writes.add(new Write(BossHealthBarConfig.MATCH_BOSS_COLORS_KEY, current.getSavedMatchBossColors()));
		}
		if (current.getSavedRareGoldBars() != null && !BossHealthBarConfig.RARE_GOLD_BARS_KEY.equals(skippedKey))
		{
			writes.add(new Write(BossHealthBarConfig.RARE_GOLD_BARS_KEY, current.getSavedRareGoldBars()));
		}
	}

	static void apply(ConfigManager configManager, BossHealthBarConfig config, String key, boolean oldValue,
		boolean newValue)
	{
		applyWrites(configManager, writes(key, oldValue, newValue,
			new Settings(config.oldschoolTheme(), config.matchBossColors(), config.rareGoldBars(),
				ConfigWrites.savedBoolean(configManager, SAVED_MATCH_BOSS_COLORS_KEY),
				ConfigWrites.savedBoolean(configManager, SAVED_RARE_GOLD_BARS_KEY))));
	}

	static void applyWrites(ConfigManager configManager, List<Write> writes)
	{
		// Each write posts its own change event straight away. Handling those again is harmless, since
		// the remembered keys are cleared before anything is turned back on.
		for (Write write : writes)
		{
			ConfigWrites.write(configManager, write.getKey(), write.getValue());
		}
	}
}
