package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BarAnimation.clamp01;
import static com.ghordrin.bosshealthbar.ColorUtil.brighten;
import static com.ghordrin.bosshealthbar.ColorUtil.darken;
import static com.ghordrin.bosshealthbar.ColorUtil.lerp;
import static com.ghordrin.bosshealthbar.ColorUtil.withAlpha;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Texture;
import net.runelite.api.TextureProvider;

class BarPainter
{
	private static final Duration FLASH_DURATION = Duration.ofMillis(350);
	private static final float BIG_HIT_FRACTION = 0.08f;
	private static final int CAP_PLATE_WIDTH = 2;
	private static final int DIAMOND_RADIUS = 3;
	private static final int CAP_RISE = 3;
	private static final int CAP_REFERENCE_HEIGHT = 8;
	private static final float CAP_SCALE_PER_PIXEL = 0.09f;
	// Cached images are drawn at twice their size and scaled down, so they stay sharp in stretched mode.
	static final float RASTER_SCALE = 2f;
	private static final int BAR_IMAGE_PAD = 8;

	private static final Color TRACK_TOP = new Color(6, 5, 5, 225);
	private static final Color TRACK_BOTTOM = new Color(26, 22, 22, 225);
	private static final Color HEAL_TINT = new Color(150, 235, 160);
	private static final Color BACKDROP = new Color(0, 0, 0, 34);
	private static final Color FRAME_OUTLINE = new Color(6, 5, 5);
	private static final Color DIAMOND_OUTLINE = new Color(6, 5, 5, 170);
	private static final Color TRACK_EDGE_SHADOW = new Color(0, 0, 0, 120);
	private static final Color MARKER_SHADOW = new Color(0, 0, 0, 170);
	private static final Color FLASH_COLOR = new Color(1f, 0.95f, 0.85f);
	private static final BasicStroke THIN_STROKE = new BasicStroke(1f);

	private final Client client;
	private final DamageTracker damageTracker;
	private final BossHealthBarConfig config;

	private ThemeColors derivedColorsSource;
	private Color healColor;
	private Color frameHighlightColor;
	private Color markerColor;

	private BufferedImage backdropImage;
	private BufferedImage endsImage;
	private int barImageWidth;
	private int barImageHeight;
	private Color barImageFrameColor;

	private final Map<Integer, BufferedImage> fillTextureCache = new HashMap<>();

	private BasicStroke frameStroke;
	private int frameStrokeHeight;

	@Inject
	BarPainter(Client client, DamageTracker damageTracker, BossHealthBarConfig config)
	{
		this.client = client;
		this.damageTracker = damageTracker;
		this.config = config;
	}

	void clearTextureCache()
	{
		fillTextureCache.clear();
	}

	private BasicStroke frameStroke(int height)
	{
		if (frameStroke == null || frameStrokeHeight != height)
		{
			frameStroke = new BasicStroke(scaledFrameStroke(height));
			frameStrokeHeight = height;
		}
		return frameStroke;
	}

