package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BossHealthBarConfig.MATCH_BOSS_COLORS_KEY;
import static com.ghordrin.bosshealthbar.BossHealthBarConfig.OLDSCHOOL_THEME_KEY;
import static com.ghordrin.bosshealthbar.BossHealthBarConfig.RARE_GOLD_BARS_KEY;
import static com.ghordrin.bosshealthbar.OldschoolToggle.SAVED_RARE_GOLD_BARS_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;

public class OldschoolToggleTest
{
	// Stands in for ConfigManager: a write that changes a value posts a change event straight away,
	// which the plugin handles again. Unset boxes read as their defaults.
	private static class FakeConfig
	{
		final Map<String, Boolean> values = new HashMap<>();
		int events;

		FakeConfig(boolean oldschool, boolean match, boolean rare)
		{
			values.put(OLDSCHOOL_THEME_KEY, oldschool);
			values.put(MATCH_BOSS_COLORS_KEY, match);
			values.put(RARE_GOLD_BARS_KEY, rare);
		}

		void set(String key, Boolean value)
		{
			final Boolean old = values.get(key);
			if (Objects.equals(old, value))
			{
				return;
			}
			if (value == null)
			{
				values.remove(key);
			}
			else
			{
				values.put(key, value);
			}

			assertTrue("too many change events", ++events < 50);
			if (!OldschoolToggle.handles(key))
			{
				return;
			}
			for (ConfigWrites.Write write : OldschoolToggle.writes(key, orDefault(key, old), orDefault(key, value), settings()))
			{
				set(write.getKey(), (Boolean) write.getValue());
			}
		}

		OldschoolToggle.Settings settings()
		{
			return new OldschoolToggle.Settings(get(OLDSCHOOL_THEME_KEY), get(RARE_GOLD_BARS_KEY),
				values.get(SAVED_RARE_GOLD_BARS_KEY));
		}

		boolean get(String key)
		{
			return orDefault(key, values.get(key));
		}

		private static boolean orDefault(String key, Boolean value)
		{
			return value != null ? value : RARE_GOLD_BARS_KEY.equals(key);
		}
	}

	@Test
	public void tickingOnRemembersAndTurnsOffRareGoldBars()
	{
		final List<ConfigWrites.Write> writes = OldschoolToggle.writes(OLDSCHOOL_THEME_KEY, false, true,
			new OldschoolToggle.Settings(true, false, null));
		assertEquals(Arrays.asList(
			new ConfigWrites.Write(SAVED_RARE_GOLD_BARS_KEY, false),
			new ConfigWrites.Write(RARE_GOLD_BARS_KEY, false)), writes);
	}

	@Test
	public void tickingOffClearsMemoryThenRestores()
	{
		final List<ConfigWrites.Write> writes = OldschoolToggle.writes(OLDSCHOOL_THEME_KEY, true, false,
			new OldschoolToggle.Settings(false, false, true));
		assertEquals(Arrays.asList(
			new ConfigWrites.Write(SAVED_RARE_GOLD_BARS_KEY, null),
			new ConfigWrites.Write(RARE_GOLD_BARS_KEY, true)), writes);
	}

	@Test
	public void nothingToDoWhenTheValueDidntChange()
	{
		final OldschoolToggle.Settings settings = new OldschoolToggle.Settings(true, false, true);
		assertTrue(OldschoolToggle.writes(OLDSCHOOL_THEME_KEY, true, true, settings).isEmpty());
		assertTrue(OldschoolToggle.writes(RARE_GOLD_BARS_KEY, false, false, settings).isEmpty());
	}

	@Test
	public void rareGoldBarsOnlyMattersWhileOldschoolIsOn()
	{
		final OldschoolToggle.Settings off = new OldschoolToggle.Settings(false, true, null);
		assertTrue(OldschoolToggle.writes(RARE_GOLD_BARS_KEY, false, true, off).isEmpty());

		final OldschoolToggle.Settings on = new OldschoolToggle.Settings(true, false, true);
		assertTrue(OldschoolToggle.writes(RARE_GOLD_BARS_KEY, true, false, on).isEmpty());
		assertTrue(OldschoolToggle.writes("barWidth", false, true, on).isEmpty());
	}

	@Test
	public void tickingOnKeepsWhatIsAlreadyRemembered()
	{
		final List<ConfigWrites.Write> writes = OldschoolToggle.writes(OLDSCHOOL_THEME_KEY, false, true,
			new OldschoolToggle.Settings(true, false, true));
		assertEquals(Arrays.asList(new ConfigWrites.Write(RARE_GOLD_BARS_KEY, false)), writes);
	}

