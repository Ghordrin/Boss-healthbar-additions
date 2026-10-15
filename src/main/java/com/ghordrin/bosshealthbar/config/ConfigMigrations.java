package com.ghordrin.bosshealthbar.config;

import com.ghordrin.bosshealthbar.config.ConfigWrites.Write;
import com.ghordrin.bosshealthbar.gamebars.NativeBossBarMode;
import com.ghordrin.bosshealthbar.render.BarFonts;
import com.ghordrin.bosshealthbar.render.BarIconPlacement;
import com.google.common.collect.ImmutableList;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.FontType;

@Singleton
public class ConfigMigrations
{
	// The checkbox nativeBossBarMode replaced. Its saved value is moved over on startup.
	static final String OLD_REPLACE_NATIVE_BOSS_BAR_KEY = "replaceNativeBossBar";
	// Remembered what Match boss colors was before the Oldschool theme turned it off. The two now work together,
	// so the key is dropped. Its value isn't restored, which leaves the look unchanged.
	static final String OLD_SAVED_MATCH_BOSS_COLORS_KEY = "oldschoolSavedMatchBossColors";
	// The checkbox iconPlacement replaced. It read as where the icon goes rather than whether it shows, so its
	// saved value is moved over together with Use boss icon, which stays on as the icon's source.
	static final String OLD_SHOW_ICONS_KEY = "showIcons";
	// Set once the old default font has been checked for a switch to the new one. Not a config item, so Reset keeps it.
	static final String FONT_DEFAULT_MIGRATED_KEY = "fontDefaultMigrated";
	// Set once a RuneScape font's saved size has been put back to its native size. Not a config item either.
	static final String PIXEL_FONT_SIZE_MIGRATED_KEY = "pixelFontSizeMigrated";
	// Set once the item fonts have been given the look they had with the name's font. Not a config item either.
	static final String ITEM_FONTS_MIGRATED_KEY = "itemFontsMigrated";
	// Set once the fight timer font has been given the kill count's font. Not a config item either.
	static final String FIGHT_TIMER_FONT_MIGRATED_KEY = "fightTimerFontMigrated";

	private static final int OLD_DEFAULT_FONT_SIZE = 17;
	static final List<String> SMALL_TEXT_FONT_KEYS = ImmutableList.of(
		BossHealthBarConfig.COMBAT_LEVEL_FONT_KEY,
		BossHealthBarConfig.HITPOINTS_FONT_KEY,
		BossHealthBarConfig.KILL_COUNT_FONT_KEY,
		BossHealthBarConfig.PARTY_DEFENCE_FONT_KEY,
		BossHealthBarConfig.SPECIAL_ATTACK_COUNTS_FONT_KEY,
		BossHealthBarConfig.WEAKNESS_FONT_KEY,
		BossHealthBarConfig.DRAIN_CAP_FONT_KEY);

	private final ConfigManager configManager;

