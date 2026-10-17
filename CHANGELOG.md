# Changelog

## 2.0.0 (2026-10-16)

### New

- Two new Hitpoints text options: Hitpoints left shows the hitpoints on their own, without the maximum, and Hitpoints left and % adds the percentage after it. Both is now named Hitpoints and %
- New Phase colors option (Look, on by default). Some bosses turn the game's own health bar another color while they shield themselves or change phase, and the bar now follows that color instead of losing it behind our own. A Phase color picker under it shows those phases in a color of your choice instead, left transparent to use the game's
- Right-click the bar and pick Support Discord to open the plugin's Discord in your browser, for help, bug reports and sharing setups
- Picking a theme now fills the Custom colors section with that theme's colors, so it can be used as a starting point. Changing one of those colors while a theme is picked asks whether to switch the theme to Custom, since that is the only theme that uses them
- New delve level after the boss's name, the way the game's own boss bar shows it. On a narrow bar the name shortens to make room for it
- New Background color in Custom colors: the color behind the fill, where the health is missing. Left transparent it keeps the dark background the themes use, so a green fill on a red background is now possible without the Oldschool theme

### Changed

- The fight timer now ends on the time the game reports for the kill. The timer used to keep counting until the boss was gone, which left it a few seconds over the time the game prints in the chat, up to six on a boss that never leaves, such as Duke Sucellus. When the game reports a time, the timer stops there and shows that time, so the bar and the chat agree. Bosses that report nothing are unchanged
- The fight timer follows the game's precise timing setting. With it on, the time is shown to hundredths, the way the game shows its own durations, and with it off it stays at whole seconds
- The bar starts at the top centre of the screen, where the game puts its own boss bar, instead of above the chatbox. A bar you have already moved stays where you put it
- The bar is drawn over the game's interfaces instead of under them, so an interface opened where the bar sits no longer hides it
- With Match boss colors on, the second boss bar now takes the colors of its own boss instead of the first one's, so two bosses at once no longer share one color
- The pillar bars now use the same fill blend and shading as the main bar: the fill moves from the full health color to the low health color as the charge drops, and the outline takes the frame's gradient. They keep a single line for a frame and no end pieces, which at their height would leave no room for the bar
- Show icons is now an Icon placement dropdown: Crests, Crest left, Crest right, Before the name, or No icon. A single crest leaves the other end with its Bar ends ornament, and the bar only gives up room on the side that has one. The icon before the name used to be the result of turning Show icons off while Use boss icon stayed on, which was hard to find. Your setting carries over, and Use boss icon now only picks which icon is used, so the theme's icon can go before the name too

### Fixed

- The Theatre of Blood's own health bar no longer shows up next to this one. It used to appear when you walk into a room, until you reach the boss, and again for a moment after the boss dies. It now stays replaced for the whole room, and comes back on its own if the room's boss is out of reach for more than a few seconds, such as in Sotetseg's maze, where this bar has nothing to follow
- The Theatre of Blood's health bar is no longer read outside the Theatre. The game keeps its last values after you leave, so they could be handed to any NPC near you, which gave a random opponent a bar with a raid's health on it and put a wrong first reading on every boss in the Tombs of Amascut
- The game's own boss health bar no longer flashes back up between kills in an instance, where it returns to the boss's idle form a few seconds before the boss itself does. It now stays replaced for as long as the game keeps it up for that boss, and our bar takes over again as soon as the boss is back
- The bar no longer disappears during a phase where the boss cannot be attacked and leaves the area. It stays for as long as the game's own boss bar keeps showing that boss, and keeps reading the health and the phase color from it
- A mechanic you have to hit while the boss is away no longer takes the bar over, so the bar no longer plays its defeat animation as if the boss died when that mechanic is destroyed. It goes on the second bar instead
- The Zaros theme shows the Zarosian icon instead of a prayer icon, and the Seren theme now has an icon of its own
- The prompt that offers to switch the theme to Custom is offered again after the theme is changed, instead of staying away for the rest of the session once it was declined
- The Custom theme's Background color now reaches under Subtle bar ends, which kept their own dark fill and showed black at both ends of the bar
- The name on the second boss bar uses the Name font instead of the Hitpoints font, so both bars read the same

## 1.7.0 (2026-10-11)

### New

