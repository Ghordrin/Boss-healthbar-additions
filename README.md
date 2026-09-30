# Boss Health Bar Additions

A bigger, themed health bar for the boss you're fighting, with a damage trail and a running total of your hits.

See the [changelog](CHANGELOG.md) for what's new in each version.

![The bar on a boss](docs/example-boss.png)

![The bar with a different theme](docs/example-monster.png)

## What it does

- Shows a wide bar with the opponent's name, combat level and hitpoints
- When you land a hit, the health you took off stays visible as a lighter trail for a moment before it drains, so you can see how big the hit was
- Adds your recent hits up into a number above the bar
- The fill changes colour as health drops and pulses when it's low
- God themes put that god's icon at both ends of the bar. The custom theme lets you pick your own colours and any item as the icon
- You can lay one of the game's own textures over the fill

It only shows what the game already tells you. No timers, attack prediction or anything like that.

## Which opponents get a bar

By default only bosses do. The plugin has a list of commonly fought bosses, and it also picks up anything the game's own boss health bar is showing, plus superior slayer monsters. If a boss is missing, turn on "Also show above combat level", or turn off "Only show for bosses" to get the bar on everything.

If you attack something smaller during a boss fight, like the minions some bosses spawn, the bar stays on the boss.

Superior slayer monsters are found by watching for the chat message when one spawns and taking the closest NPC that spawned around the same time. Works fine in practice, but it is a guess.

## Settings

Everything is in the normal RuneLite config screen, or right-click the bar and pick Configure. A few worth knowing about:

- Preview shows a fake opponent that loses and regains health, so you can try settings without fighting anything
- Replace game's boss health bar (on by default) hides the game's own boss bar and uses its numbers instead. Turn it off if you'd rather keep the game's bar, and this one stays out of the way for those fights
- Hide vanilla opponent overlay (on by default) turns off the health bar from RuneLite's Opponent Information plugin so you don't see two. Your own setting comes back when you turn this plugin off
- Choose custom icon and Choose fill texture open the pickers. Both are in the bar's right-click menu too. The icon search opens in your chatbox, so you need to be logged in

The Experimental section holds newer options that may still change:

- Choose custom icon picks the item shown at the bar ends on the Custom theme
- Use boss icon (on by default) swaps the icon at the bar ends for the boss's hiscores icon, on any theme. Raid bosses show their pet instead
- Match boss colors (off by default) gives each boss its own bar colors, on any theme. A boss keeps the same colors in every form
- Rare gold bars (on by default) turns about 1 in 250 bars gold, with a shine and a few sparkles. It's rolled once per opponent and is only for looks

Hold Alt to move the bar, or drag its edge to make it wider or narrower. Alt + right-click and Reset puts it back.

## License

BSD 2-Clause, see [LICENSE](LICENSE).
