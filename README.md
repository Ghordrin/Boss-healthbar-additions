# Boss Health Bar Additions

A bigger, themed health bar for the boss you're fighting, with a damage trail and a running total of your hits.

See the [changelog](CHANGELOG.md) for what's new in each version.

![The bar on a boss](docs/example-boss.png)

![The bar with a different theme](docs/example-monster.png)

![The Oldschool theme](docs/example-oldschool.png)

## What it does

- Shows a wide bar with the opponent's name, combat level and hitpoints
- When you land a hit, the health you took off stays visible as a lighter trail for a moment before it drains, so you can see how big the hit was
- Adds your recent hits up into a number above the bar. It can also count your RuneLite party's hits, or everyone's
- The fill changes colour as health drops and pulses when it's low
- God themes put that god's icon at both ends of the bar. The custom theme lets you pick your own colours and any item as the icon
- Use Oldschool theme switches to a plain green bar over red with a thin dark outline, closer to the game's own health bars. See [Oldschool theme](#oldschool-theme) for what applies to it
- You can lay one of the game's own textures over the fill, on any theme, but not with Use Oldschool theme on

It only shows what the game already tells you. No timers, attack prediction or anything like that.

## Which opponents get a bar

By default only bosses do. The plugin has a list of commonly fought bosses, and it also picks up anything the game's own boss health bar is showing, plus superior slayer monsters. If a boss is missing, turn on "Also show above combat level", or turn off "Only show for bosses" to get the bar on everything.

If you attack something smaller during a boss fight, like the minions some bosses spawn, the bar stays on the boss.

Switching between opponents that both get a bar, like the NPCs of a boss fought as a group, moves the bar over without playing the intro again. The game only shows an NPC's health once it's been hit, so until then the bar keeps showing the previous one, for up to 3 seconds.

Superior slayer monsters are found by watching for the chat message when one spawns and taking the closest NPC that spawned around the same time. Works fine in practice, but it is a guess.

## Settings

Everything is in the normal RuneLite config screen, or right-click the bar and pick Configure. Preview, at the top, shows a fake opponent that loses and regains health, so you can try settings without fighting anything. A few worth knowing about, by section:

Appearance

