package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.render.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.render.BarPainter.RASTER_SCALE;
import static com.ghordrin.bosshealthbar.render.BarPainter.capRise;
import static com.ghordrin.bosshealthbar.render.BarPainter.capWidth;
import static com.ghordrin.bosshealthbar.render.BarPainter.drawRasterImage;
import static com.ghordrin.bosshealthbar.render.BarPainter.setOpacity;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.data.BarState;
import com.ghordrin.bosshealthbar.data.BossStats;
import com.ghordrin.bosshealthbar.data.OpponentInfo;
import com.ghordrin.bosshealthbar.gamebars.GameBossBar;
import com.ghordrin.bosshealthbar.gamebars.TobBossBar;
import com.ghordrin.bosshealthbar.integrations.KillCounts;
import com.ghordrin.bosshealthbar.integrations.KillMoments;
import com.ghordrin.bosshealthbar.integrations.PartyDefence;
import com.ghordrin.bosshealthbar.integrations.SpecialAttackCounts;
import com.ghordrin.bosshealthbar.render.BarAnimation;
import com.ghordrin.bosshealthbar.render.BarEffects;
import com.ghordrin.bosshealthbar.render.BarEnds;
import com.ghordrin.bosshealthbar.render.BarIconPlacement;
import com.ghordrin.bosshealthbar.render.BarIcons;
import com.ghordrin.bosshealthbar.render.BarPainter;
import com.ghordrin.bosshealthbar.render.BarStateSelector;
import com.ghordrin.bosshealthbar.render.BarPosition;
import com.ghordrin.bosshealthbar.render.BarTextPainter;
import com.ghordrin.bosshealthbar.render.CrestRenderer;
import com.ghordrin.bosshealthbar.render.ExtraBar;
import com.ghordrin.bosshealthbar.render.GoldBar;
import com.ghordrin.bosshealthbar.render.HealthText;
import com.ghordrin.bosshealthbar.render.IntroAnimation;
import com.ghordrin.bosshealthbar.render.PartnerBar;
import com.ghordrin.bosshealthbar.theme.BarTheme;
import com.ghordrin.bosshealthbar.theme.CustomColors;
import com.ghordrin.bosshealthbar.theme.PhaseColors;
import com.ghordrin.bosshealthbar.theme.ThemeColors;
import com.ghordrin.bosshealthbar.tracking.DamageTracker;
import com.ghordrin.bosshealthbar.tracking.OpponentTracker;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;

@Singleton
public class BossHealthBarOverlay extends Overlay
{
	private static final float MAX_VIEWPORT_FRACTION = 0.85f;
	private static final int MIN_FITTED_BAR_WIDTH = 160;
	private static final int CREST_OVERLAP = 2;
	private static final float CLIP_REACH = 4096f;

	private final Client client;
	private final BossHealthBarConfig config;
	private final BarIcons barIcons;
	private final ItemManager itemManager;
	private final CrestRenderer crestRenderer;
	private final BarPainter barPainter;
	private final BarStateSelector selector;
	private final BarTextPainter textPainter;
	private final OpponentTracker opponentTracker;
	private final GameBossBar gameBossBar;
	private final TobBossBar tobBossBar;
	private final DamageTracker damageTracker;
	private final GoldBar goldBar;
	private final KillCounts killCounts;
	private final KillMoments killMoments;
	private final PartyDefence partyDefence;
	private final SpecialAttackCounts specialAttackCounts;
	private final PartnerBar partnerBar;
	private final ExtraBar extraBar;
	private final BarAnimation animation = new BarAnimation();
	private final Rectangle2D.Float burnClipShape = new Rectangle2D.Float();

	private final OpponentInfo opponentInfo;

	private ThemeColors themeColors;


