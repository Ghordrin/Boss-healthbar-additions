package com.ghordrin.bosshealthbar.theme;

import com.google.common.primitives.Ints;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.Value;

// The Custom colors and icon as one short line of text, to pass around wherever text goes. The colors keep
// the order the Custom colors are listed in, so a color added later goes on the end and older codes still read.
public final class PaletteCode
{
	private static final String PREFIX = "bhb1:";
	// The bar and the four shared text colors, so a pasted code can't leave any of them unset.
	private static final int MIN_COLORS = 9;
	// The background, third in the Custom colors.
	private static final int TRACK_INDEX = 2;
	private static final int NO_ICON = -1;

	@Value
	public static class Palette
	{
		// Transparent where the code leaves a color to follow another one.
		List<Color> colors;
		int iconItemId;
		int iconSpriteId;
	}

	private PaletteCode()
	{
	}

	public static String encode(Collection<Color> colors, int iconItemId, int iconSpriteId)
	{
		final String code = PREFIX + colors.stream().map(PaletteCode::encode).collect(Collectors.joining("."));
		return iconSpriteId >= 0 ? code + ":s" + iconSpriteId : iconItemId > 0 ? code + ":i" + iconItemId : code;
	}

	// A fully transparent color is left empty, and an opaque one drops its alpha, which keeps codes short.
	private static String encode(Color color)
	{
		if (color == null || color.getAlpha() == 0)
		{
			return "";
		}
		final int argb = color.getRGB();
		return color.getAlpha() == 255
			? String.format("%06X", argb & 0xFFFFFF) : String.format("%08X", argb);
	}

	// Null when the text isn't a code. Spaces and line breaks around it, as chat and Discord add, are fine.
	public static Palette decode(String text)
	{
		if (text == null)
		{
			return null;
		}
		final String code = text.trim();
		if (!code.toLowerCase(Locale.ROOT).startsWith(PREFIX))
		{
			return null;
		}

		final String[] parts = code.substring(PREFIX.length()).split(":", -1);
		if (parts.length > 2)
		{
			return null;
		}

		final List<Color> colors = new ArrayList<>();
		for (String part : parts[0].split("\\.", -1))
		{
			final Color color = decodeColor(part);
			if (color == null)
			{
				return null;
			}
			colors.add(color);
		}
		if (colors.size() < MIN_COLORS)
		{
			return null;
		}
		// Among the colors every code carries, only the background may be left transparent.
		for (int i = 0; i < MIN_COLORS; i++)
		{
			if (i != TRACK_INDEX && colors.get(i).getAlpha() == 0)
			{
				return null;
			}
		}

		int itemId = NO_ICON;
		int spriteId = NO_ICON;
		if (parts.length == 2)
		{
			final String icon = parts[1];
			final Integer id = icon.length() > 1 ? Ints.tryParse(icon.substring(1)) : null;
			if (id == null || id < 0)
			{
				return null;
			}
			switch (Character.toLowerCase(icon.charAt(0)))
			{
				case 'i':
					itemId = id;
					break;
				case 's':
					spriteId = id;
					break;
				default:
					return null;
			}
		}
		return new Palette(colors, itemId, spriteId);
	}

	private static Color decodeColor(String part)
	{
		if (part.isEmpty())
		{
			return new Color(0, 0, 0, 0);
		}
		if ((part.length() != 6 && part.length() != 8) || !part.matches("[0-9A-Fa-f]+"))
		{
			return null;
		}
		final long value = Long.parseLong(part, 16);
		return part.length() == 6 ? new Color((int) value) : new Color((int) value, true);
	}
}
