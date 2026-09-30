package com.ghordrin.bosshealthbar;

import com.google.common.base.Strings;
import static com.ghordrin.bosshealthbar.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.BarPainter.RASTER_SCALE;
import static com.ghordrin.bosshealthbar.BarPainter.drawRasterImage;
import static com.ghordrin.bosshealthbar.BarPainter.scaledCapRise;
import static com.ghordrin.bosshealthbar.BarPainter.scaledCapWidth;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Duration;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ParamID;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.client.game.SpriteManager;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

class BossHealthBarOverlay extends Overlay
{
	private static final String PREVIEW_NAME = "Preview";
	private static final int PREVIEW_COMBAT_LEVEL = 450;
	private static final int PREVIEW_MAX_HEALTH = 500;
	private static final int[] PREVIEW_HEALTH = {500, 500, 440, 385, 310, 250, 190, 120, 70, 70, 70, 500, 500};
	private static final Duration PREVIEW_STEP = Duration.ofMillis(1200);
	private static final float[] PREVIEW_PHASE_MARKERS = {0.5f};
	private static final float MAX_VIEWPORT_FRACTION = 0.85f;
	private static final int MIN_FITTED_BAR_WIDTH = 160;
	private static final int CREST_OVERLAP = 2;

	private final Client client;
	private final BossHealthBarConfig config;
	private final NPCManager npcManager;
	private final SpriteManager spriteManager;
	private final ItemManager itemManager;
	private final CrestRenderer crestRenderer;
	private final BarPainter barPainter;
	private final BarTextPainter textPainter;
	private final OpponentTracker opponentTracker;
	private final GameBossBar gameBossBar;
	private final TobBossBar tobBossBar;
	private final DamageTracker damageTracker;
	private final GoldBar goldBar;
	private final BarAnimation animation = new BarAnimation();

	private Actor trackedOpponent;
	private BarState lastState;
	private boolean showingPreview;
	private long previewStartNanos;
	private boolean rolledGold;

	private Actor infoActor;
	private int infoNpcId = -1;
	private String infoName;
	private Integer infoMaxHealth;
	private KnownBosses.Icon infoBossIcon;
	private ThemeColors infoBossColors;

	private int bossSpriteId = -1;
	private BufferedImage bossSprite;

	private ThemeColors themeColors;

