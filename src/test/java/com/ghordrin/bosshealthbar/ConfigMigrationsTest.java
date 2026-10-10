package com.ghordrin.bosshealthbar;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.config.FontType;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ConfigMigrationsTest
{
	private static final FontType OLD_DEFAULT = new FontType().withFamily(Font.SERIF).withSize(17);

	private static ConfigMigrations.Write write(String key, Object value)
	{
		return new ConfigMigrations.Write(key, value);
	}

	// FontType has no equals, so fonts are compared by what they look like.
	private static List<String> describe(List<ConfigMigrations.Write> writes)
	{
		final List<String> described = new ArrayList<>();
		for (ConfigMigrations.Write write : writes)
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
		final List<ConfigMigrations.Write> writes = ConfigMigrations.pixelFontSizeWrites(FontType.REGULAR.withSize(17));
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
		final List<ConfigMigrations.Write> expected = new ArrayList<>();
		FontDefaultMigration.itemFontWrites(name).forEach((key, font) -> expected.add(write(key, font)));
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
}
