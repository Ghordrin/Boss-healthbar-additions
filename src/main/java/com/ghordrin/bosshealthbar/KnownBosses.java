package com.ghordrin.bosshealthbar;

import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Set;

final class KnownBosses
{
	// Matched by name, since most bosses have a separate NPC ID for each form.
	private static final Set<String> NAMES = lowerCase(
		"General Graardor", "K'ril Tsutsaroth", "Commander Zilyana", "Kree'arra", "Nex",
		"Callisto", "Artio", "Vet'ion", "Calvar'ion", "Venenatis", "Spindel", "Chaos Elemental",
		"Chaos Fanatic", "Crazy archaeologist", "Scorpia", "King Black Dragon",
		"Abyssal Sire", "Kraken", "Cerberus", "Thermonuclear smoke devil", "Alchemical Hydra", "Dusk",
		"Dawn", "Araxxor",
		"The Leviathan", "Vardorvis", "Duke Sucellus", "The Whisperer",
		"Zulrah", "Vorkath", "Giant Mole", "Kalphite Queen", "Dagannoth Rex", "Dagannoth Prime",
		"Dagannoth Supreme", "Corporeal Beast", "Sarachnis", "The Nightmare", "Phosani's Nightmare",
		"Phantom Muspah", "Skotizo", "Obor", "Bryophyta", "Hespori", "Deranged archaeologist",
		"The Mimic", "Scurrius", "Amoxliatl", "The Hueycoatl", "Yama", "Branda the Fire Queen",
		"Eldric the Ice King", "Doom of Mokhaiotl", "Blood Moon", "Blue Moon", "Eclipse Moon",
		"Ahrim the Blighted", "Dharok the Wretched", "Guthan the Infested", "Karil the Tainted",
		"Torag the Corrupted", "Verac the Defiled",
		"TzTok-Jad", "TzKal-Zuk", "Sol Heredit", "Crystalline Hunllef", "Corrupted Hunllef",
		"Great Olm", "Tekton", "Vasa Nistirio", "Vespula", "Muttadile",
		"The Maiden of Sugadinti", "Pestilent Bloat", "Nylocas Vasilias", "Sotetseg", "Xarpus",
		"Verzik Vitur",
		"Akkha", "Ba-Ba", "Kephri", "Zebak", "Tumeken's Warden", "Elidinis' Warden");

	private KnownBosses()
	{
	}

	static boolean contains(String name)
	{
		return name != null && NAMES.contains(name.toLowerCase(Locale.ROOT));
	}

	private static Set<String> lowerCase(String... names)
	{
		final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
		for (String name : names)
		{
			builder.add(name.toLowerCase(Locale.ROOT));
		}
		return builder.build();
	}
}
