package com.ghordrin.bosshealthbar.render;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.data.BarState;
import com.ghordrin.bosshealthbar.data.BossStats;
import com.ghordrin.bosshealthbar.data.Health;
import com.ghordrin.bosshealthbar.data.NpcUtil;
import com.ghordrin.bosshealthbar.data.OpponentInfo;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import com.ghordrin.bosshealthbar.gamebars.GameBossBar;
import com.ghordrin.bosshealthbar.gamebars.TobBossBar;
import com.ghordrin.bosshealthbar.integrations.HealthIndicatorMarkers;
import com.ghordrin.bosshealthbar.integrations.KillCounts;
import com.ghordrin.bosshealthbar.integrations.PartyDefence;
import com.ghordrin.bosshealthbar.integrations.SpecialAttackCounts;
import com.ghordrin.bosshealthbar.tracking.BossMemory;
import com.ghordrin.bosshealthbar.tracking.DelveLevel;
import com.ghordrin.bosshealthbar.tracking.FightGroup;
import com.ghordrin.bosshealthbar.tracking.FightTimer;
import com.ghordrin.bosshealthbar.tracking.HealthReader;
import com.ghordrin.bosshealthbar.tracking.OpponentTracker;
import java.awt.Color;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

// What the bar shows this frame: which opponent it follows, whether it is held up after a defeat or a switch,
// and the preview when there is nothing to show. Keeps no Graphics, so the decisions can be tested on their own.
@Singleton
public class BarStateSelector
{
	private static final String PREVIEW_NAME = "Preview";
	private static final int PREVIEW_COMBAT_LEVEL = 450;
	private static final int PREVIEW_MAX_HEALTH = 500;
	private static final int[] PREVIEW_HEALTH = {500, 500, 440, 385, 310, 250, 190, 120, 70, 70, 70, 500, 500};
	private static final Duration PREVIEW_STEP = Duration.ofMillis(1200);
	private static final float[] PREVIEW_PHASE_MARKERS = {0.5f};
	public static final String PREVIEW_KILL_COUNT = KillCounts.text(128);
	public static final int PREVIEW_DAMAGE = 37;
	// A sample fight time, shown the way the game's precise timing setting has the real one shown.
	private static final int PREVIEW_FIGHT_TICKS = 139;
	public static final PartyDefence.Reading PREVIEW_DEFENCE = new PartyDefence.Reading("42", Color.WHITE);
	public static final PartyDefence.Reading PREVIEW_MAGIC_DEFENCE = new PartyDefence.Reading("60", Color.WHITE);
	private static final Duration SWITCH_HOLD = Duration.ofSeconds(3);
	private static final BossStats.Info PREVIEW_STATS = new BossStats.Info(BossStats.Element.FIRE,
		BossStats.weaknessText(new BossStats.Weakness(BossStats.Element.FIRE, 40)), BossStats.drainCapText(20));
	public static final BarState PREVIEW_PARTNER = BarState.healthOnly("Partner",
		new Health(300, PREVIEW_MAX_HEALTH, true, PREVIEW_MAX_HEALTH, false));

	private final BossHealthBarConfig config;
	private final OpponentTracker opponentTracker;
	private final HealthReader healthReader;
	private final GameBossBar gameBossBar;
	private final TobBossBar tobBossBar;
	private final FightTimer fightTimer;
	private final DelveLevel delveLevel;
	private final FightGroup group;
	private final OpponentInfo opponentInfo;
	private final HealthIndicatorMarkers indicatorMarkers;
	private final SpecialAttackCounts specialAttackCounts;
	private final GoldBar goldBar;
	private final ItemManager itemManager;
	private final DebugLog debugLog;

	private Actor trackedOpponent;
	private int trackedLife;
	private BarState lastState;
	// The fight lastState was built in, so a held bar doesn't pick up a later fight's time.
	private int lastStateFight = FightTimer.NO_TICK;
	private Actor leftAlive;
	// Kept up to date while the opponent is in the scene: the client empties a despawned NPC, so by the time
	// one counts as having left there is no composition on it to read an ID from.
	private Actor baseIdActor;
	private int trackedBaseId = -1;
	private boolean loggedGameBarHold;
	// The boss the bar is held for while it has no opponent of its own to read, or -1.
	private int heldBossId = -1;
	private boolean showingPreview;
	private long previewStartNanos;
	private boolean rolledGold;
	private long switchHoldUntilNanos;
	private boolean retargetPending;
	private boolean resuming;
	private Health resumedHealth;
	private List<SpecialAttackCounts.Reading> previewSpecialAttacks;