- New Save debug log option (in the new Bug reports section, or right-click the bar) saves the plugin's recent activity to a text file for bug reports
- New Second boss bar option (Boss info, on by default). The smaller bar under the main one now shows any other boss you're fighting at the same time, not only pairs, with its last known health while it's away
- New Layout spots: every item can now go inside the bar, or on a second row below the bottom row, as well as above or below the bar. Text inside the bar suits a taller bar
- New Text edge option (Fonts): shadow, outline or nothing

### Changed

- Bosses fought together count as one fight on the fight timer
- The fight timer now starts once you go for the opponent and are near it, instead of at the first hit on it
- The Oldschool theme can now take the boss's colours with Match boss colors

### Fixed

- A boss that drops to 0 hitpoints and heals back, or is only at 0 for a phase, is no longer shown as defeated
- The bar no longer disappears and replays its intro when one boss hands over to another
- The fight timer keeps running when a boss's health bar fills up again for its next phase
- The bar no longer shows a negative or empty health for a moment when a boss's health reading goes missing
- The fight timer starts over when a defeated boss is back up for the next fight, instead of carrying the previous kill's time over
- The fight timer and bar no longer carry a previous fight over when you leave the area or die and come back
- The fight timer now runs until the boss is gone, so it matches the time the game reports
- The bar and fight timer are kept when a fight moves you to another floor, into or out of an instance, or a long way in one step, as long as the boss is still there
- The fight timer now starts from the moment you go for the opponent even if you were already attacking it before the bar picked it up
- A defeated boss's bar no longer picks up the time of a new fight while it is still shown

## 1.6.0 (2026-10-10)

### New

- New Also show for and Never show for options in When to show: list NPC names or IDs, separated by commas or one per line (the box grows as you add lines), to give them the bar or keep it off them. * matches anything. Never show for wins over everything else, and the game's own boss health bar stays up for those NPCs
- New Fight timer option in Boss info (on by default) shows how long the fight has lasted, from the first hit on the opponent until it's defeated. It goes top centre by default, with its own Layout spot and font
- Walking out of sight of a boss no longer shows "Defeated". When you come back and attack it, or it comes back into view within a minute, the bar carries on with the health it had and the fight timer keeps going, without the intro. The health updates on the next hit, and if the boss healed a lot while you were away, the timer starts over
- When this bar replaces the game's boss health bar, the game's pillar bars are drawn in the same style too, in their own box you can move
- For bosses fought as a pair, a smaller bar under the main one now shows the other one's health

### Changed

- With Show icons off and Use boss icon on, the boss's icon now shows small before its name, as it does on the Oldschool theme with Show icons on
- The font settings and Smooth text moved to their own Fonts section, folded by default. Your saved fonts are kept
- Boss info now lists Party defence and Magic defence right after Kill count
- The Custom colors tooltips now say which text each color applies to

### Fixed

- Fixed the game's pillar health bars being hidden along with its boss health bar, and the game's boss health bar sometimes staying hidden after turning the plugin off
- Fixed a boss that restores its health while stunned showing "Defeated" and stopping the fight timer
- Fixed the fight timer starting over when a boss fought as a pair swaps between its two halves, or when the first of the two dies
- Fixed the bar dropping to 0 for a moment when the game's boss health bar moves to another NPC of the same fight. An NPC with no health reading yet now keeps the health it last had in the fight

## 1.5.0 (2026-10-06)

- Party defence (was Party Defence Tracker defence) now also reads the Better Party Defence plugin's info box. If both plugins are on, Better Party Defence is used
- New Magic defence option (on by default) shows the magic defence from Better Party Defence's Magic defence info box, right after Party defence
- The default font is now the RuneScape font instead of Serif, so the text stays sharp when the game is scaled up. If you were still on the old default (Serif 17), you're switched over once
- New Smooth text option in Text (on by default). Turn it off for sharp text with other fonts when the game is scaled up. The RuneScape fonts are never smoothed
- Each label part has its own font setting under Text: name, damage number, combat level, hitpoints, kill count, party defence, special attack counts, elemental weakness and defence drain limit. Icons next to the text scale with it. If you used a font other than RuneScape, the new settings are set once to match your old look
- The RuneScape fonts now use the size you set, instead of always 16. Your saved size for the name font is reset to 16 once, since it was ignored before
- Superior slayer monsters are now recognised by their NPC ID, also when another player spawned them, so another monster spawning at the same moment no longer gets the bar

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
