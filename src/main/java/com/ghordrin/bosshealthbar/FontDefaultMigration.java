package com.ghordrin.bosshealthbar;

import com.google.common.collect.ImmutableList;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.config.FontType;

final class FontDefaultMigration
{
	private static final int OLD_DEFAULT_SIZE = 17;
	static final List<String> SMALL_TEXT_FONT_KEYS = ImmutableList.of(
		BossHealthBarConfig.COMBAT_LEVEL_FONT_KEY,
		BossHealthBarConfig.HITPOINTS_FONT_KEY,
		BossHealthBarConfig.KILL_COUNT_FONT_KEY,
		BossHealthBarConfig.PARTY_DEFENCE_FONT_KEY,
		BossHealthBarConfig.SPECIAL_ATTACK_COUNTS_FONT_KEY,
		BossHealthBarConfig.WEAKNESS_FONT_KEY,
		BossHealthBarConfig.DRAIN_CAP_FONT_KEY);

	private FontDefaultMigration()
	{
	}

	// The default before the RuneScape font. RuneLite saves defaults before the plugin starts, so
	// everyone who never changed the font has this saved.
	static boolean isOldDefault(FontType font)
	{
		return font != null
			&& Font.SERIF.equals(font.getFamily())
			&& font.getSize() == OLD_DEFAULT_SIZE
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

	// The damage number always used the name's font, whatever it was. The smaller text only needs a change
	// for other fonts, since the RuneScape defaults already match.
	static Map<String, FontType> itemFontWrites(FontType name)
	{
		final Map<String, FontType> writes = new LinkedHashMap<>();
		if (name == null)
		{
			return writes;
		}
		writes.put(BossHealthBarConfig.DAMAGE_NUMBER_FONT_KEY, name);
		final FontType small = smallTextFont(name);
		if (small != null)
		{
			for (String key : SMALL_TEXT_FONT_KEYS)
			{
				writes.put(key, small);
			}
		}
		return writes;
	}

	// Before each item had its own font, other fonts drew the smaller text in the name's family at a smaller
	// size, italic like the name but never bold. Returns null for the RuneScape fonts, whose defaults already match.
	static FontType smallTextFont(FontType name)
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
