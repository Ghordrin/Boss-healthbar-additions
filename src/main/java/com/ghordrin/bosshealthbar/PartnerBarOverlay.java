package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.render.BarPainter.capRise;
import static com.ghordrin.bosshealthbar.render.BarPainter.capWidth;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.render.BarEnds;
import com.ghordrin.bosshealthbar.render.BarTextPainter;
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

	private final BossHealthBarConfig config;
	private final BossHealthBarOverlay barOverlay;
	private final PartnerBar partnerBar;
	private final BarTextPainter textPainter;

	@Inject
	private PartnerBarOverlay(BossHealthBarPlugin plugin, BossHealthBarConfig config,
		BossHealthBarOverlay barOverlay, PartnerBar partnerBar, BarTextPainter textPainter)
	{
		super(plugin);
		this.config = config;
		this.barOverlay = barOverlay;
		this.partnerBar = partnerBar;
		this.textPainter = textPainter;

		setPosition(OverlayPosition.TOP_CENTER);
		// The main bar's layer, or anything the game opens over this one hides it.
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setResizable(true);
		setMinimumSize(MIN_BAR_WIDTH);
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

		// This overlay has its own text painter, so the rows have to be set up before the row height is read.
		textPainter.updateFonts();
		textPainter.updateRows(false, false, false, false, false);
		final int width = barWidth(getPreferredSize());
		final int barThickness = partnerBar.barThickness(config.barHeight());
		final boolean flat = BarTheme.of(config).isFlat();
		final BarEnds ends = config.barEnds();
		final int capRise = capRise(barThickness, flat, ends);
		final int rowHeight = textPainter.partnerRowHeight();

		textPainter.applyTextHints(graphics);
		final Composite originalComposite = graphics.getComposite();
		partnerBar.draw(graphics, 0, originalComposite, 1f, 1f, barOverlay.themeColors(), 0, rowHeight + capRise,
			width, width, 0, capWidth(barThickness, flat, ends), barThickness, 1f, flat, ends);
		graphics.setComposite(originalComposite);

		return new Dimension(width, rowHeight + capRise * 2 + barThickness);
	}

	// Alt-dragging the overlay's edge sets a preferred size, which takes over from "Bar width" until the overlay
	// is reset. Only its width is used; the bar's height follows the main bar's.
	static int barWidth(Dimension preferred, int configured)
	{
		return preferred != null ? Math.max(MIN_BAR_WIDTH, preferred.width) : configured;
	}

	private int barWidth(Dimension preferred)
	{
		return barWidth(preferred, config.barWidth());
	}
}
