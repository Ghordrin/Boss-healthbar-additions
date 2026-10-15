package com.ghordrin.bosshealthbar.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BarIconPlacementTest
{
	// The sides the bar keeps room for. Only the chosen end gives up width, so the other keeps its end ornament.
	@Test
	public void aModernThemeKeepsTheSidesThatWereChosen()
	{
		assertTrue(BarIconPlacement.CRESTS.onTheme(false).hasLeftCrest());
		assertTrue(BarIconPlacement.CRESTS.onTheme(false).hasRightCrest());

		assertTrue(BarIconPlacement.CREST_LEFT.onTheme(false).hasLeftCrest());
		assertFalse(BarIconPlacement.CREST_LEFT.onTheme(false).hasRightCrest());

		assertFalse(BarIconPlacement.CREST_RIGHT.onTheme(false).hasLeftCrest());
		assertTrue(BarIconPlacement.CREST_RIGHT.onTheme(false).hasRightCrest());
	}

	@Test
	public void aModernThemeLeavesTheOtherChoicesAlone()
	{
		assertEquals(BarIconPlacement.BEFORE_NAME, BarIconPlacement.BEFORE_NAME.onTheme(false));
		assertEquals(BarIconPlacement.NONE, BarIconPlacement.NONE.onTheme(false));
		assertFalse(BarIconPlacement.BEFORE_NAME.hasCrest());
		assertFalse(BarIconPlacement.NONE.hasCrest());
	}

	// Picking a crest on the Oldschool theme would otherwise leave the icon nowhere to go, since it draws none.
	@Test
	public void theOldschoolThemeMovesEveryCrestChoiceToTheName()
	{
		assertEquals(BarIconPlacement.BEFORE_NAME, BarIconPlacement.CRESTS.onTheme(true));
		assertEquals(BarIconPlacement.BEFORE_NAME, BarIconPlacement.CREST_LEFT.onTheme(true));
		assertEquals(BarIconPlacement.BEFORE_NAME, BarIconPlacement.CREST_RIGHT.onTheme(true));
		assertEquals(BarIconPlacement.BEFORE_NAME, BarIconPlacement.BEFORE_NAME.onTheme(true));
	}

	// No icon means no icon on either theme, so the Oldschool fallback must not reach it.
	@Test
	public void theOldschoolThemeStillShowsNothingForNoIcon()
	{
		assertEquals(BarIconPlacement.NONE, BarIconPlacement.NONE.onTheme(true));
	}

	@Test
	public void noCrestIsEverDrawnOnTheOldschoolTheme()
	{
		for (BarIconPlacement placement : BarIconPlacement.values())
		{
			assertFalse(placement.toString(), placement.onTheme(true).hasCrest());
		}
	}
}
