# Changelog

## Unreleased

- Show kill count (off by default) shows your kill count for the boss below the left end of the bar. It reads the counts RuneLite's Chat Commands plugin saves, so that plugin needs to be on
- Show Party Defence Tracker defence (on by default) shows the boss's defence from the Party Defence Tracker plugin's info box below the bar, after the kill count. Nothing shows without that plugin
- Show special attack counts (on by default) shows the counts from RuneLite's Special Attack Counter plugin below the bar, after the Party Defence Tracker defence, with each weapon's icon. Nothing shows unless that plugin is on with its info boxes enabled
- Show elemental weakness (on by default) shows the boss's elemental weakness below the bar, as the element's rune and the extra damage percentage. Values from the OSRS Wiki
- Show defence drain limit (on by default) shows how far the boss's defence can be lowered in total below the bar, or "no drain" when it can't be lowered. Values from the OSRS Wiki

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
