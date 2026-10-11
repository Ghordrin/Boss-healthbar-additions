package com.ghordrin.bosshealthbar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TextOutlineTest
{
	private static final Color FILL = new Color(0x40C0FF);
	private static final String TEXT = "1234";

	private static class OutlineConfig implements BossHealthBarConfig
	{
		TextOutline outline = TextOutline.SHADOW;

		@Override
		public TextOutline textOutline()
		{
			return outline;
		}

		@Override
		public boolean smoothText()
		{
			return false;
		}
	}

	@Test
	public void theDefaultIsAShadow()
	{
		assertEquals(TextOutline.SHADOW, new BossHealthBarConfig()
		{
		}.textOutline());
	}

	@Test
	public void everyEdgeDrawsTheTextItselfInTheSamePlace()
	{
		List<Point> expected = null;
		for (TextOutline outline : TextOutline.values())
		{
			final OutlineConfig config = new OutlineConfig();
			config.outline = outline;
			final BarTextPainter painter = new BarTextPainter(new DamageTracker(), config);
			painter.updateFonts();

			final BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D graphics = image.createGraphics();
			painter.applyTextHints(graphics);
			painter.drawHitpointsStyleText(graphics, TEXT, 20, 25, FILL);
			graphics.dispose();

			final List<Point> pixels = filledPixels(image);
			assertTrue(outline.name() + " drew nothing", !pixels.isEmpty());
			if (expected == null)
			{
				expected = pixels;
			}
			assertEquals(outline.name(), expected, pixels);
		}
	}

	@Test
	public void theTextMetricsDoNotDependOnTheEdge()
	{
		Integer width = null;
		for (TextOutline outline : TextOutline.values())
		{
			final OutlineConfig config = new OutlineConfig();
			config.outline = outline;
			final BarTextPainter painter = new BarTextPainter(new DamageTracker(), config);
			painter.updateFonts();
			final int stringWidth = painter.hitpointsMetrics().stringWidth(TEXT);
			if (width == null)
			{
				width = stringWidth;
			}
			assertEquals(outline.name(), width.intValue(), stringWidth);
		}
	}

	// Where the fill color itself landed. The edge is drawn under it, so these pixels are the text.
	private static List<Point> filledPixels(BufferedImage image)
	{
		final List<Point> pixels = new ArrayList<>();
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if (image.getRGB(x, y) == (FILL.getRGB() | 0xFF000000))
				{
					pixels.add(new Point(x, y));
				}
			}
		}
		return pixels;
	}
}
