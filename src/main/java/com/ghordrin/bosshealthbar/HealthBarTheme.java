package com.ghordrin.bosshealthbar;

import java.awt.Color;
import net.runelite.api.gameval.SpriteID;

public enum HealthBarTheme
{
	// GodWarsIcons has no named constants: _0 is Saradomin, _1 Zamorak, _2 Bandos and _3 Armadyl.
	SARADOMIN("Saradomin", 0x2F62C8, 0x1C3C84, 0xC9A54A, 0xF4F0E0, SpriteID.GodWarsIcons._0),
	ZAMORAK("Zamorak", 0xB01818, 0x6A0C0C, 0x5A4E4E, 0xF0A030, SpriteID.GodWarsIcons._1),
	GUTHIX("Guthix", 0x3E9A3A, 0x28602A, 0x8A7450, 0xE8DDB0),
	ARMADYL("Armadyl", 0xE2DED0, 0xA4A090, 0x6E8E9A, 0x7FD6E6, SpriteID.GodWarsIcons._3),
	BANDOS("Bandos", 0x7A8A3A, 0x5A4A26, 0x7A6448, 0xD0A870, SpriteID.GodWarsIcons._2),
	// Zaros has no God Wars icon, so it borrows an Ancient prayer icon.
	ZAROS("Zaros", 0x6E2AB0, 0x3E1670, 0x5A5068, 0xC080F0, SpriteID.IconPrayerZaros01_30x30.REJUVENATION),
	SEREN("Seren", 0x7ED8E6, 0x3E9CB8, 0xB8D4DA, 0xF2FCFF),
	TUMEKEN("Tumeken", 0xE0A82E, 0xB0701C, 0x3E5E9A, 0xFFF0C0),
	ELIDINIS("Elidinis", 0x2E9EA8, 0x1E6078, 0xB89A5A, 0xC8F0F0),
	RALOS("Ralos", 0xF08A1C, 0xC04818, 0xC09040, 0xFFE890),
	RANUL("Ranul", 0x8898C8, 0x4A5488, 0x9AA0B0, 0xE8ECF8),
	CUSTOM("Custom");

	private final String label;
	private final ThemeColors colors;
	private final Integer godIconSpriteId;

	HealthBarTheme(String label)
	{
		this.label = label;
		this.colors = null;
		this.godIconSpriteId = null;
	}

	HealthBarTheme(String label, int fillHigh, int fillLow, int frame, int trail)
	{
		this(label, fillHigh, fillLow, frame, trail, null);
	}

	HealthBarTheme(String label, int fillHigh, int fillLow, int frame, int trail, Integer godIconSpriteId)
	{
		this.label = label;
		this.godIconSpriteId = godIconSpriteId;
		this.colors = ThemeColors.of(new Color(fillHigh), new Color(fillLow), new Color(frame), new Color(trail));
	}

	ThemeColors getColors()
	{
		return colors;
	}

	Integer getGodIconSpriteId()
	{
		return godIconSpriteId;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
