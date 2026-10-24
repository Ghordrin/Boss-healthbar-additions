package com.ghordrin.bosshealthbar.gamebars;

import static com.ghordrin.bosshealthbar.render.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.theme.ColorUtil.brighten;
import static com.ghordrin.bosshealthbar.theme.ColorUtil.lerp;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import com.ghordrin.bosshealthbar.BossHealthBarOverlay;
import com.ghordrin.bosshealthbar.BossHealthBarPlugin;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.render.BarPainter;
import com.ghordrin.bosshealthbar.render.BarTextPainter;
import com.ghordrin.bosshealthbar.theme.BarTheme;
import com.ghordrin.bosshealthbar.theme.ThemeColors;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

// Draws the game's pillar charge bars in the bar's style while this plugin hides the game's.
public class PillarBarsOverlay extends Overlay
{
	private static final String[] LABELS = {"North West", "North East", "South West", "South East"};
	private static final float[] PREVIEW_FRACTIONS = {1f, 0.6f, 0.25f, 0f};
	private static final boolean[] PREVIEW_FULL = {true, false, false, false};

	private static final int MIN_BAR_WIDTH = 64;
	// Small enough that dragging an edge only ever runs into the width this class clamps to itself.
	private static final int MIN_GRID_SIZE = 48;
	private static final int LABEL_PADDING = 4;
	private static final int MIN_BAR_HEIGHT = 5;
	private static final int MAX_BAR_HEIGHT = 9;
	private static final int COLUMN_GAP = 6;
	private static final int ROW_GAP = 3;
	private static final int LABEL_GAP = 1;
	private static final float FULL_BRIGHTEN = 0.75f;
	// The lift and shade the main bar gives its frame and its fill.
	private static final float FRAME_LIFT = 0.45f;
	private static final float FRAME_SHADE = 0.25f;
	private static final float FILL_LIFT = 0.3f;
	private static final float FILL_SHADE = 0.45f;

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
	// The width the labels ask for, which a dragged width takes over from.
	private int naturalBarWidth;
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
		setResizable(true);
		setMinimumSize(MIN_GRID_SIZE);
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
		final FontMetrics metrics = textPainter.nameMetrics();
		if (metrics != labelMetrics)
		{
			measureLabels(metrics);
		}

		barWidth = barWidth(naturalBarWidth, getPreferredSize());
		final int barHeight = barHeight(config.barHeight());
		final int cellHeight = cellHeight(labelHeight, barHeight);

		final ThemeColors colors = barOverlay.baseColors();
		final boolean flat = BarTheme.of(config).isFlat();
		final Color fillHigh = flat ? FLAT_FILL : colors.getFillHigh();
		final Color fillLow = flat ? FLAT_FILL : colors.getFillLow();
		final Color full = flat ? FLAT_FULL : fullColor(fillHigh);
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
			textPainter.drawNameStyleText(graphics, LABELS[corner], x + (barWidth - labelWidths[corner]) / 2,
				y + metrics.getAscent(), labelColor);

			final Paint track = flat ? FLAT_TRACK : colors.getTrack() != null ? colors.getTrack() : trackPaints[row];
			final float fraction = live ? pillarBars.fraction(corner) : PREVIEW_FRACTIONS[corner];
			final boolean isFull = live ? pillarBars.isFull(corner) : PREVIEW_FULL[corner];
			// The same blend the main bar uses, so these follow it from full health down to the low color.
			final Color fill = isFull ? full : lerp(fillLow, fillHigh, clamp01(fraction));
			drawBar(graphics, x, y + labelHeight + LABEL_GAP, barHeight, fraction, isFull, track, fill, outline, flat);
		}

		return gridSize(barWidth, cellHeight);
	}

	// The main bar's look at a tenth of its height: the frame is a single line of its gradient rather than a
	// moulding, and there are no end pieces, which at this size would leave no room for the bar itself.
	private void drawBar(Graphics2D graphics, int x, int y, int height, float fraction, boolean isFull, Paint track,
		Color fill, Color outline, boolean flat)
	{
		final int innerWidth = barWidth - 2;
		graphics.setPaint(flat ? outline : BarPainter.verticalSheen(y, height, outline, FRAME_LIFT, FRAME_SHADE));
		graphics.drawRect(x, y, barWidth - 1, height - 1);
		graphics.setPaint(track);
		graphics.fillRect(x + 1, y + 1, innerWidth, height - 2);
		final int filled = filledWidth(innerWidth, fraction, isFull);
		if (filled > 0)
		{
			graphics.setPaint(flat ? fill : BarPainter.verticalSheen(y + 1, height - 2, fill, FILL_LIFT, FILL_SHADE));
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
		naturalBarWidth = barWidth(widest);
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

	// Alt-dragging the overlay's edge sets a preferred size, which takes over from the width the labels ask
	// for until the overlay is reset. Only its width is used; the bars' height follows "Bar height".
	static int barWidth(int naturalWidth, Dimension preferred)
	{
		return preferred != null
			? Math.max(MIN_BAR_WIDTH, (preferred.width - COLUMN_GAP) / 2)
			: naturalWidth;
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
