package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.ColorUtil.brighten;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import javax.inject.Inject;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

// Draws the game's pillar charge bars in the bar's style while this plugin hides the game's.
class PillarBarsOverlay extends Overlay
{
	private static final String[] LABELS = {"North West", "North East", "South West", "South East"};
	private static final float[] PREVIEW_FRACTIONS = {1f, 0.6f, 0.25f, 0f};
	private static final boolean[] PREVIEW_FULL = {true, false, false, false};

	static final int MIN_BAR_WIDTH = 64;
	static final int LABEL_PADDING = 4;
	static final int MIN_BAR_HEIGHT = 5;
	static final int MAX_BAR_HEIGHT = 9;
	static final int COLUMN_GAP = 6;
	static final int ROW_GAP = 3;
	static final int LABEL_GAP = 1;
	private static final float FULL_BRIGHTEN = 0.75f;

	// The game's own colors, used with the Oldschool theme.
	private static final Color FLAT_TRACK = new Color(0x2D2B00);
	private static final Color FLAT_FILL = new Color(0xDBD300);
	private static final Color FLAT_FULL = Color.WHITE;
	private static final Color FLAT_OUTLINE = new Color(0x191511);

	private final BossHealthBarConfig config;
	private final PillarBars pillarBars;
	private final BossHealthBarOverlay barOverlay;
	private final BarTextPainter textPainter;

	private final int[] labelWidths = new int[PillarBars.CORNERS];
	private FontMetrics labelMetrics;
	private int barWidth;
	private int labelHeight;

	private int trackPaintHeight = -1;
	private final Paint[] trackPaints = new Paint[2];

	private Color fullSource;
	private Color fullColor;

	@Inject
	private PillarBarsOverlay(BossHealthBarPlugin plugin, BossHealthBarConfig config, PillarBars pillarBars,
		BossHealthBarOverlay barOverlay, BarTextPainter textPainter)
	{
		super(plugin);
		this.config = config;
		this.pillarBars = pillarBars;
		this.barOverlay = barOverlay;
		this.textPainter = textPainter;

		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, BossHealthBarPlugin.NAME);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final boolean live = pillarBars.isVisible();
		if (!live && !barOverlay.isShowingPreview())
		{
			return null;
		}

		textPainter.updateFonts();
		final FontMetrics metrics = textPainter.hitpointsMetrics();
		if (metrics != labelMetrics)
		{
			measureLabels(metrics);
		}

		final int barHeight = barHeight(config.barHeight());
		final int cellHeight = cellHeight(labelHeight, barHeight);

		final ThemeColors colors = barOverlay.baseColors();
		final boolean flat = BarTheme.of(config).isFlat();
		final Color fill = flat ? FLAT_FILL : colors.getFillHigh();
		final Color full = flat ? FLAT_FULL : fullColor(fill);
		final Color outline = flat ? FLAT_OUTLINE : colors.getFrame();
		final Color labelColor = colors.getHitpointsText();
		updateTrackPaints(barHeight, cellHeight);

		textPainter.applyTextHints(graphics);
		for (int corner = 0; corner < PillarBars.CORNERS; corner++)
		{
			final int column = corner % 2;
			final int row = corner / 2;
			final int x = column * (barWidth + COLUMN_GAP);
			final int y = row * (cellHeight + ROW_GAP);
			textPainter.drawHitpointsStyleText(graphics, LABELS[corner], x + (barWidth - labelWidths[corner]) / 2,
				y + metrics.getAscent(), labelColor);

			final Paint track = flat ? FLAT_TRACK : colors.getTrack() != null ? colors.getTrack() : trackPaints[row];
			final float fraction = live ? pillarBars.fraction(corner) : PREVIEW_FRACTIONS[corner];
			final boolean isFull = live ? pillarBars.isFull(corner) : PREVIEW_FULL[corner];
			drawBar(graphics, x, y + labelHeight + LABEL_GAP, barHeight, fraction, isFull, track,
				isFull ? full : fill, outline);
		}

		return gridSize(barWidth, cellHeight);
	}

	private void drawBar(Graphics2D graphics, int x, int y, int height, float fraction, boolean isFull, Paint track,
		Color fill, Color outline)
	{
		final int innerWidth = barWidth - 2;
		graphics.setColor(outline);
		graphics.drawRect(x, y, barWidth - 1, height - 1);
		graphics.setPaint(track);
		graphics.fillRect(x + 1, y + 1, innerWidth, height - 2);
		final int filled = filledWidth(innerWidth, fraction, isFull);
		if (filled > 0)
		{
			graphics.setColor(fill);
			graphics.fillRect(x + 1, y + 1, filled, height - 2);
		}
	}

	private void measureLabels(FontMetrics metrics)
	{
		int widest = 0;
		for (int corner = 0; corner < PillarBars.CORNERS; corner++)
		{
			labelWidths[corner] = metrics.stringWidth(LABELS[corner]);
			widest = Math.max(widest, labelWidths[corner]);
		}
		barWidth = barWidth(widest);
		// One more for the text shadow.
		labelHeight = metrics.getAscent() + 1;
		labelMetrics = metrics;
		trackPaintHeight = -1;
	}

	private void updateTrackPaints(int barHeight, int cellHeight)
	{
		if (barHeight == trackPaintHeight)
		{
			return;
		}
		for (int row = 0; row < trackPaints.length; row++)
		{
			final int top = row * (cellHeight + ROW_GAP) + labelHeight + LABEL_GAP + 1;
			trackPaints[row] = new GradientPaint(0, top, BarPainter.TRACK_TOP, 0, top + barHeight - 2, BarPainter.TRACK_BOTTOM);
		}
		trackPaintHeight = barHeight;
	}

	private Color fullColor(Color fill)
	{
		if (fill != fullSource)
		{
			fullColor = brighten(fill, FULL_BRIGHTEN);
			fullSource = fill;
		}
		return fullColor;
	}

	static int barWidth(int widestLabel)
	{
		return Math.max(MIN_BAR_WIDTH, widestLabel + LABEL_PADDING);
	}

	static int barHeight(int configured)
	{
		return Math.max(MIN_BAR_HEIGHT, Math.min(MAX_BAR_HEIGHT, configured));
	}

	static int cellHeight(int labelHeight, int barHeight)
	{
		return labelHeight + LABEL_GAP + barHeight;
	}

	static Dimension gridSize(int barWidth, int cellHeight)
	{
		return new Dimension(barWidth * 2 + COLUMN_GAP, cellHeight * 2 + ROW_GAP);
	}

	static int filledWidth(int innerWidth, float fraction, boolean full)
	{
		return full ? innerWidth : Math.max(0, Math.min(innerWidth, Math.round(innerWidth * fraction)));
	}
}