	@Inject
	ConfigMigrations(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	public void migrate()
	{
		migrateReplaceNativeBossBar();
		migrateShowIcons();
		apply(savedMatchBossColorsWrites(
			configManager.getConfiguration(BossHealthBarConfig.GROUP, OLD_SAVED_MATCH_BOSS_COLORS_KEY)));
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

	private void migrateShowIcons()
	{
		final String oldSaved = configManager.getConfiguration(BossHealthBarConfig.GROUP, OLD_SHOW_ICONS_KEY);
		if (oldSaved == null)
		{
			return;
		}

		apply(showIconsWrites(oldSaved,
			ConfigWrites.savedBoolean(configManager, BossHealthBarConfig.USE_BOSS_ICON_KEY),
			configManager.getConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.ICON_PLACEMENT_KEY)));
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
		final NativeBossBarMode mode = nativeBossBarMode(oldSaved, savedMode);
		if (mode != null)
		{
			writes.add(new Write(BossHealthBarConfig.NATIVE_BOSS_BAR_MODE_KEY, mode));
		}
		writes.add(new Write(OLD_REPLACE_NATIVE_BOSS_BAR_KEY, null));
		return writes;
	}

	static List<Write> showIconsWrites(String oldSaved, Boolean savedUseBossIcon, String savedPlacement)
	{
		final List<Write> writes = new ArrayList<>();
		final BarIconPlacement placement = iconPlacement(oldSaved, savedUseBossIcon, savedPlacement);
		if (placement != null)
		{
			writes.add(new Write(BossHealthBarConfig.ICON_PLACEMENT_KEY, placement));
			// With no icon shown, the old Use boss icon was never a choice of source, so it goes back to its default.
			if (placement == BarIconPlacement.NONE)
			{
				writes.add(new Write(BossHealthBarConfig.USE_BOSS_ICON_KEY, true));
			}
		}
		writes.add(new Write(OLD_SHOW_ICONS_KEY, null));
		return writes;
	}

	// The old pair: Show icons on put the icon in the crests, and off moved it before the name, where Use boss
	// icon doubled as whether it showed at all. RuneLite saves the default before the plugin starts, so a saved
	// Crests may not be the user's choice.
	static BarIconPlacement iconPlacement(String oldSaved, Boolean savedUseBossIcon, String savedPlacement)
	{
		if (oldSaved == null
			|| (savedPlacement != null && !BarIconPlacement.CRESTS.name().equals(savedPlacement)))
		{
			return null;
		}
		if (Boolean.parseBoolean(oldSaved))
		{
			return BarIconPlacement.CRESTS;
		}
		return savedUseBossIcon == null || savedUseBossIcon ? BarIconPlacement.BEFORE_NAME : BarIconPlacement.NONE;
	}

	static List<Write> savedMatchBossColorsWrites(String saved)
	{
		return saved == null
			? Collections.emptyList()
			: Collections.singletonList(new Write(OLD_SAVED_MATCH_BOSS_COLORS_KEY, null));
	}

	// The old "Replace game's boss health bar" checkbox: on replaced the game's bar, off hid ours.
	static NativeBossBarMode fromReplaceSetting(String saved)
	{
		if (saved == null)
		{
			return null;
		}
		return Boolean.parseBoolean(saved) ? NativeBossBarMode.REPLACE : NativeBossBarMode.HIDE_OURS;
	}

	// RuneLite saves the default before the plugin starts, so a saved REPLACE may not be the user's choice.
	static NativeBossBarMode nativeBossBarMode(String oldSaved, String currentSaved)
	{
		final NativeBossBarMode old = fromReplaceSetting(oldSaved);
		if (old == null || (currentSaved != null && !NativeBossBarMode.REPLACE.name().equals(currentSaved)))
		{
			return null;
		}
		return old;
	}

	static List<Write> defaultFontWrites(FontType saved)
	{
		final List<Write> writes = new ArrayList<>();
		if (isOldDefaultFont(saved))
		{
			writes.add(new Write(BossHealthBarConfig.FONT_KEY, FontType.REGULAR));
		}
		writes.add(new Write(FONT_DEFAULT_MIGRATED_KEY, true));
		return writes;
	}

	static List<Write> pixelFontSizeWrites(FontType saved)
	{
		final List<Write> writes = new ArrayList<>();
		final FontType migrated = withNativePixelSize(saved);
		if (migrated != null)
		{
			writes.add(new Write(BossHealthBarConfig.FONT_KEY, migrated));
		}
		writes.add(new Write(PIXEL_FONT_SIZE_MIGRATED_KEY, true));
		return writes;
	}

	// The item fonts are new, so their saved values are only the defaults and can be overwritten. The damage
	// number always used the name's font, whatever it was. The smaller text only needs a change for other
	// fonts, since the RuneScape defaults already match.
	static List<Write> itemFontWrites(FontType name)
	{
		final List<Write> writes = new ArrayList<>();
		if (name != null)
		{
			writes.add(new Write(BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY, name));
			final FontType small = smallTextFont(name);
			if (small != null)
			{
				for (String key : SMALL_TEXT_FONT_KEYS)
				{
					writes.add(new Write(key, small));
				}
			}
		}
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

	// The default before the RuneScape font. RuneLite saves defaults before the plugin starts, so
	// everyone who never changed the font has this saved.
	static boolean isOldDefaultFont(FontType font)
	{
		return font != null
			&& Font.SERIF.equals(font.getFamily())
			&& font.getSize() == OLD_DEFAULT_FONT_SIZE
			&& !font.isBold()
			&& !font.isItalic();
	}

	// The RuneScape fonts used to ignore the saved size, and the font picker keeps the old size when the
	// family changes, so a saved size other than the native one was never seen. Returns null to keep the font.
	static FontType withNativePixelSize(FontType font)
	{
		if (font == null || font.getFamily() == null)
		{
			return null;
		}
		for (FontType pixel : new FontType[]{FontType.REGULAR, FontType.SMALL})
		{
			if (pixel.getFamily().equals(font.getFamily()))
			{
				return font.getSize() == pixel.getSize() ? null : font.withSize(pixel.getSize());
			}
		}
		return null;
	}

	// Before each item had its own font, other fonts drew the smaller text in the name's family at a smaller
	// size, italic like the name but never bold. Returns null for the RuneScape fonts, whose defaults already match.
	public static FontType smallTextFont(FontType name)
	{
		if (name == null || name.getFamily() == null || BarFonts.isRunescapeFont(name.getFamily()))
		{
			return null;
		}
		return new FontType()
			.withFamily(name.getFamily())
			.withSize(BarFonts.smallTextSize(name.getSize()))
			.withItalic(name.isItalic());
	}
}
