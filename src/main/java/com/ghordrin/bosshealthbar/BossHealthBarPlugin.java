package com.ghordrin.bosshealthbar;

import com.google.inject.Provides;
import java.awt.Color;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.chatbox.ChatboxItemSearch;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = BossHealthBarPlugin.NAME,
	description = "Replaces the opponent health bar with a themed bar that shows a damage trail and your recent damage",
	tags = {"boss", "health", "healthbar", "hitpoints", "overlay", "pvm", "combat", "ui", "theme"}
)
public class BossHealthBarPlugin extends Plugin
{
	static final String NAME = "Boss Health Bar Additions";
	static final Duration DAMAGE_COMBO_WINDOW = Duration.ofMillis(2500);
	private static final int TOB_PROGRESS_NONE = 0;
	private static final int TOB_PROGRESS_BOSS_HEALTH = 1;
	private static final int TOB_BOSS_SEARCH_DISTANCE = 32;
	private static final String SUPERIOR_SPAWN_MESSAGE = "A superior foe has appeared";
	private static final int SUPERIOR_MATCH_TICKS = 2;
	private static final int SUPERIOR_SEARCH_DISTANCE = 15;
	private static final String VANILLA_OVERLAY_GROUP = "opponentinfo";
	private static final String VANILLA_OVERLAY_KEY = "showOpponentHealthOverlay";
	// Stored in config so the original value can still be restored if the client closes while hidden.
	private static final String SAVED_VANILLA_OVERLAY_KEY = "savedOpponentHealthOverlay";
	private static final String VANILLA_OVERLAY_HIDDEN_KEY = "opponentHealthOverlayHidden";

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
	private ChatboxItemSearch itemSearch;

	private FillTexturePickerDialog texturePickerDialog;

	@Getter(AccessLevel.PACKAGE)
	private Actor lastOpponent;

	@Getter(AccessLevel.PACKAGE)
	private long lastHitMillis;

	@Getter(AccessLevel.PACKAGE)
	private int lastHitAmount;

	@Getter(AccessLevel.PACKAGE)
	private int comboDamage;

	@Getter(AccessLevel.PACKAGE)
	private long lastDamageDealtMillis;

	private long lastInteractionLostMillis;
	private boolean nativeBarHidden;
	private NPC nativeBarNpc;
	private int nativeBarSearchedId = -1;
	private int replacedNativeBarNpcId = -1;
	private boolean tobBarHidden;
	private NPC tobBoss;
	private boolean tobBossSearchNeeded = true;
	private final Map<NPC, Integer> recentSpawnTicks = new HashMap<>();
	private final Set<NPC> superiors = new HashSet<>();
	private int superiorMessageTick = -1;
	private Actor knownBossActor;
	private String knownBossName;
	private boolean knownBoss;

