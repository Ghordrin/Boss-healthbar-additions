package com.ghordrin.bosshealthbar;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import lombok.Value;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;

final class KnownBosses
{
	@Value
	static class Icon
	{
		boolean item;
		int id;
	}

	// Matched by name, since most bosses have a separate NPC ID for each form. Raid bosses have no
	// icon of their own, so they use their pet.
	private static final Map<String, Icon> ICONS = new Builder()
		.sprite(SpriteID.IconBoss25x25.GENERAL_GRAARDOR, "General Graardor")
		.sprite(SpriteID.IconBoss25x25.KRIL_TSUTSAROTH, "K'ril Tsutsaroth")
		.sprite(SpriteID.IconBoss25x25.COMMANDER_ZILYANA, "Commander Zilyana")
		.sprite(SpriteID.IconBoss25x25.KREEARRA, "Kree'arra")
		.sprite(SpriteID.IconBoss25x25.NEX, "Nex")
		.sprite(SpriteID.IconBoss25x25.ARTIO_CALLISTO, "Callisto", "Artio")
		.sprite(SpriteID.IconBoss25x25.CALVARION_VETION, "Vet'ion", "Calvar'ion")
		.sprite(SpriteID.IconBoss25x25.SPINDEL_VENENATIS, "Venenatis", "Spindel")
		.sprite(SpriteID.IconBoss25x25.CHAOS_ELEMENTAL, "Chaos Elemental")
		.sprite(SpriteID.IconBoss25x25.CHAOS_FANATIC, "Chaos Fanatic")
		.sprite(SpriteID.IconBoss25x25.CRAZY_ARCHAEOLOGIST, "Crazy archaeologist")
		.sprite(SpriteID.IconBoss25x25.SCORPIA, "Scorpia")
		.sprite(SpriteID.IconBoss25x25.KING_BLACK_DRAGON, "King Black Dragon")
		.sprite(SpriteID.IconBoss25x25.ABYSSAL_SIRE, "Abyssal Sire")
		.sprite(SpriteID.IconBoss25x25.KRAKEN, "Kraken")
		.sprite(SpriteID.IconBoss25x25.CERBERUS, "Cerberus")
		.sprite(SpriteID.IconBoss25x25.THERMONUCLEAR_SMOKE_DEVIL, "Thermonuclear smoke devil")
		.sprite(SpriteID.IconBoss25x25.ALCHEMICAL_HYDRA, "Alchemical Hydra")
		.sprite(SpriteID.IconBoss25x25.GROTESQUE_GUARDIANS, "Dusk", "Dawn")
		.sprite(SpriteID.IconBoss25x25.ARAXXOR, "Araxxor")
		.sprite(SpriteID.IconBoss25x25.THE_LEVIATHAN, "The Leviathan")
		.sprite(SpriteID.IconBoss25x25.VARDORVIS, "Vardorvis")
		.sprite(SpriteID.IconBoss25x25.DUKE_SUCELLUS, "Duke Sucellus")
		.sprite(SpriteID.IconBoss25x25.THE_WHISPERER, "The Whisperer")
		.sprite(SpriteID.IconBoss25x25.ZULRAH, "Zulrah")
		.sprite(SpriteID.IconBoss25x25.VORKATH, "Vorkath")
		.sprite(SpriteID.IconBoss25x25.GIANT_MOLE, "Giant Mole")
		.sprite(SpriteID.IconBoss25x25.KALPHITE_QUEEN, "Kalphite Queen")
		.sprite(SpriteID.IconBoss25x25.DAGANNOTH_REX, "Dagannoth Rex")
		.sprite(SpriteID.IconBoss25x25.DAGANNOTH_PRIME, "Dagannoth Prime")
		.sprite(SpriteID.IconBoss25x25.DAGANNOTH_SUPREME, "Dagannoth Supreme")
		.sprite(SpriteID.IconBoss25x25.CORPOREAL_BEAST, "Corporeal Beast")
		.sprite(SpriteID.IconBoss25x25.SARACHNIS, "Sarachnis")
		.sprite(SpriteID.IconBoss25x25.NIGHTMARE, "The Nightmare", "Phosani's Nightmare")
		.sprite(SpriteID.IconBoss25x25.PHANTOM_MUSPAH, "Phantom Muspah")
		.sprite(SpriteID.IconBoss25x25.SKOTIZO, "Skotizo")
		.sprite(SpriteID.IconBoss25x25.OBOR, "Obor")
		.sprite(SpriteID.IconBoss25x25.BRYOPHYTA, "Bryophyta")
		.sprite(SpriteID.IconBoss25x25.HESPORI, "Hespori")
		.sprite(SpriteID.IconBoss25x25.DERANGED_ARCHAEOLOGIST, "Deranged archaeologist")
		.sprite(SpriteID.IconBoss25x25.MIMIC, "The Mimic")
		.sprite(SpriteID.IconBoss25x25.SCURRIUS, "Scurrius")
		.sprite(SpriteID.IconBoss25x25.AMOXLIATL, "Amoxliatl")
		.sprite(SpriteID.IconBoss25x25.THE_HUEYCOATL, "The Hueycoatl")
		.sprite(SpriteID.IconBoss25x25.YAMA, "Yama")
		.sprite(SpriteID.IconBoss25x25.ROYAL_TITANS, "Branda the Fire Queen", "Eldric the Ice King")
		.sprite(SpriteID.IconBoss25x25.DOOM_OF_MOKHAIOTL, "Doom of Mokhaiotl",
			"Doom of Mokhaiotl (Shielded)", "Doom of Mokhaiotl (Burrowed)")
		.sprite(SpriteID.IconBoss25x25.LUNAR_CHESTS, "Blood Moon", "Blue Moon", "Eclipse Moon")
		.sprite(SpriteID.IconBoss25x25.BARROWS_CHESTS, "Ahrim the Blighted", "Dharok the Wretched",
			"Guthan the Infested", "Karil the Tainted", "Torag the Corrupted", "Verac the Defiled")
		.sprite(SpriteID.IconBoss25x25.TZTOK_JAD, "TzTok-Jad")
		.sprite(SpriteID.IconBoss25x25.TZKAL_ZUK, "TzKal-Zuk")
		.sprite(SpriteID.IconBoss25x25.SOL_HEREDIT, "Sol Heredit")
		.sprite(SpriteID.IconBoss25x25.THE_GAUNTLET, "Crystalline Hunllef")
		.sprite(SpriteID.IconBoss25x25.THE_CORRUPTED_GAUNTLET, "Corrupted Hunllef")
		.sprite(SpriteID.IconBoss25x25.SHELLBANE_GRYPHON, "Shellbane gryphon")
		.sprite(SpriteID.IconBoss25x25.BRUTUS, "Brutus", "Demonic Brutus")
		.sprite(SpriteID.IconBoss25x25.MAGGOT_KING, "Maggot King")
		.sprite(SpriteID.IconBoss25x25.MAD_ANGEL, "Mad Angel")
		.item(ItemID.OLMPET, "Great Olm", "Great Olm (Left claw)", "Great Olm (Right claw)")
		.item(ItemID.TEKTONPET, "Tekton")
		.item(ItemID.TEKTONENRAGEDPET, "Tekton (enraged)")
		.item(ItemID.VASAPET, "Vasa Nistirio")
		.item(ItemID.VESPULAPET, "Vespula")
		.item(ItemID.DOGADILEPET, "Muttadile")
		.item(ItemID.VANGUARDPET, "Vanguard")
		.item(ItemID.MAIDENPET, "The Maiden of Sugadinti")
		.item(ItemID.BLOATPET, "Pestilent Bloat")
		.item(ItemID.NYLOCASPET, "Nylocas Vasilias")
		.item(ItemID.SOTETSEGPET, "Sotetseg")
		.item(ItemID.XARPUSPET, "Xarpus")
		.item(ItemID.VERZIKPET, "Verzik Vitur")
		.item(ItemID.WARDENPET_AKKHA, "Akkha")
		.item(ItemID.WARDENPET_BABA, "Ba-Ba")
		.item(ItemID.WARDENPET_KEPHRI, "Kephri")
		.item(ItemID.WARDENPET_ZEBAK, "Zebak")
		.item(ItemID.WARDENPET_TUMEKEN, "Tumeken's Warden")
		.item(ItemID.WARDENPET_ELIDINIS, "Elidinis' Warden")
		.build();

	private KnownBosses()
	{
	}

	static boolean contains(String name)
	{
		return icon(name) != null;
	}

	static Icon icon(String name)
	{
		return name != null ? ICONS.get(name.toLowerCase(Locale.ROOT)) : null;
	}

	private static final class Builder
	{
		private final ImmutableMap.Builder<String, Icon> icons = ImmutableMap.builder();

		Builder sprite(int spriteId, String... names)
		{
			return add(new Icon(false, spriteId), names);
		}

		Builder item(int itemId, String... names)
		{
			return add(new Icon(true, itemId), names);
		}

		private Builder add(Icon icon, String... names)
		{
			for (String name : names)
			{
				icons.put(name.toLowerCase(Locale.ROOT), icon);
			}
			return this;
		}

		Map<String, Icon> build()
		{
			return icons.build();
		}
	}
}
