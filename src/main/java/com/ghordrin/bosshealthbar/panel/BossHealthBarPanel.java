package com.ghordrin.bosshealthbar.panel;

import com.ghordrin.bosshealthbar.BossHealthBarPlugin;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.config.ConfigWrites;
import com.ghordrin.bosshealthbar.config.Pickers;
import com.ghordrin.bosshealthbar.config.SettingsTransfer;
import com.ghordrin.bosshealthbar.data.DamageNumberSource;
import com.ghordrin.bosshealthbar.debug.DebugExport;
import com.ghordrin.bosshealthbar.render.BarEnds;
import com.ghordrin.bosshealthbar.render.BarIconPlacement;
import com.ghordrin.bosshealthbar.render.BarPosition;
import com.ghordrin.bosshealthbar.render.HitpointsTextMode;
import com.ghordrin.bosshealthbar.render.IntroAnimation;
import com.ghordrin.bosshealthbar.theme.HealthBarTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ColorJButton;
import net.runelite.client.ui.components.TitleCaseListCellRenderer;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.SwingUtil;

// The plugin's own settings panel. It exists because the client's config panel never re-reads a setting that
// was written from code: a theme filling in the custom colors, or the Oldschool tick turning off the gold
// bars, left the config panel showing the old value until it was closed and opened again. It is laid out the
// way the client lays its own config panel out, so the two read as one thing.
@Slf4j
@Singleton
public class BossHealthBarPanel extends PluginPanel
{
	private static final int BORDER_OFFSET = 3;
	private static final int SPINNER_FIELD_WIDTH = 6;
	private static final int COMBO_HEIGHT = 22;
	// The client's own icons. Their paths are not API, so a renamed one leaves a control with its text
	// rather than stopping the panel from being built.
	private final ImageIcon sectionExpandIcon;
	private final ImageIcon sectionRetractIcon;
	private final ImageIcon configIcon;
	private final ImageIcon discordIcon;

	private final ConfigManager configManager;
	private final BossHealthBarConfig config;
	private final ColorPickerManager colorPickerManager;
	private final Pickers pickers;
	private final SettingsTransfer settingsTransfer;
	private final DebugExport debugExport;
	private final EventBus eventBus;
	private final BossHealthBarPlugin plugin;

	private final TitleCaseListCellRenderer listCellRenderer = new TitleCaseListCellRenderer();

	// Puts every control back to what the settings now say. One per control, in the order they were built.
	private final List<Runnable> refreshers = new ArrayList<>();

	// True while this panel is the one writing, so a setting it just wrote doesn't bounce back as a rebuild.
	private boolean writing;
	// True while the controls are being put back, so setting a control doesn't read as the user using it.
	private boolean refreshing;