	@Inject
	private BossHealthBarOverlay(
		BossHealthBarPlugin plugin,
		Client client,
		BossHealthBarConfig config,
		BarIcons barIcons,
		ItemManager itemManager,
		CrestRenderer crestRenderer,
		BarPainter barPainter,
		BarStateSelector selector,
		OpponentInfo opponentInfo,
		BarTextPainter textPainter,
		OpponentTracker opponentTracker,
		GameBossBar gameBossBar,
		TobBossBar tobBossBar,
		DamageTracker damageTracker,
		GoldBar goldBar,
		KillCounts killCounts,
		KillMoments killMoments,
		PartyDefence partyDefence,
		SpecialAttackCounts specialAttackCounts,
		PartnerBar partnerBar,
		ExtraBar extraBar)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		this.barIcons = barIcons;
		this.itemManager = itemManager;
		this.crestRenderer = crestRenderer;
		this.barPainter = barPainter;
		this.selector = selector;
		this.opponentInfo = opponentInfo;
		this.textPainter = textPainter;
		this.opponentTracker = opponentTracker;
		this.gameBossBar = gameBossBar;
		this.tobBossBar = tobBossBar;
		this.damageTracker = damageTracker;
		this.goldBar = goldBar;
		this.killCounts = killCounts;
		this.killMoments = killMoments;
		this.partyDefence = partyDefence;
		this.specialAttackCounts = specialAttackCounts;
		this.partnerBar = partnerBar;
		this.extraBar = extraBar;

