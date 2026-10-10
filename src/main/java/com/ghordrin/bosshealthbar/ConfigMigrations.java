package com.ghordrin.bosshealthbar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.FontType;

@Singleton
class ConfigMigrations
{
	// The checkbox nativeBossBarMode replaced. Its saved value is moved over on startup.
	static final String OLD_REPLACE_NATIVE_BOSS_BAR_KEY = "replaceNativeBossBar";
	// Set once the old default font has been checked for a switch to the new one. Not a config item, so Reset keeps it.
	static final String FONT_DEFAULT_MIGRATED_KEY = "fontDefaultMigrated";
	// Set once a RuneScape font's saved size has been put back to its native size. Not a config item either.
	static final String PIXEL_FONT_SIZE_MIGRATED_KEY = "pixelFontSizeMigrated";
	// Set once the item fonts have been given the look they had with the name's font. Not a config item either.
	static final String ITEM_FONTS_MIGRATED_KEY = "itemFontsMigrated";
	// Set once the fight timer font has been given the kill count's font. Not a config item either.
	static final String FIGHT_TIMER_FONT_MIGRATED_KEY = "fightTimerFontMigrated";

	// A null value means unset the key.
	@Value
	static class Write
	{
		String key;
		Object value;
	}

	private final ConfigManager configManager;

	@Inject
	ConfigMigrations(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	void migrate()
	{
		migrateReplaceNativeBossBar();
		migrateFont(FONT_DEFAULT_MIGRATED_KEY, BossHealthBarConfig.FONT_KEY, ConfigMigrations::defaultFontWrites);
		migrateFont(PIXEL_FONT_SIZE_MIGRATED_KEY, BossHealthBarConfig.FONT_KEY, ConfigMigrations::pixelFontSizeWrites);
		migrateFont(ITEM_FONTS_MIGRATED_KEY, BossHealthBarConfig.FONT_KEY, ConfigMigrations::itemFontWrites);
		migrateFont(FIGHT_TIMER_FONT_MIGRATED_KEY, BossHealthBarConfig.KILL_COUNT_FONT_KEY,
			ConfigMigrations::fightTimerFontWrites);
	}

	private void migrateReplaceNativeBossBar()
	{
		final String oldSaved = configManager.getConfiguration(BossHealthBarConfig.GROUP, OLD_REPLACE_NATIVE_BOSS_BAR_KEY);
		if (oldSaved == null)
		{
			return;
		}

		apply(replaceNativeBossBarWrites(oldSaved,
			configManager.getConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.NATIVE_BOSS_BAR_MODE_KEY)));
	}

	private void migrateFont(String migratedKey, String fontKey, Function<FontType, List<Write>> writes)
	{
		if (ConfigWrites.savedBoolean(configManager, migratedKey) != null)
		{
			return;
		}

		apply(writes.apply(configManager.getConfiguration(BossHealthBarConfig.GROUP, fontKey, FontType.class)));
	}

	private void apply(List<Write> writes)
	{
		for (Write write : writes)
		{
			ConfigWrites.write(configManager, write.getKey(), write.getValue());
		}
	}

	static List<Write> replaceNativeBossBarWrites(String oldSaved, String savedMode)
	{
		final List<Write> writes = new ArrayList<>();
		final NativeBossBarMode mode = NativeBossBarMode.migrate(oldSaved, savedMode);
		if (mode != null)
		{
			writes.add(new Write(BossHealthBarConfig.NATIVE_BOSS_BAR_MODE_KEY, mode));
		}
		writes.add(new Write(OLD_REPLACE_NATIVE_BOSS_BAR_KEY, null));
		return writes;
	}

	static List<Write> defaultFontWrites(FontType saved)
	{
		final List<Write> writes = new ArrayList<>();
		if (FontDefaultMigration.isOldDefault(saved))
		{
			writes.add(new Write(BossHealthBarConfig.FONT_KEY, FontType.REGULAR));
		}
		writes.add(new Write(FONT_DEFAULT_MIGRATED_KEY, true));
		return writes;
	}

	static List<Write> pixelFontSizeWrites(FontType saved)
	{
		final List<Write> writes = new ArrayList<>();
		final FontType migrated = FontDefaultMigration.withNativePixelSize(saved);
		if (migrated != null)
		{
			writes.add(new Write(BossHealthBarConfig.FONT_KEY, migrated));
		}
		writes.add(new Write(PIXEL_FONT_SIZE_MIGRATED_KEY, true));
		return writes;
	}

	// The item fonts are new, so their saved values are only the defaults and can be overwritten.
	static List<Write> itemFontWrites(FontType name)
	{
		final List<Write> writes = new ArrayList<>();
		FontDefaultMigration.itemFontWrites(name).forEach((key, font) -> writes.add(new Write(key, font)));
		writes.add(new Write(ITEM_FONTS_MIGRATED_KEY, true));
		return writes;
	}

	// Newer than the item fonts, so it gets its own one-time copy of the kill count font, which that migration set.
	static List<Write> fightTimerFontWrites(FontType killCount)
	{
		final List<Write> writes = new ArrayList<>();
		if (killCount != null)
		{
			writes.add(new Write(BossHealthBarConfig.FIGHT_TIMER_FONT_KEY, killCount));
		}
		writes.add(new Write(FIGHT_TIMER_FONT_MIGRATED_KEY, true));
		return writes;
	}
}