	@Inject
	private BarStateSelector(
		BossHealthBarConfig config,
		OpponentTracker opponentTracker,
		HealthReader healthReader,
		GameBossBar gameBossBar,
		TobBossBar tobBossBar,
		FightTimer fightTimer,
		DelveLevel delveLevel,
		FightGroup group,
		OpponentInfo opponentInfo,
		HealthIndicatorMarkers indicatorMarkers,
		SpecialAttackCounts specialAttackCounts,
		GoldBar goldBar,
		ItemManager itemManager,
		DebugLog debugLog)
	{
		this.config = config;
		this.opponentTracker = opponentTracker;
		this.healthReader = healthReader;
		this.gameBossBar = gameBossBar;
		this.tobBossBar = tobBossBar;
		this.fightTimer = fightTimer;
		this.delveLevel = delveLevel;
		this.group = group;
		this.opponentInfo = opponentInfo;
		this.indicatorMarkers = indicatorMarkers;
		this.specialAttackCounts = specialAttackCounts;
		this.goldBar = goldBar;
		this.itemManager = itemManager;
		this.debugLog = debugLog;
	}

	public boolean isShowingPreview()
	{
		return showingPreview;
	}

	public boolean rolledGold()
	{
		return rolledGold;
	}

	public void forgetOpponent()
	{
		trackedOpponent = null;
	}

	public void stopPreview()
	{
		showingPreview = false;
	}

	// The held bar and the switch it was waiting for. The animations themselves are reset by the overlay.
	public void resetState()
	{
		lastState = null;
		lastStateFight = FightTimer.NO_TICK;
		leftAlive = null;
		baseIdActor = null;
		trackedBaseId = -1;
		heldBossId = -1;
		switchHoldUntilNanos = 0;
		retargetPending = false;
		resuming = false;
		resumedHealth = null;
	}

	// The boss the bar is held for while it has no opponent to read, so the overlay can take that boss's phase
	// color from the game's bar. -1 while the bar follows an opponent of its own.
	public int heldBossId()
	{
		return heldBossId;
	}

