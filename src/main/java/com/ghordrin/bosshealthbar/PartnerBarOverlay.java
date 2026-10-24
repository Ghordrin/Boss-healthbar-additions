package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.render.BarPainter.capRise;
import static com.ghordrin.bosshealthbar.render.BarPainter.capWidth;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.render.BarEnds;
import com.ghordrin.bosshealthbar.render.PartnerBar;
import com.ghordrin.bosshealthbar.theme.BarTheme;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

// The second bar in a place of its own, so it can be dragged away from the main one.
public class PartnerBarOverlay extends Overlay
{
	private static final int MIN_BAR_WIDTH = 80;
	// One minimum covers both of an overlay's sides, and a drag that only moves one side keeps the other at the
	// size it was drawn at, so this has to stay under the smallest box the bar draws or a width drag would push
	// the bar thicker. The width keeps its own floor below, and the thickness has the bar's own.
	private static final int MIN_OVERLAY_SIZE = PartnerBar.MIN_HEIGHT;

	private final BossHealthBarConfig config;
	private final BossHealthBarOverlay barOverlay;
	private final PartnerBar partnerBar;

	@Inject
	private PartnerBarOverlay(BossHealthBarPlugin plugin, BossHealthBarConfig config,
		BossHealthBarOverlay barOverlay, PartnerBar partnerBar)
	{
		super(plugin);
		this.config = config;
		this.barOverlay = barOverlay;
		this.partnerBar = partnerBar;

		setPosition(OverlayPosition.TOP_CENTER);
		// The main bar's layer, or anything the game opens over this one hides it.
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setResizable(true);
		setMinimumSize(MIN_OVERLAY_SIZE);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, BossHealthBarPlugin.NAME);
		addMenuEntry(RUNELITE_OVERLAY, "Open panel", BossHealthBarPlugin.NAME, menuEntry -> plugin.openPanel());
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Nothing rendered leaves no gap in the corner this snaps to, so this returns null rather than an empty row.
		if (!config.secondBarSeparate() || !partnerBar.isLive(barOverlay.isShowingPreview()))
		{
			return null;
		}

		final Dimension preferred = getPreferredSize();
		final int width = barWidth(preferred, config.barWidth());
		final boolean flat = BarTheme.of(config).isFlat();
		final BarEnds ends = config.barEnds();
		final int rowHeight = partnerBar.rowHeight();
		final int barThickness = barThickness(preferred, rowHeight, flat, ends,
			partnerBar.barThickness(config.barHeight()));
		final int capRise = capRise(barThickness, flat, ends);

		partnerBar.applyTextHints(graphics);
		final Composite originalComposite = graphics.getComposite();
		partnerBar.draw(graphics, originalComposite, 1f, 1f, barOverlay.themeColors(), 0, rowHeight + capRise,
			width, width, 0, capWidth(barThickness, flat, ends), barThickness, 1f, flat, ends);
		graphics.setComposite(originalComposite);

		return new Dimension(width, PartnerBar.height(rowHeight, barThickness, flat, ends));
	}

	// Alt-dragging the overlay's edge sets a preferred size, which takes over from "Bar width" and "Second bar
	// scale" until the overlay is reset.
	static int barWidth(Dimension preferred, int configured)
	{
		return preferred != null ? Math.max(MIN_BAR_WIDTH, preferred.width) : configured;
	}

	// The box holds the row, the bar and the room the bar's ends reach into, and those ends grow with the bar, so
	// the thickness is the largest one whose box still fits in the dragged height.
	static int barThickness(Dimension preferred, int rowHeight, boolean flat, BarEnds ends, int configured)
	{
		if (preferred == null)
		{
			return configured;
		}
		int smallest = PartnerBar.MIN_HEIGHT;
		int largest = Math.max(smallest, preferred.height - rowHeight);
		while (smallest < largest)
		{
			final int candidate = (smallest + largest + 1) / 2;
			if (PartnerBar.height(rowHeight, candidate, flat, ends) <= preferred.height)
			{
				smallest = candidate;
			}
			else
			{
				largest = candidate - 1;
			}
		}
		return smallest;
	}
}