	@Provides
	BossHealthBarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossHealthBarConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlay.reset();
		overlayManager.add(overlay);
		applyVanillaOverlayOverride();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		restoreVanillaOverlay();
		// The dialog is only touched on the EDT, and the bar state only on the client thread.
		SwingUtilities.invokeLater(() ->
		{
			if (texturePickerDialog != null)
			{
				texturePickerDialog.dispose();
				texturePickerDialog = null;
			}
		});
		clientThread.invoke(() ->
		{
			restoreNativeBar();
			restoreTobBar();
			resetState();
		});
	}

	void openIconPicker()
	{
		itemSearch
			.tooltipText("Custom icon")
			.onItemSelected(itemId -> configManager.setConfiguration(
				BossHealthBarConfig.GROUP, BossHealthBarConfig.CUSTOM_ICON_ITEM_ID_KEY, itemId))
			.build();
	}

	void openFillTexturePicker()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (texturePickerDialog == null || !texturePickerDialog.isDisplayable())
			{
				texturePickerDialog = new FillTexturePickerDialog(client, clientThread, configManager, config);
			}
			texturePickerDialog.setVisible(true);
			texturePickerDialog.toFront();
		});
	}

	private void copyThemeToCustomColors(String previousValue)
	{
		HealthBarTheme previous = BossHealthBarConfig.DEFAULT_THEME;
		if (previousValue != null)
		{
			previous = null;
			for (HealthBarTheme theme : HealthBarTheme.values())
			{
				if (theme.name().equals(previousValue))
				{
					previous = theme;
					break;
				}
			}
		}

		final ThemeColors colors = previous != null ? previous.getColors() : null;
		if (colors == null)
		{
			return;
		}

		setCustomColor("customFillHighColor", colors.getFillHigh());
		setCustomColor("customFillLowColor", colors.getFillLow());
		setCustomColor("customTrailColor", colors.getTrail());
		setCustomColor("customFrameColor", colors.getFrame());
		setCustomColor("customTextColor", colors.getText());
		setCustomColor("customLevelTextColor", colors.getLevelText());
		setCustomColor("customHitpointsTextColor", colors.getHitpointsText());
		setCustomColor("customDefeatedTextColor", colors.getDefeatedText());
	}

	private void setCustomColor(String key, Color color)
	{
		configManager.setConfiguration(BossHealthBarConfig.GROUP, key, color);
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
		nativeBarHidden = false;
		tobBarHidden = false;
		lastOpponent = null;
		nativeBarNpc = null;
		nativeBarSearchedId = -1;
		replacedNativeBarNpcId = -1;
		tobBoss = null;
		tobBossSearchNeeded = true;
		recentSpawnTicks.clear();
		superiors.clear();
		superiorMessageTick = -1;
		knownBossActor = null;
		knownBossName = null;
		lastHitMillis = 0;
		lastInteractionLostMillis = 0;
		resetComboDamage();
		overlay.reset();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!BossHealthBarConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		overlay.invalidateColors();

		if (BossHealthBarConfig.THEME_KEY.equals(event.getKey())
			&& HealthBarTheme.CUSTOM.name().equals(event.getNewValue()))
		{
			copyThemeToCustomColors(event.getOldValue());
		}

		// The config screen has no buttons, so these two checkboxes act as one: ticking opens the
		// picker and the tick is undone straight away.
		if (BossHealthBarConfig.CHOOSE_FILL_TEXTURE_KEY.equals(event.getKey()) && "true".equals(event.getNewValue()))
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.CHOOSE_FILL_TEXTURE_KEY, false);
			openFillTexturePicker();
		}

		if (BossHealthBarConfig.CHOOSE_CUSTOM_ICON_KEY.equals(event.getKey()) && "true".equals(event.getNewValue()))
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.CHOOSE_CUSTOM_ICON_KEY, false);
			// The item search lives in the chatbox.
			clientThread.invoke(() ->
			{
				if (client.getGameState() == GameState.LOGGED_IN)
				{
					openIconPicker();
				}
			});
		}

		if (BossHealthBarConfig.HIDE_VANILLA_OVERLAY_KEY.equals(event.getKey()))
		{
			if (config.hideVanillaOverlay())
			{
				applyVanillaOverlayOverride();
			}
			else
			{
				restoreVanillaOverlay();
			}
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (event.getSource() != client.getLocalPlayer())
		{
			return;
		}

		Actor opponent = event.getTarget();

		if (opponent == null)
		{
			lastInteractionLostMillis = System.currentTimeMillis();
			log.debug("Interaction lost with {}, will clear after {}s if not resumed", lastOpponent, config.hideDelay());
			return;
		}

		lastInteractionLostMillis = 0;

		if (opponent == lastOpponent)
		{
			return;
		}

		// Attacking the smaller NPCs a boss spawns shouldn't take the bar off the boss.
		if (lastOpponent != null && !lastOpponent.isDead() && priority(opponent) < priority(lastOpponent))
		{
			log.debug("Keeping {} over lower priority target {}", lastOpponent.getName(), opponent.getName());
			return;
		}

		setOpponent(opponent);
	}

	private void setOpponent(Actor opponent)
	{
		overlay.resetAnimation();
		resetComboDamage();
		lastOpponent = opponent;
		log.debug("New opponent: {} (combat level {}, known boss: {}, game boss bar: {})",
			opponent.getName(), opponent.getCombatLevel(), KnownBosses.contains(opponent.getName()), isGameBarBoss(opponent));
	}

	private int priority(Actor actor)
	{
		if (actor == null)
		{
			return -1;
		}
		if (isNativeBarNpc(actor) || actor == findTobBoss())
		{
			return 2;
		}
		return shouldShowBarFor(actor) ? 1 : 0;
	}

	// The Theatre of Blood bar doesn't say which NPC it belongs to, so this takes the highest level
	// attackable NPC nearby. Cached until a stronger NPC spawns or the boss changes or despawns.
	private NPC findTobBoss()
	{
		if (client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE) != TOB_PROGRESS_BOSS_HEALTH
			|| client.getWidget(InterfaceID.TobHud.PROGRESS_CONTAINER) == null)
		{
			tobBoss = null;
			tobBossSearchNeeded = true;
			return null;
		}

		if (!tobBossSearchNeeded)
		{
			return tobBoss;
		}

		tobBossSearchNeeded = false;
		tobBoss = null;
		final Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc.isDead() || !isAttackable(npc)
				|| npc.getWorldLocation().distanceTo(player.getWorldLocation()) > TOB_BOSS_SEARCH_DISTANCE)
			{
				continue;
			}

			if (tobBoss == null
				|| npc.getCombatLevel() > tobBoss.getCombatLevel()
				|| (npc.getCombatLevel() == tobBoss.getCombatLevel() && size(npc) > size(tobBoss)))
			{
				tobBoss = npc;
			}
		}
		return tobBoss;
	}

	private static boolean isAttackable(NPC npc)
	{
		final NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			return false;
		}
		for (String action : composition.getActions())
		{
			if ("Attack".equals(action))
			{
				return true;
			}
		}
		return false;
	}

	private static int size(NPC npc)
	{
		final NPCComposition composition = npc.getTransformedComposition();
		return composition != null ? composition.getSize() : 0;
	}

	private boolean isGameBarBoss(Actor opponent)
	{
		return opponent != null && (isNativeBarNpc(opponent) || opponent == tobBoss);
	}

	private int nativeBarNpcId()
	{
		if (client.getVarbitValue(VarbitID.HPBAR_HUD_BOSS_DISABLED) != 0)
		{
			return -1;
		}
		return client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
	}

	boolean isNativeBarNpc(Actor actor)
	{
		if (!(actor instanceof NPC))
		{
			return false;
		}
		final int trackedId = nativeBarNpcId();
		return trackedId != -1 && compositionId((NPC) actor) == trackedId;
	}

	private NPC findNativeBarNpc()
	{
		final int trackedId = nativeBarNpcId();
		if (trackedId == -1)
		{
			nativeBarNpc = null;
			nativeBarSearchedId = -1;
			return null;
		}

		if (isNativeBarNpc(lastOpponent))
		{
			nativeBarNpc = (NPC) lastOpponent;
			return nativeBarNpc;
		}

		final Player player = client.getLocalPlayer();
		final Actor target = player != null ? player.getInteracting() : null;
		if (isNativeBarNpc(target))
		{
			nativeBarNpc = (NPC) target;
			return nativeBarNpc;
		}

		if (nativeBarNpc != null && compositionId(nativeBarNpc) == trackedId)
		{
			return nativeBarNpc;
		}

		if (nativeBarNpc == null && trackedId == nativeBarSearchedId)
		{
			return null;
		}

		nativeBarNpc = null;
		nativeBarSearchedId = trackedId;
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (compositionId(npc) == trackedId)
			{
				nativeBarNpc = npc;
				break;
			}
		}
		return nativeBarNpc;
	}

	// The game's boss bar tracks the base NPC ID, not the ID of the current form.
	private static int compositionId(NPC npc)
	{
		final NPCComposition composition = npc.getComposition();
		return composition != null ? composition.getId() : -1;
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() != lastOpponent)
		{
			return;
		}

		final Hitsplat hitsplat = event.getHitsplat();
		if (hitsplat.getAmount() <= 0 || hitsplat.getHitsplatType() == HitsplatID.HEAL)
		{
			return;
		}

		final long now = System.currentTimeMillis();
		lastHitMillis = now;
		lastHitAmount = hitsplat.getAmount();

		if (hitsplat.isMine())
		{
			comboDamage = nextComboDamage(comboDamage, lastDamageDealtMillis, now, hitsplat.getAmount());
			lastDamageDealtMillis = now;
		}
	}

	static int nextComboDamage(int previousCombo, long previousDamageMillis, long now, int hitAmount)
	{
		final boolean withinWindow = previousDamageMillis != 0 && now - previousDamageMillis <= DAMAGE_COMBO_WINDOW.toMillis();
		return (withinWindow ? previousCombo : 0) + hitAmount;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		nativeBarSearchedId = -1;
		if (mayBeTobBoss(event.getNpc()))
		{
			tobBossSearchNeeded = true;
		}
		recentSpawnTicks.put(event.getNpc(), client.getTickCount());
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if ((event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
			&& event.getMessage().contains(SUPERIOR_SPAWN_MESSAGE))
		{
			superiorMessageTick = client.getTickCount();
			log.debug("Superior spawn message on tick {}", superiorMessageTick);
		}
	}

	// The game only announces a superior in chat, so pick the attackable NPC that spawned closest to
	// the player around the same tick. The spawn and the message can arrive in either order.
	private void markSuperior()
	{
		final int tick = client.getTickCount();
		recentSpawnTicks.values().removeIf(spawnTick -> tick - spawnTick > SUPERIOR_MATCH_TICKS);

		if (superiorMessageTick == -1)
		{
			return;
		}

		final Player player = client.getLocalPlayer();
		NPC nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		if (player != null)
		{
			for (NPC npc : recentSpawnTicks.keySet())
			{
				final int distance = npc.getWorldLocation().distanceTo(player.getWorldLocation());
				if (!npc.isDead() && !superiors.contains(npc) && isAttackable(npc)
					&& distance <= SUPERIOR_SEARCH_DISTANCE && distance < nearestDistance)
				{
					nearest = npc;
					nearestDistance = distance;
				}
			}
		}

		if (nearest != null)
		{
			superiors.add(nearest);
			superiorMessageTick = -1;
			log.debug("Marked {} as a superior", nearest.getName());
		}
		else if (tick - superiorMessageTick >= SUPERIOR_MATCH_TICKS)
		{
			superiorMessageTick = -1;
			log.debug("No spawned NPC found for the superior spawn message");
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		if (event.getNpc() == tobBoss || mayBeTobBoss(event.getNpc()))
		{
			tobBossSearchNeeded = true;
		}
	}

	private boolean mayBeTobBoss(NPC npc)
	{
		return tobBoss == null || npc.getCombatLevel() >= tobBoss.getCombatLevel();
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc() == nativeBarNpc)
		{
			nativeBarNpc = null;
		}
		if (event.getNpc() == tobBoss)
		{
			tobBoss = null;
			tobBossSearchNeeded = true;
		}
		recentSpawnTicks.remove(event.getNpc());
		superiors.remove(event.getNpc());

		if (event.getNpc() != lastOpponent)
		{
			return;
		}

		log.debug("Opponent {} despawned, clearing", lastOpponent);
		lastOpponent = null;
		lastInteractionLostMillis = 0;
		resetComboDamage();
	}

	private void resetComboDamage()
	{
		comboDamage = 0;
		lastDamageDealtMillis = 0;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (tobBoss == null)
		{
			tobBossSearchNeeded = true;
		}

		markSuperior();

		final Player player = client.getLocalPlayer();
		if (lastOpponent != null
			&& player != null
			&& lastOpponent != findNativeBarNpc()
			&& lastOpponent != findTobBoss()
			&& lastInteractionLostMillis != 0
			&& player.getInteracting() == null
			&& System.currentTimeMillis() - lastInteractionLostMillis > config.hideDelay() * 1000L)
		{
			log.debug("Opponent {} timed out after {}s with no interaction, clearing", lastOpponent, config.hideDelay());
			lastOpponent = null;
		}
	}

	// Runs every frame because the game's scripts can unhide their bars whenever they update.
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		final NPC nativeBarBoss = findNativeBarNpc();
		final NPC gameBarBoss = nativeBarBoss != null ? nativeBarBoss : findTobBoss();
		if (gameBarBoss != null && !gameBarBoss.isDead() && gameBarBoss != lastOpponent)
		{
			setOpponent(gameBarBoss);
			lastInteractionLostMillis = 0;
		}

		updateNativeBar();
		updateTobBar();
	}

	private void updateNativeBar()
	{
		final Widget nativeBar = client.getWidget(InterfaceID.HpbarHud.UNIVERSE);
		if (nativeBar == null)
		{
			nativeBarHidden = false;
			return;
		}

		boolean replace = false;
		if (config.replaceNativeBossBar())
		{
			if (shouldShowBarFor(lastOpponent) && isNativeBarNpc(lastOpponent))
			{
				replace = true;
				replacedNativeBarNpcId = nativeBarNpcId();
			}
			else
			{
				replace = replacedNativeBarNpcId != -1 && nativeBarNpcId() == replacedNativeBarNpcId;
			}
		}

		if (replace)
		{
			if (!nativeBar.isSelfHidden())
			{
				nativeBar.setHidden(true);
				nativeBarHidden = true;
			}
		}
		else
		{
			replacedNativeBarNpcId = -1;
			restoreNativeBar();
		}
	}

	private void updateTobBar()
	{
		final Widget tobBar = client.getWidget(InterfaceID.TobHud.PROGRESS_CONTAINER);
		if (tobBar == null)
		{
			tobBarHidden = false;
			return;
		}

		if (config.replaceNativeBossBar() && isTobBarTracking(lastOpponent) && shouldShowBarFor(lastOpponent))
		{
			if (!tobBar.isSelfHidden())
			{
				tobBar.setHidden(true);
				tobBarHidden = true;
			}
		}
		else
		{
			restoreTobBar();
		}
	}

	boolean isTobBarTracking(Actor opponent)
	{
		return opponent != null && opponent == findTobBoss();
	}

	private void restoreNativeBar()
	{
		if (!nativeBarHidden || nativeBarNpcId() == -1)
		{
			return;
		}

		final Widget nativeBar = client.getWidget(InterfaceID.HpbarHud.UNIVERSE);
		if (nativeBar != null)
		{
			nativeBar.setHidden(false);
		}
		nativeBarHidden = false;
	}

	private void restoreTobBar()
	{
		if (!tobBarHidden)
		{
			return;
		}

		final Widget tobBar = client.getWidget(InterfaceID.TobHud.PROGRESS_CONTAINER);
		if (tobBar != null && client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE) != TOB_PROGRESS_NONE)
		{
			tobBar.setHidden(false);
		}
		tobBarHidden = false;
	}

	boolean shouldShowBarFor(Actor opponent)
	{
		return opponent != null
			&& opponent.getName() != null
			&& (!config.bossOnly()
				|| isKnownBoss(opponent)
				|| isGameBarBoss(opponent)
				|| (config.showAboveCombatLevel() && opponent.getCombatLevel() >= config.minimumCombatLevel())
				|| (config.showSuperiors() && superiors.contains(opponent)));
	}

	private boolean isKnownBoss(Actor actor)
	{
		final String name = actor.getName();
		if (actor != knownBossActor || !name.equals(knownBossName))
		{
			knownBossActor = actor;
			knownBossName = name;
			knownBoss = KnownBosses.contains(name);
		}
		return knownBoss;
	}

	private void applyVanillaOverlayOverride()
	{
		if (!config.hideVanillaOverlay())
		{
			return;
		}

		if (!isVanillaOverlayHidden())
		{
			final String current = configManager.getConfiguration(VANILLA_OVERLAY_GROUP, VANILLA_OVERLAY_KEY);
			if (current == null)
			{
				configManager.unsetConfiguration(BossHealthBarConfig.GROUP, SAVED_VANILLA_OVERLAY_KEY);
			}
			else
			{
				configManager.setConfiguration(BossHealthBarConfig.GROUP, SAVED_VANILLA_OVERLAY_KEY, current);
			}
			configManager.setConfiguration(BossHealthBarConfig.GROUP, VANILLA_OVERLAY_HIDDEN_KEY, true);
		}

		configManager.setConfiguration(VANILLA_OVERLAY_GROUP, VANILLA_OVERLAY_KEY, false);
	}

	private void restoreVanillaOverlay()
	{
		if (!isVanillaOverlayHidden())
		{
			return;
		}

		final String saved = configManager.getConfiguration(BossHealthBarConfig.GROUP, SAVED_VANILLA_OVERLAY_KEY);
		if (saved == null)
		{
			configManager.unsetConfiguration(VANILLA_OVERLAY_GROUP, VANILLA_OVERLAY_KEY);
		}
		else
		{
			configManager.setConfiguration(VANILLA_OVERLAY_GROUP, VANILLA_OVERLAY_KEY, saved);
		}

		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, SAVED_VANILLA_OVERLAY_KEY);
		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, VANILLA_OVERLAY_HIDDEN_KEY);
	}

	private boolean isVanillaOverlayHidden()
	{
		return Boolean.parseBoolean(configManager.getConfiguration(BossHealthBarConfig.GROUP, VANILLA_OVERLAY_HIDDEN_KEY));
	}
}
