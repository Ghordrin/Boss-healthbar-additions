# Changelog

## Unreleased

- Party defence (was Party Defence Tracker defence) now also reads the Better Party Defence plugin's info box. If both plugins are on, Better Party Defence is used
- New Magic defence option (on by default) shows the magic defence from Better Party Defence's Magic defence info box, right after Party defence

## 1.4.0 (2026-10-04)

- New Layout section: pick where each item around the bar goes, in one of three spots above or below it. Items in the same spot line up next to each other. The defaults keep the classic layout, and Preview now shows a sample damage number too
- The damage number now shows the damage of your latest attack, with hits that land together added up, instead of a running total that 4-tick weapons and thralls could keep going for a whole fight
- Damage number counts no longer has Everyone, since other players' hits made the number jump around. It's now Me or Party, and anyone who had Everyone is back on Me. Party keeps a running total of the party's hits while they keep coming
- New Bar ends option in Look. Subtle swaps the end pieces for a small bracket with curled tips that the frame curves into, which leaves more room for the bar. Classic (the default) keeps the current ends. The ends only show with Show icons off, and the option does nothing with Use Oldschool theme on
- Fixed some bosses showing a leftover name from another version of the boss instead of their own
- New Burn away option in Animations (off by default): after "Defeated", the bar burns away from right to left behind a glowing edge instead of fading out. Only used with Defeat animation on
- Settings regrouped into Look, Text, Boss info, Animations, Layout, Custom colors and When to show, with the less used sections folded. A few options were renamed, such as Kill count (was Show kill count), Heal speed and Superior slayer monsters. Your saved settings are kept

## 1.3.0 (2026-10-03)

- Show kill count (off by default) shows your kill count for the boss below the left end of the bar. It reads the counts RuneLite's Chat Commands plugin saves, so that plugin needs to be on
- Show Party Defence Tracker defence (on by default) shows the boss's defence from the Party Defence Tracker plugin's info box below the bar, after the kill count. Nothing shows without that plugin
- Show special attack counts (on by default) shows the counts from RuneLite's Special Attack Counter plugin below the bar, after the Party Defence Tracker defence, with each weapon's icon. Nothing shows unless that plugin is on with its info boxes enabled
- Show elemental weakness (on by default) shows the boss's elemental weakness below the bar, as the element's rune and the extra damage percentage. Values from the OSRS Wiki
- Show defence drain limit (on by default) shows how far the boss's defence can be lowered in total below the bar, or "no drain" when it can't be lowered. Values from the OSRS Wiki
- The bar no longer fades mid-fight when you stop attacking for a moment. "Hide after" now counts from the last hit on the opponent or on you, while the opponent is within 15 tiles
- New Use Oldschool theme checkbox: a plain green bar over red with a thin dark outline, like the game's own health bars. No ornaments, shine or fill texture. While it's on, the Theme dropdown isn't used and Match boss colors and Rare gold bars are turned off, and unticking it turns them back on as you had them. With Use boss icon on, the boss's icon is shown small before its name
- "Replace game's boss health bar" is now a dropdown called "Game's boss health bar", with a new Show both choice that keeps the game's bar and shows this one too. Your old setting carries over
- Show icons (on by default) can be turned off for a plain bar without the icon and crests at the ends, or the icon before the name with Use Oldschool theme on
- Settings reorganised: the Experimental section is gone, its options moved to Appearance and Choose custom icon to Custom colors

## 1.2.0 (2026-10-02)

- More bosses are recognised, mostly raid rooms that were missing, plus NPCs some bosses summon partway through the fight
- Fixed bosses with a coloured name in game not being recognised
- Switching between opponents mid-fight no longer makes the bar disappear and play its intro again. It moves over to the new target, and keeps showing the old one until the new one's health is known
- Show Boss Health Indicators lines (on by default) draws the health lines from the Boss Health Indicators plugin on this bar, since they'd otherwise be hidden along with the game's bar
- Damage number counts lets the damage number add up your RuneLite party's hits or everyone's, not just yours
- Fixed Boss Health Indicators lines not showing, because their colours weren't read correctly

## 1.1.0 (2026-09-30)

- New Experimental section in the config. Choose custom icon moved there, and the new options below live there too
- Use boss icon (on by default) shows the boss's hiscores icon at the bar ends, on any theme. Raid bosses show their pet
- Match boss colors (off by default) gives each boss its own bar colors, on any theme. A boss keeps the same colors in every form
- Rare gold bars (on by default) turns about 1 in 250 bars gold, with a shine and a few sparkles
- A few more bosses are recognised, including other forms of bosses that were already on the list
- Fixed blurry text with fonts other than the RuneScape ones

## 1.0.0 (2026-09-29)

- First release on the Plugin Hub
