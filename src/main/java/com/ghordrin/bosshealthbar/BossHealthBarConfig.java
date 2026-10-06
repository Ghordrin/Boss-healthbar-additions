package com.ghordrin.bosshealthbar;

import java.awt.Color;
import java.awt.Font;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BossHealthBarConfig.GROUP)
public interface BossHealthBarConfig extends Config
{
	String GROUP = "bosshealthbar";
	String HIDE_VANILLA_OVERLAY_KEY = "hideVanillaOverlay";
	String CUSTOM_ICON_ITEM_ID_KEY = "customIconItemId";
	String FILL_TEXTURE_ID_KEY = "fillTextureId";
	String CHOOSE_FILL_TEXTURE_KEY = "chooseFillTexture";
	String CHOOSE_CUSTOM_ICON_KEY = "chooseCustomIcon";
	String THEME_KEY = "theme";
	HealthBarTheme DEFAULT_THEME = HealthBarTheme.ZAMORAK;
	String OLDSCHOOL_THEME_KEY = "oldschoolTheme";
	String MATCH_BOSS_COLORS_KEY = "matchBossColors";
	String RARE_GOLD_BARS_KEY = "rareGoldBars";
	// What Match boss colors and Rare gold bars were set to before Oldschool turned them off.
	String SAVED_MATCH_BOSS_COLORS_KEY = "oldschoolSavedMatchBossColors";
	String SAVED_RARE_GOLD_BARS_KEY = "oldschoolSavedRareGoldBars";
	String FONT_KEY = "font";
	FontType DEFAULT_FONT = new FontType().withFamily(Font.SERIF).withSize(17);
	String NATIVE_BOSS_BAR_MODE_KEY = "nativeBossBarMode";
	// The checkbox nativeBossBarMode replaced. Its saved value is moved over on startup.
	String OLD_REPLACE_NATIVE_BOSS_BAR_KEY = "replaceNativeBossBar";

	@ConfigSection(
		name = "Look",
		description = "Theme, icons and size of the bar.",
		position = 0
	)
	String lookSection = "look";

	@ConfigSection(
		name = "Text",
		description = "The name and numbers around the bar.",
		position = 1
	)
	String textSection = "text";

	@ConfigSection(
		name = "Boss info",
		description = "Extra details about the boss shown around the bar.",
		position = 2,
		closedByDefault = true
	)
	String bossInfoSection = "bossInfo";

	@ConfigSection(
		name = "Animations",
		description = "How the bar moves when health changes, appears and goes.",
		position = 3,
		closedByDefault = true
	)
	String animationsSection = "animations";

	@ConfigSection(
		name = "Layout",
		description = "Where each item goes around the bar.",
		position = 4,
		closedByDefault = true
	)
	String layoutSection = "layout";

	@ConfigSection(
		name = "Custom colors",
		description = "The icon and colors of the Custom theme.",
		position = 5,
		closedByDefault = true
	)
	String customColorsSection = "customColors";

	@ConfigSection(
		name = "When to show",
		description = "Which opponents get a bar, and how it works with other health bars.",
		position = 6,
		closedByDefault = true
	)
	String whenToShowSection = "whenToShow";

	@ConfigItem(
		keyName = "showPreview",
		name = "Preview",
		description = "Show the bar with a sample opponent while you aren't fighting anything, to try your settings.",
		// Sections share the top-level order, so this keeps Preview above them.
		position = -1
	)
	default boolean showPreview()
	{
		return false;
	}

	@ConfigItem(
		keyName = CUSTOM_ICON_ITEM_ID_KEY,
		name = "Custom icon item",
		description = "The item whose icon the Custom theme shows at the bar ends. Set by right-clicking the bar and choosing \"Choose custom icon\".",
		hidden = true
	)
	default int customIconItemId()
	{
		return -1;
	}

	@ConfigItem(
		keyName = FILL_TEXTURE_ID_KEY,
		name = "Fill texture",
		description = "The game texture overlaid on the bar's fill. Set from \"Choose fill texture\" in the Look section or the bar's right-click menu.",
		hidden = true
	)
	default int fillTextureId()
	{
		return -1;
	}

