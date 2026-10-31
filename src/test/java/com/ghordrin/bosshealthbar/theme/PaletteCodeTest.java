package com.ghordrin.bosshealthbar.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PaletteCodeTest
{
	private static final Color NONE = BossHealthBarConfig.TRANSPARENT;

	private static List<Color> nine()
	{
		return new ArrayList<>(Arrays.asList(new Color(0x96161A), new Color(0x6A0C0C), NONE, new Color(0xD6B254),
			new Color(0x7A6C54), new Color(0xE8E2D4), new Color(0xA99F90), new Color(0xC8C0B0), new Color(0xB6AEA1)));
	}

	@Test
	public void aCodeReadsBackAsTheSameColorsAndIcon()
	{
		final List<Color> colors = nine();
		colors.add(new Color(0x80FF0000, true));
		colors.add(NONE);
		final String code = PaletteCode.encode(colors, -1, 1234);
		assertEquals("bhb1:96161A.6A0C0C..D6B254.7A6C54.E8E2D4.A99F90.C8C0B0.B6AEA1.80FF0000.:s1234", code);

		final PaletteCode.Palette palette = PaletteCode.decode(code);
		assertNotNull(palette);
		assertEquals(colors, palette.getColors());
		assertEquals(-1, palette.getIconItemId());
		assertEquals(1234, palette.getIconSpriteId());
	}

	@Test
	public void anItemIconAndNoIconBothTravel()
	{
		assertEquals(4151, PaletteCode.decode(PaletteCode.encode(nine(), 4151, -1)).getIconItemId());
		final PaletteCode.Palette none = PaletteCode.decode(PaletteCode.encode(nine(), -1, -1));
		assertEquals(-1, none.getIconItemId());
		assertEquals(-1, none.getIconSpriteId());
	}

	@Test
	public void aCodePastedFromAChatStillReads()
	{
		final String code = PaletteCode.encode(nine(), -1, -1);
		assertNotNull(PaletteCode.decode("  " + code.toUpperCase() + "\n"));
		assertNotNull(PaletteCode.decode(code.toLowerCase()));
	}

	@Test
	public void anythingElseIsNotACode()
	{
		assertNull(PaletteCode.decode(null));
		assertNull(PaletteCode.decode("{\"settings\":{}}"));
		assertNull(PaletteCode.decode("bhb1:96161A"));
		assertNull(PaletteCode.decode(PaletteCode.encode(nine(), -1, -1).replace("96161A", "96161")));
		assertNull(PaletteCode.decode(PaletteCode.encode(nine(), -1, -1).replace("96161A", "ZZZZZZ")));
		assertNull(PaletteCode.decode(PaletteCode.encode(nine(), -1, -1) + ":x12"));
		assertNull(PaletteCode.decode(PaletteCode.encode(nine(), -1, -1) + ":i12:s3"));
	}

	// A fill or text color left out would make that part of the bar vanish, so only the background may be.
	@Test
	public void onlyTheBackgroundMayBeLeftEmpty()
	{
		final List<Color> colors = nine();
		colors.set(0, NONE);
		assertNull(PaletteCode.decode(PaletteCode.encode(colors, -1, -1)));
	}

	@Test
	public void theCodeKeepsTheCustomColorsInTheirListedOrder()
	{
		final BossHealthBarConfig defaults = new BossHealthBarConfig()
		{
		};
		assertEquals(new ArrayList<>(CustomColors.keyedColors(ThemeColors.builder().build()).keySet()),
			new ArrayList<>(CustomColors.configuredColors(defaults).keySet()));
		assertNotNull(PaletteCode.decode(CustomColors.code(defaults)));
	}
}