	public BarState select(Actor opponent, long now, BarAnimation animation, Runnable resetAll)
	{
		heldBossId = -1;
		boolean holdingForNext = false;
		// While the game's bar holds the boss, something else hit in the meantime doesn't take the bar over.
		boolean keepHeldBar = false;
		final int life = opponentTracker.opponentLife();
		// The same opponent back up after being defeated is the next one, so the bar starts over for it.
		if (opponent != trackedOpponent || life != trackedLife)
		{
			trackedLife = life;
			final OpponentTracker.DespawnOutcome outcome = opponentTracker.despawnOutcome(trackedOpponent);
			if (opponent == null && trackedOpponent != null && lastState != null && !animation.isDefeatPlaying()
				&& config.showDefeatAnimation()
				&& defeatedAtDespawn(outcome, trackedOpponent.isDead(), lastState.health.ratio,
					outcome == OpponentTracker.DespawnOutcome.OUT_OF_SIGHT))
			{
				// The opponent despawned as it died, so keep drawing it until the defeat animation ends.
				animation.startDefeat(now);
				debugLog.add("Bar: defeat animation started for {}", DebugLog.describe(trackedOpponent));
			}

			// One boss can leave alive as another takes over, so the bar waits a moment for the next one.
			final boolean left = outcome == OpponentTracker.DespawnOutcome.LEFT;
			if (opponent == null && left && lastState != null && !animation.isDefeatPlaying()
				&& leftAlive != trackedOpponent)
			{
				leftAlive = trackedOpponent;
				loggedGameBarHold = false;
				switchHoldUntilNanos = now + SWITCH_HOLD.toNanos();
				debugLog.add("Bar: {} left alive, holding the bar for the next opponent",
					DebugLog.describe(trackedOpponent));
			}
			// A boss that goes untargetable mid-fight leaves the scene while the game's own bar stays up on
			// it, so the bar waits for as long as that bar does instead of only the few seconds.
			final boolean onGameBar = onGameBar();
			holdingForNext = holdsForNext(left, leftAlive == trackedOpponent, now < switchHoldUntilNanos, onGameBar);
			if (holdingForNext && onGameBar && now >= switchHoldUntilNanos && !loggedGameBarHold)
			{
				loggedGameBarHold = true;
				// The NPC is empty by now, so its ID is the only thing left to name it by.
				debugLog.add("Bar: holding the bar while the game's bar still shows NPC id {}", trackedBaseId);
			}

			// A mechanic that has to be hit while the boss is away, such as something it hides behind, is a
			// target but not the fight. The bar stays on the boss and that target goes to the second bar.
			keepHeldBar = holdingForNext && onGameBar && opponent != null && !isHeldBoss(opponent);

			final Switch next = switchTo(opponent != null, opponentTracker.resumedFromMemory(opponent),
				trackedOpponent != null && lastState != null, opponentTracker.continuesFight(opponent),
				animation.isDefeatPlaying(), holdingForNext, keepHeldBar);
			if (next == Switch.RESUME)
			{
				// An opponent back from out of sight carries on from its remembered health, without the intro.
				final Health resumed = rememberedHealth(opponent);
				if (trackedOpponent == null || lastState == null || animation.isDefeatPlaying())
				{
					resetAll.run();
					animation.skipIntro();
				}
				if (resumed == null)
				{
					// With nothing remembered there is no bar to carry over, so the previous one isn't kept up.
					lastState = null;
					lastStateFight = FightTimer.NO_TICK;
				}
				debugLog.add("Bar: resumed {} from memory at {}/{}", DebugLog.describe(opponent),
					resumed != null ? resumed.ratio : -1, resumed != null ? resumed.scale : -1);
				trackedOpponent = opponent;
				resumedHealth = resumed;
				animation.retarget();
				retargetPending = true;
				resuming = true;
			}
			else if (next == Switch.MOVE)
			{
				// Switching targets mid-fight, e.g. between the NPCs of a group boss. The bar stays up and
				// moves over, instead of playing the intro again.
				debugLog.add("Bar: moved mid-fight from {} to {}{}", DebugLog.describe(trackedOpponent),
					DebugLog.describe(opponent), healthNote(opponent));
				trackedOpponent = opponent;
				retargetPending = true;
				resuming = false;
				switchHoldUntilNanos = now + SWITCH_HOLD.toNanos();
				leftAlive = null;
			}
			else if (next == Switch.RESET)
			{
				if (opponent != null)
				{
					debugLog.add("Bar: new bar for {}{}", DebugLog.describe(opponent), healthNote(opponent));
				}
				else
				{
					debugLog.add("Bar: cleared");
				}
				trackedOpponent = opponent;
				resetAll.run();
				rolledGold = opponent != null && config.rareGoldBars() && goldBar.roll();
			}
		}

		final boolean wasShowingPreview = showingPreview;
		showingPreview = false;

		// A target that doesn't get a bar falls through to the preview, same as no target.
		rememberBaseId(trackedOpponent);

		final HealthReader.Reading reading = opponent != null ? healthReader.read(opponent) : null;
		final BarState state = opponent != null && !keepHeldBar ? readState(opponent, reading) : null;
		if (state != null)
		{
			if (resuming && resumedHealth != null
				&& BossMemory.healedSince(resumedHealth.ratio, resumedHealth.scale, resumedHealth.exactHealth,
					state.health.ratio, state.health.scale))
			{
				opponentTracker.healedOnReturn(opponent);
			}
			lastState = state;
			lastStateFight = fightTimer.getStartTick();
			resuming = false;
			resumedHealth = null;
			if (retargetPending)
			{
				animation.retarget();
				retargetPending = false;
			}

			final DefeatStep step = defeatStep(opponentTracker.isOpponentDefeated(), animation.isDefeatPlaying(),
				config.showDefeatAnimation());
			if (step == DefeatStep.CANCEL)
			{
				animation.cancelDefeat();
			}
			else if (step == DefeatStep.START)
			{
				animation.startDefeat(now);
				debugLog.add("Bar: defeat animation started for {} (in view)", DebugLog.describe(opponent));
			}
			// While the opponent is still here the bar stays on "Defeated" for up to "Hide after", in case it comes back.
			if (animation.isDefeatPlaying())
			{
				animation.holdDefeat(now, config.hideDelay() * 1_000_000_000L);
			}
			return state;
		}

		if ((animation.isDefeatPlaying() || holdingForNext) && lastState != null)
		{
			final String heldFightTime = heldFightTime(lastState.fightTime, fightTimer.getText(), lastStateFight,
				fightTimer.getStartTick());
			if (!Objects.equals(lastState.fightTime, heldFightTime))
			{
				lastState = lastState.withFightTime(heldFightTime);
			}
			// A boss held because the game's bar still shows it keeps taking its health from that bar, which
			// carries it without the NPC. A defeat plays out on the health it died at.
			if (holdingForNext && !animation.isDefeatPlaying() && onGameBar())
			{
				heldBossId = trackedBaseId;
				final HealthReader.Reading held = healthReader.readGameBar();
				if (held != null && (held.health.ratio != lastState.health.ratio
					|| held.health.scale != lastState.health.scale))
				{
					lastState = lastState.withHealth(held.health);
				}
			}
			return lastState;
		}

		// A new target has no health to show until it's been hit, so keep the previous bar up meanwhile.
		if (opponent != null && lastState != null && (resuming || now < switchHoldUntilNanos)
			&& awaitingHealth(opponent, reading))
		{
			final String fightTime = fightTimer.getText();
			if (resuming && !Objects.equals(lastState.fightTime, fightTime))
			{
				lastState = lastState.withFightTime(fightTime);
			}
			return lastState;
		}

		if (config.showPreview())
		{
			if (!wasShowingPreview)
			{
				resetAll.run();
				previewStartNanos = now;
			}
			showingPreview = true;
			return previewState(now);
		}

		return null;
	}

