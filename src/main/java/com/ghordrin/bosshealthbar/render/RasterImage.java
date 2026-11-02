package com.ghordrin.bosshealthbar.render;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

// A picture painted at RASTER_SCALE times its size for smoother edges, kept with a copy already shrunk to its
// size. Overlays draw without any scaling, so each frame only copies that one onto the screen instead of
// filtering the large one down again.
public final class RasterImage
{
	final BufferedImage large;
	private final BufferedImage small;
	public final int width;
	public final int height;

	public RasterImage(BufferedImage large)
	{
		this.large = large;
		width = Math.max(1, Math.round(large.getWidth() / BarPainter.RASTER_SCALE));
		height = Math.max(1, Math.round(large.getHeight() / BarPainter.RASTER_SCALE));
		small = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
		final Graphics2D g = small.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(large, 0, 0, width, height, 0, 0, large.getWidth(), large.getHeight(), null);
		g.dispose();
	}

	public void draw(Graphics2D graphics, int x, int y)
	{
		if (onWholePixels(graphics.getTransform()))
		{
			graphics.drawImage(small, x, y, null);
			return;
		}
		final Object interpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(large, x, y, x + width, y + height, 0, 0, large.getWidth(), large.getHeight(), null);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation != null
			? interpolation : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	}

	static boolean onWholePixels(AffineTransform transform)
	{
		final int type = transform.getType();
		return type == AffineTransform.TYPE_IDENTITY || type == AffineTransform.TYPE_TRANSLATION
			&& transform.getTranslateX() == Math.rint(transform.getTranslateX())
			&& transform.getTranslateY() == Math.rint(transform.getTranslateY());
	}
}
