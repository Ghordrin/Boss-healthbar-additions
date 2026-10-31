package com.ghordrin.bosshealthbar;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.config.ConfigMigrations;
import com.ghordrin.bosshealthbar.config.ConfigWrites;
import com.ghordrin.bosshealthbar.config.OldschoolToggle;
import com.ghordrin.bosshealthbar.config.PanelMoveNotice;
import com.ghordrin.bosshealthbar.config.Pickers;
import com.ghordrin.bosshealthbar.config.SettingsTransfer;
import com.ghordrin.bosshealthbar.data.Health;
import com.ghordrin.bosshealthbar.debug.DebugExport;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import com.ghordrin.bosshealthbar.gamebars.GameBossBar;
import com.ghordrin.bosshealthbar.gamebars.PillarBars;
import com.ghordrin.bosshealthbar.gamebars.PillarBarsOverlay;
import com.ghordrin.bosshealthbar.gamebars.TobBossBar;
import com.ghordrin.bosshealthbar.integrations.BossBarPartyHit;
import com.ghordrin.bosshealthbar.integrations.HealthIndicatorMarkers;
import com.ghordrin.bosshealthbar.integrations.KillCounts;
import com.ghordrin.bosshealthbar.integrations.KillMoments;
import com.ghordrin.bosshealthbar.integrations.OpponentInfoOverride;
import com.ghordrin.bosshealthbar.integrations.PartyDamage;
import com.ghordrin.bosshealthbar.integrations.PartyDefence;
import com.ghordrin.bosshealthbar.integrations.SpecialAttackCounts;
import com.ghordrin.bosshealthbar.panel.BossHealthBarPanel;
import com.ghordrin.bosshealthbar.theme.BarTheme;
import com.ghordrin.bosshealthbar.theme.CustomColors;
import com.ghordrin.bosshealthbar.theme.HealthBarTheme;
import com.ghordrin.bosshealthbar.tracking.DamageTracker;
import com.ghordrin.bosshealthbar.tracking.FightGroup;
import com.ghordrin.bosshealthbar.tracking.FightTimer;
import com.ghordrin.bosshealthbar.tracking.OpponentTracker;
import com.ghordrin.bosshealthbar.tracking.SuperiorTracker;
import com.google.inject.Provides;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Provider;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = BossHealthBarPlugin.NAME,
	description = "Replaces the opponent health bar with a themed bar that shows a damage trail and your recent damage",
	tags = {"boss", "health", "healthbar", "hitpoints", "overlay", "pvm", "combat", "ui", "theme"},
	internalName = BossHealthBarPlugin.INTERNAL_NAME
)
public class BossHealthBarPlugin extends Plugin
{
	public static final String NAME = "Boss Health Bar Additions";
	public static final String INTERNAL_NAME = "boss-health-bar-additions";
	public static final String DISCORD_URL = "https://discord.gg/YPugUP8rg2";
	// Keep in step with the version in build.gradle and runelite-plugin.properties.
	public static final String VERSION = "2.1.0";
	private static final int MAX_LOGGED_VALUE_LENGTH = 100;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BossHealthBarConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ConfigMigrations configMigrations;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BossHealthBarOverlay overlay;

	@Inject
	private OpponentTracker opponentTracker;

	@Inject
	private GameBossBar gameBossBar;

	@Inject
	private TobBossBar tobBossBar;

	@Inject
	private PillarBars pillarBars;

	@Inject
	private PillarBarsOverlay pillarBarsOverlay;

	@Inject
	private PartnerBarOverlay partnerBarOverlay;

	@Inject
	private SuperiorTracker superiorTracker;

	@Inject
	private DamageTracker damageTracker;

	@Inject
	private FightTimer fightTimer;

	@Inject
	private FightGroup fightGroup;

	@Inject
	private OpponentInfoOverride opponentInfoOverride;

	@Inject
	private CustomColors customColors;

	@Inject
	private Pickers pickers;

	@Inject
	private SettingsTransfer settingsTransfer;

	@Inject
	private PanelMoveNotice panelMoveNotice;

	@Inject
	private HealthIndicatorMarkers healthIndicatorMarkers;

