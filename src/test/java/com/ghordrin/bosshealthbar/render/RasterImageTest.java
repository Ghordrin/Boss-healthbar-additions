package com.ghordrin.bosshealthbar.render;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class RasterImageTest
{
	// The copy shrunk once has to look like the large picture shrunk on the screen every frame, faded or not.
	@Test
	public void theShrunkCopyMatchesShrinkingEveryFrame()
	{
		final BufferedImage large = new BufferedImage(80, 40, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D painter = large.createGraphics();
		painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		painter.setColor(new Color(201, 165, 74, 180));
		painter.fill(new Ellipse2D.Float(3.3f, 2.7f, 70f, 33f));
		painter.dispose();
		final RasterImage image = new RasterImage(large);

		for (float opacity : new float[]{1f, 0.4f})
		{
			final BufferedImage before = screen();
			final Graphics2D old = before.createGraphics();
			old.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
			old.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			old.drawImage(large, 5, 5, 5 + image.width, 5 + image.height, 0, 0, large.getWidth(), large.getHeight(), null);
			old.dispose();

			final BufferedImage after = screen();
			final Graphics2D now = after.createGraphics();
			now.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
			image.draw(now, 5, 5);
			now.dispose();

			for (int y = 0; y < before.getHeight(); y++)
			{
				for (int x = 0; x < before.getWidth(); x++)
				{
					assertTrue(x + "," + y + " at " + opacity, close(before.getRGB(x, y), after.getRGB(x, y)));
				}
			}
		}
	}

	@Test
	public void onlyWholePixelMovesUseTheShrunkCopy()
	{
		assertTrue(RasterImage.onWholePixels(new AffineTransform()));
		assertTrue(RasterImage.onWholePixels(AffineTransform.getTranslateInstance(12, -3)));
		assertFalse(RasterImage.onWholePixels(AffineTransform.getTranslateInstance(12.5, 0)));
		assertFalse(RasterImage.onWholePixels(AffineTransform.getScaleInstance(2, 2)));
	}

	private static BufferedImage screen()
	{
		final BufferedImage screen = new BufferedImage(50, 30, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = screen.createGraphics();
		g.setColor(new Color(40, 60, 30));
		g.fillRect(0, 0, 50, 30);
		g.dispose();
		return screen;
	}

	private static boolean close(int a, int b)
	{
		for (int shift = 0; shift <= 16; shift += 8)
		{
			if (Math.abs((a >> shift & 0xFF) - (b >> shift & 0xFF)) > 2)
			{
				return false;
			}
		}
		return true;
	}
}
