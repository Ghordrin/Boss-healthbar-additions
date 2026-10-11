package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.ColorUtil.brighten;
import static com.ghordrin.bosshealthbar.ColorUtil.darken;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

class CrestRenderer
{
	static final class Piece
	{
		final BufferedImage image;
		final int anchorX;
		final int anchorY;

		private Piece(BufferedImage image, int anchorX, int anchorY)
		{
			this.image = image;
			this.anchorX = anchorX;
			this.anchorY = anchorY;
		}
	}

	static final class Crest
	{
		final Piece left;
		final Piece right;
		final int iconSize;

		private Crest(Piece left, Piece right, int iconSize)
		{
			this.left = left;
			this.right = right;
			this.iconSize = iconSize;
		}
	}

	private static final Color OUTLINE = new Color(8, 6, 6, 215);
	private static final Color SHADOW = new Color(0, 0, 0, 110);
	private static final Color GLINT = new Color(255, 255, 255, 150);
	private static final Color GEM_GLINT = new Color(255, 255, 255, 70);

	private static final float LONG_POINT = 1.38f;
	private static final float SHORT_POINT = 1.14f;
	private static final float POINT_VALLEY = 0.84f;
	private static final float BEVEL = 0.8f;
	private static final float WELL = 0.68f;
	private static final float ICON_OVER_WELL = 1.15f;

	private Color cachedColor;
	private Color cachedGlowColor;
	private float cachedBarHalfExtent;
	private Crest cached;

	Crest getCrest(Color plateColor, Color glowColor, float barHalfExtent)
	{
		if (cached == null || !plateColor.equals(cachedColor) || !glowColor.equals(cachedGlowColor)
			|| barHalfExtent != cachedBarHalfExtent)
		{
			cached = build(plateColor, glowColor, barHalfExtent);
			cachedColor = plateColor;
			cachedGlowColor = glowColor;
			cachedBarHalfExtent = barHalfExtent;
		}
		return cached;
	}

	private static Crest build(Color plateColor, Color glowColor, float half)
	{
		final float reach = half + 3f;
		final float bound = reach * LONG_POINT + 1.5f;
		final Piece right = rasterise(-bound, -bound, bound, bound,
			g -> drawPlate(g, plateColor, glowColor, reach));
		final int iconSize = Math.round(reach * WELL * 2 * ICON_OVER_WELL);
		return new Crest(mirror(right), right, iconSize);
	}

	private static void drawPlate(Graphics2D g, Color plateColor, Color glowColor, float reach)
	{
		final Shape star = star(reach);
		shadow(g, star);
		metal(g, star, darken(plateColor, 0.15f), -reach * LONG_POINT, reach * LONG_POINT);

		final Shape rim = circle(reach);
		shadow(g, rim);
		metal(g, rim, plateColor, -reach, reach);

		final float bevelRadius = reach * BEVEL;
		g.setPaint(new GradientPaint(
			0, -bevelRadius, darken(plateColor, 0.6f),
			0, bevelRadius, brighten(plateColor, 0.35f)));
		g.fill(circle(bevelRadius));

		final float wellRadius = reach * WELL;
		final Shape well = circle(wellRadius);
		g.setPaint(new RadialGradientPaint(
			new Point2D.Float(0, -wellRadius * 0.2f), wellRadius * 1.1f,
			new float[]{0f, 0.55f, 1f},
			new Color[]{brighten(glowColor, 0.25f), darken(glowColor, 0.45f), darken(glowColor, 0.85f)}));
		g.fill(well);
		g.setStroke(new BasicStroke(0.6f));
		g.setColor(OUTLINE);
		g.draw(well);

		g.setColor(GEM_GLINT);
		g.fill(new Ellipse2D.Float(-wellRadius * 0.7f, -wellRadius * 0.85f, wellRadius * 0.9f, wellRadius * 0.55f));
		g.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(GLINT);
		final float glintRadius = reach * 0.9f;
		g.draw(new Arc2D.Float(-glintRadius, -glintRadius, glintRadius * 2, glintRadius * 2, 105, 60, Arc2D.OPEN));
	}

	private static Shape star(float reach)
	{
		final Path2D.Float path = new Path2D.Float();
		for (int i = 0; i < 16; i++)
		{
			final double angle = Math.PI / 8 * i - Math.PI / 2;
			final float radius = i % 2 == 1 ? reach * POINT_VALLEY
				: i % 4 == 0 ? reach * LONG_POINT : reach * SHORT_POINT;
			final float x = (float) (Math.cos(angle) * radius);
			final float y = (float) (Math.sin(angle) * radius);
			if (i == 0)
			{
				path.moveTo(x, y);
			}
			else
			{
				path.lineTo(x, y);
			}
		}
		path.closePath();
		return path;
	}

	private static Shape circle(float radius)
	{
		return new Ellipse2D.Float(-radius, -radius, radius * 2, radius * 2);
	}

	private static Piece mirror(Piece piece)
	{
		final BufferedImage source = piece.image;
		final int width = source.getWidth();
		final int height = source.getHeight();
		final BufferedImage flipped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = flipped.createGraphics();
		g.drawImage(source, width, 0, 0, height, 0, 0, width, height, null);
		g.dispose();
		final int logicalWidth = Math.round(width / BarPainter.RASTER_SCALE);
		return new Piece(flipped, logicalWidth - piece.anchorX, piece.anchorY);
	}

	private static Piece rasterise(float minX, float minY, float maxX, float maxY, Consumer<Graphics2D> painter)
	{
		final int pad = 2;
		final int anchorX = pad + (int) Math.ceil(-minX);
		final int anchorY = pad + (int) Math.ceil(-minY);
		final int width = anchorX + (int) Math.ceil(maxX) + pad;
		final int height = anchorY + (int) Math.ceil(maxY) + pad;

		final float rasterScale = BarPainter.RASTER_SCALE;
		final BufferedImage image = new BufferedImage(Math.round(width * rasterScale), Math.round(height * rasterScale),
			BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.scale(rasterScale, rasterScale);
		g.translate(anchorX, anchorY);
		painter.accept(g);
		g.dispose();
		return new Piece(image, anchorX, anchorY);
	}

	private static void shadow(Graphics2D g, Shape shape)
	{
		final AffineTransform base = g.getTransform();
		g.translate(0.8, 1.2);
		g.setColor(SHADOW);
		g.fill(shape);
		g.setTransform(base);
	}

	private static void metal(Graphics2D g, Shape shape, Color color, float top, float bottom)
	{
		g.setPaint(new GradientPaint(
			0, top, brighten(color, 0.5f),
			0, bottom, darken(color, 0.5f)));
		g.fill(shape);
		g.setStroke(new BasicStroke(1f));
		g.setColor(OUTLINE);
		g.draw(shape);
	}
}