	@Inject
	BossHealthBarPanel(ConfigManager configManager, BossHealthBarConfig config, ColorPickerManager colorPickerManager,
		Pickers pickers, SettingsTransfer settingsTransfer, DebugExport debugExport, EventBus eventBus,
		BossHealthBarPlugin plugin)
	{
		super(false);
		this.configManager = configManager;
		this.config = config;
		this.colorPickerManager = colorPickerManager;
		this.pickers = pickers;
		this.settingsTransfer = settingsTransfer;
		this.debugExport = debugExport;
		this.eventBus = eventBus;
		this.plugin = plugin;

		final BufferedImage arrow = clientImage("/util/arrow_right.png");
		final BufferedImage dimmed = arrow != null ? ImageUtil.luminanceOffset(arrow, -121) : null;
		sectionExpandIcon = dimmed != null ? new ImageIcon(dimmed) : null;
		sectionRetractIcon = dimmed != null ? new ImageIcon(ImageUtil.rotateImage(dimmed, Math.PI / 2)) : null;
		configIcon = clientIcon("/net/runelite/client/plugins/config/config_edit_icon.png");
		discordIcon = clientIcon("/net/runelite/client/plugins/info/discord_icon.png");

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel settingsPanel = new FixedWidthPanel();
		settingsPanel.setBorder(new EmptyBorder(8, 10, 10, 10));
		settingsPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
		settingsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		// Above the sections, the way it sits above them on the settings screen: it is what you turn on
		// while you work through everything below it.
		checkbox(settingsPanel, "Preview",
			"Show the bar with a sample opponent while you aren't fighting anything, to try your settings.",
			BossHealthBarConfig.SHOW_PREVIEW_KEY, config::showPreview);
		settingsPanel.add(section("Look", true, this::lookRows));
		settingsPanel.add(section("Text", false, this::textRows));
		settingsPanel.add(section("Boss info", false, this::bossInfoRows));
		settingsPanel.add(section("Animations", false, this::animationRows));
		settingsPanel.add(section("Layout", false, this::layoutRows));
		settingsPanel.add(section("Custom colors", false, this::colorRows));
		settingsPanel.add(section("Backup", false, this::backupRows));

		final JPanel northPanel = new FixedWidthPanel();
		northPanel.setLayout(new BorderLayout());
		northPanel.add(settingsPanel, BorderLayout.NORTH);

		final JScrollPane scrollPane = new JScrollPane(northPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(topPanel(), BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	private JPanel topPanel()
	{
		final JPanel top = new JPanel();
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);
		top.setBorder(new EmptyBorder(10, 10, 10, 10));
		top.setLayout(new BorderLayout(0, BORDER_OFFSET));

		final JLabel title = new JLabel("Boss Health Bar");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());

		final JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttons.add(iconButton(configIcon, "Settings", "The rest of this plugin's settings", this::openSettings));
		buttons.add(iconButton(discordIcon, "Discord", "Help, bug reports and sharing setups, on Discord",
			() -> LinkBrowser.browse(BossHealthBarPlugin.DISCORD_URL)));

		final JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		titleRow.add(title, BorderLayout.CENTER);
		titleRow.add(buttons, BorderLayout.EAST);

		// Kept where they can be reached whatever is open below, since a setup is loaded and saved as a whole.
		final JPanel transfer = new JPanel(new GridLayout(1, 2, 6, 0));
		transfer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		transfer.setBorder(new EmptyBorder(8, 0, 0, 0));
		transfer.add(plainButton("Import", "Load a saved settings file", settingsTransfer::startImport));
		transfer.add(plainButton("Export", "Save every setting to a file and your clipboard",
			settingsTransfer::export));

		top.add(titleRow, BorderLayout.NORTH);
		top.add(transfer, BorderLayout.CENTER);
		return top;
	}

	private static ImageIcon clientIcon(String path)
	{
		final BufferedImage image = clientImage(path);
		return image != null ? new ImageIcon(image) : null;
	}

	private static BufferedImage clientImage(String path)
	{
		try
		{
			return ImageUtil.loadImageResource(BossHealthBarPanel.class, path);
		}
		catch (RuntimeException e)
		{
			log.debug("The client has no image at {}", path, e);
			return null;
		}
	}

	private JButton plainButton(String label, String tooltip, Runnable onClick)
	{
		final JButton button = new JButton(label);
		button.setToolTipText(tooltip);
		button.setFocusable(false);
		button.addActionListener(e -> onClick.run());
		return button;
	}

	// With the client's icon missing the button keeps its text, so the control is still there to press.
	private JButton iconButton(ImageIcon icon, String label, String tooltip, Runnable onClick)
	{
		final JButton button = icon != null ? new JButton(icon) : new JButton(label);
		SwingUtil.removeButtonDecorations(button);
		button.setToolTipText(tooltip);
		button.setBackground(ColorScheme.DARK_GRAY_COLOR);
		button.addActionListener(e -> onClick.run());
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				button.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				button.setBackground(ColorScheme.DARK_GRAY_COLOR);
			}
		});
		return button;
	}

	// The client has no way for a plugin to open its own settings, but it does open them for an overlay whose
	// Configure entry was clicked. The overlay here is never drawn: it only carries the plugin that names them.
	private void openSettings()
	{
		eventBus.post(new OverlayMenuClicked(
			new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, "Configure", BossHealthBarPlugin.NAME),
			new PluginOverlay(plugin)));
	}

	private void lookRows(JPanel body)
	{
		combo(body, "Theme", "The colors of the bar. Custom starts from the theme you had and uses the Custom"
			+ " colors section. Not used with Use Oldschool theme on.",
			BossHealthBarConfig.THEME_KEY, HealthBarTheme.class, config::theme);
		checkbox(body, "Oldschool theme", "A plain green bar over red, like the game's own. Turns off Rare gold"
			+ " bars until you untick it. The Custom colors aren't used either. With Match boss colors on, the"
			+ " bar takes the boss's color instead of green.",
			BossHealthBarConfig.OLDSCHOOL_THEME_KEY, config::oldschoolTheme);
		checkbox(body, "Match boss colors", "Color the bar to suit the boss you're fighting, on any theme.",
			BossHealthBarConfig.MATCH_BOSS_COLORS_KEY, config::matchBossColors);
		checkbox(body, "Phase colors", "Some bosses turn the game's own health bar another color while they shield"
			+ " themselves or change phase. Follow that color instead of losing it. It takes over from Match boss"
			+ " colors while the phase lasts.",
			BossHealthBarConfig.PHASE_COLORS_KEY, config::phaseColors);
		color(body, "Phase color", "Show a phase in this color instead of the game's. Transparent uses the game's.",
			BossHealthBarConfig.PHASE_COLOR_KEY, config::phaseColor);
		checkbox(body, "Rare gold bars", "About 1 in 250 bars turns gold, only for looks. Ticking this turns off"
			+ " Use Oldschool theme.",
			BossHealthBarConfig.RARE_GOLD_BARS_KEY, config::rareGoldBars);
		checkbox(body, "Show phase markers", "Show the same phase markers as the game's own boss health bar, when"
			+ " it has them.",
			BossHealthBarConfig.SHOW_PHASE_MARKERS_KEY, config::showPhaseMarkers);
		combo(body, "Icon placement", "Where the icon goes. Crests puts one at each end of the bar, Crest left and"
			+ " Crest right only on that end, and Before the name puts a small one there. With Use Oldschool theme"
			+ " on it always goes before the name.",
			BossHealthBarConfig.ICON_PLACEMENT_KEY, BarIconPlacement.class, config::iconPlacement);
		checkbox(body, "Use boss icon", "Use the boss's icon instead of the theme's.",
			BossHealthBarConfig.USE_BOSS_ICON_KEY, config::useBossIcon);
		combo(body, "Bar ends", "The ornament at each end of the bar. A crest covers the end it sits on, so an end"
			+ " only shows this when Icon placement leaves it free. Not used with Use Oldschool theme on.",
			BossHealthBarConfig.BAR_ENDS_KEY, BarEnds.class, config::barEnds);
		spinner(body, "Bar width", "The width of the bar, in pixels. Alt-dragging its edge overrides this until"
			+ " you reset the overlay.",
			BossHealthBarConfig.BAR_WIDTH_KEY, 200, 1400, config::barWidth);
		spinner(body, "Bar height", "The height of the bar itself in pixels, without the text around it.",
			BossHealthBarConfig.BAR_HEIGHT_KEY, 4, 24, config::barHeight);
		checkbox(body, "Fit to game view", "Make the bar narrower when the game view is small. The height and text"
			+ " keep their size.",
			BossHealthBarConfig.FIT_TO_GAME_VIEW_KEY, config::fitToGameView);
		button(body, "Choose fill texture", "Pick one of the game's textures to lay over the fill.",
			pickers::openFillTexturePicker);
	}

	private void textRows(JPanel body)
	{
		checkbox(body, "Show name", "Show the opponent's name.",
			BossHealthBarConfig.SHOW_BOSS_NAME_KEY, config::showBossName);
		checkbox(body, "Show combat level", "Show the opponent's combat level next to its name.",
			BossHealthBarConfig.SHOW_COMBAT_LEVEL_KEY, config::showCombatLevel);
		combo(body, "Hitpoints text", "What to show: a percentage, the hitpoints out of the maximum, the hitpoints"
			+ " left on their own, either of those with the percentage after it, or nothing. Shows a percentage"
			+ " when the max hitpoints aren't known.",
			BossHealthBarConfig.HITPOINTS_TEXT_MODE_KEY, HitpointsTextMode.class, config::hitpointsTextMode);
		checkbox(body, "Show damage number", "Show the damage of the latest attack. Hits that land together are"
			+ " added up.", BossHealthBarConfig.SHOW_DAMAGE_NUMBER_KEY, config::showDamageNumber);
		combo(body, "Damage number counts", "Me shows your latest attack. Party adds up your party's hits while"
			+ " they keep coming, for members who also use this plugin.",
			BossHealthBarConfig.DAMAGE_NUMBER_SOURCE_KEY, DamageNumberSource.class, config::damageNumberSource);
	}

	private void bossInfoRows(JPanel body)
	{
		checkbox(body, "Kill count", "Show your kill count for the boss. Needs RuneLite's Chat Commands plugin to"
			+ " have seen a kill count message for it.",
			BossHealthBarConfig.SHOW_KILL_COUNT_KEY, config::showKillCount);
		checkbox(body, "Party defence", "Show the defence from the Party Defence Tracker or Better Party Defence"
			+ " plugin's info box. Needs one of them installed and on.",
			BossHealthBarConfig.SHOW_PARTY_DEFENCE_KEY, config::showPartyDefence);
		checkbox(body, "Magic defence", "Show the magic defence from the Better Party Defence plugin's Magic"
			+ " defence info box. Needs that plugin with its Magic defence info box on.",
			BossHealthBarConfig.SHOW_MAGIC_DEFENCE_KEY, config::showMagicDefence);
		checkbox(body, "Special attack counts", "Show the counts from RuneLite's Special Attack Counter plugin."
			+ " Needs that plugin on with its info boxes.",
			BossHealthBarConfig.SHOW_SPECIAL_ATTACK_COUNTS_KEY, config::showSpecialAttackCounts);
		checkbox(body, "Elemental weakness", "Show the boss's elemental weakness and extra damage, for example"
			+ " +40%. Values from the OSRS Wiki.", BossHealthBarConfig.SHOW_WEAKNESS_KEY, config::showWeakness);
		checkbox(body, "Defence drain limit", "Show how far the boss's defence can be lowered in total, or"
			+ " \"no drain\". Values from the OSRS Wiki.",
			BossHealthBarConfig.SHOW_DRAIN_CAP_KEY, config::showDrainCap);
		checkbox(body, "Fight timer", "Show how long the fight has lasted, from when you go for the opponent and"
			+ " are near it. It stops once the opponent is gone.",
			BossHealthBarConfig.SHOW_FIGHT_TIMER_KEY, config::showFightTimer);
		checkbox(body, "Second boss bar", "A smaller bar for another boss you're fighting at the same time, with"
			+ " its last known health while it's away.",
			BossHealthBarConfig.SHOW_PARTNER_BAR_KEY, config::showPartnerBar);
		checkbox(body, "Second bar on its own", "Give the second bar its own place on screen, so it can be dragged"
			+ " away from the first one. Drag it with alt held, like any overlay.",
			BossHealthBarConfig.SECOND_BAR_SEPARATE_KEY, config::secondBarSeparate);
	}

	private void animationRows(JPanel body)
	{
		checkbox(body, "Damage trail", "After a hit, keep the lost health visible as a lighter section before it"
			+ " drains away.", BossHealthBarConfig.SHOW_DAMAGE_TRAIL_KEY, config::showDamageTrail);
		spinner(body, "Heal speed", "How quickly the bar refills when the opponent heals. Higher is faster.",
			BossHealthBarConfig.ANIMATION_SPEED_KEY, 1, 10, config::animationSpeed);
		checkbox(body, "Low health effect", "Make the fill pulse and glow at or below the low health threshold.",
			BossHealthBarConfig.LOW_HEALTH_EFFECT_KEY, config::lowHealthEffect);
		spinner(body, "Low health threshold", "The health percentage at or below which the low health effect"
			+ " starts.", BossHealthBarConfig.LOW_HEALTH_THRESHOLD_KEY, 5, 50, config::lowHealthThreshold);
		checkbox(body, "Flash on big hits", "Briefly flash the border when a hit takes a large part of the health.",
			BossHealthBarConfig.FLASH_ON_BIG_HITS_KEY, config::flashOnBigHits);
		combo(body, "Intro animation", "How the bar appears for a new opponent.",
			BossHealthBarConfig.INTRO_ANIMATION_KEY, IntroAnimation.class, config::introAnimation);
		checkbox(body, "Defeat animation", "Hold the empty bar with a \"Defeated\" label for a moment before it"
			+ " goes.", BossHealthBarConfig.SHOW_DEFEAT_ANIMATION_KEY, config::showDefeatAnimation);
		checkbox(body, "Burn away", "Burn the bar away from right to left instead of fading it out. Only used with"
			+ " Defeat animation on.", BossHealthBarConfig.BURN_AWAY_DEFEAT_KEY, config::burnAwayDefeat);
		checkbox(body, "Fortify", "Mark the moment a boss goes into a phase: a flash and a sweep across the bar, a"
			+ " shield at the ends and the frame in the phase's color. Needs Phase colors, and does nothing with"
			+ " the Oldschool theme.",
			BossHealthBarConfig.FORTIFY_PHASE_KEY, config::fortifyPhase);
	}

	private void layoutRows(JPanel body)
	{
		combo(body, "Name", "Where the name goes, with its icon and combat level. It's shortened when it doesn't"
			+ " fit.", BossHealthBarConfig.NAME_POSITION_KEY, BarPosition.class, config::namePosition);
		combo(body, "Damage number", "Where the damage number goes.",
			BossHealthBarConfig.DAMAGE_NUMBER_POSITION_KEY, BarPosition.class, config::damageNumberPosition);
		combo(body, "Hitpoints", "Where the hitpoints text goes. \"Defeated\" shows centred on the same row.",
			BossHealthBarConfig.HITPOINTS_POSITION_KEY, BarPosition.class, config::hitpointsPosition);
		combo(body, "Kill count", "Where the kill count goes.",
			BossHealthBarConfig.KILL_COUNT_POSITION_KEY, BarPosition.class, config::killCountPosition);
		combo(body, "Party defence", "Where the party defence and magic defence go.",
			BossHealthBarConfig.PARTY_DEFENCE_POSITION_KEY, BarPosition.class, config::partyDefencePosition);
		combo(body, "Special attack counts", "Where the special attack counts go.",
			BossHealthBarConfig.SPECIAL_ATTACK_COUNTS_POSITION_KEY, BarPosition.class,
			config::specialAttackCountsPosition);
		combo(body, "Elemental weakness", "Where the elemental weakness goes.",
			BossHealthBarConfig.WEAKNESS_POSITION_KEY, BarPosition.class, config::weaknessPosition);
		combo(body, "Defence drain limit", "Where the defence drain limit goes.",
			BossHealthBarConfig.DRAIN_CAP_POSITION_KEY, BarPosition.class, config::drainCapPosition);
		combo(body, "Fight timer", "Where the fight timer goes.",
			BossHealthBarConfig.FIGHT_TIMER_POSITION_KEY, BarPosition.class, config::fightTimerPosition);
	}

	private void colorRows(JPanel body)
	{
		color(body, "Fill (full health)", "The fill color at full health.",
			BossHealthBarConfig.CUSTOM_FILL_HIGH_COLOR_KEY, config::customFillHighColor);
		color(body, "Fill (low health)", "The fill color near zero health.",
			BossHealthBarConfig.CUSTOM_FILL_LOW_COLOR_KEY, config::customFillLowColor);
		color(body, "Background", "The color behind the fill, where the health is missing.",
			BossHealthBarConfig.CUSTOM_TRACK_COLOR_KEY, config::customTrackColor);
		color(body, "Damage trail", "The color of the damage trail.",
			BossHealthBarConfig.CUSTOM_TRAIL_COLOR_KEY, config::customTrailColor);
		color(body, "Frame", "The frame, the bar ends, the crests and the phase markers.",
			BossHealthBarConfig.CUSTOM_FRAME_COLOR_KEY, config::customFrameColor);
		color(body, "Name and damage", "The name and the damage number.",
			BossHealthBarConfig.CUSTOM_TEXT_COLOR_KEY, config::customTextColor);
		color(body, "Level and info", "The combat level and the info below the bar.",
			BossHealthBarConfig.CUSTOM_LEVEL_TEXT_COLOR_KEY, config::customLevelTextColor);
		color(body, "Hitpoints", "The hitpoints text.",
			BossHealthBarConfig.CUSTOM_HITPOINTS_TEXT_COLOR_KEY, config::customHitpointsTextColor);
		color(body, "Defeated", "The \"Defeated\" text.",
			BossHealthBarConfig.CUSTOM_DEFEATED_TEXT_COLOR_KEY, config::customDefeatedTextColor);
		button(body, "Choose custom icon", "Pick one of the game's items or sprites for the Custom theme's"
			+ " icon.", pickers::openIconPicker);
	}

	private void backupRows(JPanel body)
	{
		button(body, "Paste settings", "Use the settings on your clipboard.",
			settingsTransfer::importFromClipboard);
		button(body, "Save debug log", "Save what the plugin did recently, to attach to a bug report.",
			debugExport::save);
	}

	// A titled block of rows, built the way the client's own config sections are, which the header folds away.
	private JPanel section(String title, boolean open, Consumer<JPanel> rows)
	{
		final JPanel section = new JPanel();
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setMinimumSize(new Dimension(PANEL_WIDTH, 0));

		final JPanel header = new JPanel();
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setLayout(new BorderLayout());
		header.setMinimumSize(new Dimension(PANEL_WIDTH, 0));
		header.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(0, 0, 3, 1)));

		final JButton toggle = new JButton();
		setToggleIcon(toggle, open);
		SwingUtil.removeButtonDecorations(toggle);
		toggle.setPreferredSize(new Dimension(18, 0));
		toggle.setBorder(new EmptyBorder(0, 0, 0, 5));
		toggle.setToolTipText(open ? "Retract" : "Expand");
		header.add(toggle, BorderLayout.WEST);

		final JLabel name = new JLabel(title);
		name.setForeground(ColorScheme.BRAND_ORANGE);
		name.setFont(FontManager.getRunescapeBoldFont());
		header.add(name, BorderLayout.CENTER);

		final JPanel contents = new JPanel();
		contents.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contents.setLayout(new DynamicGridLayout(0, 1, 0, 5));
		contents.setMinimumSize(new Dimension(PANEL_WIDTH, 0));
		contents.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(BORDER_OFFSET, 0, BORDER_OFFSET, 0)));
		contents.setVisible(open);

		// The whole header folds the section, not only the arrow, which is a small target.
		final MouseAdapter click = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggleSection(toggle, contents);
			}
		};
		toggle.addActionListener(e -> toggleSection(toggle, contents));
		name.addMouseListener(click);
		header.addMouseListener(click);

		section.add(header);
		section.add(contents);
		rows.accept(contents);
		return section;
	}

	private void toggleSection(JButton toggle, JPanel contents)
	{
		final boolean open = !contents.isVisible();
		contents.setVisible(open);
		setToggleIcon(toggle, open);
		toggle.setToolTipText(open ? "Retract" : "Expand");
		SwingUtilities.invokeLater(contents::revalidate);
	}

	// Without the client's arrow, a plain sign still says which way the section goes.
	private void setToggleIcon(JButton toggle, boolean open)
	{
		final ImageIcon icon = open ? sectionRetractIcon : sectionExpandIcon;
		if (icon != null)
		{
			toggle.setIcon(icon);
		}
		else
		{
			toggle.setText(open ? "-" : "+");
		}
	}

	private JPanel row(JPanel parent, String label, String tooltip)
	{
		final JPanel row = new JPanel();
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setLayout(new BorderLayout());
		row.setMinimumSize(new Dimension(PANEL_WIDTH, 0));

		final JLabel text = new JLabel(label);
		text.setForeground(Color.WHITE);
		text.setToolTipText("<html>" + label + ":<br>" + tooltip + "</html>");

		row.add(text, BorderLayout.CENTER);
		parent.add(row);
		return row;
	}

	private void checkbox(JPanel parent, String label, String tooltip, String key, BooleanSupplier value)
	{
		final JCheckBox box = new JCheckBox();
		box.setFocusable(false);
		box.setSelected(value.getAsBoolean());
		box.addActionListener(e ->
		{
			if (!refreshing)
			{
				write(key, box.isSelected());
			}
		});
		row(parent, label, tooltip).add(box, BorderLayout.EAST);
		refreshers.add(() -> box.setSelected(value.getAsBoolean()));
	}

	private <T extends Enum<T>> void combo(JPanel parent, String label, String tooltip, String key, Class<T> type,
		Supplier<T> value)
	{
		final JComboBox<T> box = new JComboBox<>(type.getEnumConstants());
		// Set before it is measured: the renderer builds the entries the preferred width is taken from.
		box.setRenderer(listCellRenderer);
		box.setPreferredSize(new Dimension(box.getPreferredSize().width, COMBO_HEIGHT));
		box.setFocusable(false);
		box.setSelectedItem(value.get());
		box.addItemListener(e ->
		{
			if (!refreshing && e.getStateChange() == ItemEvent.SELECTED)
			{
				write(key, box.getSelectedItem());
			}
		});
		row(parent, label, tooltip).add(box, BorderLayout.EAST);
		refreshers.add(() -> box.setSelectedItem(value.get()));
	}

	private void spinner(JPanel parent, String label, String tooltip, String key, int min, int max, IntSupplier value)
	{
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(value.getAsInt(), min, max, 1));
		final JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
		field.setColumns(SPINNER_FIELD_WIDTH);
		spinner.addChangeListener(e ->
		{
			if (!refreshing)
			{
				write(key, spinner.getValue());
			}
		});
		row(parent, label, tooltip).add(spinner, BorderLayout.EAST);
		refreshers.add(() -> spinner.setValue(value.getAsInt()));
	}

	// Every color here can be transparent, so the swatch and the hex beside it both carry the alpha.
	private void color(JPanel parent, String label, String tooltip, String key, Supplier<Color> value)
	{
		final ColorJButton button = new ColorJButton(hex(value.get()), value.get());
		button.setFocusable(false);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				final RuneliteColorPicker picker = colorPickerManager.create(
					SwingUtilities.getWindowAncestor(BossHealthBarPanel.this), button.getColor(), label, false);
				picker.setLocationRelativeTo(button);
				picker.setOnColorChange(chosen ->
				{
					button.setColor(chosen);
					button.setText(hex(chosen));
				});
				picker.setOnClose(chosen -> write(key, chosen));
				picker.setVisible(true);
			}
		});
		row(parent, label, tooltip).add(button, BorderLayout.EAST);
		refreshers.add(() ->
		{
			button.setColor(value.get());
			button.setText(hex(value.get()));
		});
	}

	private static String hex(Color color)
	{
		return "#" + ColorUtil.colorToAlphaHexCode(color).toUpperCase();
	}

	// An action rather than a setting, so it takes a row of its own instead of sitting beside a label.
	private void button(JPanel parent, String label, String tooltip, Runnable onClick)
	{
		final JButton button = new JButton(label);
		button.setToolTipText(tooltip);
		button.setFocusable(false);
		button.addActionListener(e -> onClick.run());

		final JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMinimumSize(new Dimension(PANEL_WIDTH, 0));
		row.setBorder(new EmptyBorder(BORDER_OFFSET, 0, 0, 0));
		row.add(button, BorderLayout.CENTER);
		parent.add(row);
	}

	private void write(String key, Object value)
	{
		writing = true;
		try
		{
			ConfigWrites.write(configManager, key, value);
		}
		finally
		{
			writing = false;
		}
	}

	// A setting changed somewhere else: the config screen, a theme filling in the colors, an import, or the
	// Oldschool tick writing its companions. Everything is put back, since one change can move several.
	public void settingsChanged()
	{
		if (writing)
		{
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			refreshing = true;
			try
			{
				for (Runnable refresher : refreshers)
				{
					refresher.run();
				}
			}
			finally
			{
				refreshing = false;
			}
		});
	}

	// Keeps the rows at the panel's width however wide their contents would like to be, so a long dropdown
	// entry doesn't stretch the panel and leave every row scrolling sideways.
	private static class FixedWidthPanel extends JPanel
	{
		FixedWidthPanel()
		{
			setBackground(ColorScheme.DARK_GRAY_COLOR);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(PANEL_WIDTH, super.getPreferredSize().height);
		}
	}

	// Carries the plugin for the event that opens its settings, and is never added to anything or drawn.
	private static class PluginOverlay extends Overlay
	{
		PluginOverlay(BossHealthBarPlugin plugin)
		{
			super(plugin);
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			return null;
		}
	}
}
