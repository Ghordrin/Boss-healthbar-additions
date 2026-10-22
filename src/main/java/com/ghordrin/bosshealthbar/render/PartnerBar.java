package com.ghordrin.bosshealthbar.render;

import static com.ghordrin.bosshealthbar.render.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.render.BarPainter.capRise;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.data.BarState;
import com.ghordrin.bosshealthbar.data.KnownBosses;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import com.ghordrin.bosshealthbar.gamebars.GameBossBar;
import com.ghordrin.bosshealthbar.theme.PhaseColors;
import com.ghordrin.bosshealthbar.theme.ThemeColors;
import com.ghordrin.bosshealthbar.tracking.FightGroup;
import com.google.common.annotations.VisibleForTesting;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;

// The smaller bars for the other bosses of the same fight, in the main bar's style but without its extras. Drawn
// either under the main bar or in a box of their own, so everything they need is kept here.
@Singleton
public class PartnerBar
{
	// The room left between the main bar and the first of these.
	public static final int GAP = 3;
	private static final int MIN_HEIGHT = 4;

	private final BossHealthBarConfig config;
	private final BarPainter barPainter;
	private final BarStateSelector selector;
	private final BarTextPainter textPainter;
	private final GameBossBar gameBossBar;
	private final FightGroup group;
	private final DebugLog debugLog;

	private final BarAnimation[] animations = new BarAnimation[FightGroup.MAX_EXTRA_BARS];
	private final FightGroup.Member[] members = new FightGroup.Member[FightGroup.MAX_EXTRA_BARS];
	private final boolean[] loggedDead = new boolean[FightGroup.MAX_EXTRA_BARS];
	private final BarState[] textStates = new BarState[FightGroup.MAX_EXTRA_BARS];
	private final HitpointsTextMode[] textModes = new HitpointsTextMode[FightGroup.MAX_EXTRA_BARS];
	private final String[] texts = new String[FightGroup.MAX_EXTRA_BARS];

	@Inject
	private PartnerBar(BossHealthBarConfig config, BarPainter barPainter, BarStateSelector selector,
		BarTextPainter textPainter, GameBossBar gameBossBar, FightGroup group, DebugLog debugLog)
	{
		this.config = config;
		this.barPainter = barPainter;
		this.selector = selector;
		this.textPainter = textPainter;
		this.gameBossBar = gameBossBar;
		this.group = group;
		this.debugLog = debugLog;

		for (int extra = 0; extra < animations.length; extra++)
		{
			animations[extra] = new BarAnimation();
		}
	}

	// The one gate both places that can draw these ask, so they can never both show the same boss.
	public boolean isLive(boolean preview)
	{
		return isLive(config.showPartnerBar(), preview, group.isGroupFight());
	}

	@VisibleForTesting
	static boolean isLive(boolean show, boolean preview, boolean groupFight)
	{
		return show && (preview || groupFight);
	}

	public static int barThickness(int barHeight)
	{
		return Math.max(MIN_HEIGHT, Math.round(barHeight * 0.5f));
	}

	// The room one of these takes, without the gap before it.
	public int height(int barHeight, boolean flat, BarEnds ends)
	{
		return height(textPainter.partnerRowHeight(), barThickness(barHeight), flat, ends);
	}

	@VisibleForTesting
	static int height(int rowHeight, int barThickness, boolean flat, BarEnds ends)
	{
		return rowHeight + capRise(barThickness, flat, ends) * 2 + barThickness;
	}

	public void reset()
	{
		for (int extra = 0; extra < animations.length; extra++)
		{
			animations[extra].reset();
			members[extra] = null;
			loggedDead[extra] = false;
			textStates[extra] = null;
			textModes[extra] = null;
			texts[extra] = null;
		}
	}

	public void draw(Graphics2D graphics, int extra, Composite originalComposite, float barOpacity, float textOpacity,
		ThemeColors themeColors, int rowTop, int barY, int width, int shownWidth, int shownInset, int capWidth,
		int barThickness, float fillProgress, boolean flat, BarEnds ends)
	{
		final boolean preview = selector.isShowingPreview();
		final FightGroup.Member member = preview ? null : group.partner(extra);
		final BarAnimation animation = animations[extra];
		if (member != members[extra])
		{
			// The bars swapped, so jump to the new boss's health instead of animating from the old one's.
			animation.reset();
			members[extra] = member;
			loggedDead[extra] = false;
		}
		final BarState state = preview ? BarStateSelector.PREVIEW_PARTNER : member != null ? member.state : null;
		if (state == null)
		{
			return;
		}
		final boolean dead = member != null && member.isDead();
		if (member != null && dead != loggedDead[extra])
		{
			if (dead)
			{
				debugLog.add("Second bar: {} defeated", state.name);
			}
			loggedDead[extra] = dead;
		}
		animation.tick(dead ? 0f : clamp01(state.health.ratio / (float) state.health.scale),
			config.animationSpeed(), config.showDamageTrail(), 0);

		final ThemeColors colors = colors(themeColors, state, member != null ? member.npc() : null, flat, preview);

		setOpacity(graphics, originalComposite, textOpacity);
		textPainter.drawPartnerRow(graphics, state.name, hitpointsText(extra, state), dead, colors, width, capWidth,
			rowTop);

		setOpacity(graphics, originalComposite, barOpacity);
		graphics.translate(shownInset, 0);
		barPainter.drawBar(graphics, colors, state, animation, dead, barY, shownWidth, barThickness, fillProgress,
			false, flat, ends, false, false, -1);
		graphics.translate(-shownInset, 0);
	}

	// Each of these follows its own boss, so two bosses at once don't both take the first one's color.
	private ThemeColors colors(ThemeColors themeColors, BarState state, Actor npc, boolean flat, boolean preview)
	{
		final KnownBosses.Boss boss = config.matchBossColors() && !preview ? KnownBosses.find(state.name) : null;
		final ThemeColors colors = ThemeColors.withBoss(themeColors, boss != null ? boss.getColors() : null, flat);
		// These have no color held for a boss they stand in for, so only the phase the game paints now counts.
		final PhaseColors phase = config.phaseColors() ? gameBossBar.phaseColors(npc) : null;
		return phase != null ? phase.applyTo(colors, config.phaseColor(), config.fortifyPhase() && !flat) : colors;
	}

	private String hitpointsText(int extra, BarState state)
	{
		final HitpointsTextMode mode = config.hitpointsTextMode();
		if (state != textStates[extra] || mode != textModes[extra])
		{
			texts[extra] = HealthText.hitpointsText(mode, state.health);
			textStates[extra] = state;
			textModes[extra] = mode;
		}
		return texts[extra];
	}

	private static void setOpacity(Graphics2D graphics, Composite originalComposite, float opacity)
	{
		graphics.setComposite(opacity >= 1f
			? originalComposite
			: AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(opacity)));
	}
}