	@Test
	public void repeatedTogglingRoundTrips()
	{
		final FakeConfig config = new FakeConfig(false, true, true);
		for (int i = 0; i < 5; i++)
		{
			config.set(OLDSCHOOL_THEME_KEY, true);
			assertFalse(config.get(RARE_GOLD_BARS_KEY));

			config.set(OLDSCHOOL_THEME_KEY, false);
			assertTrue(config.get(RARE_GOLD_BARS_KEY));
			assertNull(config.values.get(SAVED_RARE_GOLD_BARS_KEY));
		}
	}

	@Test
	public void lastClickWinsWithoutLooping()
	{
		final FakeConfig config = new FakeConfig(false, false, true);
		config.set(OLDSCHOOL_THEME_KEY, true);
		assertFalse(config.get(RARE_GOLD_BARS_KEY));

		config.set(RARE_GOLD_BARS_KEY, true);
		assertFalse(config.get(OLDSCHOOL_THEME_KEY));
		assertTrue(config.get(RARE_GOLD_BARS_KEY));
		assertNull(config.values.get(SAVED_RARE_GOLD_BARS_KEY));
	}

	@Test
	public void tickingRareGoldBarsTurnsOldschoolOffAndClearsTheMemory()
	{
		final List<ConfigWrites.Write> writes = OldschoolToggle.writes(RARE_GOLD_BARS_KEY, false, true,
			new OldschoolToggle.Settings(true, true, false));
		assertEquals(Arrays.asList(
			new ConfigWrites.Write(SAVED_RARE_GOLD_BARS_KEY, null),
			new ConfigWrites.Write(OLDSCHOOL_THEME_KEY, false)), writes);
	}

	@Test
	public void unsetRareGoldBarsCountsAsOn()
	{
		final FakeConfig config = new FakeConfig(false, false, true);
		config.values.remove(RARE_GOLD_BARS_KEY);
		config.set(OLDSCHOOL_THEME_KEY, true);
		assertEquals(Boolean.TRUE, config.values.get(SAVED_RARE_GOLD_BARS_KEY));
		assertFalse(config.get(RARE_GOLD_BARS_KEY));

		config.set(OLDSCHOOL_THEME_KEY, false);
		assertTrue(config.get(RARE_GOLD_BARS_KEY));
	}

	// The two settings now work together: neither one turns the other off.
	@Test
	public void oldschoolAndMatchBossColorsNoLongerExcludeEachOther()
	{
		assertFalse(OldschoolToggle.handles(MATCH_BOSS_COLORS_KEY));

		final FakeConfig config = new FakeConfig(false, false, true);
		config.set(OLDSCHOOL_THEME_KEY, true);
		assertTrue(config.get(OLDSCHOOL_THEME_KEY));
		assertFalse(config.get(MATCH_BOSS_COLORS_KEY));

		config.set(MATCH_BOSS_COLORS_KEY, true);
		assertTrue(config.get(OLDSCHOOL_THEME_KEY));
		assertTrue(config.get(MATCH_BOSS_COLORS_KEY));

		config.set(OLDSCHOOL_THEME_KEY, false);
		config.set(OLDSCHOOL_THEME_KEY, true);
		assertTrue(config.get(MATCH_BOSS_COLORS_KEY));
	}

	@Test
	public void resetEndsOnTheDefaultsInAnyOrder()
	{
		final String[][] orders = {
			{OLDSCHOOL_THEME_KEY, MATCH_BOSS_COLORS_KEY, RARE_GOLD_BARS_KEY},
			{OLDSCHOOL_THEME_KEY, RARE_GOLD_BARS_KEY, MATCH_BOSS_COLORS_KEY},
			{MATCH_BOSS_COLORS_KEY, OLDSCHOOL_THEME_KEY, RARE_GOLD_BARS_KEY},
			{MATCH_BOSS_COLORS_KEY, RARE_GOLD_BARS_KEY, OLDSCHOOL_THEME_KEY},
			{RARE_GOLD_BARS_KEY, OLDSCHOOL_THEME_KEY, MATCH_BOSS_COLORS_KEY},
			{RARE_GOLD_BARS_KEY, MATCH_BOSS_COLORS_KEY, OLDSCHOOL_THEME_KEY},
		};
		for (String[] order : orders)
		{
			final FakeConfig config = new FakeConfig(false, true, true);
			config.set(OLDSCHOOL_THEME_KEY, true);

			// RuneLite's Reset puts each setting back to its default, then the plugin's reset writes run.
			for (String key : order)
			{
				config.set(key, RARE_GOLD_BARS_KEY.equals(key));
			}
			for (ConfigWrites.Write write : OldschoolToggle.resetWrites())
			{
				config.set(write.getKey(), (Boolean) write.getValue());
			}

			final String name = Arrays.toString(order);
			assertFalse(name, config.get(OLDSCHOOL_THEME_KEY));
			assertFalse(name, config.get(MATCH_BOSS_COLORS_KEY));
			assertTrue(name, config.get(RARE_GOLD_BARS_KEY));
			assertNull(name, config.values.get(SAVED_RARE_GOLD_BARS_KEY));
		}
	}
}