	@ConfigItem(
		keyName = THEME_KEY,
		name = "Theme",
		description = "The colors of the bar. Custom starts from the theme you had and uses the Custom colors section. Not used with Use Oldschool theme on.",
		position = 0,
		section = lookSection
	)
	default HealthBarTheme theme()
	{
		return DEFAULT_THEME;
	}

	@ConfigItem(
		keyName = OLDSCHOOL_THEME_KEY,
		name = "Use Oldschool theme",
		description = "A plain green bar over red, like the game's own. Turns off Match boss colors and Rare gold bars until you untick it. The Custom colors aren't used either.",
		position = 1,
		section = lookSection
	)
	default boolean oldschoolTheme()
	{
		return false;
	}

	@ConfigItem(
		keyName = MATCH_BOSS_COLORS_KEY,
		name = "Match boss colors",
		description = "Color the bar to suit the boss you're fighting, on any theme. Ticking this turns off Use Oldschool theme.",
		position = 2,
		section = lookSection
	)
	default boolean matchBossColors()
	{
		return false;
	}

	@ConfigItem(
		keyName = RARE_GOLD_BARS_KEY,
		name = "Rare gold bars",
		description = "About 1 in 250 bars turns gold, only for looks. Ticking this turns off Use Oldschool theme.",
		position = 3,
		section = lookSection
	)
	default boolean rareGoldBars()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showIcons",
		name = "Show icons",
		description = "Show an icon in a crest at the bar ends, or small before the name with Use Oldschool theme on.",
		position = 4,
		section = lookSection
	)
	default boolean showIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "useBossIcon",
		name = "Use boss icon",
		description = "Use the icon of the boss you're fighting instead of the theme's. Raid bosses show their pet. Needs Show icons.",
		position = 5,
		section = lookSection
	)
	default boolean useBossIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "barEnds",
		name = "Bar ends",
		description = "The ornament at each end of the bar. Only visible with Show icons off. Not used with Use Oldschool theme on.",
		position = 6,
		section = lookSection
	)
	default BarEnds barEnds()
	{
		return BarEnds.CLASSIC;
	}

	@ConfigItem(
		keyName = CHOOSE_FILL_TEXTURE_KEY,
		name = "Choose fill texture",
		description = "Click to pick one of the game's textures to lay over the fill. Not used with Use Oldschool theme on.",
		position = 7,
		section = lookSection
	)
	default boolean chooseFillTexture()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showPhaseMarkers",
		name = "Show phase markers",
		description = "Show the same phase markers as the game's own boss health bar, when it has them.",
		position = 8,
		section = lookSection
	)
	default boolean showPhaseMarkers()
	{
		return true;
	}

	@Range(min = 200, max = 1400)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "barWidth",
		name = "Bar width",
		description = "The width of the bar. Alt-dragging its edge overrides this until you reset the overlay.",
		position = 9,
		section = lookSection
	)
	default int barWidth()
	{
		return 600;
	}

	@Range(min = 4, max = 24)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "barHeight",
		name = "Bar height",
		description = "The height of the bar itself, without the text around it.",
		position = 10,
		section = lookSection
	)
	default int barHeight()
	{
		return 7;
	}

	@ConfigItem(
		keyName = "fitToGameView",
		name = "Fit to game view",
		description = "Make the bar narrower when the game view is small. The height and text keep their size.",
		position = 11,
		section = lookSection
	)
	default boolean fitToGameView()
	{
		return true;
	}

	@ConfigItem(
		keyName = FONT_KEY,
		name = "Font",
		description = "The font of the name and damage number. The smaller text uses a smaller version of it.",
		position = 0,
		section = textSection
	)
	default FontType font()
	{
		return DEFAULT_FONT;
	}

	@ConfigItem(
		keyName = "showBossName",
		name = "Show name",
		description = "Show the opponent's name.",
		position = 1,
		section = textSection
	)
	default boolean showBossName()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCombatLevel",
		name = "Show combat level",
		description = "Show the opponent's combat level next to its name.",
		position = 2,
		section = textSection
	)
	default boolean showCombatLevel()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hitpointsTextMode",
		name = "Hitpoints text",
		description = "Show the hitpoints as a percentage, a value, or both. Shows a percentage when the max hitpoints aren't known.",
		position = 3,
		section = textSection
	)
	default HitpointsTextMode hitpointsTextMode()
	{
		return HitpointsTextMode.NONE;
	}

	@ConfigItem(
		keyName = "showDamageNumber",
		name = "Show damage number",
		description = "Show the damage of the latest attack. Hits that land together are added up.",
		position = 4,
		section = textSection
	)
	default boolean showDamageNumber()
	{
		return true;
	}

	@ConfigItem(
		keyName = "damageNumberSource",
		name = "Damage number counts",
		description = "Me shows your latest attack. Party adds up your party's hits while they keep coming, for members who also use this plugin.",
		position = 5,
		section = textSection
	)
	default DamageNumberSource damageNumberSource()
	{
		return DamageNumberSource.ME;
	}

	@ConfigItem(
		keyName = "showKillCount",
		name = "Kill count",
		description = "Show your kill count for the boss. Needs RuneLite's Chat Commands plugin to have seen a kill count message for it.",
		position = 0,
		section = bossInfoSection
	)
	default boolean showKillCount()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showWeakness",
		name = "Elemental weakness",
		description = "Show the boss's elemental weakness and extra damage, for example +40%. Values from the OSRS Wiki.",
		position = 1,
		section = bossInfoSection
	)
	default boolean showWeakness()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDrainCap",
		name = "Defence drain limit",
		description = "Show how far the boss's defence can be lowered in total, or \"no drain\". Values from the OSRS Wiki.",
		position = 2,
		section = bossInfoSection
	)
	default boolean showDrainCap()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSpecialAttackCounts",
		name = "Special attack counts",
		description = "Show the counts from RuneLite's Special Attack Counter plugin. Needs that plugin on with its info boxes.",
		position = 3,
		section = bossInfoSection
	)
	default boolean showSpecialAttackCounts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPartyDefence",
		name = "Party defence",
		description = "Show the defence from the Party Defence Tracker or Better Party Defence plugin's info box. Needs one of them installed and on.",
		position = 4,
		section = bossInfoSection
	)
	default boolean showPartyDefence()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMagicDefence",
		name = "Magic defence",
		description = "Show the magic defence from the Better Party Defence plugin's Magic defence info box. Needs that plugin with its Magic defence info box on.",
		position = 5,
		section = bossInfoSection
	)
	default boolean showMagicDefence()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDamageTrail",
		name = "Damage trail",
		description = "After a hit, keep the lost health visible as a lighter section before it drains away.",
		position = 0,
		section = animationsSection
	)
	default boolean showDamageTrail()
	{
		return true;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "animationSpeed",
		name = "Heal speed",
		description = "How quickly the bar refills when the opponent heals. Higher is faster.",
		position = 1,
		section = animationsSection
	)
	default int animationSpeed()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "lowHealthEffect",
		name = "Low health effect",
		description = "Make the fill pulse and glow at or below the low health threshold.",
		position = 2,
		section = animationsSection
	)
	default boolean lowHealthEffect()
	{
		return true;
	}

	@Range(min = 5, max = 50)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "lowHealthThreshold",
		name = "Low health threshold",
		description = "The health at or below which the low health effect starts.",
		position = 3,
		section = animationsSection
	)
	default int lowHealthThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "flashOnBigHits",
		name = "Flash on big hits",
		description = "Briefly flash the border when a hit takes a large part of the health.",
		position = 4,
		section = animationsSection
	)
	default boolean flashOnBigHits()
	{
		return false;
	}

	@ConfigItem(
		keyName = "introAnimation",
		name = "Intro animation",
		description = "How the bar appears for a new opponent.",
		position = 5,
		section = animationsSection
	)
	default IntroAnimation introAnimation()
	{
		return IntroAnimation.SLIDE_AND_EXPAND;
	}

	@ConfigItem(
		keyName = "showDefeatAnimation",
		name = "Defeat animation",
		description = "Hold the empty bar with a \"Defeated\" label for a moment before it goes.",
		position = 6,
		section = animationsSection
	)
	default boolean showDefeatAnimation()
	{
		return true;
	}

	@ConfigItem(
		keyName = "burnAwayDefeat",
		name = "Burn away",
		description = "Burn the bar away from right to left instead of fading it out. Only used with Defeat animation on.",
		position = 7,
		section = animationsSection
	)
	default boolean burnAwayDefeat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "namePosition",
		name = "Name",
		description = "Where the name goes, with its icon and combat level. It's shortened when it doesn't fit.",
		position = 0,
		section = layoutSection
	)
	default BarPosition namePosition()
	{
		return BarPosition.TOP_LEFT;
	}

	@ConfigItem(
		keyName = "damageNumberPosition",
		name = "Damage number",
		description = "Where the damage number goes.",
		position = 1,
		section = layoutSection
	)
	default BarPosition damageNumberPosition()
	{
		return BarPosition.TOP_RIGHT;
	}

	@ConfigItem(
		keyName = "hitpointsPosition",
		name = "Hitpoints",
		description = "Where the hitpoints text goes. \"Defeated\" shows centred on the same row.",
		position = 2,
		section = layoutSection
	)
	default BarPosition hitpointsPosition()
	{
		return BarPosition.BOTTOM_RIGHT;
	}

	@ConfigItem(
		keyName = "killCountPosition",
		name = "Kill count",
		description = "Where the kill count goes.",
		position = 3,
		section = layoutSection
	)
	default BarPosition killCountPosition()
	{
		return BarPosition.BOTTOM_LEFT;
	}

	@ConfigItem(
		keyName = "partyDefencePosition",
		name = "Party defence",
		description = "Where the party defence and magic defence go.",
		position = 4,
		section = layoutSection
	)
	default BarPosition partyDefencePosition()
	{
		return BarPosition.BOTTOM_LEFT;
	}

	@ConfigItem(
		keyName = "specialAttackCountsPosition",
		name = "Special attack counts",
		description = "Where the special attack counts go.",
		position = 5,
		section = layoutSection
	)
	default BarPosition specialAttackCountsPosition()
	{
		return BarPosition.BOTTOM_LEFT;
	}

	@ConfigItem(
		keyName = "weaknessPosition",
		name = "Elemental weakness",
		description = "Where the elemental weakness goes.",
		position = 6,
		section = layoutSection
	)
	default BarPosition weaknessPosition()
	{
		return BarPosition.BOTTOM_LEFT;
	}

	@ConfigItem(
		keyName = "drainCapPosition",
		name = "Defence drain limit",
		description = "Where the defence drain limit goes.",
		position = 7,
		section = layoutSection
	)
	default BarPosition drainCapPosition()
	{
		return BarPosition.BOTTOM_LEFT;
	}

	@ConfigItem(
		keyName = CHOOSE_CUSTOM_ICON_KEY,
		name = "Choose custom icon",
		description = "Click to search for an item in your chatbox. Its icon is shown on the Custom theme. You need to be logged in.",
		position = 0,
		section = customColorsSection
	)
	default boolean chooseCustomIcon()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "customFillHighColor",
		name = "Fill (full health)",
		description = "The fill color at full health. It blends towards the low health color as health drops.",
		position = 1,
		section = customColorsSection
	)
	default Color customFillHighColor()
	{
		return new Color(0x96161A);
	}

	@Alpha
	@ConfigItem(
		keyName = "customFillLowColor",
		name = "Fill (low health)",
		description = "The fill color near zero health. Use the full health color for a single color.",
		position = 2,
		section = customColorsSection
	)
	default Color customFillLowColor()
	{
		return new Color(0x96161A);
	}

	@Alpha
	@ConfigItem(
		keyName = "customTrailColor",
		name = "Damage trail color",
		description = "The color of the damage trail.",
		position = 3,
		section = customColorsSection
	)
	default Color customTrailColor()
	{
		return new Color(0xD6B254);
	}

	@Alpha
	@ConfigItem(
		keyName = "customFrameColor",
		name = "Frame",
		description = "The color of the frame, end pieces, underline and phase markers.",
		position = 4,
		section = customColorsSection
	)
	default Color customFrameColor()
	{
		return new Color(0x7A6C54);
	}

	@Alpha
	@ConfigItem(
		keyName = "customTextColor",
		name = "Name and damage text",
		description = "The color of the name and the damage number.",
		position = 5,
		section = customColorsSection
	)
	default Color customTextColor()
	{
		return new Color(0xE8E2D4);
	}

	@Alpha
	@ConfigItem(
		keyName = "customLevelTextColor",
		name = "Combat level text",
		description = "The color of the combat level next to the name.",
		position = 6,
		section = customColorsSection
	)
	default Color customLevelTextColor()
	{
		return new Color(0xA99F90);
	}

	@Alpha
	@ConfigItem(
		keyName = "customHitpointsTextColor",
		name = "Hitpoints text",
		description = "The color of the hitpoints text.",
		position = 7,
		section = customColorsSection
	)
	default Color customHitpointsTextColor()
	{
		return new Color(0xC8C0B0);
	}

	@Alpha
	@ConfigItem(
		keyName = "customDefeatedTextColor",
		name = "Defeated text",
		description = "The color of the \"Defeated\" label.",
		position = 8,
		section = customColorsSection
	)
	default Color customDefeatedTextColor()
	{
		return new Color(0xB6AEA1);
	}

	@ConfigItem(
		keyName = "bossOnly",
		name = "Only show for bosses",
		description = "Only show the bar for known bosses and anything the game's boss health bar shows. Turn off to show it for any opponent.",
		position = 0,
		section = whenToShowSection
	)
	default boolean bossOnly()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAboveCombatLevel",
		name = "Also show above combat level",
		description = "With Only show for bosses on, also show the bar for any opponent at or above the minimum combat level.",
		position = 1,
		section = whenToShowSection
	)
	default boolean showAboveCombatLevel()
	{
		return false;
	}

	@Range(min = 1, max = 1000)
	@ConfigItem(
		keyName = "minimumCombatLevel",
		name = "Minimum combat level",
		description = "The combat level used by Also show above combat level.",
		position = 2,
		section = whenToShowSection
	)
	default int minimumCombatLevel()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "showSuperiors",
		name = "Superior slayer monsters",
		description = "With Only show for bosses on, also show the bar for superior slayer monsters you spawn.",
		position = 3,
		section = whenToShowSection
	)
	default boolean showSuperiors()
	{
		return true;
	}

	@Range(min = 1, max = 60)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "hideDelay",
		name = "Hide after",
		description = "How long the bar stays once the fight goes quiet. It stays while the game's boss health bar shows the opponent.",
		position = 4,
		section = whenToShowSection
	)
	default int hideDelay()
	{
		return 5;
	}

	@ConfigItem(
		keyName = NATIVE_BOSS_BAR_MODE_KEY,
		name = "Game's boss health bar",
		description = "For bosses with the game's own health bar: replace it with this bar, show both, or hide this bar.",
		position = 5,
		section = whenToShowSection
	)
	default NativeBossBarMode nativeBossBarMode()
	{
		return NativeBossBarMode.REPLACE;
	}

	@ConfigItem(
		keyName = HIDE_VANILLA_OVERLAY_KEY,
		name = "Hide Opponent Information bar",
		description = "Turn off the health bar of RuneLite's Opponent Information plugin while this plugin is on.",
		position = 6,
		section = whenToShowSection
	)
	default boolean hideVanillaOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHealthIndicatorMarkers",
		name = "Boss Health Indicators lines",
		description = "Draw the health lines from the Boss Health Indicators plugin on this bar too.",
		position = 7,
		section = whenToShowSection
	)
	default boolean showHealthIndicatorMarkers()
	{
		return true;
	}
}
