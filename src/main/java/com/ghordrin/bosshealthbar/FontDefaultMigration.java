package com.ghordrin.bosshealthbar;

import java.awt.Font;
import net.runelite.client.config.FontType;

final class FontDefaultMigration
{
	private static final int OLD_DEFAULT_SIZE = 17;

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
}
