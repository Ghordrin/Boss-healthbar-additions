# Supported bosses

These are the bosses the plugin recognises by name. They get their own icon and colours, and they count as bosses for "Only show for bosses". Anything the game's own boss health bar is showing also counts as a boss, so you can still get a bar for opponents that aren't on this list.

The elemental weakness and defence drain limit values come from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki). They are built into the plugin, so they may lag behind game updates. If a value is wrong or a boss is missing, please [open an issue](https://github.com/Ghordrin/Boss-healthbar-additions/issues).

"—" means the plugin has no value for that boss and shows nothing.

## God Wars Dungeon

| Boss | Weakness | Drain limit |
|---|---|---|
| General Graardor | Earth +40% | — |
| K'ril Tsutsaroth | Water +30% | — |
| Commander Zilyana | — | — |
| Kree'arra | Air +30% | — |
| Nex | — | — |

## Wilderness

| Boss | Weakness | Drain limit |
|---|---|---|
| Callisto / Artio | Fire +30% (Callisto only) | — |
| Vet'ion / Calvar'ion | — | — |
| Venenatis / Spindel | Fire +40% (Venenatis), +25% (Spindel) | — |
| Chaos Elemental | Air +50% | — |
| Chaos Fanatic | — | — |
| Crazy archaeologist | — | — |
| Scorpia | Fire +35% | — |
| King Black Dragon | Water +50% | — |

## Slayer

| Boss | Weakness | Drain limit |
|---|---|---|
| Abyssal Sire | — | — |
| Kraken | Earth +50% | — |
| Cerberus | Water +40% | — |
| Thermonuclear smoke devil | Air +20% | — |
| Alchemical Hydra | Earth +50% | — |
| Dusk / Dawn | Earth +40% (Dusk), +70% (Dawn) | — |
| Araxxor | Fire +50% | — |
| Shellbane gryphon | Air +50% | — |

## Desert Treasure II

| Boss | Weakness | Drain limit |
|---|---|---|
| The Leviathan | — | — |
| Vardorvis | Fire +35% | — |
| Duke Sucellus | — | — |
| The Whisperer | Earth +60% | — |

## Chambers of Xeric

| Boss | Weakness | Drain limit |
|---|---|---|
| Great Olm / Great Olm (Left claw) / Great Olm (Right claw) | Earth +50% | — |
| Tekton / Tekton (enraged) | Water +20% | — |
| Vasa Nistirio | — | — |
| Vespula / Abyssal portal | Fire +50% | — |
| Muttadile | Earth +40% | — |
| Vanguard | — | — |
| Ice demon | Fire +150% | — |
| Guardian | — | — |

## Theatre of Blood

| Boss | Weakness | Drain limit |
|---|---|---|
| The Maiden of Sugadinti | — | — |
| Pestilent Bloat | — | — |
| Nylocas Vasilias | Fire +15% | — |
| Sotetseg | — | -100 (-50 in Entry Mode) |
| Xarpus | Air +50% | — |
| Verzik Vitur | Fire +50% (final phase) | no drain |

## Tombs of Amascut

| Boss | Weakness | Drain limit |
|---|---|---|
| Akkha | — | -10 |
| Ba-Ba | — | -20 |
| Kephri | Fire +40% (shielded), +35% (final phase) | -20 |
| Zebak | — | -20 |
| Tumeken's Warden / Elidinis' Warden | Earth +50% | -30 (final phase) [^wardens] |

## Minigames

| Boss | Weakness | Drain limit |
|---|---|---|
| Ahrim the Blighted / Dharok the Wretched / Guthan the Infested / Karil the Tainted / Torag the Corrupted / Verac the Defiled | Air +50% | — |
| TzTok-Jad | Water +40% | — |
| TzKal-Zuk | Water +40% | — |
| Sol Heredit | — | — |
| Crystalline Hunllef | — | — |
| Corrupted Hunllef | — | — |

## Other bosses

| Boss | Weakness | Drain limit |
|---|---|---|
| Zulrah | Fire +50% | — |
| Vorkath | Fire +40% | — |
| Giant Mole | Earth +50% | — |
| Kalphite Queen | Fire +40% | — |
| Dagannoth Rex | Earth +35% | — |
| Dagannoth Prime | Earth +35% | — |
| Dagannoth Supreme | Earth +35% | — |
| Corporeal Beast | Earth +10% | — |
| Sarachnis | Fire +40% | — |
| The Nightmare / Phosani's Nightmare | — | -30 |
| Phantom Muspah | Air +65% | — |
| Skotizo | Water +40% | — |
| Obor | Earth +20% | — |
| Bryophyta | Fire +50% | — |
| Hespori | Fire +100% | — |
| Deranged archaeologist | — | — |
| The Mimic | — | — |
| Scurrius | — | — |
| Amoxliatl | Fire +30% | — |
| The Hueycoatl | Earth +60% | — |
| Blood Moon / Blue Moon / Eclipse Moon | Air +15% | — |
| Branda the Fire Queen / Eldric the Ice King | Water +50% (Branda), Fire +50% (Eldric) | — |
| Yama | Water +50% | -80 |
| Judge of Yama | Water +15% | — |
| Doom of Mokhaiotl / Doom of Mokhaiotl (Shielded) / Doom of Mokhaiotl (Burrowed) | — | -30 |
| Brutus / Demonic Brutus | Earth +25% | — |
| Maggot King | — [^maggot] | — |
| Mad Angel | Earth +15% | — |
| Zalcano | — | — |

[^wardens]: The plugin shows -30 for the Wardens' final phase. After they enrage the real limit is -60, but the enraged Wardens use the same NPC id, so the plugin can't tell the two apart and keeps showing -30.

[^maggot]: The Maggot King's weakness changes with your distance from it, so the plugin doesn't show one.
