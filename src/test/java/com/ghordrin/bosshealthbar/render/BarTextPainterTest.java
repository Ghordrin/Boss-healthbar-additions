package com.ghordrin.bosshealthbar.render;

import static org.junit.Assert.assertEquals;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class BarTextPainterTest
{
	private static final FontMetrics METRICS = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
		.createGraphics().getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

	@Test
	public void theFullNameIsKeptWhileItFits()
	{
		final int room = METRICS.stringWidth("Doom of Mokhaiotl");
		assertEquals("Doom of Mokhaiotl",
			BarTextPainter.fittedName("Doom of Mokhaiotl", "Doom", METRICS, room));
		assertEquals("Doom of Mokhaiotl",
			BarTextPainter.fittedName("Doom of Mokhaiotl", "Doom", METRICS, room + 50));
	}

	@Test
	public void theShortNameTakesOverOnceTheFullOneDoesNotFit()
	{
		final int room = METRICS.stringWidth("Doom of Mokhaiotl") - 1;
		assertEquals("Doom", BarTextPainter.fittedName("Doom of Mokhaiotl", "Doom", METRICS, room));
		assertEquals("Doom (Burrowed)",
			BarTextPainter.fittedName("Doom of Mokhaiotl (Burrowed)", "Doom (Burrowed)", METRICS, room));
	}

	@Test
	public void aBossWithoutAShortNameKeepsItsFullNameToBeEllipsized()
	{
		assertEquals("General Graardor", BarTextPainter.fittedName("General Graardor", null, METRICS, 0));
	}


	@Test
	public void thePartnerRowFitsTheHitpointsFontWhileItHasText()
	{
		assertEquals(14, BarTextPainter.partnerRowHeight(true, false, 10, 4));
		assertEquals(14, BarTextPainter.partnerRowHeight(false, true, 10, 4));
		assertEquals(0, BarTextPainter.partnerRowHeight(false, false, 10, 4));
	}

	@Test
	public void theSlotSizingSwapsEveryDigit()
	{
		assertEquals("0:00", BarTextPainter.withDigits("1:23", '0'));
		assertEquals("4:44:44", BarTextPainter.withDigits("1:05:09", '4'));
	}
}
