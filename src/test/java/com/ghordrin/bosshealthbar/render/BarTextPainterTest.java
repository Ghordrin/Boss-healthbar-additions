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


	// The name font is usually the taller of the two, so the row has to follow it and not the hitpoints font.
	@Test
	public void thePartnerRowFitsTheTallestFontThatHasText()
	{
		assertEquals(24, BarTextPainter.partnerRowHeight(true, true, 18, 6, 10, 4));
		assertEquals(24, BarTextPainter.partnerRowHeight(true, false, 18, 6, 10, 4));
		assertEquals(14, BarTextPainter.partnerRowHeight(false, true, 18, 6, 10, 4));
		assertEquals(0, BarTextPainter.partnerRowHeight(false, false, 18, 6, 10, 4));
	}

	// Each side reaches a different distance above its baseline, so one of them sets where the shared baseline sits.
	@Test
	public void thePartnerRowMixesTheTwoFontsOnOneBaseline()
	{
		assertEquals(26, BarTextPainter.partnerRowHeight(true, true, 18, 2, 10, 8));
		assertEquals(18, BarTextPainter.partnerBaseline(true, true, 18, 10));
		assertEquals(10, BarTextPainter.partnerBaseline(false, true, 18, 10));
	}

	@Test
	public void theSlotSizingSwapsEveryDigit()
	{
		assertEquals("0:00", BarTextPainter.withDigits("1:23", '0'));
		assertEquals("4:44:44", BarTextPainter.withDigits("1:05:09", '4'));
	}
}