	@Inject
	private PartyDamage partyDamage;

	@Inject
	private KillCounts killCounts;

	@Inject
	private KillMoments killMoments;

	@Inject
	private PartyDefence partyDefence;

	@Inject
	private SpecialAttackCounts specialAttackCounts;

	@Inject
	private DebugLog debugLog;

	@Inject
	private DebugExport debugExport;

	@Inject
	private ClientToolbar clientToolbar;

	// Asked for rather than injected: the client loads plugins before it sets its look and feel, so a panel
	// built while the plugin is injected is built out of plain Swing widgets and stays that way. The panel is
	// made in startUp instead, by which time the client's own look is in place.
	@Inject
	private Provider<BossHealthBarPanel> panelProvider;

	private BossHealthBarPanel panel;

	private NavigationButton navButton;

	@Provides
	BossHealthBarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossHealthBarConfig.class);
	}

	@Override
	protected void startUp()
	{
		debugLog.add("Plugin started (version {})", VERSION);
		configMigrations.migrate();
		opponentTracker.loadLists();
		overlay.reset();
		healthIndicatorMarkers.invalidate();
		killCounts.invalidate();
		partyDefence.reset();
		specialAttackCounts.reset();
		partyDamage.startUp();
		overlayManager.add(overlay);
		overlayManager.add(pillarBarsOverlay);
		overlayManager.add(partnerBarOverlay);
		opponentInfoOverride.apply();
		panel = panelProvider.get();
		showSidebarIcon(config.showSidebarIcon());
	}

	// The icon is added and removed as the setting is ticked, so turning it off gives the sidebar space back.
	private void showSidebarIcon(boolean show)
	{
		if (show == (navButton != null) || (show && panel == null))
		{
			return;
		}

		if (!show)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
			return;
		}

		navButton = NavigationButton.builder()
			.tooltip(NAME)
			.icon(ImageUtil.loadImageResource(BossHealthBarPlugin.class, "panel_icon.png"))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	// Asked for from the bar's right-click menu. The panel can't be opened without its icon, so asking for it
	// puts the icon back.
	public void openPanel()
	{
		if (navButton == null)
		{
			ConfigWrites.write(configManager, BossHealthBarConfig.SHOW_SIDEBAR_ICON_KEY, true);
		}

		if (navButton != null)
		{
			clientToolbar.openPanel(navButton);
		}
	}

	@Override
	protected void shutDown()
	{
		debugLog.add("Plugin stopped");
		showSidebarIcon(false);
		panel = null;
		overlayManager.remove(overlay);
		overlayManager.remove(pillarBarsOverlay);
		overlayManager.remove(partnerBarOverlay);
		partyDamage.shutDown();
		opponentInfoOverride.restore();
		pickers.close();
		clientThread.invoke(() ->
		{
			tobBossBar.restore();
			resetState();
		});
	}

	public Filepath dataDirectory() throws IOException
	{
		return getPluginDirectory();
	}

	// Reset can walk the settings in any order, so put these back to their defaults once it's done.
	@Override
	public void resetConfiguration()
	{
		OldschoolToggle.applyWrites(configManager, OldschoolToggle.resetWrites());
	}

	private static boolean parseBoolean(String value, boolean defaultValue)
	{
		return value != null ? Boolean.parseBoolean(value) : defaultValue;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		final GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN || state == GameState.LOADING || state == GameState.LOGIN_SCREEN
			|| state == GameState.HOPPING)
		{
			debugLog.add("Game state {}", state);
		}
		if (state == GameState.LOADING)
		{
			opponentTracker.onSceneLoading();
		}
		// A frame or an NPC spawn can reach us before the first tick after a load, so this can't wait for it.
		if (state == GameState.LOGGED_IN)
		{
			checkLeftArea(true);
			panelMoveNotice.onLoggedIn();
		}
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			resetState();
		}
	}

	private void checkLeftArea(boolean onArrival)
	{
		if (opponentTracker.takeLeftArea(onArrival))
		{
			debugLog.add("Forgetting the fight");
			resetState();
		}
	}

	private void resetState()
	{
		gameBossBar.reset();
		pillarBars.reset();
		tobBossBar.reset();
		superiorTracker.reset();
		opponentTracker.reset();
		damageTracker.reset();
		killCounts.invalidate();
		partyDefence.reset();
		specialAttackCounts.reset();
		overlay.reset();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (HealthIndicatorMarkers.CONFIG_GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(healthIndicatorMarkers::invalidate);
			return;
		}

		// On the bar's clock, which the overlay reads the moment back on.
		final long nowMillis = System.nanoTime() / 1_000_000L;
		if (KillCounts.CONFIG_GROUP.equals(event.getGroup()))
		{
			killMoments.killCountSaved(event.getKey(), event.getOldValue(), event.getNewValue(), nowMillis);
			clientThread.invoke(killCounts::invalidate);
			return;
		}

		if (KillMoments.PERSONAL_BEST_GROUP.equals(event.getGroup()))
		{
			killMoments.personalBestSaved(event.getKey(), event.getOldValue(), event.getNewValue(), nowMillis);
			return;
		}

		if (!BossHealthBarConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		overlay.invalidateColors();
		// The panel shows the same settings, so it follows a change made anywhere else.
		if (panel != null)
		{
			panel.settingsChanged(event.getKey());
		}

		if (BossHealthBarConfig.SHOW_SIDEBAR_ICON_KEY.equals(event.getKey()))
		{
			showSidebarIcon(config.showSidebarIcon());
			return;
		}

		// A button rather than a setting: ticking it opens the panel and the tick is undone straight away.
		// Only a change to true counts, so an import writing its default doesn't open anything.
		if (BossHealthBarConfig.OPEN_PANEL_KEY.equals(event.getKey()))
		{
			if ("true".equals(event.getNewValue()))
			{
				ConfigWrites.write(configManager, BossHealthBarConfig.OPEN_PANEL_KEY, false);
				openPanel();
			}
			return;
		}

		debugLog.add("Setting {}: {} -> {}", event.getKey(), DebugLog.cut(event.getOldValue(), MAX_LOGGED_VALUE_LENGTH),
			DebugLog.cut(event.getNewValue(), MAX_LOGGED_VALUE_LENGTH));

		// These three settings follow each other around, which an import must not set off: it writes a whole
		// set that was chosen together, and they would answer by writing over what was just loaded.
		if (!settingsTransfer.isImporting())
		{
			// The Custom colors are a template of the bar as it looks, so they follow the theme and the Oldschool
			// checkbox until Custom is picked, which leaves them alone to be tweaked from there.
			if (BossHealthBarConfig.THEME_KEY.equals(event.getKey())
				|| BossHealthBarConfig.OLDSCHOOL_THEME_KEY.equals(event.getKey()))
			{
				pickers.themeChanged();
				if (config.theme() != HealthBarTheme.CUSTOM)
				{
					customColors.copyFrom(BarTheme.of(config).getColors());
				}
			}

			if (CustomColors.isColorKey(event.getKey()) && !customColors.isCopying())
			{
				pickers.confirmCustomTheme();
			}

			if (OldschoolToggle.handles(event.getKey()))
			{
				final boolean defaultValue = BossHealthBarConfig.RARE_GOLD_BARS_KEY.equals(event.getKey());
				OldschoolToggle.apply(configManager, config, event.getKey(),
					parseBoolean(event.getOldValue(), defaultValue), parseBoolean(event.getNewValue(), defaultValue));
			}
		}

		if (BossHealthBarConfig.ALSO_SHOW_FOR_KEY.equals(event.getKey())
			|| BossHealthBarConfig.NEVER_SHOW_FOR_KEY.equals(event.getKey()))
		{
			clientThread.invoke(opponentTracker::loadLists);
		}

		if (BossHealthBarConfig.HIDE_VANILLA_OVERLAY_KEY.equals(event.getKey()))
		{
			if (config.hideVanillaOverlay())
			{
				opponentInfoOverride.apply();
			}
			else
			{
				opponentInfoOverride.restore();
			}
		}
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		clientThread.invoke(() ->
		{
			healthIndicatorMarkers.invalidate();
			partyDefence.invalidatePlugin();
			specialAttackCounts.invalidatePlugin();
		});
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		clientThread.invoke(killCounts::invalidate);
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		opponentTracker.onInteractingChanged(event);
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		opponentTracker.onHitsplatApplied(event.getActor(), event.getHitsplat());
		if (OpponentTracker.isCombatHit(event.getHitsplat().getHitsplatType()))
		{
			fightTimer.onCombatHit(event.getActor(), client.getTickCount());
		}
		if (event.getActor() == opponentTracker.getOpponent())
		{
			final Hitsplat hitsplat = event.getHitsplat();
			damageTracker.recordHit(hitsplat, client.getTickCount(), partyDamage.countsPartyHits());
			if (hitsplat.isMine() && hitsplat.getHitsplatType() != HitsplatID.HEAL)
			{
				partyDamage.sendHit(event.getActor(), hitsplat.getAmount());
			}
		}
	}

	@Subscribe
	public void onBossBarPartyHit(BossBarPartyHit event)
	{
		clientThread.invoke(() -> partyDamage.onPartyHit(event));
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		gameBossBar.onNpcSpawned();
		tobBossBar.onNpcSpawned(event.getNpc());
		superiorTracker.onNpcSpawned(event.getNpc());
		opponentTracker.onNpcSpawned(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		tobBossBar.onNpcChanged(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		gameBossBar.onNpcDespawned(event.getNpc());
		tobBossBar.onNpcDespawned(event.getNpc());
		superiorTracker.onNpcDespawned(event.getNpc());
		opponentTracker.onNpcDespawned(event.getNpc());
		fightGroup.onNpcDespawned(event.getNpc());
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		superiorTracker.onChatMessage(event);
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		final int reported = FightTimer.reportedTicks(event.getMessage());
		// A message with a time the reading missed is logged too, so a wording we don't know shows up.
		if (reported != FightTimer.NO_TICK || FightTimer.mentionsDuration(event.getMessage()))
		{
			final int ticks = fightTimer.getElapsedTicks(client.getTickCount());
			debugLog.add("Game says: \"{}\" (the game's {} ticks, our timer: {})", Text.removeTags(event.getMessage()),
				reported == FightTimer.NO_TICK ? "unread" : String.valueOf(reported),
				ticks == FightTimer.NO_TICK ? "not started" : ticks + " ticks, " + fightTimer.getText());
			fightTimer.onReportedTime(reported, opponentTracker.isOpponentDefeated(), client.getTickCount());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		checkLeftArea(false);
		gameBossBar.onGameTick();
		tobBossBar.onGameTick();
		superiorTracker.onGameTick();
		opponentTracker.onGameTick();
		final Actor opponent = opponentTracker.getOpponent();
		fightGroup.onGameTick(opponent, client.getTickCount());
		opponentTracker.updateOpponentDown();
		fightTimer.setPreciseTiming(client.getVarbitValue(VarbitID.OPTION_PRECISE_TIMING) != 0);
		fightTimer.onGameTick(opponent, opponentTracker.isOpponentDefeated(), opponentTracker.zeroSince(),
			client.getTickCount());
		partyDefence.update(opponent);
		specialAttackCounts.update(opponent);
	}

	// Runs every frame because the game's scripts can unhide their bars whenever they update.
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		opponentTracker.followGameBars();

		final Actor opponent = opponentTracker.getOpponent();
		final boolean replace = config.nativeBossBarMode().hidesGameBar();
		final boolean opponentGetsBar = opponentTracker.shouldShowBarFor(opponent);
		final boolean trackedGetsBar = opponentTracker.gameBarNpcGetsBar(gameBossBar.trackedNpcId());
		final boolean replacing = gameBossBar.update(opponent, replace, opponentGetsBar, trackedGetsBar);
		pillarBars.update(replacing);
		final NPC raidBoss = tobBossBar.findBoss();
		tobBossBar.update(replace, raidBoss, opponentTracker.raidBarBossGetsBar(raidBoss));
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptEvent() != null)
		{
			pillarBars.onScript(event.getScriptId(), event.getScriptEvent().getArguments());
		}
	}
}
