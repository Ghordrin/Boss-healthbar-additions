package com.ghordrin.bosshealthbar.render;

import static com.ghordrin.bosshealthbar.render.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.render.BarPainter.capRise;
import static com.ghordrin.bosshealthbar.render.BarPainter.setOpacity;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.data.BarState;
import com.ghordrin.bosshealthbar.data.KnownBosses;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import com.ghordrin.bosshealthbar.gamebars.GameBossBar;
import com.ghordrin.bosshealthbar.theme.PhaseColors;
import com.ghordrin.bosshealthbar.theme.ThemeColors;
import com.ghordrin.bosshealthbar.tracking.FightGroup;
import com.google.common.annotations.VisibleForTesting;
import java.awt.Composite;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;

// The smaller bar for the other boss of the same fight, in the main bar's style but without its extras. Drawn
// either under the main bar or in a box of its own, so everything it needs is kept here.
@Singleton
public class PartnerBar
{
	// The room left between the main bar and this one.
	public static final int GAP = 3;
	public static final int MIN_HEIGHT = 4;

	private final BossHealthBarConfig config;
	private final BarPainter barPainter;
	private final BarStateSelector selector;
	private final BarTextPainter textPainter;
	private final GameBossBar gameBossBar;
	private final FightGroup group;
	private final DebugLog debugLog;
	private final BarAnimation animation = new BarAnimation();

	private FightGroup.Member member;
	private boolean loggedDead;
	private BarState textState;
	private HitpointsTextMode textMode;
	private String text;

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
	}

	// The one gate both places that can draw this ask, so they can never both show the same boss.
	public boolean isLive(boolean preview)
	{
		return isLive(config.showPartnerBar(), preview, group.isGroupFight());
	}

	@VisibleForTesting
	static boolean isLive(boolean show, boolean preview, boolean groupFight)
	{
		return show && (preview || groupFight);
	}

	// A share of the main bar's height, so the two keep their proportion whatever that height is set to.
	public int barThickness(int barHeight)
	{
		return barThickness(barHeight, config.secondBarScale());
	}

	@VisibleForTesting
	static int barThickness(int barHeight, int scalePercent)
	{
		return Math.max(MIN_HEIGHT, Math.round(barHeight * scalePercent / 100f));
	}

	public int rowHeight()
	{
		prepare();
		return textPainter.partnerRowHeight();
	}

	// The room this takes, without the gap before it.
	public int height(int barHeight, boolean flat, BarEnds ends)
	{
		return height(rowHeight(), barThickness(barHeight), flat, ends);
	}

	public static int height(int rowHeight, int barThickness, boolean flat, BarEnds ends)
	{
		return rowHeight + capRise(barThickness, flat, ends) * 2 + barThickness;
	}

	public void applyTextHints(Graphics2D graphics)
	{
		textPainter.applyTextHints(graphics);
	}

	// This holds its own text painter, so the fonts and the rows have to be set up before either is read. The
	// row only shows the name and the hitpoints, so the items the other rows carry are left out.
	private void prepare()
	{
		textPainter.updateFonts();
		textPainter.updateRows(false, false, false, false, false);
	}

	public void reset()
	{
		animation.reset();
		member = null;
		loggedDead = false;
		textState = null;
		textMode = null;
		text = null;
	}

	public void draw(Graphics2D graphics, Composite originalComposite, float barOpacity, float textOpacity,
		ThemeColors themeColors, int rowTop, int barY, int width, int shownWidth, int shownInset, int capWidth,
		int barThickness, float fillProgress, boolean flat, BarEnds ends)
	{
		prepare();
		final boolean preview = selector.isShowingPreview();
		final FightGroup.Member next = preview ? null : group.partner();
		if (next != member)
		{
			// The bars swapped, so jump to the new boss's health instead of animating from the old one's.
			animation.reset();
			member = next;
			loggedDead = false;
		}
		final BarState state = preview ? BarStateSelector.PREVIEW_PARTNER : member != null ? member.state : null;
		if (state == null)
		{
			return;
		}
		final boolean dead = member != null && member.isDead();
		if (member != null && dead != loggedDead)
		{
			if (dead)
			{
				debugLog.add("Second bar: {} defeated", state.name);
			}
			loggedDead = dead;
		}
		animation.tick(dead ? 0f : clamp01(state.health.ratio / (float) state.health.scale),
			config.animationSpeed(), config.showDamageTrail(), 0);

		final ThemeColors colors = colors(themeColors, state, member != null ? member.npc() : null, flat, preview);

		setOpacity(graphics, originalComposite, textOpacity);
		textPainter.drawPartnerRow(graphics, state.name, hitpointsText(state), dead, colors, width, capWidth, rowTop);

		setOpacity(graphics, originalComposite, barOpacity);
		graphics.translate(shownInset, 0);
		barPainter.drawBar(graphics, colors, state, animation, dead, barY, shownWidth, barThickness, fillProgress,
			false, flat, ends, false, false, -1);
		graphics.translate(-shownInset, 0);
	}

	// This follows its own boss, so two bosses at once don't both take the first one's color.
	private ThemeColors colors(ThemeColors themeColors, BarState state, Actor npc, boolean flat, boolean preview)
	{
		final KnownBosses.Boss boss = config.matchBossColors() && !preview ? KnownBosses.find(state.name) : null;
		final ThemeColors colors = ThemeColors.withBoss(themeColors, boss != null ? boss.getColors() : null, flat);
		// This has no color held for a boss it stands in for, so only the phase the game paints now counts.
		final PhaseColors phase = config.phaseColors() ? gameBossBar.phaseColors(npc) : null;
		return phase != null ? phase.applyTo(colors, config.phaseColor(), config.fortifyPhase() && !flat) : colors;
	}

	private String hitpointsText(BarState state)
	{
		final HitpointsTextMode mode = config.hitpointsTextMode();
		if (state != textState || mode != textMode)
		{
			text = HealthText.hitpointsText(mode, state.health);
			textState = state;
			textMode = mode;
		}
		return text;
	}
}