	void drawBar(Graphics2D graphics, ThemeColors colors, BarState state, BarAnimation animation, boolean defeated,
		int y, int width, int height, float fillProgress, boolean useImageCache)
	{
		updateDerivedColors(colors);
		final float displayedFraction = animation.getDisplayedFraction();
		final float[] phaseMarkers = defeated ? BarState.NO_PHASE_MARKERS : state.phaseMarkers;
		final float lowHealthPulse = animation.lowHealthPulse(defeated, config.lowHealthEffect(), config.lowHealthThreshold());
		final Color frameColor = colors.getFrame();
		final int barX = scaledCapWidth(height);
		final int barWidth = width - scaledCapWidth(height) * 2;

		final int innerX = barX + 1;
		final int innerY = y + 1;
		final int innerWidth = barWidth - 2;
		final int innerHeight = height - 2;

		final Color baseFill = lerp(colors.getFillLow(), colors.getFillHigh(), clamp01(displayedFraction));
		final Color fill = lowHealthPulse > 0f ? brighten(baseFill, 0.55f * lowHealthPulse) : baseFill;
		final int fillWidth = Math.round(innerWidth * clamp01(displayedFraction) * fillProgress);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (useImageCache)
		{
			updateBarImages(width, height, frameColor);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			drawRasterImage(graphics, backdropImage, -BAR_IMAGE_PAD, y - BAR_IMAGE_PAD, width + BAR_IMAGE_PAD * 2, height + BAR_IMAGE_PAD * 2);
		}
		else
		{
			drawBackdrop(graphics, barX, y, barWidth, height);
		}

		if (lowHealthPulse > 0f && fillWidth > 0)
		{
			final Color glow = brighten(baseFill, 0.2f);
			for (int i = 4; i >= 1; i--)
			{
				final int spread = i * 3;
				final int arc = height + spread * 2;
				graphics.setColor(withAlpha(glow, Math.round(lowHealthPulse * 130 / i)));
				graphics.fillRoundRect(innerX - spread, y - spread, fillWidth + spread * 2, height + spread * 2, arc, arc);
			}
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		graphics.setPaint(new GradientPaint(0, y, TRACK_TOP, 0, y + height, TRACK_BOTTOM));
		graphics.fillRect(barX, y, barWidth, height);

		final float trailFraction = animation.getTrailFraction();
		if (config.showDamageTrail() && trailFraction > displayedFraction)
		{
			Color trail = colors.getTrail();
			int trailWidth = Math.round(innerWidth * clamp01(trailFraction) * fillProgress);
			graphics.setPaint(verticalSheen(innerY, innerHeight, trail, 0.2f, 0.35f));
			graphics.fillRect(innerX, innerY, trailWidth, innerHeight);
		}

		final int healWidth = Math.round(innerWidth * clamp01(animation.getActualFraction()) * fillProgress);
		if (healWidth > fillWidth)
		{
			graphics.setPaint(verticalSheen(innerY, innerHeight, healColor, 0.35f, 0.3f));
			graphics.fillRect(innerX, innerY, healWidth, innerHeight);
		}

		if (fillWidth > 0)
		{
			graphics.setPaint(verticalSheen(innerY, innerHeight, fill, 0.3f, 0.45f));
			graphics.fillRect(innerX, innerY, fillWidth, innerHeight);

			final BufferedImage fillTexture = resolveFillTexture();
			if (fillTexture != null)
			{
				final Composite textureComposite = graphics.getComposite();
				graphics.setPaint(new TexturePaint(fillTexture, new Rectangle(innerX, innerY, fillTexture.getWidth(), fillTexture.getHeight())));
				graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
				graphics.fillRect(innerX, innerY, fillWidth, innerHeight);
				graphics.setComposite(textureComposite);
			}

			graphics.setColor(withAlpha(brighten(fill, 0.6f), 90));
			graphics.drawLine(innerX, innerY, innerX + fillWidth - 1, innerY);
		}

		final int filledWidth = Math.max(fillWidth, healWidth);
		if (filledWidth < innerWidth)
		{
			graphics.setColor(TRACK_EDGE_SHADOW);
			graphics.drawLine(innerX + filledWidth, innerY, innerX + innerWidth - 1, innerY);
		}

		graphics.setStroke(frameStroke(height));
		if (lowHealthPulse > 0f)
		{
			final Color frame = lerp(frameColor, brighten(baseFill, 0.3f), 0.85f * lowHealthPulse);
			drawFrame(graphics, barX, y, barWidth, height, frame, withAlpha(brighten(frame, 0.35f), 120));
		}
		else
		{
			drawFrame(graphics, barX, y, barWidth, height, frameColor, frameHighlightColor);
		}

		graphics.setStroke(THIN_STROKE);
		for (float marker : phaseMarkers)
		{
			final int markerX = innerX + Math.round((innerWidth - 1) * marker);
			graphics.setColor(MARKER_SHADOW);
			graphics.drawLine(markerX + 1, innerY, markerX + 1, innerY + innerHeight - 1);
			graphics.setColor(markerColor);
			graphics.drawLine(markerX, y - 2, markerX, y + height + 1);
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (useImageCache)
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			drawRasterImage(graphics, endsImage, -BAR_IMAGE_PAD, y - BAR_IMAGE_PAD, width + BAR_IMAGE_PAD * 2, height + BAR_IMAGE_PAD * 2);
		}
		else
		{
			drawEnds(graphics, barX, y, barWidth, height, frameColor);
		}

		if (config.flashOnBigHits() && state.maxHealth != null)
		{
			final long lastHit = damageTracker.getLastHitMillis();
			if (lastHit != 0 && damageTracker.getLastHitAmount() >= state.maxHealth * BIG_HIT_FRACTION)
			{
				final long since = System.currentTimeMillis() - lastHit;
				if (since < FLASH_DURATION.toMillis())
				{
					float alpha = 1f - (since / (float) FLASH_DURATION.toMillis());
					graphics.setColor(withAlpha(FLASH_COLOR, Math.round(clamp01(alpha) * 0.8f * 255)));
					graphics.drawRect(barX, y, barWidth - 1, height - 1);
				}
			}
		}
	}

	private void updateDerivedColors(ThemeColors colors)
	{
		if (colors == derivedColorsSource)
		{
			return;
		}

		healColor = lerp(colors.getFillHigh(), HEAL_TINT, 0.45f);
		frameHighlightColor = withAlpha(brighten(colors.getFrame(), 0.35f), 120);
		markerColor = withAlpha(brighten(colors.getFrame(), 0.55f), 235);
		derivedColorsSource = colors;
	}

	private BufferedImage resolveFillTexture()
	{
		final int textureId = config.fillTextureId();
		if (textureId < 0)
		{
			return null;
		}

		final BufferedImage cached = fillTextureCache.get(textureId);
		if (cached != null)
		{
			return cached;
		}

		final TextureProvider textureProvider = client.getTextureProvider();
		if (textureProvider == null)
		{
			return null;
		}
		final Texture[] textures = textureProvider.getTextures();
		if (textureId >= textures.length || textures[textureId] == null)
		{
			return null;
		}
		final int[] pixels = textureProvider.load(textureId);
		if (pixels == null || pixels.length == 0)
		{
			return null;
		}
		final int side = (int) Math.round(Math.sqrt(pixels.length));
		if (side * side != pixels.length)
		{
			return null;
		}

		final BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, side, side, pixels, 0, side);
		fillTextureCache.put(textureId, image);
		return image;
	}

