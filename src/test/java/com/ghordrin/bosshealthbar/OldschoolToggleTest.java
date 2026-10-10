package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BossHealthBarConfig.MATCH_BOSS_COLORS_KEY;
import static com.ghordrin.bosshealthbar.BossHealthBarConfig.OLDSCHOOL_THEME_KEY;
import static com.ghordrin.bosshealthbar.BossHealthBarConfig.RARE_GOLD_BARS_KEY;
import static com.ghordrin.bosshealthbar.OldschoolToggle.SAVED_MATCH_BOSS_COLORS_KEY;
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
			for (OldschoolToggle.Write write : OldschoolToggle.writes(key, orDefault(key, old), orDefault(key, value), settings()))
			{
				set(write.getKey(), write.getValue());
			}
		}

		OldschoolToggle.Settings settings()
		{
			return new OldschoolToggle.Settings(get(OLDSCHOOL_THEME_KEY), get(MATCH_BOSS_COLORS_KEY),
				get(RARE_GOLD_BARS_KEY), values.get(SAVED_MATCH_BOSS_COLORS_KEY), values.get(SAVED_RARE_GOLD_BARS_KEY));
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
	public void tickingOnRemembersAndTurnsOffTheOtherBoxes()
	{
		final List<OldschoolToggle.Write> writes = OldschoolToggle.writes(OLDSCHOOL_THEME_KEY, false, true,
			new OldschoolToggle.Settings(true, true, false, null, null));
		assertEquals(Arrays.asList(
			new OldschoolToggle.Write(SAVED_MATCH_BOSS_COLORS_KEY, true),
			new OldschoolToggle.Write(SAVED_RARE_GOLD_BARS_KEY, false),
			new OldschoolToggle.Write(MATCH_BOSS_COLORS_KEY, false),
			new OldschoolToggle.Write(RARE_GOLD_BARS_KEY, false)), writes);
	}

	@Test
	public void tickingOffClearsMemoryThenRestores()
	{
		final List<OldschoolToggle.Write> writes = OldschoolToggle.writes(OLDSCHOOL_THEME_KEY, true, false,
			new OldschoolToggle.Settings(false, false, false, true, false));
		assertEquals(Arrays.asList(
			new OldschoolToggle.Write(SAVED_MATCH_BOSS_COLORS_KEY, null),
			new OldschoolToggle.Write(SAVED_RARE_GOLD_BARS_KEY, null),
			new OldschoolToggle.Write(MATCH_BOSS_COLORS_KEY, true),
			new OldschoolToggle.Write(RARE_GOLD_BARS_KEY, false)), writes);
	}

	@Test
	public void tickingAConflictTurnsOldschoolOffAndRestoresOnlyTheOther()
	{
		final List<OldschoolToggle.Write> writes = OldschoolToggle.writes(MATCH_BOSS_COLORS_KEY, false, true,
			new OldschoolToggle.Settings(true, true, false, false, true));
		assertEquals(Arrays.asList(
			new OldschoolToggle.Write(SAVED_MATCH_BOSS_COLORS_KEY, null),
			new OldschoolToggle.Write(SAVED_RARE_GOLD_BARS_KEY, null),
			new OldschoolToggle.Write(RARE_GOLD_BARS_KEY, true),
			new OldschoolToggle.Write(OLDSCHOOL_THEME_KEY, false)), writes);
	}

	@Test
	public void nothingToDoWhenTheValueDidntChange()
	{
		final OldschoolToggle.Settings settings = new OldschoolToggle.Settings(true, false, false, true, true);
		assertTrue(OldschoolToggle.writes(OLDSCHOOL_THEME_KEY, true, true, settings).isEmpty());
		assertTrue(OldschoolToggle.writes(MATCH_BOSS_COLORS_KEY, false, false, settings).isEmpty());
	}

	@Test
	public void conflictBoxesOnlyMatterWhileOldschoolIsOn()
	{
		final OldschoolToggle.Settings off = new OldschoolToggle.Settings(false, true, true, null, null);
		assertTrue(OldschoolToggle.writes(MATCH_BOSS_COLORS_KEY, false, true, off).isEmpty());
		assertTrue(OldschoolToggle.writes(RARE_GOLD_BARS_KEY, false, true, off).isEmpty());

		final OldschoolToggle.Settings on = new OldschoolToggle.Settings(true, false, false, true, true);
		assertTrue(OldschoolToggle.writes(RARE_GOLD_BARS_KEY, true, false, on).isEmpty());
		assertTrue(OldschoolToggle.writes("barWidth", false, true, on).isEmpty());
	}

	@Test
	public void tickingOnKeepsWhatIsAlreadyRemembered()
	{
		final List<OldschoolToggle.Write> writes = OldschoolToggle.writes(OLDSCHOOL_THEME_KEY, false, true,
			new OldschoolToggle.Settings(true, false, false, true, true));
		assertEquals(Arrays.asList(
			new OldschoolToggle.Write(MATCH_BOSS_COLORS_KEY, false),
			new OldschoolToggle.Write(RARE_GOLD_BARS_KEY, false)), writes);
	}

	@Test
	public void repeatedTogglingRoundTrips()
	{
		final FakeConfig config = new FakeConfig(false, true, false);
		for (int i = 0; i < 5; i++)
		{
			config.set(OLDSCHOOL_THEME_KEY, true);
			assertFalse(config.get(MATCH_BOSS_COLORS_KEY));
			assertFalse(config.get(RARE_GOLD_BARS_KEY));

			config.set(OLDSCHOOL_THEME_KEY, false);
			assertTrue(config.get(MATCH_BOSS_COLORS_KEY));
			assertFalse(config.get(RARE_GOLD_BARS_KEY));
			assertNull(config.values.get(SAVED_MATCH_BOSS_COLORS_KEY));
			assertNull(config.values.get(SAVED_RARE_GOLD_BARS_KEY));
		}
	}

	@Test
	public void lastClickWinsWithoutLooping()
	{
		final FakeConfig config = new FakeConfig(false, false, true);
		config.set(OLDSCHOOL_THEME_KEY, true);
		assertFalse(config.get(RARE_GOLD_BARS_KEY));

		config.set(MATCH_BOSS_COLORS_KEY, true);
		assertFalse(config.get(OLDSCHOOL_THEME_KEY));
		assertTrue(config.get(MATCH_BOSS_COLORS_KEY));
		assertTrue(config.get(RARE_GOLD_BARS_KEY));
		assertNull(config.values.get(SAVED_MATCH_BOSS_COLORS_KEY));
		assertNull(config.values.get(SAVED_RARE_GOLD_BARS_KEY));

		config.set(OLDSCHOOL_THEME_KEY, true);
		config.set(RARE_GOLD_BARS_KEY, true);
		assertFalse(config.get(OLDSCHOOL_THEME_KEY));
		assertTrue(config.get(MATCH_BOSS_COLORS_KEY));
		assertTrue(config.get(RARE_GOLD_BARS_KEY));
	}

	@Test
	public void tickingRareGoldBarsTurnsOldschoolOffAndRestoresMatchBossColors()
	{
		final List<OldschoolToggle.Write> writes = OldschoolToggle.writes(RARE_GOLD_BARS_KEY, false, true,
			new OldschoolToggle.Settings(true, false, true, true, false));
		assertEquals(Arrays.asList(
			new OldschoolToggle.Write(SAVED_MATCH_BOSS_COLORS_KEY, null),
			new OldschoolToggle.Write(SAVED_RARE_GOLD_BARS_KEY, null),
			new OldschoolToggle.Write(MATCH_BOSS_COLORS_KEY, true),
			new OldschoolToggle.Write(OLDSCHOOL_THEME_KEY, false)), writes);
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
			final FakeConfig config = new FakeConfig(false, true, false);
			config.set(OLDSCHOOL_THEME_KEY, true);

			// RuneLite's Reset puts each setting back to its default, then the plugin's reset writes run.
			for (String key : order)
			{
				config.set(key, RARE_GOLD_BARS_KEY.equals(key));
			}
			for (OldschoolToggle.Write write : OldschoolToggle.resetWrites())
			{
				config.set(write.getKey(), write.getValue());
			}

			final String name = Arrays.toString(order);
			assertFalse(name, config.get(OLDSCHOOL_THEME_KEY));
			assertFalse(name, config.get(MATCH_BOSS_COLORS_KEY));
			assertTrue(name, config.get(RARE_GOLD_BARS_KEY));
			assertNull(name, config.values.get(SAVED_MATCH_BOSS_COLORS_KEY));
			assertNull(name, config.values.get(SAVED_RARE_GOLD_BARS_KEY));
		}
	}
}