	@Inject
	private BossHealthBarOverlay(
		BossHealthBarPlugin plugin,
		Client client,
		BossHealthBarConfig config,
		NPCManager npcManager,
		SpriteManager spriteManager,
		ItemManager itemManager,
		CrestRenderer crestRenderer,
		BarPainter barPainter,
		BarTextPainter textPainter,
		OpponentTracker opponentTracker,
		GameBossBar gameBossBar,
		TobBossBar tobBossBar,
		DamageTracker damageTracker,
		GoldBar goldBar,
		Pickers pickers)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		this.npcManager = npcManager;
		this.spriteManager = spriteManager;
		this.itemManager = itemManager;
		this.crestRenderer = crestRenderer;
		this.barPainter = barPainter;
		this.textPainter = textPainter;
		this.opponentTracker = opponentTracker;
		this.gameBossBar = gameBossBar;
		this.tobBossBar = tobBossBar;
		this.damageTracker = damageTracker;
		this.goldBar = goldBar;

		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setResizable(true);
		setMinimumSize(MIN_FITTED_BAR_WIDTH);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, BossHealthBarPlugin.NAME);
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

	void resetAnimation()
	{
		animation.reset();
		lastState = null;
	}

	void invalidateColors()
	{
		themeColors = null;
	}

	private ThemeColors updateThemeColors()
	{
		if (themeColors == null)
		{
			final HealthBarTheme theme = config.theme();
			themeColors = theme.getColors() != null ? theme.getColors() : ThemeColors.builder()
				.fillHigh(config.customFillHighColor())
				.fillLow(config.customFillLowColor())
				.trail(config.customTrailColor())
				.frame(config.customFrameColor())
				.text(config.customTextColor())
				.levelText(config.customLevelTextColor())
				.hitpointsText(config.customHitpointsTextColor())
				.defeatedText(config.customDefeatedTextColor())
				.build();
		}
		return themeColors;
	}

	private BarState selectState(Actor opponent, long now)
	{
		if (opponent != trackedOpponent)
		{
			if (opponent == null && trackedOpponent != null && lastState != null && !animation.isDefeatPlaying()
				&& config.showDefeatAnimation() && (trackedOpponent.isDead() || lastState.ratio <= 0))
			{
				// The opponent despawned as it died, so keep drawing it until the defeat animation ends.
				animation.startDefeat(now);
			}

			if (opponent != null || !animation.isDefeatPlaying())
			{
				trackedOpponent = opponent;
				resetAnimation();
				rolledGold = opponent != null && config.rareGoldBars() && goldBar.roll();
			}
		}

		final boolean wasShowingPreview = showingPreview;
		showingPreview = false;

		// A target that doesn't get a bar falls through to the preview, same as no target.
		final BarState state = opponent != null ? readState(opponent) : null;
		if (state != null)
		{
			lastState = state;

			if (!opponent.isDead())
			{
				animation.cancelDefeat();
			}
			else if (!animation.isDefeatPlaying() && config.showDefeatAnimation())
			{
				animation.startDefeat(now);
			}
			return state;
		}

		if (animation.isDefeatPlaying() && lastState != null)
		{
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

	private float defeatOpacity(Actor opponent, long now)
	{
		final float opacity = animation.defeatOpacity(now);
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
		final float defeatOpacity = defeatOpacity(opponent, now);
		if (defeatOpacity < 0f)
		{
			return null;
		}

		animation.tick(defeated ? 0f : clamp01(state.ratio / (float) state.scale),
			config.animationSpeed(), config.showDamageTrail(), damageTracker.getLastHitMillis());

		final int barHeight = config.barHeight();
		textPainter.updateFonts();
		final boolean showHeader = config.showBossName() || config.showDamageNumber();
		final int headerHeight = showHeader ? textPainter.headerHeight() : 0;
		final String footerText = textPainter.footerText(state, defeated);
		final int footerHeight = footerText != null || config.showDefeatAnimation() ? textPainter.footerHeight() : 0;
		final ThemeColors baseColors = config.matchBossColors() && !showingPreview && infoBossColors != null
			? infoBossColors : updateThemeColors();
		final boolean gold = rolledGold && !showingPreview && config.rareGoldBars();
		final ThemeColors colors = gold ? goldBar.colors(baseColors) : baseColors;
		final long nowMillis = now / 1_000_000L;

		final int capRise = scaledCapRise(barHeight);
		final int capWidth = scaledCapWidth(barHeight);

		final BufferedImage icon = resolveIcon();
		final CrestRenderer.Crest crest = icon != null
			? crestRenderer.getCrest(colors.getFrame(), colors.getFillHigh(), 1f, barHeight / 2f + capRise + 0.5f) : null;
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
		final float textOpacity = animation.textOpacity(intro, now);
		final int minShownWidth = Math.min(width, capWidth * 2 + 4);
		final int shownWidth = Math.round(minShownWidth + (width - minShownWidth) * expandProgress);
		final int shownInset = (width - shownWidth) / 2;

		final Composite originalComposite = graphics.getComposite();
		final float opacity = animation.fadeInOpacity(now) * defeatOpacity;
		setOpacity(graphics, originalComposite, opacity);

		textPainter.applyTextHints(graphics);

		graphics.translate(leftExtent, topOffset + slideOffset);

		if (showHeader)
		{
			setOpacity(graphics, originalComposite, opacity * textOpacity);
			textPainter.drawHeader(graphics, state.name, state.combatLevel, width, capWidth,
				headerHeight - textPainter.headerBaselineGap(), colors);
			setOpacity(graphics, originalComposite, opacity);
		}

		graphics.translate(shownInset, 0);
		barPainter.drawBar(graphics, colors, state, animation, defeated, barY, shownWidth, barHeight, fillProgress, shownWidth == width);
		if (gold)
		{
			goldBar.drawShine(graphics, capWidth - 1, barY - 1, shownWidth - capWidth * 2 + 2, barHeight + 2,
				animation.introElapsedMillis(now));
		}
		graphics.translate(-shownInset, 0);

		setOpacity(graphics, originalComposite, opacity * textOpacity);
		if (footerText != null)
		{
			textPainter.drawFooter(graphics, footerText, defeated, width, capWidth, barY + barHeight + capRise, colors);
		}
		setOpacity(graphics, originalComposite, opacity);

		graphics.translate(-leftExtent, -(topOffset + slideOffset));

		int totalHeight = topOffset + barY + barHeight + capRise + footerHeight;
		final int centerY = topOffset + headerHeight + barCenterOffset;
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

		graphics.setComposite(originalComposite);

		return new Dimension(leftExtent + width + rightExtent, totalHeight);
	}

	private BufferedImage resolveIcon()
	{
		if (config.useBossIcon() && !showingPreview && infoBossIcon != null)
		{
			final BufferedImage bossIcon = bossIcon(infoBossIcon);
			if (bossIcon != null)
			{
				return bossIcon;
			}
		}

		final HealthBarTheme theme = config.theme();
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

	private BarState readState(Actor opponent)
	{
		if (!opponentTracker.shouldShowBarFor(opponent))
		{
			return null;
		}

		final boolean nativeBar = gameBossBar.isTracking(opponent);
		final boolean tobBar = tobBossBar.isTracking(opponent);
		if ((nativeBar || tobBar) && !config.replaceNativeBossBar())
		{
			return null;
		}

		updateOpponentInfo(opponent);
		final String name = infoName;
		final Integer maxHealth = infoMaxHealth;

		// The game's bars have exact hitpoints, and some bosses stop sending overhead health updates
		// while they're shown, so prefer them.
		final int nativeMaxHealth = nativeBar ? gameBossBar.maxHealth() : 0;
		if (nativeMaxHealth > 0)
		{
			final float[] markers = config.showPhaseMarkers()
				? gameBossBar.phaseMarkers(nativeMaxHealth) : BarState.NO_PHASE_MARKERS;
			return new BarState(name, opponent.getCombatLevel(), nativeMaxHealth,
				gameBossBar.health(), nativeMaxHealth, true, gameBossBar.isPercentOnly(), markers);
		}

		final int tobMax = tobBar ? tobBossBar.maxHealth() : 0;
		if (tobMax > 0)
		{
			return new BarState(name, opponent.getCombatLevel(), maxHealth, tobBossBar.health(tobMax), tobMax,
				false, false, BarState.NO_PHASE_MARKERS);
		}

		if (opponent.getHealthScale() > 0)
		{
			return new BarState(name, opponent.getCombatLevel(), maxHealth,
				opponent.getHealthRatio(), opponent.getHealthScale(), false, false, BarState.NO_PHASE_MARKERS);
		}

		return null;
	}

	private BarState previewState(long now)
	{
		final int step = (int) ((now - previewStartNanos) / PREVIEW_STEP.toNanos() % PREVIEW_HEALTH.length);
		return new BarState(PREVIEW_NAME, PREVIEW_COMBAT_LEVEL, PREVIEW_MAX_HEALTH, PREVIEW_HEALTH[step],
			PREVIEW_MAX_HEALTH, true, false, config.showPhaseMarkers() ? PREVIEW_PHASE_MARKERS : BarState.NO_PHASE_MARKERS);
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
		Integer maxHealth = null;
		boolean complete = true;
		infoBossIcon = null;
		infoBossColors = null;
		if (opponent instanceof NPC)
		{
			infoBossIcon = KnownBosses.icon(name);
			infoBossColors = KnownBosses.colors(name);
			if (composition != null)
			{
				final String longName = composition.getStringValue(ParamID.NPC_HP_NAME);
				if (!Strings.isNullOrEmpty(longName))
				{
					name = longName;
				}
				maxHealth = npcManager.getHealth(npcId);
			}
			else
			{
				complete = false;
			}
		}

		infoName = name;
		infoMaxHealth = maxHealth;
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