	static float capScale(int barHeight)
	{
		return 1f + Math.max(0, barHeight - CAP_REFERENCE_HEIGHT) * CAP_SCALE_PER_PIXEL;
	}

	static int scaledCapPlateWidth(int barHeight)
	{
		return Math.round(CAP_PLATE_WIDTH * capScale(barHeight));
	}

	static int scaledDiamondRadius(int barHeight)
	{
		return Math.round(DIAMOND_RADIUS * capScale(barHeight));
	}

	static int scaledCapRise(int barHeight)
	{
		return Math.round(CAP_RISE * capScale(barHeight));
	}

	static int scaledCapWidth(int barHeight)
	{
		return scaledCapPlateWidth(barHeight) + scaledDiamondRadius(barHeight) * 2 + 2;
	}

	static float scaledFrameStroke(int barHeight)
	{
		return Math.max(1f, capScale(barHeight));
	}

	private static void drawFrame(Graphics2D graphics, int x, int y, int width, int height, Color frameColor,
		Color highlightColor)
	{
		graphics.setColor(FRAME_OUTLINE);
		graphics.drawRect(x - 1, y - 1, width + 1, height + 1);

		graphics.setColor(frameColor);
		graphics.drawRect(x, y, width - 1, height - 1);

		graphics.setColor(highlightColor);
		graphics.drawLine(x + 1, y, x + width - 2, y);
	}

	private void updateBarImages(int width, int height, Color frameColor)
	{
		if (backdropImage != null && width == barImageWidth && height == barImageHeight
			&& frameColor.equals(barImageFrameColor))
		{
			return;
		}

		final int barX = scaledCapWidth(height);
		final int barWidth = width - scaledCapWidth(height) * 2;
		backdropImage = barImage(width, height, g -> drawBackdrop(g, barX, BAR_IMAGE_PAD, barWidth, height));
		endsImage = barImage(width, height, g -> drawEnds(g, barX, BAR_IMAGE_PAD, barWidth, height, frameColor));
		barImageWidth = width;
		barImageHeight = height;
		barImageFrameColor = frameColor;
	}