- Theme, and Choose fill texture, which opens a picker with the game's own textures to lay over the fill. It's in the bar's right-click menu too
- Use Oldschool theme (off by default) switches to the plain Oldschool bar. The Theme dropdown isn't used while it's on. See [Oldschool theme](#oldschool-theme)
- Bar width and Bar height, in pixels. Fit to game view makes the bar narrower when the game view is small
- Show icons (on by default) shows the icon in a crest at both bar ends, or small before the name with Use Oldschool theme on. Turn it off for a plain bar without the crests
- Use boss icon (on by default) swaps the icon for the boss's hiscores icon, on any theme. Raid bosses show their pet instead. With Use Oldschool theme on, the icon is shown small before the boss's name. Needs Show icons
- Match boss colors (off by default) gives each boss its own bar colors, on any theme. A boss keeps the same colors in every form. Ticking it turns off Use Oldschool theme
- Rare gold bars (on by default) turns about 1 in 250 bars gold, with a shine and a few sparkles. It's rolled once per opponent and is only for looks. Ticking it turns off Use Oldschool theme
- Show damage trail and Heal animation speed, Low health effect and its threshold, Flash on big hits, Intro animation, Defeat animation and Show phase markers

Custom colors

- Choose custom icon picks the item shown at the bar ends on the Custom theme. It's in the bar's right-click menu too. The search opens in your chatbox, so you need to be logged in
- The colors the Custom theme uses

Text

- Font, Show name, Show combat level and Show damage number
- Damage number counts picks whose hits the number above the bar adds up: just yours (the default), your RuneLite party's, or everyone hitting the opponent. Party only counts members who also have this plugin, since each one sends their own hits over the party connection. Everyone needs no party, but includes players outside it
- Hitpoints text shows the hitpoints below the bar as a percentage, a value, or both
- Show kill count (off by default) shows your kill count for the boss below the left end of the bar. It uses the counts RuneLite's Chat Commands plugin saves from the game's kill count messages, so that plugin needs to be on, and a boss shows nothing until you've had a kill count message for it with the plugin running. Raid bosses show nothing, since the count belongs to the whole raid
- Show Party Defence Tracker defence (on by default): if you use the Party Defence Tracker plugin, the defence from its info box is shown below the left end of the bar, after the kill count, with the Defence icon and a red down arrow, in the same colour as the info box. It only shows while that plugin has an info box for the opponent you're fighting, which is after the first defence-lowering hit. Nothing shows if that plugin isn't installed or isn't on
- Show special attack counts (on by default): if RuneLite's Special Attack Counter plugin is on, its counts are shown below the bar, after the defence, as each weapon's icon and its count, in the same colour as the info box. Weapons like the Bandos godsword count damage, not hits. The counts are that plugin's own: they reset when you use a special attack on a different opponent and go away when the opponent dies, so the bar keeps the last counts while it shows "Defeated". Nothing shows unless that plugin is on with its info boxes enabled. If the weapons don't all fit, none of them are shown
- Show elemental weakness (on by default) shows the boss's elemental weakness below the left end of the bar, after the kill count, defence and special attack counts: the element's rune and how much extra damage it takes from that element, for example +40%. Bosses without a weakness show nothing
- Show defence drain limit (on by default) shows how far the boss's defence can be lowered in total, after the weakness: the Defence icon and the limit, for example -20, or "no drain" for a boss whose defence can't be lowered at all. Only bosses with a limit show it. It's a fixed value per boss form, so a limit that changes partway through a fight isn't followed

Behaviour

- Only show for bosses, Also show above combat level, Minimum combat level and Show for superior slayer monsters pick which opponents get a bar (see above)
- Hide after sets how long the bar stays once the fight goes quiet
- Game's boss health bar decides what happens for bosses that show the game's own health bar at the top of the screen. Replace it (the default) hides the game's bar and uses its numbers on this one. Show both keeps the game's bar and shows this one too. Hide this bar keeps the game's bar and leaves this one out of those fights
- Hide Opponent Information bar (on by default) turns off the health bar from RuneLite's Opponent Information plugin so you don't see two. Your own setting comes back when you turn this plugin off
- Show Boss Health Indicators lines (on by default): if you use the Boss Health Indicators plugin, the health lines you set up there are drawn on this bar too, in your colours. Without this they'd be lost when this bar replaces the game's bar

Hold Alt to move the bar, or drag its edge to make it wider or narrower. Alt + right-click and Reset puts it back.

## Oldschool theme

Tick Use Oldschool theme in the Appearance section for a simpler bar in the style of the game's own health bars. While it's on, the Theme dropdown isn't used. It has:

- A flat green bar over red with a thin dark outline, and plain text
- The boss's icon, small before its name, with Show icons and Use boss icon on
- The damage trail, heals, the low health pulse (without the glow), flash on big hits, phase markers and Boss Health Indicators lines
- The intro and defeat animations, and every text and footer option

These don't apply to it:

- Fill texture and the Custom colors section. They stay saved and come back when you untick it. Picking Custom in the Theme dropdown while it's on starts the Custom colours from the theme the dropdown had before
- The end pieces, crest and shine

Match boss colors and Rare gold bars don't fit this bar, so ticking Use Oldschool theme turns them off, and unticking it turns them back on as you had them. If you tick either of them while it's on, Use Oldschool theme turns off instead, and the other one goes back to how you had it. RuneLite's settings panel can't grey out settings, so the plugin changes these checkboxes itself.

## Data

The elemental weaknesses and defence drain limits come from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki) and are built into the plugin, so it doesn't go online for them.

## Contact

Questions or ideas? Reach me on Discord at comrade9932 (Kuringe), or in game at Ultra Cringe.

## License

BSD 2-Clause, see [LICENSE](LICENSE).