	enum Switch
	{
		RESUME,
		MOVE,
		RESET,
		KEEP
	}

	// What the bar does when the opponent changes. RESET starts a new bar, or clears it when there's no opponent.
	// sameFight is the fight timer's call that the new opponent carries on the fight, so the bar moves when the timer
	// keeps counting. holdingForNext is set for a short while after the last opponent left alive.
	private boolean onGameBar()
	{
		return trackedBaseId != -1 && gameBossBar.trackedNpcId() == trackedBaseId;
	}

	private boolean isHeldBoss(Actor opponent)
	{
		return opponent instanceof NPC && NpcUtil.baseId((NPC) opponent) == trackedBaseId;
	}

	// Only when the opponent changes, since the ID can't change under one NPC and the overlay asks every frame.
	private void rememberBaseId(Actor opponent)
	{
		if (opponent == baseIdActor)
		{
			return;
		}
		baseIdActor = opponent;
		if (opponent instanceof NPC)
		{
			final int baseId = NpcUtil.baseId((NPC) opponent);
			if (baseId != -1)
			{
				trackedBaseId = baseId;
			}
		}
	}

	// The bar is held for the boss that left while the switch window lasts, and for as long as the game's own
	// bar still shows that boss, which is how an untargetable phase says the fight is still on.
	static boolean holdsForNext(boolean left, boolean sameLeaver, boolean withinWindow, boolean onGameBar)
	{
		return left && sameLeaver && (withinWindow || onGameBar);
	}

	static Switch switchTo(boolean hasOpponent, boolean resumed, boolean hasBar, boolean sameFight,
		boolean defeatPlaying, boolean holdingForNext, boolean keepHeldBar)
	{
		if (keepHeldBar)
		{
			return Switch.KEEP;
		}
		if (hasOpponent && resumed)
		{
			return Switch.RESUME;
		}
		if (hasOpponent && hasBar && (sameFight || holdingForNext) && !defeatPlaying)
		{
			return Switch.MOVE;
		}
		if (!hasOpponent && holdingForNext)
		{
			return Switch.KEEP;
		}
		return hasOpponent || !defeatPlaying ? Switch.RESET : Switch.KEEP;
	}

	enum DefeatStep
	{
		START,
		CANCEL,
		NONE
	}

	static DefeatStep defeatStep(boolean defeated, boolean playing, boolean animationOn)
	{
		if (!defeated)
		{
			return DefeatStep.CANCEL;
		}
		return !playing && animationOn ? DefeatStep.START : DefeatStep.NONE;
	}

	// Whether an opponent that's gone died. The tracker's reading of its despawn decides when there is one, otherwise
	// the bar's last reading of it.
	static boolean defeatedAtDespawn(OpponentTracker.DespawnOutcome outcome, boolean dead, int lastRatio,
		boolean outOfSight)
	{
		if (outcome == OpponentTracker.DespawnOutcome.LEFT)
		{
			return false;
		}
		return outcome == OpponentTracker.DespawnOutcome.DEFEATED || !outOfSight && (dead || lastRatio <= 0);
	}

