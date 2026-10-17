# Boss Health Bar Additions

A bigger, themed health bar for the boss you're fighting, with a damage trail, your latest hit and extra boss info around it.

Join the [support Discord](https://discord.gg/YPugUP8rg2) for help, bugs and ideas, or right-click the bar and pick Support Discord.

See the [changelog](CHANGELOG.md) for what's new in each version.

![The bar on a boss](docs/example-boss.png)

![The bar with a different theme](docs/example-monster.png)

![The Oldschool theme](docs/example-oldschool.png)

## What it does

- Replaces the opponent health bar with a wide, themed bar: name, combat level and hitpoints, a damage trail after each hit, a low health pulse, and the damage of your latest attack (or your party's)
- Takes over the game's own boss health bar, using its exact hitpoints and phase markers. Other bars the game shows next to it, like pillar bars, are drawn in the same style in their own movable box
- A smaller second bar shows another boss you're fighting at the same time, with its last known health while it's away
- Remembers a boss you walk out of sight of: come back to it in the same area and the bar carries on with its health and fight time, without "Defeated" or the intro
- Optional info around the bar: kill count, fight timer, elemental weakness, defence drain limit, party and magic defence, special attack counts
- Themes: god themes, a custom theme with your own colours and icon, the boss's own icon and colours, a plain Oldschool look, and the game's textures over the fill
- Its own panel in the sidebar for the look of the bar, with the game's items and sprites for the icon, and Import and Export to keep a setup or pass it on

It only shows what the game already tells you, plus a timer for how long the fight has lasted. No attack timers or prediction.

## Which opponents get a bar

Bosses from a [built-in list](docs/bosses.md), anything the game's boss health bar shows, and superior slayer monsters. Add or leave out NPCs by name or ID with Also show for and Never show for, or turn off Only show for bosses to get the bar on everything.

The bar stays on the boss while you hit its minions, follows the game's boss bar between the NPCs of a group fight, and stays up while the fight goes on, even when you stop to move or eat.

## Works with

| Plugin | What the bar does with it |
|---|---|
| Opponent Information (RuneLite) | Turns its health bar off while this plugin runs, so you don't see two. Your setting comes back after |
| Chat Commands (RuneLite) | Reads the kill counts it saves |
| Special Attack Counter (RuneLite) | Shows its counts with each weapon's icon |
| Party (RuneLite) | Damage number can add up the hits of party members who also run this plugin |
| Boss Health Indicators | Draws the health lines you set up there on this bar |
| Party Defence Tracker / Better Party Defence | Shows the defence (and magic defence) from their info boxes |

## Settings

The settings live in two places. The plugin's own panel, opened from its sidebar icon or by right-clicking the bar and picking Open panel, holds the look of the bar, the custom colours and the backup buttons. The rest is in the RuneLite config panel, which the settings button at the top of the panel opens, or right-click the bar and pick Configure. Preview, at the top of the panel, shows a sample opponent so you can try settings without a fight. Hold Alt to move the bar or drag its edge to resize it; the pillar bars box resizes the same way.

In the panel:

| Section | What's in it |
|---|---|
| Look | Theme, Oldschool theme, boss colours, phase colours, rare gold bars, icons, bar ends, fill texture, phase markers, size |
| Custom colors | The colours of the Custom theme and its icon |
| Backup | Paste settings, and Save debug log |

Import and Export sit at the top of the panel, so a setup can be saved to a file and your clipboard, or loaded back, whatever is open below.

On the settings screen:

| Section | What's in it |
|---|---|
| Text | Name, combat level, hitpoints text (percentage, value, both or none), damage number and whose hits it counts |
| Fonts | Font and size of each text around the bar, Smooth text, and the text edge (shadow, outline or nothing) |
| Boss info | Kill count, party defence, magic defence, special attack counts, elemental weakness, defence drain limit, fight timer, second boss bar |
| Animations | Damage trail, heal speed, low health effect, flash on big hits, intro, defeat animation, burn away |
| Layout | Where each item goes: left, centre or right, above the bar, inside it, or on one of the two rows below it |
| When to show | Which opponents get a bar, Also show for / Never show for, Hide after, what happens to the game's boss bar (replace it, show both, or hide this one), and the other plugins above |
| Sidebar icon | Turn it off to give the sidebar its space back; the panel still opens from the bar's right-click menu |

The tooltip on each setting explains it in more detail.

## Reporting a bug

Right after the bar does something wrong, press Save debug log (Backup section of the panel) or right-click the bar and pick Save debug log. The file goes to `.runelite/plugin-data/boss-health-bar-additions/` and the chat says where. Attach it to a GitHub issue or post it in the [Discord](https://discord.gg/YPugUP8rg2). It holds the plugin and RuneLite versions, your settings for this plugin, which related plugins are on, and the plugin's last 1000 events (NPC names and IDs, times, game ticks). No player names, chat or account details; it's kept in memory only and nothing is sent anywhere.

## Data

The elemental weaknesses and defence drain limits come from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki) and are built into the plugin, so it doesn't go online for them. [Supported bosses](docs/bosses.md) lists them per boss.

## Contact

Questions, ideas or bugs? Join the [support Discord](https://discord.gg/YPugUP8rg2), open an [issue on GitHub](https://github.com/Ghordrin/Boss-healthbar-additions/issues), or reach me on Discord at comrade9932 (Kuringe), or in game at Ultra Cringe.

## License

BSD 2-Clause, see [LICENSE](LICENSE).
