package com.ghordrin.bosshealthbar;

import com.google.common.collect.ImmutableMap;
import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.Value;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.util.Text;

final class KnownBosses
{
	@Value
	static class Icon
	{
		boolean item;
		int id;
	}

	@Value
	private static class Boss
	{
		Icon icon;
		ThemeColors colors;
	}

	// Matched by name, since most bosses have a separate NPC ID for each form. Raid bosses have no
	// icon of their own, so they use their pet if they have one. Every form of a boss gets the same colors, so the bar
	// never changes color mid-fight.
	private static final Map<String, Boss> BOSSES = new Builder()
		.sprite(SpriteID.IconBoss25x25.GENERAL_GRAARDOR, HealthBarTheme.BANDOS, "General Graardor")
		.sprite(SpriteID.IconBoss25x25.KRIL_TSUTSAROTH, HealthBarTheme.ZAMORAK, "K'ril Tsutsaroth")
		.sprite(SpriteID.IconBoss25x25.COMMANDER_ZILYANA, HealthBarTheme.SARADOMIN, "Commander Zilyana")
		.sprite(SpriteID.IconBoss25x25.KREEARRA, HealthBarTheme.ARMADYL, "Kree'arra")
		.sprite(SpriteID.IconBoss25x25.NEX, HealthBarTheme.ZAROS, "Nex")
		.sprite(SpriteID.IconBoss25x25.ARTIO_CALLISTO, 0x8A5A34, "Callisto", "Artio")
		.sprite(SpriteID.IconBoss25x25.CALVARION_VETION, 0x7A58A8, "Vet'ion", "Calvar'ion")
		.sprite(SpriteID.IconBoss25x25.SPINDEL_VENENATIS, 0x6E9A2A, "Venenatis", "Spindel")
		.sprite(SpriteID.IconBoss25x25.CHAOS_ELEMENTAL, 0xB0287A, "Chaos Elemental")
		.sprite(SpriteID.IconBoss25x25.CHAOS_FANATIC, 0x8A2E6A, "Chaos Fanatic")
		.sprite(SpriteID.IconBoss25x25.CRAZY_ARCHAEOLOGIST, 0xC07A2E, "Crazy archaeologist")
		.sprite(SpriteID.IconBoss25x25.SCORPIA, 0x8A3A1E, "Scorpia")
		.sprite(SpriteID.IconBoss25x25.KING_BLACK_DRAGON, 0x9A2218, "King Black Dragon")
		.sprite(SpriteID.IconBoss25x25.ABYSSAL_SIRE, 0xA02A4A, "Abyssal Sire")
		.sprite(SpriteID.IconBoss25x25.KRAKEN, 0x2A6E9A, "Kraken")
		.sprite(SpriteID.IconBoss25x25.CERBERUS, 0xD0501E, "Cerberus")
		.sprite(SpriteID.IconBoss25x25.THERMONUCLEAR_SMOKE_DEVIL, 0x6A6A7A, "Thermonuclear smoke devil")
		.sprite(SpriteID.IconBoss25x25.ALCHEMICAL_HYDRA, 0x3A8A7A, "Alchemical Hydra")
		.sprite(SpriteID.IconBoss25x25.GROTESQUE_GUARDIANS, 0x7A7E8A, "Dusk", "Dawn")
		.sprite(SpriteID.IconBoss25x25.ARAXXOR, 0x5A9A2E, "Araxxor")
		.sprite(SpriteID.IconBoss25x25.THE_LEVIATHAN, 0x2A5A9A, "The Leviathan")
		.sprite(SpriteID.IconBoss25x25.VARDORVIS, 0x9A1E2A, "Vardorvis")
		.sprite(SpriteID.IconBoss25x25.DUKE_SUCELLUS, 0x5AA8C8, "Duke Sucellus")
		.sprite(SpriteID.IconBoss25x25.THE_WHISPERER, 0x3A7A8A, "The Whisperer")
		.sprite(SpriteID.IconBoss25x25.ZULRAH, 0x2FA58A, "Zulrah")
		.sprite(SpriteID.IconBoss25x25.VORKATH, 0xD2561E, "Vorkath")
		.sprite(SpriteID.IconBoss25x25.GIANT_MOLE, 0x7A5A3A, "Giant Mole")
		.sprite(SpriteID.IconBoss25x25.KALPHITE_QUEEN, 0x8A9A2E, "Kalphite Queen")
		// All three kings share one color so it says nothing about which style each one attacks with.
		.sprite(SpriteID.IconBoss25x25.DAGANNOTH_REX, 0x3A7A6A, "Dagannoth Rex")
		.sprite(SpriteID.IconBoss25x25.DAGANNOTH_PRIME, 0x3A7A6A, "Dagannoth Prime")
		.sprite(SpriteID.IconBoss25x25.DAGANNOTH_SUPREME, 0x3A7A6A, "Dagannoth Supreme")
		.sprite(SpriteID.IconBoss25x25.CORPOREAL_BEAST, 0x6A8AC0, "Corporeal Beast")
		.sprite(SpriteID.IconBoss25x25.SARACHNIS, 0x9A2A22, "Sarachnis")
		.sprite(SpriteID.IconBoss25x25.NIGHTMARE, 0x5A3A7A, "The Nightmare", "Phosani's Nightmare")
		.sprite(SpriteID.IconBoss25x25.PHANTOM_MUSPAH, 0x4A6AC0, "Phantom Muspah")
		.sprite(SpriteID.IconBoss25x25.SKOTIZO, 0x5A2A6A, "Skotizo")
		.sprite(SpriteID.IconBoss25x25.OBOR, 0x8A6A4A, "Obor")
		.sprite(SpriteID.IconBoss25x25.BRYOPHYTA, 0x4E8A3A, "Bryophyta")
		.sprite(SpriteID.IconBoss25x25.HESPORI, 0x3E9A4A, "Hespori")
		.sprite(SpriteID.IconBoss25x25.DERANGED_ARCHAEOLOGIST, 0x5E8A3A, "Deranged archaeologist")
		.sprite(SpriteID.IconBoss25x25.MIMIC, 0xC8A040, "The Mimic")
		.sprite(SpriteID.IconBoss25x25.SCURRIUS, 0x7A6A5A, "Scurrius")
		.sprite(SpriteID.IconBoss25x25.AMOXLIATL, 0x7AB8E0, "Amoxliatl")
		.sprite(SpriteID.IconBoss25x25.THE_HUEYCOATL, 0x2A9A7A, "The Hueycoatl")
		.sprite(SpriteID.IconBoss25x25.YAMA, 0xC03A1E, "Yama", "Judge of Yama")
		.sprite(SpriteID.IconBoss25x25.ROYAL_TITANS, 0x8A7A62, "Branda the Fire Queen", "Eldric the Ice King")
		.sprite(SpriteID.IconBoss25x25.DOOM_OF_MOKHAIOTL, 0xA0401E, "Doom of Mokhaiotl",
			"Doom of Mokhaiotl (Shielded)", "Doom of Mokhaiotl (Burrowed)")
		.sprite(SpriteID.IconBoss25x25.LUNAR_CHESTS, 0x8AA0D0, "Blood Moon", "Blue Moon", "Eclipse Moon")
		.sprite(SpriteID.IconBoss25x25.BARROWS_CHESTS, 0x5A6A5A, "Ahrim the Blighted", "Dharok the Wretched",
			"Guthan the Infested", "Karil the Tainted", "Torag the Corrupted", "Verac the Defiled")
		.sprite(SpriteID.IconBoss25x25.TZTOK_JAD, 0xE0701E, "TzTok-Jad")
		.sprite(SpriteID.IconBoss25x25.TZKAL_ZUK, 0xC0301A, "TzKal-Zuk")
		.sprite(SpriteID.IconBoss25x25.SOL_HEREDIT, 0xE0A030, "Sol Heredit")
		.sprite(SpriteID.IconBoss25x25.THE_GAUNTLET, HealthBarTheme.SEREN, "Crystalline Hunllef")
		.sprite(SpriteID.IconBoss25x25.THE_CORRUPTED_GAUNTLET, 0xC02A2A, "Corrupted Hunllef")
		.sprite(SpriteID.IconBoss25x25.SHELLBANE_GRYPHON, 0xB08A4A, "Shellbane gryphon")
		.sprite(SpriteID.IconBoss25x25.BRUTUS, 0x8A4A2A, "Brutus", "Demonic Brutus")
		.sprite(SpriteID.IconBoss25x25.MAGGOT_KING, 0x9AA03A, "Maggot King")
		.sprite(SpriteID.IconBoss25x25.MAD_ANGEL, 0x9A2A6A, "Mad Angel")
		.sprite(SpriteID.IconBoss25x25.ZALCANO, 0x9A7A4A, "Zalcano")
		.item(ItemID.OLMPET, 0x5A8A3A, "Great Olm", "Great Olm (Left claw)", "Great Olm (Right claw)")
		.item(ItemID.TEKTONPET, 0xC8641E, "Tekton")
		.item(ItemID.TEKTONENRAGEDPET, 0xC8641E, "Tekton (enraged)")
		.item(ItemID.VASAPET, 0xC04A9A, "Vasa Nistirio")
		.item(ItemID.VESPULAPET, 0xD0A02A, "Vespula", "Abyssal portal")
		.item(ItemID.DOGADILEPET, 0x5A7A3A, "Muttadile")
		.item(ItemID.VANGUARDPET, 0x7A8290, "Vanguard")
		.noIcon(0x5A9AD0, "Ice demon")
		.noIcon(0x8A8278, "Guardian")
		.item(ItemID.MAIDENPET, 0xA01A2A, "The Maiden of Sugadinti")
		.item(ItemID.BLOATPET, 0x7A8A3A, "Pestilent Bloat")
		.item(ItemID.NYLOCASPET, 0x6A5A8A, "Nylocas Vasilias")
		.item(ItemID.SOTETSEGPET, 0xC01E3A, "Sotetseg")
		.item(ItemID.XARPUSPET, 0x5A9A2A, "Xarpus")
		.item(ItemID.VERZIKPET, 0x8E1020, "Verzik Vitur")
		.item(ItemID.WARDENPET_AKKHA, 0x3E6AB0, "Akkha")
		.item(ItemID.WARDENPET_BABA, 0xC08A3A, "Ba-Ba")
		.item(ItemID.WARDENPET_KEPHRI, 0x3A9A7A, "Kephri")
		.item(ItemID.WARDENPET_ZEBAK, 0x4A8A5A, "Zebak")
		.item(ItemID.WARDENPET_TUMEKEN, HealthBarTheme.TUMEKEN, "Tumeken's Warden")
		.item(ItemID.WARDENPET_ELIDINIS, HealthBarTheme.ELIDINIS, "Elidinis' Warden")
		.build();

