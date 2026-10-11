package com.ghordrin.bosshealthbar;

import com.google.common.base.Strings;
import static com.ghordrin.bosshealthbar.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.BarPainter.RASTER_SCALE;
import static com.ghordrin.bosshealthbar.BarPainter.drawRasterImage;
import static com.ghordrin.bosshealthbar.BarPainter.capRise;
import static com.ghordrin.bosshealthbar.BarPainter.capWidth;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ParamID;
import net.runelite.api.gameval.ItemID;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Singleton
class BossHealthBarOverlay extends Overlay
{
	private static final String PREVIEW_NAME = "Preview";
	private static final int PREVIEW_COMBAT_LEVEL = 450;
	private static final int PREVIEW_MAX_HEALTH = 500;
	private static final int[] PREVIEW_HEALTH = {500, 500, 440, 385, 310, 250, 190, 120, 70, 70, 70, 500, 500};
	private static final Duration PREVIEW_STEP = Duration.ofMillis(1200);
	private static final float[] PREVIEW_PHASE_MARKERS = {0.5f};
	private static final String PREVIEW_KILL_COUNT = KillCounts.text(128);
	private static final int PREVIEW_DAMAGE = 37;
	private static final String PREVIEW_FIGHT_TIME = "1:23";
	private static final PartyDefence.Reading PREVIEW_DEFENCE = new PartyDefence.Reading("42", Color.WHITE);
	private static final PartyDefence.Reading PREVIEW_MAGIC_DEFENCE = new PartyDefence.Reading("60", Color.WHITE);
	private static final BossStats.Info PREVIEW_STATS = new BossStats.Info(BossStats.Element.FIRE,
		BossStats.weaknessText(new BossStats.Weakness(BossStats.Element.FIRE, 40)), BossStats.drainCapText(20));
	private static final float MAX_VIEWPORT_FRACTION = 0.85f;
	private static final int MIN_FITTED_BAR_WIDTH = 160;
	private static final int CREST_OVERLAP = 2;
	private static final Duration SWITCH_HOLD = Duration.ofSeconds(3);
	private static final float CLIP_REACH = 4096f;
	private static final BarState PREVIEW_PARTNER = BarState.healthOnly("Partner",
		new Health(300, PREVIEW_MAX_HEALTH, true, PREVIEW_MAX_HEALTH, false));
	private static final int PARTNER_GAP = 3;
	private static final int PARTNER_MIN_HEIGHT = 4;

	private final Client client;
	private final BossHealthBarConfig config;
	private final SpriteManager spriteManager;
	private final ItemManager itemManager;
	private final CrestRenderer crestRenderer;
	private final BarPainter barPainter;
	private final BarTextPainter textPainter;
	private final OpponentTracker opponentTracker;
	private final HealthReader healthReader;
	private final GameBossBar gameBossBar;
	private final TobBossBar tobBossBar;
	private final DamageTracker damageTracker;
	private final GoldBar goldBar;
	private final HealthIndicatorMarkers indicatorMarkers;
	private final KillCounts killCounts;
	private final PartyDefence partyDefence;
	private final SpecialAttackCounts specialAttackCounts;
	private final FightTimer fightTimer;
	private final FightGroup group;
	private final DebugLog debugLog;
	private final BarAnimation animation = new BarAnimation();
	private final BarAnimation partnerAnimation = new BarAnimation();
	private final Rectangle2D.Float burnClipShape = new Rectangle2D.Float();

	private Actor trackedOpponent;
	private int trackedLife;
	private BarState lastState;
	private Actor leftAlive;
	private boolean showingPreview;
	private long previewStartNanos;
	private boolean rolledGold;
	private long switchHoldUntilNanos;
	private boolean retargetPending;
	private FightGroup.Member partnerMember;
	private boolean partnerLoggedDead;
	private BarState partnerTextState;
	private HitpointsTextMode partnerTextMode;
	private String partnerText;
	private boolean resuming;
	private Health resumedHealth;

	private Actor infoActor;
	private int infoNpcId = -1;
	private String infoName;
	private String infoKillCountKey;
	private BossStats.Info infoStats;
	private KnownBosses.Icon infoBossIcon;
	private ThemeColors infoBossColors;

