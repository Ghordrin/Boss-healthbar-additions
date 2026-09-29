# Boss Health Bar Additions

RuneLite already shows you an opponent's health, tucked in a small bar up in
the corner that's easy to forget about. This swaps that for a wide bar with
the opponent's name, a frame, and some animation, so a fight actually reads
at a glance. A hit for 70 doesn't look the same as a hit for 3 anymore.

- Damage drops the fill straight away, but the health you just took off
  hangs around as a lighter trail for a moment before draining, so you can
  see how big the hit was.
- Your recent hits add up into a number above the right end, resetting a
  couple seconds after you stop.
- The fill shifts color as health drops and pulses once the opponent's low.

That's the whole idea. No timers, no attack prediction, no mechanic
warnings, just the same information the game already gives you, drawn
bigger. You can drag it around like any other overlay.

The five god themes (Saradomin, Zamorak, Bandos, Armadyl, Zaros) draw that
god's real in-game icon at both bar ends, pulled from RuneLite's own sprite
cache. Custom theme shows whatever item icon you pick instead.

## Settings

Everything's in RuneLite's normal config screen: find "Boss Health Bar
Additions" in the plugin list, or right-click the bar and pick Configure.

Turn on **Preview** to see a fake opponent loop through losing and
regaining health, so you can tune settings without finding something to
fight.

Right-clicking the bar also gives you two pickers:

- **Choose custom icon** opens the in-game item search in your chatbox,
  the same one bank tags uses for tab icons. The item you pick shows at
  the bar ends on the Custom theme.
- **Choose fill texture** opens a small window with the game's own
  textures to lay over the fill, on any theme. "None" takes it off again.

Both are also in the config screen, right under Theme: click the
**Choose fill texture** or **Choose custom icon** checkbox and the same
picker opens.

Worth knowing about a few of the settings:

- **Theme** picks the colors and, for the five gods above, the icons.
  Custom opens up its own color section, starting from whichever theme
  you were on.
- **Only show for bosses** (on by default) keeps the bar off regular
  monsters. Something counts as a boss when it's on the plugin's built-in
  list of commonly fought bosses (world bosses, Wilderness and slayer
  bosses, raid bosses and so on), when the game's own boss bar
  is showing it, or when it's a superior slayer monster. For anything the
  list misses, turn on **Also show above combat level**, which lets
  anything at or above the level you set through, bosses or not.
- **Replace game's boss health bar** (on by default): some bosses, including
  Theatre of Blood, have their own bar at the top of the screen with exact
  hitpoints and phase markers. This plugin hides that bar and uses its
  numbers instead. Turn the setting off to keep the game's bars and have
  this one step aside for those fights.
- **Hide vanilla opponent overlay** (on by default) turns off RuneLite's
  own "Opponent Information" health bar while this plugin runs, so you're
  not looking at two, and restores your setting when you disable the
  plugin.

The rest are what they sound like: bar size, damage trail, phase markers,
flash on big hits, intro/defeat animations, low health pulse, name/combat
level/hitpoints display, and how long the bar lingers after a fight.

## How it picks a target

It follows whatever you're interacting with, same as the Opponent
Information plugin — attacking something weaker mid-fight, like an add a
boss throws out, doesn't bump the bar off the boss.

Superior slayer monsters are a special case, since the game doesn't flag
them anywhere the plugin can read directly. It watches for the chat message
announcing one, then guesses the attackable NPC that spawned nearest you
around the same tick. Reliable in practice, but a guess.

## License

BSD 2-Clause, see [LICENSE](LICENSE).
