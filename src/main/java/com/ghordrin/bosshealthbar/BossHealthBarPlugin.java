package com.ghordrin.bosshealthbar;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Actor;
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
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = BossHealthBarPlugin.NAME,
	description = "Replaces the opponent health bar with a themed bar that shows a damage trail and your recent damage",
	tags = {"boss", "health", "healthbar", "hitpoints", "overlay", "pvm", "combat", "ui", "theme"}
)
public class BossHealthBarPlugin extends Plugin
{
	static final String NAME = "Boss Health Bar Additions";

	@Inject
	private ClientThread clientThread;

	@Inject
	private BossHealthBarConfig config;

	@Inject
	private ConfigManager configManager;

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
	private SuperiorTracker superiorTracker;

	@Inject
	private DamageTracker damageTracker;

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

	@Provides
	BossHealthBarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossHealthBarConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlay.reset();
		healthIndicatorMarkers.invalidate();
		partyDamage.startUp();
		overlayManager.add(overlay);
		opponentInfoOverride.apply();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		partyDamage.shutDown();
		opponentInfoOverride.restore();
		pickers.close();
		clientThread.invoke(() ->
		{
			gameBossBar.restore();
			tobBossBar.restore();
			resetState();
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			resetState();
		}
	}

	private void resetState()
	{
		gameBossBar.reset();
		tobBossBar.reset();
		superiorTracker.reset();
		opponentTracker.reset();
		damageTracker.reset();
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

		if (!BossHealthBarConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		overlay.invalidateColors();

		if (BossHealthBarConfig.THEME_KEY.equals(event.getKey())
			&& HealthBarTheme.CUSTOM.name().equals(event.getNewValue()))
		{
			customColors.copyFrom(event.getOldValue());
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
		clientThread.invoke(healthIndicatorMarkers::invalidate);
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		opponentTracker.onInteractingChanged(event);
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() == opponentTracker.getOpponent())
		{
			final Hitsplat hitsplat = event.getHitsplat();
			damageTracker.recordHit(hitsplat, config.damageNumberSource());
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
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		superiorTracker.onChatMessage(event);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		tobBossBar.onGameTick();
		superiorTracker.onGameTick();
		opponentTracker.onGameTick();
	}

	// Runs every frame because the game's scripts can unhide their bars whenever they update.
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		opponentTracker.followGameBars();

		final Actor opponent = opponentTracker.getOpponent();
		final boolean replace = config.replaceNativeBossBar();
		final boolean opponentGetsBar = opponentTracker.shouldShowBarFor(opponent);
		gameBossBar.update(opponent, replace, opponentGetsBar);
		tobBossBar.update(opponent, replace, opponentGetsBar);
	}
}
