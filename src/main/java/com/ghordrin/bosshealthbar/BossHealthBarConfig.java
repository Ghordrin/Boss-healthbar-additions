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
	String FONT_KEY = "font";
	FontType DEFAULT_FONT = new FontType().withFamily(Font.SERIF).withSize(17);

	@ConfigSection(
		name = "Appearance",
		description = "How the bar looks and animates.",
		position = 0
	)
	String appearanceSection = "appearance";

	@ConfigSection(
		name = "Custom colors",
		description = "The colors used when Theme is set to Custom.",
		position = 1,
		closedByDefault = true
	)
	String customColorsSection = "customColors";

	@ConfigSection(
		name = "Text",
		description = "The name, numbers and labels around the bar.",
		position = 2
	)
	String textSection = "text";

	@ConfigSection(
		name = "Behaviour",
		description = "Which opponents get a bar, and how it works alongside other health bars.",
		position = 3
	)
	String behaviourSection = "behaviour";

	@ConfigItem(
		keyName = "showPreview",
		name = "Preview",
		description = "Show the bar with a sample opponent while you aren't fighting anything, so you can see how your settings look. The sample loses and regains health on a loop.",
		position = 0
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
		description = "The game texture overlaid on the bar's fill. Set from \"Choose fill texture\" in the Appearance section or the bar's right-click menu.",
		hidden = true
	)
	default int fillTextureId()
	{
		return -1;
	}

	@ConfigItem(
		keyName = THEME_KEY,
		name = "Theme",
		description = "The colors of the health bar. Choose Custom to tweak the colors in the Custom colors section, starting from the theme you had selected.",
		position = 0,
		section = appearanceSection
	)
	default HealthBarTheme theme()
	{
		return DEFAULT_THEME;
	}

	@ConfigItem(
		keyName = CHOOSE_FILL_TEXTURE_KEY,
		name = "Choose fill texture",
		description = "Click to open a window with the game's own textures to lay over the fill, on any theme. \"None\" in that window takes it off again.",
		position = 1,
		section = appearanceSection
	)
	default boolean chooseFillTexture()
	{
		return false;
	}

	@ConfigItem(
		keyName = CHOOSE_CUSTOM_ICON_KEY,
		name = "Choose custom icon",
		description = "Click to search for an item in your chatbox. Its icon is shown at the bar ends on the Custom theme. You need to be logged in.",
		position = 2,
		section = appearanceSection
	)
	default boolean chooseCustomIcon()
	{
		return false;
	}

	@ConfigItem(
		keyName = "useBossIcon",
		name = "Use boss icon",
		description = "Show the icon of the boss you're fighting at the bar ends, on any theme. Raid bosses show their pet. Other opponents keep the theme's icon.",
		position = 3,
		section = appearanceSection
	)
	default boolean useBossIcon()
	{
		return false;
	}

	@Range(min = 200, max = 1400)
	@ConfigItem(
		keyName = "barWidth",
		name = "Bar width",
		description = "The width of the health bar in pixels. You can also hold Alt and drag the bar's edge to resize it, which takes over from this setting until you reset the overlay.",
		position = 4,
		section = appearanceSection
	)
	default int barWidth()
	{
		return 600;
	}

	@ConfigItem(
		keyName = "fitToGameView",
		name = "Fit to game view",
		description = "Make the bar narrower when it would take up too much of the game view, such as in fixed mode or a small window. The bar height and text keep their size so they stay readable.",
		position = 5,
		section = appearanceSection
	)
	default boolean fitToGameView()
	{
		return true;
	}

	@Range(min = 4, max = 24)
	@ConfigItem(
		keyName = "barHeight",
		name = "Bar height",
		description = "The height of the health bar itself in pixels, not counting the text around it.",
		position = 6,
		section = appearanceSection
	)
	default int barHeight()
	{
		return 7;
	}

	@ConfigItem(
		keyName = "showDamageTrail",
		name = "Show damage trail",
		description = "After a hit, keep the lost health visible as a lighter section for a moment before it drains away.",
		position = 7,
		section = appearanceSection
	)
	default boolean showDamageTrail()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPhaseMarkers",
		name = "Show phase markers",
		description = "When this bar replaces the game's own boss health bar, show the same phase markers the game's bar shows.",
		position = 8,
		section = appearanceSection
	)
	default boolean showPhaseMarkers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashOnBigHits",
		name = "Flash on big hits",
		description = "Briefly flash the bar's border when a hit removes a large part of the opponent's health.",
		position = 9,
		section = appearanceSection
	)
	default boolean flashOnBigHits()
	{
		return false;
	}

	@ConfigItem(
		keyName = "introAnimation",
		name = "Intro animation",
		description = "How the bar appears for a new opponent: fade in, rise into place, widen from the center with the fill sweeping up, or rise and widen.",
		position = 10,
		section = appearanceSection
	)
	default IntroAnimation introAnimation()
	{
		return IntroAnimation.SLIDE_AND_EXPAND;
	}

	@ConfigItem(
		keyName = "showDefeatAnimation",
		name = "Defeat animation",
		description = "When the opponent dies, hold the empty bar with a \"Defeated\" label for a moment before fading it out.",
		position = 11,
		section = appearanceSection
	)
	default boolean showDefeatAnimation()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lowHealthEffect",
		name = "Low health effect",
		description = "Make the fill pulse and glow while the opponent's health is at or below the low health threshold.",
		position = 12,
		section = appearanceSection
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
		description = "The health percentage at or below which the low health effect starts.",
		position = 13,
		section = appearanceSection
	)
	default int lowHealthThreshold()
	{
		return 25;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "animationSpeed",
		name = "Heal animation speed",
		description = "How quickly the bar refills when the opponent heals. Higher is faster. Damage always lowers the bar immediately.",
		position = 14,
		section = appearanceSection
	)
	default int animationSpeed()
	{
		return 6;
	}

	@Alpha
	@ConfigItem(
		keyName = "customFillHighColor",
		name = "Fill (full health)",
		description = "The fill color at full health. The fill blends towards the low health color as health drops.",
		position = 0,
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
		description = "The fill color as the opponent's health reaches zero. Set it to the same color as full health for a single color.",
		position = 1,
		section = customColorsSection
	)
	default Color customFillLowColor()
	{
		return new Color(0x96161A);
	}

	@Alpha
	@ConfigItem(
		keyName = "customTrailColor",
		name = "Damage trail",
		description = "The color of the damage trail.",
		position = 2,
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
		description = "The color of the bar's frame, end pieces, underline and phase markers.",
		position = 3,
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
		description = "The color of the opponent's name and the damage number.",
		position = 4,
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
		position = 5,
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
		description = "The color of the hitpoints text below the bar.",
		position = 6,
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
		position = 7,
		section = customColorsSection
	)
	default Color customDefeatedTextColor()
	{
		return new Color(0xB6AEA1);
	}

	@ConfigItem(
		keyName = FONT_KEY,
		name = "Font",
		description = "The font, size and style of the name and damage number. The combat level and hitpoints text use a smaller version of it. Lists the RuneScape fonts, the fonts installed on your computer, and any .ttf or .otf files added to the .runelite/fonts folder.",
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
		description = "Show the opponent's name above the health bar.",
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
		keyName = "showDamageNumber",
		name = "Show damage number",
		description = "Show the total damage of your recent hits above the right end of the bar. It resets a few seconds after your last hit.",
		position = 3,
		section = textSection
	)
	default boolean showDamageNumber()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hitpointsTextMode",
		name = "Hitpoints text",
		description = "Show the opponent's hitpoints below the bar as a percentage, a value (when the max hitpoints are known), or both. Bosses the game only shows as a percentage always show a percentage.",
		position = 4,
		section = textSection
	)
	default HitpointsTextMode hitpointsTextMode()
	{
		return HitpointsTextMode.NONE;
	}

	@ConfigItem(
		keyName = "bossOnly",
		name = "Only show for bosses",
		description = "Only show the bar for bosses: a built-in list of commonly fought bosses and anything the game's own boss health bar is showing, plus opponents at or above the combat level and superior slayer monsters when those settings are on. Turn off to show it for any opponent.",
		position = 0,
		section = behaviourSection
	)
	default boolean bossOnly()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAboveCombatLevel",
		name = "Also show above combat level",
		description = "When \"Only show for bosses\" is on, also show the bar for any opponent at or above the combat level below, boss or not. Useful for bosses missing from the built-in list that the game doesn't give a health bar either.",
		position = 1,
		section = behaviourSection
	)
	default boolean showAboveCombatLevel()
	{
		return false;
	}

	@Range(min = 1, max = 1000)
	@ConfigItem(
		keyName = "minimumCombatLevel",
		name = "Combat level",
		description = "The combat level used by \"Also show above combat level\".",
		position = 2,
		section = behaviourSection
	)
	default int minimumCombatLevel()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "showSuperiors",
		name = "Show for superior slayer monsters",
		description = "When \"Only show for bosses\" is on, also show the bar for superior slayer monsters you spawn, whatever their combat level.",
		position = 3,
		section = behaviourSection
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
		description = "How long the bar stays after you stop attacking. While the game's own boss health bar shows the opponent, the bar stays regardless.",
		position = 4,
		section = behaviourSection
	)
	default int hideDelay()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "replaceNativeBossBar",
		name = "Replace game's boss health bar",
		description = "Some bosses show the game's own health bar at the top of the screen. When on, that bar is hidden while you fight the boss and this bar is shown instead. When off, this bar is hidden for those bosses.",
		position = 5,
		section = behaviourSection
	)
	default boolean replaceNativeBossBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = HIDE_VANILLA_OVERLAY_KEY,
		name = "Hide vanilla opponent overlay",
		description = "Turn off the health bar of RuneLite's \"Opponent Information\" plugin while this plugin is on, so two health bars aren't shown at once.",
		position = 6,
		section = behaviourSection
	)
	default boolean hideVanillaOverlay()
	{
		return true;
	}
}