		// Where the game puts its own boss bar, which this one stands in for.
		setPosition(OverlayPosition.TOP_CENTER);
		// Above the interfaces, or anything the game opens over the bar hides it.
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setResizable(true);
		setMinimumSize(MIN_FITTED_BAR_WIDTH);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, BossHealthBarPlugin.NAME);
		addMenuEntry(RUNELITE_OVERLAY, "Open panel", BossHealthBarPlugin.NAME, menuEntry -> plugin.openPanel());
		addMenuEntry(RUNELITE_OVERLAY, "Support Discord", BossHealthBarPlugin.NAME,
			menuEntry -> LinkBrowser.browse(BossHealthBarPlugin.DISCORD_URL));
	}

	void reset()
	{
		selector.forgetOpponent();
		opponentInfo.reset();
		barPainter.clearTextureCache();
		animation.resetFrameTime();
		selector.stopPreview();
		resetAnimation();
		invalidateColors();
	}

	private void resetAnimation()
	{
		animation.reset();
		partnerBar.reset();
		extraBar.reset();
		selector.resetState();
	}

	void invalidateColors()
	{
		themeColors = null;
	}

	public boolean isShowingPreview()
	{
		return selector.isShowingPreview();
	}

	// The bar's colors before any gold, for other boxes drawn in the bar's style.
	public ThemeColors baseColors()
	{
		final ThemeColors bossColors = config.matchBossColors() && !selector.isShowingPreview()
			? opponentInfo.bossColors() : null;
		return ThemeColors.withBoss(themeColors(), bossColors, BarTheme.of(config).isFlat());
	}

	// The phase the game is painting the bar in, or null when it paints its usual colors. heldBossId names
	// the boss a bar without an opponent of its own is held for, whose phase the game's bar still shows.
	private PhaseColors phaseFor(Actor opponent, int heldBossId)
	{
		if (!config.phaseColors())
		{
			return null;
		}
		final PhaseColors phase = gameBossBar.phaseColors(opponent);
		return phase != null ? phase : gameBossBar.phaseColorsFor(heldBossId);
	}

	// A phase color says what the boss is doing now, so it wins over a color picked for the boss itself.
	// Only the ornate bar lets the phase reach its frame: the flat bar's frame is the game's own outline.
	private ThemeColors withPhase(ThemeColors colors, PhaseColors phase, boolean flat)
	{
		return phase != null ? phase.applyTo(colors, config.phaseColor(), config.fortifyPhase() && !flat) : colors;
	}

	// The theme's own colors, also for a bar drawn in the same style that follows a boss of its own.
	public ThemeColors themeColors()
	{
		if (themeColors == null)
		{
			final BarTheme theme = BarTheme.of(config);
			themeColors = theme.getColors() != null ? theme.getColors() : CustomColors.fromConfig(config);
		}
		return themeColors;
	}


	private float defeatOpacity(Actor opponent, long now, boolean burnAway)
	{
		final float opacity = animation.defeatOpacity(now, burnAway);
		if (opacity < 0f && opponent == null)
		{
			selector.forgetOpponent();
			resetAnimation();
		}
		return opacity;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final Actor opponent = opponentTracker.getOpponent();
		final long now = System.nanoTime();

		final BarState state = selector.select(opponent, now, animation, this::resetAnimation);
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
		final String hitpointsText = HealthText.hitpointsText(config.hitpointsTextMode(), state.health,
			config.percentDecimals());
		final String killCountText = !config.showKillCount() ? null
			: selector.isShowingPreview() ? BarStateSelector.PREVIEW_KILL_COUNT : killCounts.textFor(state.killCountKey);
		final boolean showFightTime = config.showFightTimer();
		final String fightTimeText = showFightTime ? state.fightTime : null;
		final PartyDefence.Reading defence = defeated || !partyDefence.isAvailable() ? null
			: selector.isShowingPreview() ? BarStateSelector.PREVIEW_DEFENCE : partyDefence.readingFor(opponent);
		final PartyDefence.Reading magicDefence = defeated || !partyDefence.isMagicAvailable() ? null
			: selector.isShowingPreview() ? BarStateSelector.PREVIEW_MAGIC_DEFENCE : partyDefence.magicReadingFor(opponent);
		final List<SpecialAttackCounts.Reading> specialAttacks = specialAttackCounts.isAvailable()
			? state.specialAttacks : Collections.emptyList();
		final BossStats.Info stats = state.stats;
		final String weaknessText = stats != null && config.showWeakness() ? stats.getWeaknessText() : null;
		final String drainCapText = stats != null && config.showDrainCap() ? stats.getDrainCapText() : null;
		textPainter.updateRows(partyDefence.isAvailable(), partyDefence.isMagicAvailable(),
			specialAttackCounts.isAvailable(), weaknessText != null, drainCapText != null);
		final int headerHeight = textPainter.rowHeight(BarPosition.Row.TOP);
		final int footerHeight = textPainter.rowHeight(BarPosition.Row.BOTTOM);
		final int secondFooterHeight = textPainter.rowHeight(BarPosition.Row.BOTTOM_2);
		// Text inside a bar shorter than the text reaches past it, so that much room is kept above and below.
		final int inBarOverflow = BarTextPainter.inBarOverflow(textPainter.rowHeight(BarPosition.Row.IN_BAR), barHeight);
		final int inBarAbove = inBarOverflow / 2;
		final int inBarBelow = inBarOverflow - inBarAbove;
		final BarTheme theme = BarTheme.of(config);
		final boolean flat = theme.isFlat();
		final PhaseColors phase = phaseFor(opponent, selector.heldBossId());
		animation.fortify(now, phase != null ? phase.key() : 0);
		// The flat bar is the game's own look, so the run never plays on it and its colors are never held.
		final boolean fortify = config.fortifyPhase() && !flat;
		final long fortifyMillis = fortify ? animation.fortifyMillis(now) : -1;
		final ThemeColors baseColors = withPhase(baseColors(),
			fortify && animation.fortifyHoldsPhase(now) ? null : phase, flat);
		final long nowMillis = now / 1_000_000L;
		// A milestone or a new personal best turns the kill gold, and the flat bar its colors at least.
		final String momentText = defeated && !selector.isShowingPreview()
			? killMoments.labelFor(state.killCountKey, nowMillis) : null;
		final boolean gold = showsGold(momentText != null
			|| selector.rolledGold() && !selector.isShowingPreview() && config.rareGoldBars(), flat);
		final ThemeColors colors = gold || momentText != null ? goldBar.colors(baseColors) : baseColors;

		final BarEnds ends = config.barEnds();
		final int capRise = capRise(barHeight, flat, ends);
		final int capWidth = capWidth(barHeight, flat, ends);

		final BarIconPlacement placement = config.iconPlacement();
		final BufferedImage icon = barIcons.crestIcon(theme, opponentInfo.bossIcon(), selector.isShowingPreview());
		final CrestRenderer.Crest crest = icon != null
			? crestRenderer.getCrest(colors.getFrame(), colors.getFillHigh(), barHeight / 2f + capRise + 0.5f) : null;
		// One crest leaves the other end as it would be without any, so only the side that has one takes room.
		final boolean leftCrest = crest != null && placement.hasLeftCrest();
		final boolean rightCrest = crest != null && placement.hasRightCrest();
		final int leftExtent = leftCrest ? Math.max(0, crest.left.anchorX - CREST_OVERLAP) : 0;
		final int rightExtent = rightCrest
			? Math.max(0, Math.round(crest.right.image.getWidth() / RASTER_SCALE) - crest.right.anchorX - CREST_OVERLAP) : 0;
		final int width = barWidth(leftExtent + rightExtent);
		final int barCenterOffset = capRise + barHeight / 2;
		final int aboveBar = headerHeight + inBarAbove;
		final int crestAnchorY = Math.max(leftCrest ? crest.left.anchorY : 0, rightCrest ? crest.right.anchorY : 0);
		final int topOffset = crest != null ? Math.max(0, crestAnchorY - (aboveBar + barCenterOffset)) : 0;
		final int barY = aboveBar + capRise;

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
		final boolean partnerArea = !config.secondBarSeparate() && partnerBar.isLive(selector.isShowingPreview());
		final int partnerBarHeight = partnerBar.barThickness(barHeight);
		final int partnerCapRise = capRise(partnerBarHeight, flat, ends);
		final int partnerRowHeight = partnerArea ? partnerBar.rowHeight() : 0;
		final int partnerHeight = partnerArea ? PartnerBar.GAP + partnerBar.height(barHeight, flat, ends) : 0;

		// This one follows the game's own extra bar, so it comes and goes with it rather than lasting the fight.
		final boolean extraArea = extraBar.isLive(selector.isShowingPreview());
		final int extraHeight = extraArea ? ExtraBar.GAP + ExtraBar.height(barHeight, flat, ends) : 0;
		// Directly under the bar, so it reads as part of it and the rows below keep their order.
		final int belowBar = barY + barHeight + capRise + inBarBelow + extraHeight;

		final Composite originalComposite = graphics.getComposite();
		final float opacity = animation.fadeInOpacity(now) * defeatOpacity;
		setOpacity(graphics, originalComposite, opacity);

		textPainter.applyTextHints(graphics);
		final BufferedImage nameIcon = barIcons.nameIcon(theme, opponentInfo.bossIcon(), selector.isShowingPreview());
		textPainter.layoutText(state.name, state.shortName, state.combatLevel, state.delveLevel, nameIcon,
			hitpointsText, defeated, momentText,
			selector.isShowingPreview() ? BarStateSelector.PREVIEW_DAMAGE : 0, showFightTime, fightTimeText,
			killCountText, defence,
			defence != null || drainCapText != null ? partyDefence.icon() : null,
			magicDefence, magicDefence != null ? partyDefence.magicIcon() : null, specialAttacks, weaknessText,
			weaknessText != null ? itemManager.getImage(stats.getWeaknessElement().getRuneItemId()) : null, drainCapText,
			width, capWidth, barY + barHeight / 2, belowBar);

		graphics.translate(leftExtent, topOffset + slideOffset);

		if (headerHeight > 0)
		{
			setOpacity(graphics, originalComposite, opacity * textOpacity);
			textPainter.drawRow(graphics, BarPosition.Row.TOP, colors);
			setOpacity(graphics, originalComposite, opacity);
		}

		graphics.translate(shownInset, 0);
		final Shape barClip = burn > 0f ? graphics.getClip() : null;
		if (burn > 0f)
		{
			graphics.clip(burnClip(burn, totalWidth, leftExtent + shownInset));
		}
		barPainter.drawBar(graphics, colors, state, animation, defeated, barY, shownWidth, barHeight, fillProgress,
			shownWidth == width, flat, ends, true, true, fortifyMillis);
		// The shield sits where a bar end's ornament does, so it is only drawn on an end that has no crest.
		if (fortify && phase != null && !animation.fortifyHoldsPhase(now) && !defeated)
		{
			final int badgeY = barY + barHeight / 2;
			if (!leftCrest)
			{
				barPainter.drawFortifyBadge(graphics, capWidth, badgeY, barHeight, colors.getFrame(), fortifyMillis);
			}
			if (!rightCrest)
			{
				barPainter.drawFortifyBadge(graphics, shownWidth - capWidth, badgeY, barHeight, colors.getFrame(),
					fortifyMillis);
			}
		}
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

		if (extraArea)
		{
			extraBar.draw(graphics, themeColors(), barY + barHeight + capRise + inBarBelow, shownWidth, shownInset,
				barHeight, fillProgress, flat, ends);
		}

		setOpacity(graphics, originalComposite, opacity * textOpacity);
		textPainter.drawRow(graphics, BarPosition.Row.IN_BAR, colors);
		textPainter.drawRow(graphics, BarPosition.Row.BOTTOM, colors);
		textPainter.drawRow(graphics, BarPosition.Row.BOTTOM_2, colors);
		setOpacity(graphics, originalComposite, opacity);

		if (partnerArea)
		{
			final int partnerTop = belowBar + footerHeight + secondFooterHeight + PartnerBar.GAP;
			partnerBar.draw(graphics, originalComposite, opacity * animation.defeatTextOpacity(now, burnAway),
				opacity * textOpacity, themeColors(), partnerTop, partnerTop + partnerRowHeight + partnerCapRise,
				width, shownWidth, shownInset, capWidth, partnerBarHeight, fillProgress, flat, ends);
			setOpacity(graphics, originalComposite, opacity);
		}

		graphics.translate(-leftExtent, -(topOffset + slideOffset));

		int totalHeight = topOffset + belowBar + footerHeight + secondFooterHeight + partnerHeight;
		final int centerY = topOffset + aboveBar + barCenterOffset;
		// Crests and sparkles burn away with the bar.
		final Shape clip = burn > 0f ? graphics.getClip() : null;
		if (burn > 0f)
		{
			graphics.clip(burnClip(burn, totalWidth, 0));
		}
		if (crest != null)
		{
			totalHeight = Math.max(totalHeight, drawCrest(graphics, crest, icon, leftCrest, rightCrest,
				leftExtent + shownInset + CREST_OVERLAP,
				leftExtent + shownInset + shownWidth - CREST_OVERLAP, centerY, slideOffset));
		}

		if (gold)
		{
			final float sparkleY = centerY + slideOffset;
			final float leftX = leftCrest ? leftExtent + shownInset + CREST_OVERLAP : leftExtent + shownInset + capWidth / 2f;
			final float rightX = rightCrest
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

	// Everything left of the burning edge. originX is where the current origin is across the overlay.
	private Shape burnClip(float burn, int totalWidth, int originX)
	{
		final float edge = totalWidth * (1f - burn) - originX;
		burnClipShape.setRect(-CLIP_REACH, -CLIP_REACH, edge + CLIP_REACH, CLIP_REACH * 2);
		return burnClipShape;
	}

	static boolean showsGold(boolean rolledGold, boolean flat)
	{
		return rolledGold && !flat;
	}










	private int drawCrest(Graphics2D graphics, CrestRenderer.Crest crest, BufferedImage icon,
		boolean drawLeft, boolean drawRight, int leftX, int rightX, int centerY, int slideOffset)
	{
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		int bottom = 0;
		if (drawLeft)
		{
			bottom = Math.max(bottom, drawCrestPiece(graphics, crest, crest.left, icon, leftX, centerY, slideOffset));
		}
		if (drawRight)
		{
			bottom = Math.max(bottom, drawCrestPiece(graphics, crest, crest.right, icon, rightX, centerY, slideOffset));
		}
		return bottom;
	}

	private int drawCrestPiece(Graphics2D graphics, CrestRenderer.Crest crest, CrestRenderer.Piece piece,
		BufferedImage icon, int x, int centerY, int slideOffset)
	{
		final int width = Math.round(piece.image.getWidth() / RASTER_SCALE);
		final int height = Math.round(piece.image.getHeight() / RASTER_SCALE);
		final int y = centerY - piece.anchorY;
		drawRasterImage(graphics, piece.image, x - piece.anchorX, y + slideOffset, width, height);

		// Drawn every frame rather than baked into the plate image, because ItemManager hands out a
		// placeholder image that only fills in once the icon has loaded.
		final int iconSize = crest.iconSize;
		graphics.drawImage(icon, x - iconSize / 2, centerY + slideOffset - iconSize / 2, iconSize, iconSize, null);

		return y + height;
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
}
