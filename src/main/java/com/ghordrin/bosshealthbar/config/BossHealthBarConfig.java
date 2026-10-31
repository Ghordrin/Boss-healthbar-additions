package com.ghordrin.bosshealthbar.config;

import com.ghordrin.bosshealthbar.data.DamageNumberSource;
import com.ghordrin.bosshealthbar.data.Health;
import com.ghordrin.bosshealthbar.gamebars.NativeBossBarMode;
import com.ghordrin.bosshealthbar.render.BarEnds;
import com.ghordrin.bosshealthbar.render.BarIconPlacement;
import com.ghordrin.bosshealthbar.render.BarPosition;
import com.ghordrin.bosshealthbar.render.HealthText;
import com.ghordrin.bosshealthbar.render.HitpointsTextMode;
import com.ghordrin.bosshealthbar.render.IntroAnimation;
import com.ghordrin.bosshealthbar.render.TextOutline;
import com.ghordrin.bosshealthbar.theme.HealthBarTheme;
import java.awt.Color;
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
	String CUSTOM_ICON_SPRITE_ID_KEY = "customIconSpriteId";
	String FILL_TEXTURE_ID_KEY = "fillTextureId";
	String CHOOSE_FILL_TEXTURE_KEY = "chooseFillTexture";
	String CHOOSE_CUSTOM_ICON_KEY = "chooseCustomIcon";
	String THEME_KEY = "theme";
	HealthBarTheme DEFAULT_THEME = HealthBarTheme.ZAMORAK;
	String CUSTOM_FILL_HIGH_COLOR_KEY = "customFillHighColor";
	String CUSTOM_FILL_LOW_COLOR_KEY = "customFillLowColor";
	String CUSTOM_TRACK_COLOR_KEY = "customTrackColor";
	// A background the user hasn't picked, which leaves the themed one in place.
	Color TRANSPARENT = new Color(0, 0, 0, 0);
	String CUSTOM_TRAIL_COLOR_KEY = "customTrailColor";
	String CUSTOM_FRAME_COLOR_KEY = "customFrameColor";
	String CUSTOM_TEXT_COLOR_KEY = "customTextColor";
	String CUSTOM_LEVEL_TEXT_COLOR_KEY = "customLevelTextColor";
	String CUSTOM_HITPOINTS_TEXT_COLOR_KEY = "customHitpointsTextColor";
	String CUSTOM_DEFEATED_TEXT_COLOR_KEY = "customDefeatedTextColor";
	String OLDSCHOOL_THEME_KEY = "oldschoolTheme";
	String MATCH_BOSS_COLORS_KEY = "matchBossColors";
	String PHASE_COLORS_KEY = "phaseColors";
	String PHASE_COLOR_KEY = "phaseColor";
	String RARE_GOLD_BARS_KEY = "rareGoldBars";
	String ICON_PLACEMENT_KEY = "iconPlacement";
	String USE_BOSS_ICON_KEY = "useBossIcon";
	String BAR_ENDS_KEY = "barEnds";
	String SHOW_PHASE_MARKERS_KEY = "showPhaseMarkers";
	String BAR_WIDTH_KEY = "barWidth";
	String BAR_HEIGHT_KEY = "barHeight";
	String FIT_TO_GAME_VIEW_KEY = "fitToGameView";
	String SHOW_SIDEBAR_ICON_KEY = "showSidebarIcon";
	String OPEN_PANEL_KEY = "openPanel";
	String FONT_KEY = "font";
	FontType DEFAULT_FONT = FontType.REGULAR;
	String NATIVE_BOSS_BAR_MODE_KEY = "nativeBossBarMode";
	String ALSO_SHOW_FOR_KEY = "alsoShowFor";
	String NEVER_SHOW_FOR_KEY = "neverShowFor";
	String DAMAGE_NUMBER_FONT_KEY = "damageNumberFont";
	String COMBAT_LEVEL_FONT_KEY = "combatLevelFont";
	String HITPOINTS_FONT_KEY = "hitpointsFont";
	String KILL_COUNT_FONT_KEY = "killCountFont";
	String PARTY_DEFENCE_FONT_KEY = "partyDefenceFont";
	String SPECIAL_ATTACK_COUNTS_FONT_KEY = "specialAttackCountsFont";
	String WEAKNESS_FONT_KEY = "weaknessFont";
	String DRAIN_CAP_FONT_KEY = "drainCapFont";
	String FIGHT_TIMER_FONT_KEY = "fightTimerFont";
	String SAVE_DEBUG_LOG_KEY = "saveDebugLog";
	String SHOW_PREVIEW_KEY = "showPreview";
	String EXPORT_SETTINGS_KEY = "exportSettings";
	String IMPORT_SETTINGS_KEY = "importSettings";
	String SHOW_BOSS_NAME_KEY = "showBossName";
	String SHOW_COMBAT_LEVEL_KEY = "showCombatLevel";
	String HITPOINTS_TEXT_MODE_KEY = "hitpointsTextMode";
	String PERCENT_DECIMALS_KEY = "percentDecimals";
	String SHOW_DAMAGE_NUMBER_KEY = "showDamageNumber";
	String DAMAGE_NUMBER_SOURCE_KEY = "damageNumberSource";
	String SHOW_KILL_COUNT_KEY = "showKillCount";
	String SHOW_PARTY_DEFENCE_KEY = "showPartyDefence";
	String SHOW_MAGIC_DEFENCE_KEY = "showMagicDefence";
	String SHOW_SPECIAL_ATTACK_COUNTS_KEY = "showSpecialAttackCounts";
	String SHOW_WEAKNESS_KEY = "showWeakness";
	String SHOW_DRAIN_CAP_KEY = "showDrainCap";
	String SHOW_FIGHT_TIMER_KEY = "showFightTimer";
	String SHOW_PARTNER_BAR_KEY = "showPartnerBar";
	String SECOND_BAR_SEPARATE_KEY = "secondBarSeparate";
	String SECOND_BAR_SCALE_KEY = "secondBarScale";
	String SHOW_DAMAGE_TRAIL_KEY = "showDamageTrail";
	String ANIMATION_SPEED_KEY = "animationSpeed";
	String LOW_HEALTH_EFFECT_KEY = "lowHealthEffect";
	String LOW_HEALTH_THRESHOLD_KEY = "lowHealthThreshold";
	String FLASH_ON_BIG_HITS_KEY = "flashOnBigHits";
	String INTRO_ANIMATION_KEY = "introAnimation";
	String SHOW_DEFEAT_ANIMATION_KEY = "showDefeatAnimation";
	String BURN_AWAY_DEFEAT_KEY = "burnAwayDefeat";
	String FORTIFY_PHASE_KEY = "fortifyPhase";
	String NAME_POSITION_KEY = "namePosition";
	String DAMAGE_NUMBER_POSITION_KEY = "damageNumberPosition";
	String HITPOINTS_POSITION_KEY = "hitpointsPosition";
	String KILL_COUNT_POSITION_KEY = "killCountPosition";
	String PARTY_DEFENCE_POSITION_KEY = "partyDefencePosition";
	String SPECIAL_ATTACK_COUNTS_POSITION_KEY = "specialAttackCountsPosition";
	String WEAKNESS_POSITION_KEY = "weaknessPosition";
	String DRAIN_CAP_POSITION_KEY = "drainCapPosition";
	String FIGHT_TIMER_POSITION_KEY = "fightTimerPosition";

	@ConfigSection(
		name = "The rest of the settings",
		description = "The look, the text, the boss info, the animations and the layout are in the plugin's own panel.",
		position = 1
	)
	String panelSection = "panel";

	@ConfigSection(
		name = "Fonts",
		description = "The font and size of each text around the bar.",
		position = 2,
		closedByDefault = true
	)
	String fontsSection = "fonts";

	@ConfigSection(
		name = "When to show",
		description = "Which opponents get a bar, and how it works with other health bars.",
		position = 7,
		closedByDefault = true
	)
	String whenToShowSection = "whenToShow";

	@ConfigItem(
		keyName = OPEN_PANEL_KEY,
		name = "Open panel",
		description = "Open the plugin's panel, which holds the look, the text, the boss info, the animations,"
			+ " the layout and the custom colors. It unticks itself again.",
		section = panelSection,
		position = 0
	)
	default boolean openPanel()
	{
		return false;
	}

	@ConfigItem(
		keyName = SHOW_SIDEBAR_ICON_KEY,
		name = "Sidebar icon",
		description = "Show this plugin's icon in the sidebar, which opens the panel holding its look and its"
			+ " colors. Turning it off leaves those where they are, only without a way in.",
		// Sections share the top-level order, so this sits with Preview above them.
		position = -2
	)
	default boolean showSidebarIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_PREVIEW_KEY,
		name = "Preview",
		description = "Show the bar with a sample opponent while you aren't fighting anything, to try your settings.",
		position = -1,
		hidden = true
	)
	default boolean showPreview()
	{
		return false;
	}

	@ConfigItem(
		keyName = CUSTOM_ICON_ITEM_ID_KEY,
		name = "Custom icon item",
		description = "The item whose icon the Custom theme shows, wherever Icon placement puts it."
			+ " Set by right-clicking the bar and choosing \"Choose custom icon\".",
		hidden = true
	)
	default int customIconItemId()
	{
		return -1;
	}

	@ConfigItem(
		keyName = CUSTOM_ICON_SPRITE_ID_KEY,
		name = "Custom icon sprite",
		description = "The game sprite the Custom theme shows, wherever Icon placement puts it. A sprite wins"
			+ " over an item, and \"Choose custom icon\" only ever sets one of the two.",
		hidden = true
	)
	default int customIconSpriteId()
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
		description = "The colors of the bar. Custom starts from the theme you had and uses the Custom colors section. With Use Oldschool theme on, only Custom is used, to color the plain bar.",
		position = 0,
		hidden = true
	)
	default HealthBarTheme theme()
	{
		return DEFAULT_THEME;
	}

	@ConfigItem(
		keyName = OLDSCHOOL_THEME_KEY,
		name = "Use Oldschool theme",
		description = "A plain green bar over red, like the game's own. Turns off Rare gold bars until you untick it. Pick the Custom theme to choose its colors. With Match boss colors on, the bar takes the boss's color instead of green.",
		position = 1,
		hidden = true
	)
	default boolean oldschoolTheme()
	{
		return false;
	}

	@ConfigItem(
		keyName = MATCH_BOSS_COLORS_KEY,
		name = "Match boss colors",
		description = "Color the bar to suit the boss you're fighting, on any theme.",
		position = 2,
		hidden = true
	)
	default boolean matchBossColors()
	{
		return false;
	}

	@ConfigItem(
		keyName = PHASE_COLORS_KEY,
		name = "Phase colors",
		description = "Some bosses turn the game's own health bar another color while they shield themselves or"
			+ " change phase. Follow that color instead of losing it. It takes over from Match boss colors"
			+ " while the phase lasts.",
		position = 3,
		hidden = true
	)
	default boolean phaseColors()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = PHASE_COLOR_KEY,
		name = "Phase color",
		description = "The color to show a special phase in instead of the game's. Leave it transparent to use"
			+ " the game's own color.",
		position = 4,
		hidden = true
	)
	default Color phaseColor()
	{
		return TRANSPARENT;
	}

	@ConfigItem(
		keyName = RARE_GOLD_BARS_KEY,
		name = "Rare gold bars",
		description = "About 1 in 250 bars turns gold, only for looks. Ticking this turns off Use Oldschool theme.",
		position = 5,
		hidden = true
	)
	default boolean rareGoldBars()
	{
		return true;
	}

	@ConfigItem(
		keyName = ICON_PLACEMENT_KEY,
		name = "Icon placement",
		description = "Where the icon goes. Crests puts one at each end of the bar, Crest left and Crest right only"
			+ " on that end, and Before the name puts a small one there. With Use Oldschool theme on it always"
			+ " goes before the name.",
		position = 6,
		hidden = true
	)
	default BarIconPlacement iconPlacement()
	{
		return BarIconPlacement.CRESTS;
	}

	@ConfigItem(
		keyName = USE_BOSS_ICON_KEY,
		name = "Use boss icon",
		description = "Use the icon of the boss you're fighting instead of the theme's. Raid bosses show their pet.",
		position = 7,
		hidden = true
	)
	default boolean useBossIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = BAR_ENDS_KEY,
		name = "Bar ends",
		description = "The ornament at each end of the bar. A crest covers the end it sits on, so an end only shows this when Icon placement leaves it free. Not used with Use Oldschool theme on.",
		position = 8,
		hidden = true
	)
	default BarEnds barEnds()
	{
		return BarEnds.CLASSIC;
	}

	@ConfigItem(
		keyName = SHOW_PHASE_MARKERS_KEY,
		name = "Show phase markers",
		description = "Show the same phase markers as the game's own boss health bar, when it has them.",
		position = 10,
		hidden = true
	)
	default boolean showPhaseMarkers()
	{
		return true;
	}

	@Range(min = 200, max = 1400)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = BAR_WIDTH_KEY,
		name = "Bar width",
		description = "The width of the bar. Alt-dragging its edge overrides this until you reset the overlay.",
		position = 11,
		hidden = true
	)
	default int barWidth()
	{
		return 600;
	}

	@Range(min = 4, max = 24)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = BAR_HEIGHT_KEY,
		name = "Bar height",
		description = "The height of the bar itself, without the text around it.",
		position = 12,
		hidden = true
	)
	default int barHeight()
	{
		return 7;
	}

	@ConfigItem(
		keyName = FIT_TO_GAME_VIEW_KEY,
		name = "Fit to game view",
		description = "Make the bar narrower when the game view is small. The height and text keep their size.",
		position = 13,
		hidden = true
	)
	default boolean fitToGameView()
	{
		return true;
	}

	@ConfigItem(
		keyName = FONT_KEY,
		name = "Name font",
		description = "The font and size of the name.",
		position = 0,
		section = fontsSection
	)
	default FontType font()
	{
		return DEFAULT_FONT;
	}

	@ConfigItem(
		keyName = DAMAGE_NUMBER_FONT_KEY,
		name = "Damage number font",
		description = "The font and size of the damage number.",
		position = 1,
		section = fontsSection
	)
	default FontType damageNumberFont()
	{
		return FontType.REGULAR;
	}

	@ConfigItem(
		keyName = COMBAT_LEVEL_FONT_KEY,
		name = "Combat level font",
		description = "The font and size of the combat level next to the name.",
		position = 2,
		section = fontsSection
	)
	default FontType combatLevelFont()
	{
		return FontType.SMALL;
	}

	@ConfigItem(
		keyName = HITPOINTS_FONT_KEY,
		name = "Hitpoints font",
		description = "The font and size of the hitpoints text and the \"Defeated\" label.",
		position = 3,
		section = fontsSection
	)
	default FontType hitpointsFont()
	{
		return FontType.SMALL;
	}

	@ConfigItem(
		keyName = KILL_COUNT_FONT_KEY,
		name = "Kill count font",
		description = "The font and size of the kill count.",
		position = 4,
		section = fontsSection
	)
	default FontType killCountFont()
	{
		return FontType.SMALL;
	}

	@ConfigItem(
		keyName = PARTY_DEFENCE_FONT_KEY,
		name = "Party defence font",
		description = "The font and size of the party defence and magic defence. Their icons follow the size.",
		position = 5,
		section = fontsSection
	)
	default FontType partyDefenceFont()
	{
		return FontType.SMALL;
	}

	@ConfigItem(
		keyName = SPECIAL_ATTACK_COUNTS_FONT_KEY,
		name = "Special attack counts font",
		description = "The font and size of the special attack counts. The weapon icons follow the size.",
		position = 6,
		section = fontsSection
	)
	default FontType specialAttackCountsFont()
	{
		return FontType.SMALL;
	}

	@ConfigItem(
		keyName = WEAKNESS_FONT_KEY,
		name = "Elemental weakness font",
		description = "The font and size of the elemental weakness. The rune icon follows the size.",
		position = 7,
		section = fontsSection
	)
	default FontType weaknessFont()
	{
		return FontType.SMALL;
	}

	@ConfigItem(
		keyName = DRAIN_CAP_FONT_KEY,
		name = "Defence drain limit font",
		description = "The font and size of the defence drain limit. The icon follows the size.",
		position = 8,
		section = fontsSection
	)
	default FontType drainCapFont()
	{
		return FontType.SMALL;
	}

	@ConfigItem(
		keyName = FIGHT_TIMER_FONT_KEY,
		name = "Fight timer font",
		description = "The font and size of the fight timer.",
		position = 9,
		section = fontsSection
	)
	default FontType fightTimerFont()
	{
		return FontType.SMALL;
	}

	@ConfigItem(
		keyName = "smoothText",
		name = "Smooth text",
		description = "Smooth the edges of the text. Turn off for sharp text when the game is scaled up (for example with xBR). The RuneScape fonts are never smoothed.",
		position = 10,
		section = fontsSection
	)
	default boolean smoothText()
	{
		return true;
	}

	@ConfigItem(
		keyName = "textOutline",
		name = "Text edge",
		description = "How text is lifted off the bar: a drop shadow, an outline all round, or nothing.",
		position = 11,
		section = fontsSection
	)
	default TextOutline textOutline()
	{
		return TextOutline.SHADOW;
	}

	@ConfigItem(
		keyName = SHOW_BOSS_NAME_KEY,
		name = "Show name",
		description = "Show the opponent's name.",
		position = 0,
		hidden = true
	)
	default boolean showBossName()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_COMBAT_LEVEL_KEY,
		name = "Show combat level",
		description = "Show the opponent's combat level next to its name.",
		position = 1,
		hidden = true
	)
	default boolean showCombatLevel()
	{
		return false;
	}

	@ConfigItem(
		keyName = HITPOINTS_TEXT_MODE_KEY,
		name = "Hitpoints text",
		description = "What to show: a percentage, the hitpoints out of the maximum, the hitpoints left on their"
			+ " own, either of those with the percentage after it, or nothing. Shows a percentage when the max"
			+ " hitpoints aren't known.",
		position = 2,
		hidden = true
	)
	default HitpointsTextMode hitpointsTextMode()
	{
		return HitpointsTextMode.NONE;
	}

	@Range(max = HealthText.MAX_DECIMALS)
	@ConfigItem(
		keyName = PERCENT_DECIMALS_KEY,
		name = "Percentage decimals",
		description = "How many decimals the percentage shows. Some opponents only tell their health in rough steps,"
			+ " so the last decimals can jump instead of counting down.",
		position = 2,
		hidden = true
	)
	default int percentDecimals()
	{
		return 0;
	}

	@ConfigItem(
		keyName = SHOW_DAMAGE_NUMBER_KEY,
		name = "Show damage number",
		description = "Show the damage of the latest attack. Hits that land together are added up.",
		position = 3,
		hidden = true
	)
	default boolean showDamageNumber()
	{
		return true;
	}

	@ConfigItem(
		keyName = DAMAGE_NUMBER_SOURCE_KEY,
		name = "Damage number counts",
		description = "Me shows your latest attack. Party adds up your party's hits while they keep coming, for members who also use this plugin.",
		position = 4,
		hidden = true
	)
	default DamageNumberSource damageNumberSource()
	{
		return DamageNumberSource.ME;
	}

	@ConfigItem(
		keyName = SHOW_KILL_COUNT_KEY,
		name = "Kill count",
		description = "Show your kill count for the boss. Needs RuneLite's Chat Commands plugin to have seen a kill count message for it.",
		position = 0,
		hidden = true
	)
	default boolean showKillCount()
	{
		return false;
	}

	@ConfigItem(
		keyName = SHOW_WEAKNESS_KEY,
		name = "Elemental weakness",
		description = "Show the boss's elemental weakness and extra damage, for example +40%. Values from the OSRS Wiki.",
		position = 4,
		hidden = true
	)
	default boolean showWeakness()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_DRAIN_CAP_KEY,
		name = "Defence drain limit",
		description = "Show how far the boss's defence can be lowered in total, or \"no drain\". Values from the OSRS Wiki.",
		position = 5,
		hidden = true
	)
	default boolean showDrainCap()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_SPECIAL_ATTACK_COUNTS_KEY,
		name = "Special attack counts",
		description = "Show the counts from RuneLite's Special Attack Counter plugin. Needs that plugin on with its info boxes.",
		position = 3,
		hidden = true
	)
	default boolean showSpecialAttackCounts()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_PARTY_DEFENCE_KEY,
		name = "Party defence",
		description = "Show the defence from the Party Defence Tracker or Better Party Defence plugin's info box. Needs one of them installed and on.",
		position = 1,
		hidden = true
	)
	default boolean showPartyDefence()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_MAGIC_DEFENCE_KEY,
		name = "Magic defence",
		description = "Show the magic defence from the Better Party Defence plugin's Magic defence info box. Needs that plugin with its Magic defence info box on.",
		position = 2,
		hidden = true
	)
	default boolean showMagicDefence()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_FIGHT_TIMER_KEY,
		name = "Fight timer",
		description = "Show how long the fight has lasted, from when you go for the opponent and are near it. It stops once the opponent is gone.",
		position = 6,
		hidden = true
	)
	default boolean showFightTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = SHOW_PARTNER_BAR_KEY,
		name = "Second boss bar",
		description = "Shows a smaller bar for another boss you're fighting at the same time, with its last known health while it's away.",
		position = 7,
		hidden = true
	)
	default boolean showPartnerBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = SECOND_BAR_SEPARATE_KEY,
		name = "Second bar on its own",
		description = "Give the second bar its own place on screen, so it can be dragged away from the first one. Drag it with alt held, like any overlay.",
		position = 8,
		hidden = true
	)
	default boolean secondBarSeparate()
	{
		return false;
	}

	@Range(min = 20, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = SECOND_BAR_SCALE_KEY,
		name = "Second bar scale",
		description = "How tall the second bar is next to the first one, as a share of the bar height.",
		position = 9,
		hidden = true
	)
	default int secondBarScale()
	{
		return 50;
	}

	@ConfigItem(
		keyName = SHOW_DAMAGE_TRAIL_KEY,
		name = "Damage trail",
		description = "After a hit, keep the lost health visible as a lighter section before it drains away.",
		position = 0,
		hidden = true
	)
	default boolean showDamageTrail()
	{
		return true;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = ANIMATION_SPEED_KEY,
		name = "Heal speed",
		description = "How quickly the bar refills when the opponent heals. Higher is faster.",
		position = 1,
		hidden = true
	)
	default int animationSpeed()
	{
		return 6;
	}

	@ConfigItem(
		keyName = LOW_HEALTH_EFFECT_KEY,
		name = "Low health effect",
		description = "Make the fill pulse and glow at or below the low health threshold.",
		position = 2,
		hidden = true
	)
	default boolean lowHealthEffect()
	{
		return true;
	}

	@Range(min = 5, max = 50)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = LOW_HEALTH_THRESHOLD_KEY,
		name = "Low health threshold",
		description = "The health at or below which the low health effect starts.",
		position = 3,
		hidden = true
	)
	default int lowHealthThreshold()
	{
		return 25;
	}

	@ConfigItem(
		keyName = FLASH_ON_BIG_HITS_KEY,
		name = "Flash on big hits",
		description = "Briefly flash the border when a hit takes a large part of the health.",
		position = 4,
		hidden = true
	)
	default boolean flashOnBigHits()
	{
		return false;
	}

	@ConfigItem(
		keyName = INTRO_ANIMATION_KEY,
		name = "Intro animation",
		description = "How the bar appears for a new opponent.",
		position = 5,
		hidden = true
	)
	default IntroAnimation introAnimation()
	{
		return IntroAnimation.SLIDE_AND_EXPAND;
	}

	@ConfigItem(
		keyName = SHOW_DEFEAT_ANIMATION_KEY,
		name = "Defeat animation",
		description = "Hold the empty bar with a \"Defeated\" label for a moment before it goes.",
		position = 6,
		hidden = true
	)
	default boolean showDefeatAnimation()
	{
		return true;
	}

	@ConfigItem(
		keyName = BURN_AWAY_DEFEAT_KEY,
		name = "Burn away",
		description = "Burn the bar away from right to left instead of fading it out. Only used with Defeat animation on.",
		position = 7,
		hidden = true
	)
	default boolean burnAwayDefeat()
	{
		return false;
	}

	@ConfigItem(
		keyName = FORTIFY_PHASE_KEY,
		name = "Fortify",
		description = "Mark the moment a boss goes into a phase: a flash and a sweep across the bar, a shield at"
			+ " the ends and the frame in the phase's color. Needs Phase colors, and does nothing with the"
			+ " Oldschool theme.",
		position = 8,
		hidden = true
	)
	default boolean fortifyPhase()
	{
		return false;
	}

	@ConfigItem(
		keyName = NAME_POSITION_KEY,
		name = "Name",
		description = "Where the name goes, with its icon and combat level. It's shortened when it doesn't fit.",
		position = 0,
		hidden = true
	)
	default BarPosition namePosition()
	{
		return BarPosition.TOP_LEFT;
	}

	@ConfigItem(
		keyName = DAMAGE_NUMBER_POSITION_KEY,
		name = "Damage number",
		description = "Where the damage number goes.",
		position = 1,
		hidden = true
	)
	default BarPosition damageNumberPosition()
	{
		return BarPosition.TOP_RIGHT;
	}

	@ConfigItem(
		keyName = HITPOINTS_POSITION_KEY,
		name = "Hitpoints",
		description = "Where the hitpoints text goes. \"Defeated\" shows centred on the same row.",
		position = 2,
		hidden = true
	)
	default BarPosition hitpointsPosition()
	{
		return BarPosition.BOTTOM_RIGHT;
	}

	@ConfigItem(
		keyName = KILL_COUNT_POSITION_KEY,
		name = "Kill count",
		description = "Where the kill count goes.",
		position = 3,
		hidden = true
	)
	default BarPosition killCountPosition()
	{
		return BarPosition.BOTTOM_LEFT;
	}

	@ConfigItem(
		keyName = PARTY_DEFENCE_POSITION_KEY,
		name = "Party defence",
		description = "Where the party defence and magic defence go.",
		position = 4,
		hidden = true
	)
	default BarPosition partyDefencePosition()
	{
		return BarPosition.BOTTOM_LEFT;
	}

	@ConfigItem(
		keyName = SPECIAL_ATTACK_COUNTS_POSITION_KEY,
		name = "Special attack counts",
		description = "Where the special attack counts go.",
		position = 5,
		hidden = true
	)
	default BarPosition specialAttackCountsPosition()
	{
		return BarPosition.BOTTOM_LEFT;
	}

	@ConfigItem(
		keyName = WEAKNESS_POSITION_KEY,
		name = "Elemental weakness",
		description = "Where the elemental weakness goes.",
		position = 6,
		hidden = true
	)
	default BarPosition weaknessPosition()
	{
		return BarPosition.BOTTOM_LEFT;
	}

	@ConfigItem(
		keyName = DRAIN_CAP_POSITION_KEY,
		name = "Defence drain limit",
		description = "Where the defence drain limit goes.",
		position = 7,
		hidden = true
	)
	default BarPosition drainCapPosition()
	{
		return BarPosition.BOTTOM_LEFT;
	}

	@ConfigItem(
		keyName = FIGHT_TIMER_POSITION_KEY,
		name = "Fight timer",
		description = "Where the fight timer goes.",
		position = 8,
		hidden = true
	)
	default BarPosition fightTimerPosition()
	{
		return BarPosition.TOP_CENTER;
	}

	@Alpha
	@ConfigItem(
		keyName = CUSTOM_FILL_HIGH_COLOR_KEY,
		name = "Fill (full health)",
		description = "The fill color at full health. It blends towards the low health color as health drops,"
			+ " and tints the crests and the other bars the game shows next to the boss bar.",
		position = 1,
		hidden = true
	)
	default Color customFillHighColor()
	{
		return new Color(0x96161A);
	}

	@Alpha
	@ConfigItem(
		keyName = CUSTOM_FILL_LOW_COLOR_KEY,
		name = "Fill (low health)",
		description = "The fill color near zero health. Use the full health color for a single color.",
		position = 2,
		hidden = true
	)
	default Color customFillLowColor()
	{
		return new Color(0x96161A);
	}

	@Alpha
	@ConfigItem(
		keyName = CUSTOM_TRACK_COLOR_KEY,
		name = "Background",
		description = "The color behind the fill, where the health is missing."
			+ " Fully transparent keeps the dark background the themes use.",
		position = 3,
		hidden = true
	)
	default Color customTrackColor()
	{
		return TRANSPARENT;
	}

	@Alpha
	@ConfigItem(
		keyName = CUSTOM_TRAIL_COLOR_KEY,
		name = "Damage trail color",
		description = "The color of the damage trail.",
		position = 4,
		hidden = true
	)
	default Color customTrailColor()
	{
		return new Color(0xD6B254);
	}

	@Alpha
	@ConfigItem(
		keyName = CUSTOM_FRAME_COLOR_KEY,
		name = "Frame",
		description = "The color of the frame, the bar ends, the crests, the underline, the phase markers"
			+ " and the outline of the other bars the game shows.",
		position = 5,
		hidden = true
	)
	default Color customFrameColor()
	{
		return new Color(0x7A6C54);
	}

	@Alpha
	@ConfigItem(
		keyName = CUSTOM_TEXT_COLOR_KEY,
		name = "Name and damage text",
		description = "The color of the name, the delve level and the damage number, on both bars, and of the party"
			+ " and magic defence and special attack counts when they have no color of their own.",
		position = 6,
		hidden = true
	)
	default Color customTextColor()
	{
		return new Color(0xE8E2D4);
	}

	@Alpha
	@ConfigItem(
		keyName = CUSTOM_LEVEL_TEXT_COLOR_KEY,
		name = "Level and info text",
		description = "The color of the combat level, fight timer, kill count, elemental weakness and defence"
			+ " drain limit, which share one color and are drawn a little dimmer than the name.",
		position = 7,
		hidden = true
	)
	default Color customLevelTextColor()
	{
		return new Color(0xA99F90);
	}

	@Alpha
	@ConfigItem(
		keyName = CUSTOM_HITPOINTS_TEXT_COLOR_KEY,
		name = "Hitpoints text",
		description = "The color of the hitpoints text on both bars, and of the labels on the other bars the game shows.",
		position = 8,
		hidden = true
	)
	default Color customHitpointsTextColor()
	{
		return new Color(0xC8C0B0);
	}

	@Alpha
	@ConfigItem(
		keyName = CUSTOM_DEFEATED_TEXT_COLOR_KEY,
		name = "Defeated text",
		description = "The color of the \"Defeated\" label on both bars.",
		position = 9,
		hidden = true
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
		description = "With Only show for bosses on, also show the bar for superior slayer monsters.",
		position = 3,
		section = whenToShowSection
	)
	default boolean showSuperiors()
	{
		return true;
	}

	@ConfigItem(
		keyName = ALSO_SHOW_FOR_KEY,
		name = "Also show for",
		description = "Also show the bar for these NPCs. Names or IDs, separated by commas or one per line. * matches anything.",
		position = 4,
		section = whenToShowSection
	)
	default String alsoShowFor()
	{
		return "";
	}

	@ConfigItem(
		keyName = NEVER_SHOW_FOR_KEY,
		name = "Never show for",
		description = "Never show the bar for these NPCs, even bosses. Names or IDs, separated by commas or one per line. * matches anything.",
		position = 5,
		section = whenToShowSection
	)
	default String neverShowFor()
	{
		return "";
	}

	@Range(min = 1, max = 60)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "hideDelay",
		name = "Hide after",
		description = "How long the bar stays once the fight goes quiet. It stays while the game's boss health bar shows the opponent.",
		position = 6,
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
		position = 7,
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
		position = 8,
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
		position = 9,
		section = whenToShowSection
	)
	default boolean showHealthIndicatorMarkers()
	{
		return true;
	}
}
