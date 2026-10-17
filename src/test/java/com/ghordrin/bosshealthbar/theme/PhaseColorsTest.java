package com.ghordrin.bosshealthbar.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import java.awt.Color;
import org.junit.Test;

public class PhaseColorsTest
{
	// Read off the game's own bar during a kill: green for the usual phases, purple for a shielded one.
	// Nothing else in the code holds these, and a wrong green would color every boss.
	private static final int GAME_FILL = 0x00C800;
	private static final int GAME_TRAIL = 0x009000;
	private static final int GAME_TRACK = 0xC80000;
	private static final int SHIELD_FILL = 0x7040C8;
	private static final int SHIELD_TRAIL = 0x382068;
	private static final int SHIELD_TRACK = 0x100820;

	private static final ThemeColors THEME = ThemeColors.builder()
		.fillHigh(new Color(1))
		.fillLow(new Color(2))
		.trail(new Color(3))
		.track(new Color(4))
		.frame(new Color(5))
		.text(new Color(6))
		.levelText(new Color(7))
		.hitpointsText(new Color(8))
		.defeatedText(new Color(9))
		.build();

	@Test
	public void theColorsEveryBossGetsAreNotAPhase()
	{
		assertNull(PhaseColors.of(GAME_FILL, GAME_TRAIL, GAME_TRACK));
	}

	// The bar reads as black before the game fills it in, which is not the game saying anything.
	@Test
	public void aBarWithNoColorYetIsNotAPhase()
	{
		assertNull(PhaseColors.of(0, 0, 0));
	}

	// Only what the game recolors is recolored, so the frame and the text stay the theme's.
	@Test
	public void thePhaseTakesTheFillTheTrailAndTheTrack()
	{
		final ThemeColors colors = PhaseColors.of(SHIELD_FILL, SHIELD_TRAIL, SHIELD_TRACK)
			.applyTo(THEME, BossHealthBarConfig.TRANSPARENT, false);

		assertEquals(new Color(SHIELD_FILL), colors.getFillHigh());
		assertEquals(new Color(SHIELD_FILL), colors.getFillLow());
		assertEquals(new Color(SHIELD_TRAIL), colors.getTrail());
		assertEquals(new Color(SHIELD_TRACK), colors.getTrack());
		assertEquals(THEME.getFrame(), colors.getFrame());
		assertEquals(THEME.getText(), colors.getText());
		assertEquals(THEME.getDefeatedText(), colors.getDefeatedText());
	}

	// A picked color is one color, so the fill is it and the rest comes off it.
	@Test
	public void anOverrideIsUsedInsteadOfTheGameSColor()
	{
		final Color override = new Color(0xB48200);
		final ThemeColors colors = PhaseColors.of(SHIELD_FILL, SHIELD_TRAIL, SHIELD_TRACK)
			.applyTo(THEME, override, false);

		assertEquals(override, colors.getFillHigh());
		assertEquals(override, colors.getFillLow());
		assertEquals(THEME.getFrame(), colors.getFrame());
	}

	// Fortify puts the phase on the frame too, lifted off the fill so it still reads as an outline.
	@Test
	public void fortifyGivesTheFrameThePhaseSColor()
	{
		final ThemeColors colors = PhaseColors.of(SHIELD_FILL, SHIELD_TRAIL, SHIELD_TRACK)
			.applyTo(THEME, BossHealthBarConfig.TRANSPARENT, true);

		assertNotEquals(THEME.getFrame(), colors.getFrame());
		assertNotEquals(colors.getFillHigh(), colors.getFrame());
		assertEquals(new Color(SHIELD_FILL), colors.getFillHigh());
	}

	// A phase the player picked a color for puts that color on the frame, not the game's.
	@Test
	public void fortifyFollowsTheOverrideOnTheFrame()
	{
		final Color override = new Color(0xB48200);
		final ThemeColors game = PhaseColors.of(SHIELD_FILL, SHIELD_TRAIL, SHIELD_TRACK)
			.applyTo(THEME, BossHealthBarConfig.TRANSPARENT, true);
		final ThemeColors picked = PhaseColors.of(SHIELD_FILL, SHIELD_TRAIL, SHIELD_TRACK)
			.applyTo(THEME, override, true);

		assertNotEquals(game.getFrame(), picked.getFrame());
	}

	// One phase to the next is a change, so the run plays again instead of the bar sitting still.
	@Test
	public void eachPhaseHasItsOwnKey()
	{
		assertNotEquals(PhaseColors.of(SHIELD_FILL, SHIELD_TRAIL, SHIELD_TRACK).key(),
			PhaseColors.of(0xC87040, SHIELD_TRAIL, SHIELD_TRACK).key());
	}

	// Zero is how the bar says there is no phase at all, so no phase color may land on it.
	@Test
	public void noPhaseSKeyIsNeverAPhaseSKey()
	{
		assertNotEquals(0, PhaseColors.of(SHIELD_FILL, SHIELD_TRAIL, SHIELD_TRACK).key());
	}
}