	private KnownBosses()
	{
	}

	static boolean contains(String name)
	{
		return boss(name) != null;
	}

	static Icon icon(String name)
	{
		final Boss boss = boss(name);
		return boss != null ? boss.getIcon() : null;
	}

	static ThemeColors colors(String name)
	{
		final Boss boss = boss(name);
		return boss != null ? boss.getColors() : null;
	}

	private static Boss boss(String name)
	{
		return name != null ? BOSSES.get(Text.removeTags(name).toLowerCase(Locale.ROOT)) : null;
	}

	private static final class Builder
	{
		private final ImmutableMap.Builder<String, Boss> bosses = ImmutableMap.builder();
		private final Map<Integer, ThemeColors> palettes = new HashMap<>();

		Builder sprite(int spriteId, int color, String... names)
		{
			return add(new Icon(false, spriteId), palette(color), names);
		}

		Builder sprite(int spriteId, HealthBarTheme theme, String... names)
		{
			return add(new Icon(false, spriteId), theme.getColors(), names);
		}

		Builder item(int itemId, int color, String... names)
		{
			return add(new Icon(true, itemId), palette(color), names);
		}

		Builder item(int itemId, HealthBarTheme theme, String... names)
		{
			return add(new Icon(true, itemId), theme.getColors(), names);
		}

		Builder noIcon(int color, String... names)
		{
			return add(null, palette(color), names);
		}

		private ThemeColors palette(int color)
		{
			return palettes.computeIfAbsent(color, rgb -> ThemeColors.fromBase(new Color(rgb)));
		}

		private Builder add(Icon icon, ThemeColors colors, String... names)
		{
			final Boss boss = new Boss(icon, colors);
			for (String name : names)
			{
				bosses.put(name.toLowerCase(Locale.ROOT), boss);
			}
			return this;
		}

		Map<String, Boss> build()
		{
			return bosses.build();
		}
	}
}
