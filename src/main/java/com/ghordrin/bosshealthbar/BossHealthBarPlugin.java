package com.ghordrin.bosshealthbar;

import com.google.inject.Provides;
import java.util.List;
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
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.FontType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
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
	private Client client;

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

	@Inject
	private KillCounts killCounts;

	@Inject
	private PartyDefence partyDefence;

	@Inject
	private SpecialAttackCounts specialAttackCounts;

	@Provides
	BossHealthBarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossHealthBarConfig.class);
	}

	@Override
	protected void startUp()
	{
		migrateReplaceNativeBossBar();
		migrateDefaultFont();
		migratePixelFontSize();
		migrateItemFonts();
		overlay.reset();
		healthIndicatorMarkers.invalidate();
		killCounts.invalidate();
		partyDefence.reset();
		specialAttackCounts.reset();
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

	private void migrateReplaceNativeBossBar()
	{
		final String oldSaved = configManager.getConfiguration(
			BossHealthBarConfig.GROUP, BossHealthBarConfig.OLD_REPLACE_NATIVE_BOSS_BAR_KEY);
		if (oldSaved == null)
		{
			return;
		}

		final NativeBossBarMode mode = NativeBossBarMode.migrate(oldSaved,
			configManager.getConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.NATIVE_BOSS_BAR_MODE_KEY));
		if (mode != null)
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.NATIVE_BOSS_BAR_MODE_KEY, mode);
		}
		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.OLD_REPLACE_NATIVE_BOSS_BAR_KEY);
	}

	private void migrateDefaultFont()
	{
		if (savedBoolean(BossHealthBarConfig.FONT_DEFAULT_MIGRATED_KEY) != null)
		{
			return;
		}

		final FontType saved = configManager.getConfiguration(
			BossHealthBarConfig.GROUP, BossHealthBarConfig.FONT_KEY, FontType.class);
		if (FontDefaultMigration.isOldDefault(saved))
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.FONT_KEY, FontType.REGULAR);
		}
		configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.FONT_DEFAULT_MIGRATED_KEY, true);
	}

	private void migratePixelFontSize()
	{
		if (savedBoolean(BossHealthBarConfig.PIXEL_FONT_SIZE_MIGRATED_KEY) != null)
		{
			return;
		}

		final FontType migrated = FontDefaultMigration.withNativePixelSize(configManager.getConfiguration(
			BossHealthBarConfig.GROUP, BossHealthBarConfig.FONT_KEY, FontType.class));
		if (migrated != null)
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.FONT_KEY, migrated);
		}
		configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.PIXEL_FONT_SIZE_MIGRATED_KEY, true);
	}

	// The item fonts are new, so their saved values are only the defaults and can be overwritten.
	private void migrateItemFonts()
	{
		if (savedBoolean(BossHealthBarConfig.ITEM_FONTS_MIGRATED_KEY) != null)
		{
			return;
		}

		final FontType name = configManager.getConfiguration(
			BossHealthBarConfig.GROUP, BossHealthBarConfig.FONT_KEY, FontType.class);
		FontDefaultMigration.itemFontWrites(name)
			.forEach((key, font) -> configManager.setConfiguration(BossHealthBarConfig.GROUP, key, font));
		configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.ITEM_FONTS_MIGRATED_KEY, true);
	}

	// Reset can walk the settings in any order, so put these back to their defaults once it's done.
	@Override
	public void resetConfiguration()
	{
		applyWrites(OldschoolToggle.resetWrites());
	}

	private void applyOldschoolToggle(String key, boolean oldValue, boolean newValue)
	{
		applyWrites(OldschoolToggle.writes(key, oldValue, newValue,
			new OldschoolToggle.Settings(config.oldschoolTheme(), config.matchBossColors(), config.rareGoldBars(),
				savedBoolean(BossHealthBarConfig.SAVED_MATCH_BOSS_COLORS_KEY),
				savedBoolean(BossHealthBarConfig.SAVED_RARE_GOLD_BARS_KEY))));
	}

	private void applyWrites(List<OldschoolToggle.Write> writes)
	{
		// Each write posts its own change event straight away. Handling those again is harmless, since
		// the remembered keys are cleared before anything is turned back on.
		for (OldschoolToggle.Write write : writes)
		{
			if (write.getValue() == null)
			{
				configManager.unsetConfiguration(BossHealthBarConfig.GROUP, write.getKey());
			}
			else
			{
				configManager.setConfiguration(BossHealthBarConfig.GROUP, write.getKey(), write.getValue());
			}
		}
	}

	private Boolean savedBoolean(String key)
	{
		final String saved = configManager.getConfiguration(BossHealthBarConfig.GROUP, key);
		return saved != null ? Boolean.valueOf(saved) : null;
	}

	private static boolean parseBoolean(String value, boolean defaultValue)
	{
		return value != null ? Boolean.parseBoolean(value) : defaultValue;
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

		if (BossHealthBarConfig.THEME_KEY.equals(event.getKey())
			&& HealthBarTheme.CUSTOM.name().equals(event.getNewValue()))
		{
			customColors.copyFrom(event.getOldValue());
		}

		if (OldschoolToggle.handles(event.getKey()))
		{
			final boolean defaultValue = BossHealthBarConfig.RARE_GOLD_BARS_KEY.equals(event.getKey());
			applyOldschoolToggle(event.getKey(), parseBoolean(event.getOldValue(), defaultValue),
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
		partyDefence.update(opponentTracker.getOpponent());
		specialAttackCounts.update(opponentTracker.getOpponent());
	}

	// Runs every frame because the game's scripts can unhide their bars whenever they update.
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		opponentTracker.followGameBars();

		final Actor opponent = opponentTracker.getOpponent();
		final boolean replace = config.nativeBossBarMode().hidesGameBar();
		final boolean opponentGetsBar = opponentTracker.shouldShowBarFor(opponent);
		gameBossBar.update(opponent, replace, opponentGetsBar);
		tobBossBar.update(opponent, replace, opponentGetsBar);
	}
}
