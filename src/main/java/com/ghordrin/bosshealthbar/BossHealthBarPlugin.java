package com.ghordrin.bosshealthbar;

import com.google.inject.Provides;
import java.io.IOException;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
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
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Filepath;

@PluginDescriptor(
	name = BossHealthBarPlugin.NAME,
	description = "Replaces the opponent health bar with a themed bar that shows a damage trail and your recent damage",
	tags = {"boss", "health", "healthbar", "hitpoints", "overlay", "pvm", "combat", "ui", "theme"},
	internalName = BossHealthBarPlugin.INTERNAL_NAME
)
public class BossHealthBarPlugin extends Plugin
{
	static final String NAME = "Boss Health Bar Additions";
	static final String INTERNAL_NAME = "boss-health-bar-additions";
	// Keep in step with the version in build.gradle and runelite-plugin.properties.
	static final String VERSION = "1.6.0";
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
	private HealthIndicatorMarkers healthIndicatorMarkers;

	@Inject
	private PartyDamage partyDamage;

	@Inject
	private KillCounts killCounts;

	@Inject
	private PartyDefence partyDefence;

	@Inject
	private SpecialAttackCounts specialAttackCounts;

	@Inject
	private DebugLog debugLog;

	@Inject
	private DebugExport debugExport;

	@Provides
	BossHealthBarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossHealthBarConfig.class);
	}

	@Override
	protected void startUp()
	{
		debugLog.add("Plugin started (version {})", VERSION);
		if (config.saveDebugLog())
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.SAVE_DEBUG_LOG_KEY, false);
		}
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
		opponentInfoOverride.apply();
	}

	@Override
	protected void shutDown()
	{
		debugLog.add("Plugin stopped");
		overlayManager.remove(overlay);
		overlayManager.remove(pillarBarsOverlay);
		partyDamage.shutDown();
		opponentInfoOverride.restore();
		pickers.close();
		clientThread.invoke(() ->
		{
			tobBossBar.restore();
			resetState();
		});
	}

	Filepath dataDirectory() throws IOException
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
		if (state == GameState.LOGGED_IN || state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			debugLog.add("Game state {}", state);
		}
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
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

		if (KillCounts.CONFIG_GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(killCounts::invalidate);
			return;
		}

		if (!BossHealthBarConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		overlay.invalidateColors();

		if (BossHealthBarConfig.SAVE_DEBUG_LOG_KEY.equals(event.getKey()))
		{
			// Same as the picker checkboxes: ticking saves the log and the tick is undone straight away.
			if ("true".equals(event.getNewValue()))
			{
				configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.SAVE_DEBUG_LOG_KEY, false);
				debugExport.save();
			}
			return;
		}

		debugLog.add("Setting {}: {} -> {}", event.getKey(), DebugLog.cut(event.getOldValue(), MAX_LOGGED_VALUE_LENGTH),
			DebugLog.cut(event.getNewValue(), MAX_LOGGED_VALUE_LENGTH));

		if (BossHealthBarConfig.THEME_KEY.equals(event.getKey())
			&& HealthBarTheme.CUSTOM.name().equals(event.getNewValue()))
		{
			customColors.copyFrom(event.getOldValue());
		}

		if (OldschoolToggle.handles(event.getKey()))
		{
			final boolean defaultValue = BossHealthBarConfig.RARE_GOLD_BARS_KEY.equals(event.getKey());
			OldschoolToggle.apply(configManager, config, event.getKey(), parseBoolean(event.getOldValue(), defaultValue),
				parseBoolean(event.getNewValue(), defaultValue));
		}

		// The config screen has no buttons, so these two checkboxes act as one: ticking opens the
		// picker and the tick is undone straight away.
		if (BossHealthBarConfig.CHOOSE_FILL_TEXTURE_KEY.equals(event.getKey()) && "true".equals(event.getNewValue()))
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.CHOOSE_FILL_TEXTURE_KEY, false);
			pickers.openFillTexturePicker();
		}

		if (BossHealthBarConfig.CHOOSE_CUSTOM_ICON_KEY.equals(event.getKey()) && "true".equals(event.getNewValue()))
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.CHOOSE_CUSTOM_ICON_KEY, false);
			pickers.openIconPickerIfLoggedIn();
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
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		gameBossBar.onGameTick();
		tobBossBar.onGameTick();
		superiorTracker.onGameTick();
		opponentTracker.onGameTick();
		final Actor opponent = opponentTracker.getOpponent();
		fightGroup.onGameTick(opponent, client.getTickCount());
		fightTimer.onGameTick(opponent, opponentTracker.updateOpponentDefeated(), client.getTickCount());
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
		final boolean replacing = gameBossBar.update(opponent, replace, opponentGetsBar);
		pillarBars.update(replacing);
		tobBossBar.update(opponent, replace, opponentGetsBar);
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