	// A bar held up after its fight follows the timer while it is still that fight's, and keeps its last time once a
	// cleared timer or another fight has taken it over.
	static String heldFightTime(String heldTime, String timerText, int heldFight, int timerFight)
	{
		return timerText != null && timerFight == heldFight ? timerText : heldTime;
	}

	private BarState readState(Actor opponent, HealthReader.Reading reading)
	{
		if (!opponentTracker.shouldShowBarFor(opponent))
		{
			return null;
		}

		if ((gameBossBar.isTracking(opponent) || tobBossBar.isTracking(opponent))
			&& config.nativeBossBarMode().hidesOurBar())
		{
			return null;
		}

		opponentInfo.update(opponent);

		if (reading != null)
		{
			final float[] markers = reading.source == HealthReader.Source.GAME_BAR && config.showPhaseMarkers()
				? gameBossBar.phaseMarkers(reading.health.scale) : BarState.NO_PHASE_MARKERS;
			return barState(opponent, reading.health, markers);
		}

		// No reading yet, such as an NPC that left and came back or a boss bar that just moved over, so it keeps the
		// health it last had in this fight.
		final Health remembered = rememberedHealth(opponent);
		if (remembered != null)
		{
			return barState(opponent, remembered, BarState.NO_PHASE_MARKERS);
		}
		return null;
	}

	// The opponent's extras come from OpponentInfo, which readState updates first.
	private BarState barState(Actor opponent, Health health, float[] phaseMarkers)
	{
		final String name = opponentInfo.name();
		return new BarState(name, opponentInfo.shortName(), opponent.getCombatLevel(),
			delveLevel.forOpponent(name), health, phaseMarkers,
			indicatorMarkers.markersFor(name), opponentInfo.killCountKey(), opponentInfo.stats(),
			specialAttackCounts.readings(), fightTimer.getText());
	}

	private String healthNote(Actor opponent)
	{
		if (opponent.getHealthScale() > 0 && opponent.getHealthRatio() >= 0)
		{
			return ", health " + opponent.getHealthRatio() + "/" + opponent.getHealthScale();
		}
		final Health remembered = rememberedHealth(opponent);
		return remembered != null ? ", no reading yet, remembered " + remembered.ratio + "/" + remembered.scale : ", no reading yet";
	}

	// Nothing is remembered before the fight starts, so a new fight doesn't start from an older reading.
	private Health rememberedHealth(Actor opponent)
	{
		return opponent instanceof NPC && fightTimer.getStartTick() != FightTimer.NO_TICK
			? group.rememberedHealth(((NPC) opponent).getIndex(), Text.removeTags(opponent.getName()))
			: null;
	}

	private boolean awaitingHealth(Actor opponent, HealthReader.Reading reading)
	{
		return reading == null && opponentTracker.shouldShowBarFor(opponent);
	}

	private BarState previewState(long now)
	{
		final int step = (int) ((now - previewStartNanos) / PREVIEW_STEP.toNanos() % PREVIEW_HEALTH.length);
		return new BarState(PREVIEW_NAME, null, PREVIEW_COMBAT_LEVEL, BarState.NO_DELVE_LEVEL,
			new Health(PREVIEW_HEALTH[step], PREVIEW_MAX_HEALTH, true, PREVIEW_MAX_HEALTH, false),
			config.showPhaseMarkers() ? PREVIEW_PHASE_MARKERS : BarState.NO_PHASE_MARKERS,
			HealthIndicatorMarkers.NONE, null, PREVIEW_STATS,
			specialAttackCounts.isAvailable() ? previewSpecialAttacks() : Collections.emptyList(),
			FightTimer.format(PREVIEW_FIGHT_TICKS, fightTimer.isPreciseTiming()));
	}

	private List<SpecialAttackCounts.Reading> previewSpecialAttacks()
	{
		if (previewSpecialAttacks == null)
		{
			previewSpecialAttacks = Arrays.asList(
				new SpecialAttackCounts.Reading(itemManager.getImage(ItemID.DRAGON_WARHAMMER), "2", Color.WHITE),
				new SpecialAttackCounts.Reading(itemManager.getImage(ItemID.BGS), "31", Color.WHITE));
		}
		return previewSpecialAttacks;
	}
}
