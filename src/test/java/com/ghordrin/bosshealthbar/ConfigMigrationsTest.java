package com.ghordrin.bosshealthbar;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.config.FontType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ConfigMigrationsTest
{
	private static final FontType OLD_DEFAULT = new FontType().withFamily(Font.SERIF).withSize(17);

	private static ConfigWrites.Write write(String key, Object value)
	{
		return new ConfigWrites.Write(key, value);
	}

	private static Object written(List<ConfigWrites.Write> writes, String key)
	{
		for (ConfigWrites.Write write : writes)
		{
			if (write.getKey().equals(key))
			{
				return write.getValue();
			}
		}
		return null;
	}

	// FontType has no equals, so fonts are compared by what they look like.
	private static List<String> describe(List<ConfigWrites.Write> writes)
	{
		final List<String> described = new ArrayList<>();
		for (ConfigWrites.Write write : writes)
		{
			final Object value = write.getValue();
			if (value instanceof FontType)
			{
				final FontType font = (FontType) value;
				described.add(write.getKey() + "=" + font.getFamily() + "/" + font.getSize() + "/" + font.isBold()
					+ "/" + font.isItalic());
			}
			else
			{
				described.add(write.getKey() + "=" + value);
			}
		}
		return described;
	}

	// Saved settings and the markers of migrations that already ran are stored under these names.
	@Test
	public void keysAreUnchanged()
	{
		assertEquals("bosshealthbar", BossHealthBarConfig.GROUP);
		assertEquals("replaceNativeBossBar", ConfigMigrations.OLD_REPLACE_NATIVE_BOSS_BAR_KEY);
		assertEquals("fontDefaultMigrated", ConfigMigrations.FONT_DEFAULT_MIGRATED_KEY);
		assertEquals("pixelFontSizeMigrated", ConfigMigrations.PIXEL_FONT_SIZE_MIGRATED_KEY);
		assertEquals("itemFontsMigrated", ConfigMigrations.ITEM_FONTS_MIGRATED_KEY);
		assertEquals("fightTimerFontMigrated", ConfigMigrations.FIGHT_TIMER_FONT_MIGRATED_KEY);
		assertEquals("oldschoolSavedMatchBossColors", OldschoolToggle.SAVED_MATCH_BOSS_COLORS_KEY);
		assertEquals("oldschoolSavedRareGoldBars", OldschoolToggle.SAVED_RARE_GOLD_BARS_KEY);
	}

	@Test
	public void oldReplaceCheckboxMovesToTheModeAndIsUnset()
	{
		assertEquals(Arrays.asList(
			write(BossHealthBarConfig.NATIVE_BOSS_BAR_MODE_KEY, NativeBossBarMode.HIDE_OURS),
			write(ConfigMigrations.OLD_REPLACE_NATIVE_BOSS_BAR_KEY, null)),
			ConfigMigrations.replaceNativeBossBarWrites("false", NativeBossBarMode.REPLACE.name()));
		assertEquals(Arrays.asList(
			write(BossHealthBarConfig.NATIVE_BOSS_BAR_MODE_KEY, NativeBossBarMode.REPLACE),
			write(ConfigMigrations.OLD_REPLACE_NATIVE_BOSS_BAR_KEY, null)),
			ConfigMigrations.replaceNativeBossBarWrites("true", null));
	}

	@Test
	public void oldReplaceCheckboxKeepsAModeTheUserChose()
	{
		assertEquals(Collections.singletonList(write(ConfigMigrations.OLD_REPLACE_NATIVE_BOSS_BAR_KEY, null)),
			ConfigMigrations.replaceNativeBossBarWrites("false", NativeBossBarMode.BOTH.name()));
	}

	@Test
	public void defaultFontWritesTheNewDefaultThenTheMarker()
	{
		assertEquals(Arrays.asList(
			write(BossHealthBarConfig.FONT_KEY, FontType.REGULAR),
			write(ConfigMigrations.FONT_DEFAULT_MIGRATED_KEY, true)),
			ConfigMigrations.defaultFontWrites(OLD_DEFAULT));
		assertEquals(Collections.singletonList(write(ConfigMigrations.FONT_DEFAULT_MIGRATED_KEY, true)),
			ConfigMigrations.defaultFontWrites(FontType.BOLD));
		assertEquals(Collections.singletonList(write(ConfigMigrations.FONT_DEFAULT_MIGRATED_KEY, true)),
			ConfigMigrations.defaultFontWrites(null));
	}

	@Test
	public void pixelFontSizeWritesTheNativeSizeThenTheMarker()
	{
		final List<ConfigWrites.Write> writes = ConfigMigrations.pixelFontSizeWrites(FontType.REGULAR.withSize(17));
		assertEquals(2, writes.size());
		assertEquals(BossHealthBarConfig.FONT_KEY, writes.get(0).getKey());
		assertEquals(FontType.REGULAR.getSize(), ((FontType) writes.get(0).getValue()).getSize());
		assertEquals(write(ConfigMigrations.PIXEL_FONT_SIZE_MIGRATED_KEY, true), writes.get(1));

		assertEquals(Collections.singletonList(write(ConfigMigrations.PIXEL_FONT_SIZE_MIGRATED_KEY, true)),
			ConfigMigrations.pixelFontSizeWrites(FontType.REGULAR));
		assertEquals(Collections.singletonList(write(ConfigMigrations.PIXEL_FONT_SIZE_MIGRATED_KEY, true)),
			ConfigMigrations.pixelFontSizeWrites(null));
	}

	@Test
	public void itemFontsWriteEachFontInOrderThenTheMarker()
	{
		final FontType name = new FontType().withFamily(Font.SANS_SERIF).withSize(20);
		final List<ConfigWrites.Write> expected = new ArrayList<>();
		expected.add(write(BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY, name));
		for (String key : ConfigMigrations.SMALL_TEXT_FONT_KEYS)
		{
			expected.add(write(key, ConfigMigrations.smallTextFont(name)));
		}
		expected.add(write(ConfigMigrations.ITEM_FONTS_MIGRATED_KEY, true));
		assertEquals(describe(expected), describe(ConfigMigrations.itemFontWrites(name)));

		assertEquals(Collections.singletonList(write(ConfigMigrations.ITEM_FONTS_MIGRATED_KEY, true)),
			ConfigMigrations.itemFontWrites(null));
	}

	@Test
	public void fightTimerFontCopiesTheKillCountFontThenTheMarker()
	{
		assertEquals(Arrays.asList(
			write(BossHealthBarConfig.FIGHT_TIMER_FONT_KEY, FontType.SMALL),
			write(ConfigMigrations.FIGHT_TIMER_FONT_MIGRATED_KEY, true)),
			ConfigMigrations.fightTimerFontWrites(FontType.SMALL));
		assertEquals(Collections.singletonList(write(ConfigMigrations.FIGHT_TIMER_FONT_MIGRATED_KEY, true)),
			ConfigMigrations.fightTimerFontWrites(null));
	}

	@Test
	public void oldCheckboxMapsToTheMatchingMode()
	{
		assertEquals(NativeBossBarMode.REPLACE, ConfigMigrations.fromReplaceSetting("true"));
		assertEquals(NativeBossBarMode.HIDE_OURS, ConfigMigrations.fromReplaceSetting("false"));
		assertNull(ConfigMigrations.fromReplaceSetting(null));
	}

	@Test
	public void migrationOverridesTheSavedDefaultButNotAnotherChoice()
	{
		assertEquals(NativeBossBarMode.HIDE_OURS, ConfigMigrations.nativeBossBarMode("false", "REPLACE"));
		assertEquals(NativeBossBarMode.HIDE_OURS, ConfigMigrations.nativeBossBarMode("false", null));
		assertEquals(NativeBossBarMode.REPLACE, ConfigMigrations.nativeBossBarMode("true", "REPLACE"));
		assertNull(ConfigMigrations.nativeBossBarMode("false", "BOTH"));
		assertNull(ConfigMigrations.nativeBossBarMode("true", "HIDE_OURS"));
		assertNull(ConfigMigrations.nativeBossBarMode(null, "REPLACE"));
	}

	@Test
	public void oldDefaultFontIsMigrated()
	{
		assertTrue(ConfigMigrations.isOldDefaultFont(OLD_DEFAULT));
	}

	@Test
	public void otherDefaultFontsAreKept()
	{
		assertFalse(ConfigMigrations.isOldDefaultFont(null));
		assertFalse(ConfigMigrations.isOldDefaultFont(OLD_DEFAULT.withSize(18)));
		assertFalse(ConfigMigrations.isOldDefaultFont(OLD_DEFAULT.withBold(true)));
		assertFalse(ConfigMigrations.isOldDefaultFont(OLD_DEFAULT.withItalic(true)));
		assertFalse(ConfigMigrations.isOldDefaultFont(OLD_DEFAULT.withFamily(Font.SANS_SERIF)));
		assertFalse(ConfigMigrations.isOldDefaultFont(FontType.REGULAR));
		assertFalse(ConfigMigrations.isOldDefaultFont(FontType.BOLD));
		assertFalse(ConfigMigrations.isOldDefaultFont(FontType.SMALL));
	}

	@Test
	public void runescapeFontsGetTheirNativeSize()
	{
		final FontType regular = ConfigMigrations.withNativePixelSize(FontType.REGULAR.withSize(17));
		assertEquals(FontType.REGULAR.getFamily(), regular.getFamily());
		assertEquals(16, regular.getSize());

		final FontType small = ConfigMigrations.withNativePixelSize(FontType.SMALL.withSize(17));
		assertEquals(FontType.SMALL.getFamily(), small.getFamily());
		assertEquals(16, small.getSize());

		final FontType bold = ConfigMigrations.withNativePixelSize(
			FontType.REGULAR.withBold(true).withItalic(true).withSize(20));
		assertEquals(16, bold.getSize());
		assertTrue(bold.isBold());
		assertTrue(bold.isItalic());
	}

	@Test
	public void otherFontsKeepTheirSize()
	{
		assertNull(ConfigMigrations.withNativePixelSize(FontType.REGULAR));
		assertNull(ConfigMigrations.withNativePixelSize(FontType.SMALL));
		assertNull(ConfigMigrations.withNativePixelSize(OLD_DEFAULT));
		assertNull(ConfigMigrations.withNativePixelSize(OLD_DEFAULT.withSize(24)));
		assertNull(ConfigMigrations.withNativePixelSize(null));
	}

	@Test
	public void migrationKeepsTheOldSmallText()
	{
		final FontType small = ConfigMigrations.smallTextFont(OLD_DEFAULT);
		assertEquals(Font.SERIF, small.getFamily());
		assertEquals(12, small.getSize());
		assertFalse(small.isBold());
		assertFalse(small.isItalic());

		final FontType styled = ConfigMigrations.smallTextFont(OLD_DEFAULT.withBold(true).withItalic(true).withSize(20));
		assertEquals(14, styled.getSize());
		assertFalse(styled.isBold());
		assertTrue(styled.isItalic());

		assertEquals(8, ConfigMigrations.smallTextFont(OLD_DEFAULT.withSize(8)).getSize());
	}

	@Test
	public void migrationLeavesRunescapeFontsAlone()
	{
		assertNull(ConfigMigrations.smallTextFont(FontType.REGULAR));
		assertNull(ConfigMigrations.smallTextFont(FontType.SMALL));
		assertNull(ConfigMigrations.smallTextFont(FontType.BOLD));
		assertNull(ConfigMigrations.smallTextFont(FontType.REGULAR.withSize(20)));
		assertNull(ConfigMigrations.smallTextFont(null));
	}

	@Test
	public void migrationAlwaysGivesTheDamageNumberTheNameFont()
	{
		final FontType bold = FontType.BOLD.withItalic(true);
		final List<ConfigWrites.Write> pixelWrites = ConfigMigrations.itemFontWrites(bold);
		assertEquals(2, pixelWrites.size());
		assertSame(bold, written(pixelWrites, BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY));
		assertTrue(((FontType) written(pixelWrites, BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY)).isBold());

		final List<ConfigWrites.Write> serifWrites = ConfigMigrations.itemFontWrites(OLD_DEFAULT);
		assertEquals(9, serifWrites.size());
		assertSame(OLD_DEFAULT, written(serifWrites, BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY));
		assertEquals(12, ((FontType) written(serifWrites, BossHealthBarConfig.HITPOINTS_FONT_KEY)).getSize());
	}

	@Test
	public void migrationCoversEverySmallTextFont()
	{
		assertEquals(7, ConfigMigrations.SMALL_TEXT_FONT_KEYS.size());
		assertFalse(ConfigMigrations.SMALL_TEXT_FONT_KEYS.contains(BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY));
	}
}