	private static BufferedImage barImage(int width, int height, Consumer<Graphics2D> painter)
	{
		final int logicalWidth = width + BAR_IMAGE_PAD * 2;
		final int logicalHeight = height + BAR_IMAGE_PAD * 2;
		final BufferedImage image = new BufferedImage(Math.round(logicalWidth * RASTER_SCALE),
			Math.round(logicalHeight * RASTER_SCALE), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.scale(RASTER_SCALE, RASTER_SCALE);
		g.translate(BAR_IMAGE_PAD, 0);
		painter.accept(g);
		g.dispose();
		return image;
	}

	static void drawRasterImage(Graphics2D graphics, BufferedImage image, int x, int y, int logicalWidth, int logicalHeight)
	{
		graphics.drawImage(image, x, y, x + logicalWidth, y + logicalHeight, 0, 0, image.getWidth(), image.getHeight(), null);
	}

	private static void drawBackdrop(Graphics2D graphics, int barX, int y, int barWidth, int height)
	{
		graphics.setColor(BACKDROP);
		for (int i = 3; i >= 1; i--)
		{
			graphics.fillRoundRect(barX - i * 2, y - i - 1, barWidth + i * 4, height + i * 2 + 2, height + i * 2, height + i * 2);
		}
	}

	private static void drawEnds(Graphics2D graphics, int barX, int y, int barWidth, int height, Color frameColor)
	{
		drawFinials(graphics, barX, y, barWidth, height, frameColor);
		drawUnderline(graphics, barX, y + height + 1, barWidth, frameColor);
	}

	private static LinearGradientPaint verticalSheen(int y, int height, Color base, float lift, float shade)
	{
		return new LinearGradientPaint(
			0, y, 0, y + Math.max(1, height),
			new float[]{0f, 0.45f, 1f},
			new Color[]{brighten(base, lift), base, darken(base, shade)});
	}

	private static void drawFinials(Graphics2D graphics, int barX, int y, int barWidth, int height, Color frameColor)
	{
		final int plateWidth = scaledCapPlateWidth(height);
		final int diamond = scaledDiamondRadius(height);
		final int top = y - scaledCapRise(height);
		final int bottom = y + height + scaledCapRise(height);
		final int midY = y + height / 2;
		final int rightEdge = barX + barWidth;

		graphics.setPaint(new GradientPaint(0, top, brighten(frameColor, 0.35f), 0, bottom, darken(frameColor, 0.35f)));
		graphics.fillRect(barX - plateWidth, top, plateWidth, bottom - top);
		graphics.fillRect(rightEdge, top, plateWidth, bottom - top);

		final int leftCx = barX - plateWidth - diamond - 1;
		final int rightCx = rightEdge + plateWidth + diamond + 1;
		final int[] ys = {midY - diamond, midY, midY + diamond, midY};
		final int[] leftXs = {leftCx, leftCx + diamond, leftCx, leftCx - diamond};
		final int[] rightXs = {rightCx, rightCx + diamond, rightCx, rightCx - diamond};
		graphics.fillPolygon(leftXs, ys, 4);
		graphics.fillPolygon(rightXs, ys, 4);

		graphics.setStroke(new BasicStroke(scaledFrameStroke(height)));
		graphics.setColor(DIAMOND_OUTLINE);
		graphics.drawPolygon(leftXs, ys, 4);
		graphics.drawPolygon(rightXs, ys, 4);

		graphics.setColor(withAlpha(brighten(frameColor, 0.7f), 200));
		graphics.fillRect(leftCx, midY - diamond + 1, 1, 1);
		graphics.fillRect(rightCx, midY - diamond + 1, 1, 1);
	}

	private static void drawUnderline(Graphics2D graphics, int barX, int y, int barWidth, Color frameColor)
	{
		graphics.setPaint(new LinearGradientPaint(
			barX, 0, barX + barWidth, 0,
			new float[]{0f, 0.5f, 1f},
			new Color[]{withAlpha(frameColor, 0), withAlpha(brighten(frameColor, 0.2f), 80), withAlpha(frameColor, 0)}));
		graphics.fillRect(barX, y + 1, barWidth, 1);
	}
}
