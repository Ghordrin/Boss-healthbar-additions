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
import java.util.Arrays;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ParamID;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.client.game.SpriteManager;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
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

	// Set to 1 on NPCs whose game boss bar only shows a percentage. No gameval constant exists for it.
	private static final int PARAM_HP_PERCENTAGE_ONLY = 2289;

	private static final int[] PHASE_MARKER_VARBITS = {
		VarbitID.HPBAR_HUD_LOWER_THRESHOLD,
		VarbitID.HPBAR_HUD_UPPER_THRESHOLD,
		VarbitID.HPBAR_HUD_HP_1,
		VarbitID.HPBAR_HUD_HP_2,
	};

	private static final int CREST_OVERLAP = 2;

	private final Client client;
	private final BossHealthBarPlugin plugin;
	private final BossHealthBarConfig config;
	private final NPCManager npcManager;
	private final SpriteManager spriteManager;
	private final ItemManager itemManager;
	private final CrestRenderer crestRenderer;
	private final BarPainter barPainter;
	private final BarTextPainter textPainter;
	private final BarAnimation animation = new BarAnimation();

	private Actor trackedOpponent;
	private BarState lastState;
	private int percentOnlyNpcId = -1;
	private boolean percentOnly;
	private boolean showingPreview;
	private long previewStartNanos;

	private Actor infoActor;
	private int infoNpcId = -1;
	private String infoName;
	private Integer infoMaxHealth;

	private final int[] phaseMarkerValues = new int[PHASE_MARKER_VARBITS.length];
	private int phaseMarkerMaxHealth;
	private float[] phaseMarkers = BarState.NO_PHASE_MARKERS;

	private ThemeColors themeColors;

	@Inject
	private BossHealthBarOverlay(
		Client client,
		BossHealthBarPlugin plugin,
		BossHealthBarConfig config,
		NPCManager npcManager,
		SpriteManager spriteManager,
		ItemManager itemManager,
		CrestRenderer crestRenderer,
		BarPainter barPainter,
		BarTextPainter textPainter)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.npcManager = npcManager;
		this.spriteManager = spriteManager;
		this.itemManager = itemManager;
		this.crestRenderer = crestRenderer;
		this.barPainter = barPainter;
		this.textPainter = textPainter;

		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setResizable(false);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, BossHealthBarPlugin.NAME);
		addMenuEntry(RUNELITE_OVERLAY, "Choose custom icon", BossHealthBarPlugin.NAME, menuEntry -> plugin.openIconPicker());
		addMenuEntry(RUNELITE_OVERLAY, "Choose fill texture", BossHealthBarPlugin.NAME, menuEntry -> plugin.openFillTexturePicker());
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
		final Actor opponent = plugin.getLastOpponent();
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
			config.animationSpeed(), config.showDamageTrail(), plugin.getLastHitMillis());

		final int barHeight = config.barHeight();
		textPainter.updateFonts();
		final boolean showHeader = config.showBossName() || config.showDamageNumber();
		final int headerHeight = showHeader ? textPainter.headerHeight() : 0;
		final String footerText = textPainter.footerText(state, defeated);
		final int footerHeight = footerText != null || config.showDefeatAnimation() ? textPainter.footerHeight() : 0;
		final ThemeColors colors = updateThemeColors();

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
		graphics.translate(-shownInset, 0);

		setOpacity(graphics, originalComposite, opacity * textOpacity);
		if (footerText != null)
		{
			textPainter.drawFooter(graphics, footerText, defeated, width, capWidth, barY + barHeight + capRise, colors);
		}
		setOpacity(graphics, originalComposite, opacity);

		graphics.translate(-leftExtent, -(topOffset + slideOffset));

		int totalHeight = topOffset + barY + barHeight + capRise + footerHeight;
		if (crest != null)
		{
			final int centerY = topOffset + headerHeight + barCenterOffset;
			totalHeight = Math.max(totalHeight, drawCrest(graphics, crest, icon, leftExtent + shownInset + CREST_OVERLAP,
				leftExtent + shownInset + shownWidth - CREST_OVERLAP, centerY, slideOffset));
		}

		graphics.setComposite(originalComposite);

		return new Dimension(leftExtent + width + rightExtent, totalHeight);
	}

	private BufferedImage resolveIcon()
	{
		final HealthBarTheme theme = config.theme();
		if (theme == HealthBarTheme.CUSTOM)
		{
			final int itemId = config.customIconItemId();
			return itemId > 0 ? itemManager.getImage(itemId) : null;
		}

		final Integer godIconSpriteId = theme.getGodIconSpriteId();
		return godIconSpriteId != null ? spriteManager.getSprite(godIconSpriteId, 0) : null;
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
		if (!plugin.shouldShowBarFor(opponent))
		{
			return null;
		}

		final boolean nativeBar = plugin.isNativeBarNpc(opponent);
		final boolean tobBar = plugin.isTobBarTracking(opponent);
		if ((nativeBar || tobBar) && !config.replaceNativeBossBar())
		{
			return null;
		}

		updateOpponentInfo(opponent);
		final String name = infoName;
		final Integer maxHealth = infoMaxHealth;

		// The game's bars have exact hitpoints, and some bosses stop sending overhead health updates
		// while they're shown, so prefer them.
		final int nativeMaxHealth = nativeBar ? client.getVarbitValue(VarbitID.HPBAR_HUD_BASEHP) : 0;
		if (nativeMaxHealth > 0)
		{
			return new BarState(name, opponent.getCombatLevel(), nativeMaxHealth,
				client.getVarbitValue(VarbitID.HPBAR_HUD_HP), nativeMaxHealth, true,
				isNativeBarPercentOnly(), readPhaseMarkers(nativeMaxHealth));
		}

		final int tobMax = tobBar ? client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_MAX) : 0;
		if (tobMax > 0)
		{
			final int tobValue = Math.max(0, Math.min(tobMax, client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)));
			return new BarState(name, opponent.getCombatLevel(), maxHealth, tobValue, tobMax, false, false, BarState.NO_PHASE_MARKERS);
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
		if (opponent instanceof NPC)
		{
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

	private float[] readPhaseMarkers(int maxHealth)
	{
		if (!config.showPhaseMarkers())
		{
			return BarState.NO_PHASE_MARKERS;
		}

		boolean changed = maxHealth != phaseMarkerMaxHealth;
		for (int i = 0; i < PHASE_MARKER_VARBITS.length; i++)
		{
			final int value = client.getVarbitValue(PHASE_MARKER_VARBITS[i]);
			if (value != phaseMarkerValues[i])
			{
				phaseMarkerValues[i] = value;
				changed = true;
			}
		}
		if (!changed)
		{
			return phaseMarkers;
		}
		phaseMarkerMaxHealth = maxHealth;

		float[] markers = null;
		int count = 0;
		for (int value : phaseMarkerValues)
		{
			if (value > 0 && value <= maxHealth + 1)
			{
				if (markers == null)
				{
					markers = new float[PHASE_MARKER_VARBITS.length];
				}
				markers[count++] = markerFraction(value, maxHealth);
			}
		}
		phaseMarkers = markers == null ? BarState.NO_PHASE_MARKERS : Arrays.copyOf(markers, count);
		return phaseMarkers;
	}

	// Same placement as the game's own bar: a marker for value v sits at (v - 1) / max health.
	static float markerFraction(int value, int maxHealth)
	{
		return clamp01((value - 1) / (float) maxHealth);
	}

	private boolean isNativeBarPercentOnly()
	{
		final int npcId = client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
		if (npcId != percentOnlyNpcId)
		{
			final NPCComposition composition = npcId != -1 ? client.getNpcDefinition(npcId) : null;
			percentOnly = composition != null && composition.getIntValue(PARAM_HP_PERCENTAGE_ONLY) == 1;
			percentOnlyNpcId = npcId;
		}
		return percentOnly;
	}

	private int barWidth(int crestWidth)
	{
		final int width = config.barWidth();
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