	private int bossSpriteId = -1;
	private BufferedImage bossSprite;

	private ThemeColors themeColors;

	private List<SpecialAttackCounts.Reading> previewSpecialAttacks;

	@Inject
	private BossHealthBarOverlay(
		BossHealthBarPlugin plugin,
		Client client,
		BossHealthBarConfig config,
		SpriteManager spriteManager,
		ItemManager itemManager,
		CrestRenderer crestRenderer,
		BarPainter barPainter,
		BarTextPainter textPainter,
		OpponentTracker opponentTracker,
		HealthReader healthReader,
		GameBossBar gameBossBar,
		TobBossBar tobBossBar,
		DamageTracker damageTracker,
		GoldBar goldBar,
		HealthIndicatorMarkers indicatorMarkers,
		KillCounts killCounts,
		PartyDefence partyDefence,
		SpecialAttackCounts specialAttackCounts,
		FightTimer fightTimer,
		FightGroup group,
		Pickers pickers,
		DebugLog debugLog,
		DebugExport debugExport)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		this.spriteManager = spriteManager;
		this.itemManager = itemManager;
		this.crestRenderer = crestRenderer;
		this.barPainter = barPainter;
		this.textPainter = textPainter;
		this.opponentTracker = opponentTracker;
		this.healthReader = healthReader;
		this.gameBossBar = gameBossBar;
		this.tobBossBar = tobBossBar;
		this.damageTracker = damageTracker;
		this.goldBar = goldBar;
		this.indicatorMarkers = indicatorMarkers;
		this.killCounts = killCounts;
		this.partyDefence = partyDefence;
		this.specialAttackCounts = specialAttackCounts;
		this.fightTimer = fightTimer;
		this.group = group;
		this.debugLog = debugLog;

		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setResizable(true);
		setMinimumSize(MIN_FITTED_BAR_WIDTH);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, BossHealthBarPlugin.NAME);
		addMenuEntry(RUNELITE_OVERLAY, "Save debug log", BossHealthBarPlugin.NAME, menuEntry -> debugExport.save());
		addMenuEntry(RUNELITE_OVERLAY, "Choose custom icon", BossHealthBarPlugin.NAME, menuEntry -> pickers.openIconPicker());
		addMenuEntry(RUNELITE_OVERLAY, "Choose fill texture", BossHealthBarPlugin.NAME, menuEntry -> pickers.openFillTexturePicker());
	}

	void reset()
	{
		trackedOpponent = null;
		infoActor = null;
		barPainter.clearTextureCache();
		animation.resetFrameTime();
		showingPreview = false;
		resetAnimation();
		invalidateColors();
	}

	private void resetAnimation()
	{
		animation.reset();
		lastState = null;
		leftAlive = null;
		switchHoldUntilNanos = 0;
		retargetPending = false;
		partnerAnimation.reset();
		partnerMember = null;
		resuming = false;
		resumedHealth = null;
	}

	void invalidateColors()
	{
		themeColors = null;
	}

	boolean isShowingPreview()
	{
		return showingPreview;
	}

	// The bar's colors before any gold, for other boxes drawn in the bar's style.
	ThemeColors baseColors()
	{
		return barColors(updateThemeColors(), config.matchBossColors() && !showingPreview ? infoBossColors : null,
			BarTheme.of(config).isFlat());
	}

	private ThemeColors updateThemeColors()
	{
		if (themeColors == null)
		{
			final BarTheme theme = BarTheme.of(config);
			themeColors = theme.getColors() != null ? theme.getColors() : CustomColors.fromConfig(config);
		}
		return themeColors;
	}

	private BarState selectState(Actor opponent, long now)
	{
		boolean holdingForNext = false;
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
				switchHoldUntilNanos = now + SWITCH_HOLD.toNanos();
				debugLog.add("Bar: {} left alive, holding the bar for the next opponent",
					DebugLog.describe(trackedOpponent));
			}
			holdingForNext = left && leftAlive == trackedOpponent && now < switchHoldUntilNanos;

			final Switch next = switchTo(opponent != null, opponentTracker.resumedFromMemory(opponent),
				trackedOpponent != null && lastState != null, opponentTracker.continuesFight(opponent),
				animation.isDefeatPlaying(), holdingForNext);
			if (next == Switch.RESUME)
			{
				// An opponent back from out of sight carries on from its remembered health, without the intro.
				final Health resumed = rememberedHealth(opponent);
				if (trackedOpponent == null || lastState == null || animation.isDefeatPlaying())
				{
					resetAnimation();
					animation.skipIntro();
				}
				if (resumed == null)
				{
					// With nothing remembered there is no bar to carry over, so the previous one isn't kept up.
					lastState = null;
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
				resetAnimation();
				rolledGold = opponent != null && config.rareGoldBars() && goldBar.roll();
			}
		}

		final boolean wasShowingPreview = showingPreview;
		showingPreview = false;

		// A target that doesn't get a bar falls through to the preview, same as no target.
		final HealthReader.Reading reading = opponent != null ? healthReader.read(opponent) : null;
		final BarState state = opponent != null ? readState(opponent, reading) : null;
		if (state != null)
		{
			if (resuming && resumedHealth != null
				&& BossMemory.healedSince(resumedHealth.ratio, resumedHealth.scale, resumedHealth.exactHealth,
					state.health.ratio, state.health.scale))
			{
				opponentTracker.healedOnReturn(opponent);
			}
			lastState = state;
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
			// A cleared timer leaves the held bar on its last time instead of dropping the line.
			final String heldFightTime = fightTimer.getText();
			if (heldFightTime != null && !Objects.equals(lastState.fightTime, heldFightTime))
			{
				lastState = lastState.withFightTime(heldFightTime);
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
				resetAnimation();
				previewStartNanos = now;
			}
			showingPreview = true;
			return previewState(now);
		}

		return null;
	}

	private float defeatOpacity(Actor opponent, long now, boolean burnAway)
	{
		final float opacity = animation.defeatOpacity(now, burnAway);
		if (opacity < 0f && opponent == null)
		{
			trackedOpponent = null;
			resetAnimation();
		}
		return opacity;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final Actor opponent = opponentTracker.getOpponent();
		final long now = System.nanoTime();

		final BarState state = selectState(opponent, now);
		if (state == null)
		{
			return null;
		}

		final boolean defeated = animation.isDefeatPlaying();
		final boolean burnAway = config.burnAwayDefeat();
		final float defeatOpacity = defeatOpacity(opponent, now, burnAway);
		if (defeatOpacity < 0f)
		{
			return null;
		}
		final long defeatMillis = animation.defeatEffectMillis(now);
		final float burn = burnAway ? BarEffects.burnProgress(defeatMillis) : 0f;

		animation.tick(defeated ? 0f : clamp01(state.health.ratio / (float) state.health.scale),
			config.animationSpeed(), config.showDamageTrail(), damageTracker.getLastHitMillis());

		final int barHeight = config.barHeight();
		textPainter.updateFonts();
		final String hitpointsText = HealthText.hitpointsText(config.hitpointsTextMode(), state.health);
		final String killCountText = !config.showKillCount() ? null
			: showingPreview ? PREVIEW_KILL_COUNT : killCounts.textFor(state.killCountKey);
		final boolean showFightTime = config.showFightTimer();
		final String fightTimeText = showFightTime ? state.fightTime : null;
		final PartyDefence.Reading defence = defeated || !partyDefence.isAvailable() ? null
			: showingPreview ? PREVIEW_DEFENCE : partyDefence.readingFor(opponent);
		final PartyDefence.Reading magicDefence = defeated || !partyDefence.isMagicAvailable() ? null
			: showingPreview ? PREVIEW_MAGIC_DEFENCE : partyDefence.magicReadingFor(opponent);
		final List<SpecialAttackCounts.Reading> specialAttacks = specialAttackCounts.isAvailable()
			? state.specialAttacks : Collections.emptyList();
		final BossStats.Info stats = state.stats;
		final String weaknessText = stats != null && config.showWeakness() ? stats.getWeaknessText() : null;
		final String drainCapText = stats != null && config.showDrainCap() ? stats.getDrainCapText() : null;
		textPainter.updateRows(partyDefence.isAvailable(), partyDefence.isMagicAvailable(),
			specialAttackCounts.isAvailable(), weaknessText != null, drainCapText != null);
		final int headerHeight = textPainter.topRowHeight();
		final int footerHeight = textPainter.bottomRowHeight();
		final BarTheme theme = BarTheme.of(config);
		final boolean flat = theme.isFlat();
		final ThemeColors baseColors = baseColors();
		final boolean gold = showsGold(rolledGold && !showingPreview && config.rareGoldBars(), flat);
		final ThemeColors colors = gold ? goldBar.colors(baseColors) : baseColors;
		final long nowMillis = now / 1_000_000L;

		final BarEnds ends = config.barEnds();
		final int capRise = capRise(barHeight, flat, ends);
		final int capWidth = capWidth(barHeight, flat, ends);

		final BufferedImage icon = resolveIcon(theme);
		final CrestRenderer.Crest crest = icon != null && !flat
			? crestRenderer.getCrest(colors.getFrame(), colors.getFillHigh(), barHeight / 2f + capRise + 0.5f) : null;
		final int leftExtent = crest != null ? Math.max(0, crest.left.anchorX - CREST_OVERLAP) : 0;
		final int rightExtent = crest != null
			? Math.max(0, Math.round(crest.right.image.getWidth() / RASTER_SCALE) - crest.right.anchorX - CREST_OVERLAP) : 0;
		final int width = barWidth(leftExtent + rightExtent);
		final int barCenterOffset = capRise + barHeight / 2;
		final int topOffset = crest != null
			? Math.max(0, Math.max(crest.left.anchorY, crest.right.anchorY) - (headerHeight + barCenterOffset)) : 0;
		final int barY = headerHeight + capRise;

		final IntroAnimation intro = config.introAnimation();
		final int slideOffset = animation.slideOffset(intro, now);
		final float expandProgress = animation.expandProgress(intro, now);
		final float fillProgress = animation.fillProgress(intro, now);
		final float textOpacity = animation.textOpacity(intro, now) * animation.defeatTextOpacity(now, burnAway);
		final int minShownWidth = Math.min(width, capWidth * 2 + 4);
		final int shownWidth = Math.round(minShownWidth + (width - minShownWidth) * expandProgress);
		final int shownInset = (width - shownWidth) / 2;
		final int totalWidth = leftExtent + width + rightExtent;

		// Kept for the whole fight, so the bar doesn't move when the other boss's health first shows.
		final boolean partnerArea = config.showPartnerBar() && (showingPreview || group.isGroupFight());
		final int partnerBarHeight = Math.max(PARTNER_MIN_HEIGHT, Math.round(barHeight * 0.5f));
		final int partnerCapRise = capRise(partnerBarHeight, flat, ends);
		final int partnerRowHeight = partnerArea ? textPainter.partnerRowHeight() : 0;
		final int partnerHeight = partnerArea ? PARTNER_GAP + partnerRowHeight + partnerCapRise * 2 + partnerBarHeight : 0;

		final Composite originalComposite = graphics.getComposite();
		final float opacity = animation.fadeInOpacity(now) * defeatOpacity;
		setOpacity(graphics, originalComposite, opacity);

		textPainter.applyTextHints(graphics);
		textPainter.layoutText(state.name, state.combatLevel, flat ? icon : nameBossIcon(), hitpointsText, defeated,
			showingPreview ? PREVIEW_DAMAGE : 0, showFightTime, fightTimeText, killCountText, defence,
			defence != null || drainCapText != null ? partyDefence.icon() : null,
			magicDefence, magicDefence != null ? partyDefence.magicIcon() : null, specialAttacks, weaknessText,
			weaknessText != null ? itemManager.getImage(stats.getWeaknessElement().getRuneItemId()) : null, drainCapText,
			width, capWidth, barY + barHeight + capRise);

		graphics.translate(leftExtent, topOffset + slideOffset);

		if (headerHeight > 0)
		{
			setOpacity(graphics, originalComposite, opacity * textOpacity);
			textPainter.drawRow(graphics, true, colors);
			setOpacity(graphics, originalComposite, opacity);
		}

		graphics.translate(shownInset, 0);
		final Shape barClip = burn > 0f ? graphics.getClip() : null;
		if (burn > 0f)
		{
			graphics.clip(burnClip(burn, totalWidth, leftExtent + shownInset));
		}
		barPainter.drawBar(graphics, colors, state, animation, defeated, barY, shownWidth, barHeight, fillProgress,
			shownWidth == width, flat, ends, true, true);
		if (gold)
		{
			goldBar.drawShine(graphics, capWidth - 1, barY - 1, shownWidth - capWidth * 2 + 2, barHeight + 2,
				animation.introElapsedMillis(now));
		}
		if (burn > 0f)
		{
			graphics.setClip(barClip);
		}
		graphics.translate(-shownInset, 0);

		setOpacity(graphics, originalComposite, opacity * textOpacity);
		textPainter.drawRow(graphics, false, colors);
		setOpacity(graphics, originalComposite, opacity);

		if (partnerArea)
		{
			final int partnerTop = barY + barHeight + capRise + footerHeight + PARTNER_GAP;
			drawPartner(graphics, originalComposite, opacity * animation.defeatTextOpacity(now, burnAway),
				opacity * textOpacity, baseColors, partnerTop, partnerTop + partnerRowHeight + partnerCapRise, width,
				shownWidth, shownInset, capWidth, partnerBarHeight, fillProgress, flat, ends);
			setOpacity(graphics, originalComposite, opacity);
		}

		graphics.translate(-leftExtent, -(topOffset + slideOffset));

		int totalHeight = topOffset + barY + barHeight + capRise + footerHeight + partnerHeight;
		final int centerY = topOffset + headerHeight + barCenterOffset;
		// Crests and sparkles burn away with the bar.
		final Shape clip = burn > 0f ? graphics.getClip() : null;
		if (burn > 0f)
		{
			graphics.clip(burnClip(burn, totalWidth, 0));
		}
		if (crest != null)
		{
			totalHeight = Math.max(totalHeight, drawCrest(graphics, crest, icon, leftExtent + shownInset + CREST_OVERLAP,
				leftExtent + shownInset + shownWidth - CREST_OVERLAP, centerY, slideOffset));
		}

		if (gold)
		{
			final float sparkleY = centerY + slideOffset;
			final float leftX = crest != null ? leftExtent + shownInset + CREST_OVERLAP : leftExtent + shownInset + capWidth / 2f;
			final float rightX = crest != null
				? leftExtent + shownInset + shownWidth - CREST_OVERLAP : leftExtent + shownInset + shownWidth - capWidth / 2f;
			final float radius = crest != null ? crest.iconSize * 0.75f : Math.max(6f, barHeight);
			goldBar.drawSparkles(graphics, leftX, sparkleY, radius, false, nowMillis);
			goldBar.drawSparkles(graphics, rightX, sparkleY, radius, true, nowMillis);
		}
		if (burn > 0f)
		{
			graphics.setClip(clip);
		}

		if (burn > 0f && burn < 1f)
		{
			final int barTop = topOffset + slideOffset + barY;
			barPainter.drawBurn(graphics, burn, defeatMillis, 0, totalWidth, barTop - capRise - 1,
				barTop + barHeight + capRise + 1, centerY + slideOffset);
		}

		graphics.setComposite(originalComposite);

		return new Dimension(leftExtent + width + rightExtent, totalHeight);
	}

	// Another boss of the same fight, in the main bar's colours but without its extras.
	private void drawPartner(Graphics2D graphics, Composite originalComposite, float barOpacity, float textOpacity,
		ThemeColors colors, int rowTop, int barY, int width, int shownWidth, int shownInset, int capWidth, int barHeight,
		float fillProgress, boolean flat, BarEnds ends)
	{
		final FightGroup.Member member = showingPreview ? null : group.partner();
		if (member != partnerMember)
		{
			// The bars swapped, so jump to the new partner's health instead of animating from the old one's.
			partnerAnimation.reset();
			partnerMember = member;
			partnerLoggedDead = false;
		}
		final BarState partner = showingPreview ? PREVIEW_PARTNER : member != null ? member.state : null;
		if (partner == null)
		{
			return;
		}
		final boolean dead = member != null && member.isDead();
		if (member != null && dead != partnerLoggedDead)
		{
			if (dead)
			{
				debugLog.add("Second bar: {} defeated", partner.name);
			}
			partnerLoggedDead = dead;
		}
		partnerAnimation.tick(dead ? 0f : clamp01(partner.health.ratio / (float) partner.health.scale),
			config.animationSpeed(), config.showDamageTrail(), 0);

		setOpacity(graphics, originalComposite, textOpacity);
		textPainter.drawPartnerRow(graphics, partner.name, partnerHitpointsText(partner), dead, colors, width, capWidth,
			rowTop);

		setOpacity(graphics, originalComposite, barOpacity);
		graphics.translate(shownInset, 0);
		barPainter.drawBar(graphics, colors, partner, partnerAnimation, dead, barY, shownWidth, barHeight, fillProgress,
			false, flat, ends, false, false);
		graphics.translate(-shownInset, 0);
	}

	private String partnerHitpointsText(BarState partner)
	{
		final HitpointsTextMode mode = config.hitpointsTextMode();
		if (partner != partnerTextState || mode != partnerTextMode)
		{
			partnerText = HealthText.hitpointsText(mode, partner.health);
			partnerTextState = partner;
			partnerTextMode = mode;
		}
		return partnerText;
	}

	// Everything left of the burning edge. originX is where the current origin is across the overlay.
	private Shape burnClip(float burn, int totalWidth, int originX)
	{
		final float edge = totalWidth * (1f - burn) - originX;
		burnClipShape.setRect(-CLIP_REACH, -CLIP_REACH, edge + CLIP_REACH, CLIP_REACH * 2);
		return burnClipShape;
	}

	static ThemeColors barColors(ThemeColors themeColors, ThemeColors bossColors, boolean flat)
	{
		return bossColors != null && !flat ? bossColors : themeColors;
	}

	static boolean showsGold(boolean rolledGold, boolean flat)
	{
		return rolledGold && !flat;
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
	static Switch switchTo(boolean hasOpponent, boolean resumed, boolean hasBar, boolean sameFight,
		boolean defeatPlaying, boolean holdingForNext)
	{
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

	// Without crests, the boss's icon goes small before the name, as on the Oldschool theme.
	private BufferedImage nameBossIcon()
	{
		if (config.showIcons() || !config.useBossIcon() || showingPreview || infoBossIcon == null)
		{
			return null;
		}
		return bossIcon(infoBossIcon);
	}

	private BufferedImage resolveIcon(BarTheme theme)
	{
		if (!config.showIcons())
		{
			return null;
		}

		if (config.useBossIcon() && !showingPreview && infoBossIcon != null)
		{
			final BufferedImage bossIcon = bossIcon(infoBossIcon);
			if (bossIcon != null)
			{
				return bossIcon;
			}
		}

		if (theme == HealthBarTheme.CUSTOM)
		{
			final int itemId = config.customIconItemId();
			return itemId > 0 ? itemManager.getImage(itemId) : null;
		}

		final Integer godIconSpriteId = theme.getGodIconSpriteId();
		return godIconSpriteId != null ? spriteManager.getSprite(godIconSpriteId, 0) : null;
	}

	private BufferedImage bossIcon(KnownBosses.Icon icon)
	{
		if (icon.isItem())
		{
			return itemManager.getImage(icon.getId());
		}

		if (icon.getId() != bossSpriteId)
		{
			final BufferedImage sprite = spriteManager.getSprite(icon.getId(), 0);
			if (sprite == null)
			{
				return null;
			}
			// The crest draws its icon into a square, so pad the sprite rather than stretch it.
			final int size = Math.max(sprite.getWidth(), sprite.getHeight());
			bossSprite = ImageUtil.resizeCanvas(sprite, size, size);
			bossSpriteId = icon.getId();
		}
		return bossSprite;
	}

	private int drawCrest(Graphics2D graphics, CrestRenderer.Crest crest, BufferedImage icon,
		int leftX, int rightX, int centerY, int slideOffset)
	{
		final CrestRenderer.Piece left = crest.left;
		final CrestRenderer.Piece right = crest.right;
		final int leftWidth = Math.round(left.image.getWidth() / RASTER_SCALE);
		final int leftHeight = Math.round(left.image.getHeight() / RASTER_SCALE);
		final int rightWidth = Math.round(right.image.getWidth() / RASTER_SCALE);
		final int rightHeight = Math.round(right.image.getHeight() / RASTER_SCALE);
		final int leftY = centerY - left.anchorY;
		final int rightY = centerY - right.anchorY;

		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		drawRasterImage(graphics, left.image, leftX - left.anchorX, leftY + slideOffset, leftWidth, leftHeight);
		drawRasterImage(graphics, right.image, rightX - right.anchorX, rightY + slideOffset, rightWidth, rightHeight);

		// Drawn every frame rather than baked into the plate image, because ItemManager hands out a
		// placeholder image that only fills in once the icon has loaded.
		final int iconSize = crest.iconSize;
		graphics.drawImage(icon, leftX - iconSize / 2, centerY + slideOffset - iconSize / 2, iconSize, iconSize, null);
		graphics.drawImage(icon, rightX - iconSize / 2, centerY + slideOffset - iconSize / 2, iconSize, iconSize, null);

		return Math.max(leftY + leftHeight, rightY + rightHeight);
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

		updateOpponentInfo(opponent);

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

	// The opponent's extras come from updateOpponentInfo, which readState calls first.
	private BarState barState(Actor opponent, Health health, float[] phaseMarkers)
	{
		return new BarState(infoName, opponent.getCombatLevel(), health, phaseMarkers,
			indicatorMarkers.markersFor(infoName), infoKillCountKey, infoStats, specialAttackCounts.readings(),
			fightTimer.getText());
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
		return new BarState(PREVIEW_NAME, PREVIEW_COMBAT_LEVEL,
			new Health(PREVIEW_HEALTH[step], PREVIEW_MAX_HEALTH, true, PREVIEW_MAX_HEALTH, false),
			config.showPhaseMarkers() ? PREVIEW_PHASE_MARKERS : BarState.NO_PHASE_MARKERS,
			HealthIndicatorMarkers.NONE, null, PREVIEW_STATS,
			specialAttackCounts.isAvailable() ? previewSpecialAttacks() : Collections.emptyList(), PREVIEW_FIGHT_TIME);
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

	private void updateOpponentInfo(Actor opponent)
	{
		final NPCComposition composition = opponent instanceof NPC
			? ((NPC) opponent).getTransformedComposition() : null;
		final int npcId = composition != null ? composition.getId() : -1;
		if (opponent == infoActor && npcId == infoNpcId)
		{
			return;
		}

		String name = Text.removeTags(opponent.getName());
		boolean complete = true;
		infoBossIcon = null;
		infoBossColors = null;
		infoKillCountKey = null;
		infoStats = null;
		if (opponent instanceof NPC)
		{
			final KnownBosses.Boss boss = KnownBosses.find(name);
			if (boss != null)
			{
				infoBossIcon = boss.getIcon();
				infoBossColors = boss.getColors();
				infoKillCountKey = boss.getKillCountKey();
			}
			infoStats = BossStats.info(NpcUtil.currentId((NPC) opponent));
			if (composition != null)
			{
				final String longName = composition.getStringValue(ParamID.NPC_HP_NAME);
				if (!Strings.isNullOrEmpty(longName) && longName.contains(name))
				{
					name = longName;
				}
			}
			else
			{
				complete = false;
			}
		}

		infoName = name;
		infoActor = complete ? opponent : null;
		infoNpcId = npcId;
	}

	private int barWidth(int crestWidth)
	{
		// Alt-dragging the overlay's edge sets a preferred size, which takes over from "Bar width" until
		// the overlay is reset. Only its width is used; the bar's height has its own setting.
		final Dimension preferred = getPreferredSize();
		final int width = preferred != null
			? Math.max(MIN_FITTED_BAR_WIDTH, preferred.width - crestWidth)
			: config.barWidth();
		final int viewportWidth = client.getViewportWidth();
		if (!config.fitToGameView() || viewportWidth <= 0)
		{
			return width;
		}
		final int available = Math.round(viewportWidth * MAX_VIEWPORT_FRACTION) - crestWidth;
		return Math.max(MIN_FITTED_BAR_WIDTH, Math.min(width, available));
	}

	private static void setOpacity(Graphics2D graphics, Composite originalComposite, float opacity)
	{
		graphics.setComposite(opacity >= 1f
			? originalComposite
			: AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(opacity)));
	}
}
