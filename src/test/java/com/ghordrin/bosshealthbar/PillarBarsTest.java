package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.PillarBars.FADE_IN_SCRIPT;
import static com.ghordrin.bosshealthbar.PillarBars.FADE_OUT_SCRIPT;
import static com.ghordrin.bosshealthbar.PillarBars.UPDATE_SCRIPT;
import java.awt.Dimension;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.InterfaceID;
import org.junit.Test;

public class PillarBarsTest
{
	private static final int NW = InterfaceID.NightmareTotems.TOTEM_NW_BAR_BACK;
	private static final int NE = InterfaceID.NightmareTotems.TOTEM_NE_BAR_BACK;
	private static final int SW = InterfaceID.NightmareTotems.TOTEM_SW_BAR_BACK;
	private static final int SE = InterfaceID.NightmareTotems.TOTEM_SE_BAR_BACK;
	private static final float DELTA = 0.0001f;

	private final DebugLog debugLog = new DebugLog(null);
	private final PillarBars bars = new PillarBars(null, debugLog);

	private static Object[] update(int[]... groups)
	{
		final Object[] args = new Object[PillarBars.UPDATE_ARGS];
		args[0] = UPDATE_SCRIPT;
		for (int group = 0; group < groups.length; group++)
		{
			for (int i = 0; i < 4; i++)
			{
				args[1 + group * 4 + i] = groups[group][i];
			}
		}
		args[PillarBars.UPDATE_ARGS - 1] = 0;
		return args;
	}

	private static int[] group(int back, int max, int current)
	{
		return new int[]{back, back + 1, max, current};
	}

	@Test
	public void parsesByComponentInAnyOrder()
	{
		bars.onScript(UPDATE_SCRIPT, update(group(SE, 100, 10), group(NW, 100, 40), group(NE, 100, 70), group(SW, 50, 25)));

		assertEquals(0.4f, bars.fraction(0), DELTA);
		assertEquals(0.7f, bars.fraction(1), DELTA);
		assertEquals(0.5f, bars.fraction(2), DELTA);
		assertEquals(0.1f, bars.fraction(3), DELTA);
	}

	@Test
	public void unchangedValueKeepsTheBar()
	{
		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, 40), group(NE, 100, 40), group(SW, 100, 40), group(SE, 100, 40)));
		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, -1), group(NE, -1, 50), group(SW, 0, 80), group(SE, 100, 60)));

		assertEquals(0.4f, bars.fraction(0), DELTA);
		assertEquals(0.5f, bars.fraction(1), DELTA);
		assertEquals(0.4f, bars.fraction(2), DELTA);
		assertEquals(0.6f, bars.fraction(3), DELTA);
	}

	@Test
	public void onlyChangedValuesAreLogged()
	{
		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, 40), group(NE, 100, 40), group(SW, 100, 40), group(SE, 100, 40)));
		assertEquals(4, debugLog.snapshot().size());

		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, 40), group(NE, -1, -1), group(SW, 100, -1), group(SE, -1, 40)));
		assertEquals(4, debugLog.snapshot().size());

		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, 41), group(NE, -1, -1), group(SW, 200, -1), group(SE, -1, -1)));
		assertEquals(5, debugLog.snapshot().size());
		assertEquals("Pillar bar 2: 40/200", debugLog.snapshot().get(4).message);

		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, 100), group(NE, -1, -1), group(SW, -1, -1), group(SE, -1, -1)));
		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, 0), group(NE, -1, -1), group(SW, -1, -1), group(SE, -1, -1)));
		final List<DebugLog.Entry> entries = debugLog.snapshot();
		assertEquals(7, entries.size());
		assertEquals("Pillar bar 0: 100/100", entries.get(5).message);
		assertEquals("Pillar bar 0: 0/100", entries.get(6).message);
	}

	@Test
	public void fadesAreLoggedOnlyWhenShownChanges()
	{
		bars.onScript(FADE_OUT_SCRIPT, null);
		assertTrue(debugLog.snapshot().isEmpty());
		bars.onScript(FADE_IN_SCRIPT, null);
		bars.onScript(FADE_IN_SCRIPT, null);
		assertEquals(1, debugLog.snapshot().size());
		assertEquals(1, debugLog.snapshot().get(0).repeats);
	}

	@Test
	public void anUnchangedCurrentIsRescaledByANewMax()
	{
		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, 40), group(NE, 100, 100), group(SW, 100, 40), group(SE, 100, 40)));
		bars.onScript(UPDATE_SCRIPT, update(group(NW, 200, -1), group(NE, 200, -1), group(SW, -1, -1), group(SE, 50, -1)));

		assertEquals(0.2f, bars.fraction(0), DELTA);
		assertEquals(0.5f, bars.fraction(1), DELTA);
		assertFalse(bars.isFull(1));
		assertEquals(0.4f, bars.fraction(2), DELTA);
		assertEquals(0.8f, bars.fraction(3), DELTA);
	}

	@Test
	public void anUnchangedValueWithNothingStoredIsSkipped()
	{
		bars.onScript(UPDATE_SCRIPT, update(group(NW, -1, 50), group(NE, 100, -1), group(SW, -1, -1), group(SE, 0, 50)));
		for (int corner = 0; corner < PillarBars.CORNERS; corner++)
		{
			assertEquals(0f, bars.fraction(corner), DELTA);
		}

		// The max from the first update is kept for a later current.
		bars.onScript(UPDATE_SCRIPT, update(group(NW, -1, -1), group(NE, -1, 25), group(SW, -1, -1), group(SE, -1, -1)));
		assertEquals(0.25f, bars.fraction(1), DELTA);
		assertEquals(0f, bars.fraction(0), DELTA);
	}

	@Test
	public void resetForgetsTheStoredValues()
	{
		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, 40), group(NE, 100, 40), group(SW, 100, 40), group(SE, 100, 40)));
		bars.reset();
		bars.onScript(UPDATE_SCRIPT, update(group(NW, -1, 50), group(NE, 200, -1), group(SW, -1, -1), group(SE, -1, -1)));
		for (int corner = 0; corner < PillarBars.CORNERS; corner++)
		{
			assertEquals(0f, bars.fraction(corner), DELTA);
		}
	}

	@Test
	public void clampsAndSetsFull()
	{
		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, 150), group(NE, 100, 100), group(SW, 100, 99), group(SE, 100, 0)));

		assertEquals(1f, bars.fraction(0), DELTA);
		assertTrue(bars.isFull(0));
		assertTrue(bars.isFull(1));
		assertFalse(bars.isFull(2));
		assertFalse(bars.isFull(3));
		assertEquals(0f, bars.fraction(3), DELTA);

		bars.onScript(UPDATE_SCRIPT, update(group(NW, 100, 0), group(NE, 100, 100), group(SW, 100, 99), group(SE, 100, 0)));
		assertFalse(bars.isFull(0));
		assertEquals(0f, bars.fraction(0), DELTA);
	}

	@Test
	public void fadeScriptsShowAndHide()
	{
		assertFalse(bars.isShown());
		bars.onScript(FADE_IN_SCRIPT, new Object[]{FADE_IN_SCRIPT});
		assertTrue(bars.isShown());
		bars.onScript(FADE_OUT_SCRIPT, null);
		assertFalse(bars.isShown());
	}

	@Test
	public void ignoresOtherScriptsAndBadArguments()
	{
		bars.onScript(UPDATE_SCRIPT + 1, update(group(NW, 100, 50), group(NE, 100, 50), group(SW, 100, 50), group(SE, 100, 50)));
		bars.onScript(UPDATE_SCRIPT, null);
		bars.onScript(UPDATE_SCRIPT, new Object[]{UPDATE_SCRIPT, NW, NW + 1, 100, 50});

		final Object[] wrongTypes = update(group(NW, 100, 50), group(NE, 100, 50), group(SW, 100, 50), group(SE, 100, 50));
		wrongTypes[1] = "text";
		wrongTypes[8] = null;
		wrongTypes[12] = "text";
		wrongTypes[13] = 12345;
		bars.onScript(UPDATE_SCRIPT, wrongTypes);

		for (int corner = 0; corner < PillarBars.CORNERS; corner++)
		{
			assertEquals(0f, bars.fraction(corner), DELTA);
			assertFalse(bars.isFull(corner));
		}
		assertFalse(bars.isShown());
		assertFalse(bars.isVisible());
	}

	@Test
	public void mapsCorners()
	{
		assertEquals(0, PillarBars.corner(NW));
		assertEquals(1, PillarBars.corner(NE));
		assertEquals(2, PillarBars.corner(SW));
		assertEquals(3, PillarBars.corner(SE));
		assertEquals(-1, PillarBars.corner(-1));
	}

	@Test
	public void seedsFromWidths()
	{
		assertEquals(0.5f, PillarBars.seedFraction(30, 60), DELTA);
		assertEquals(1f, PillarBars.seedFraction(60, 60), DELTA);
		assertEquals(0f, PillarBars.seedFraction(30, 0), DELTA);
		assertEquals(0f, PillarBars.seedFraction(0, -1), DELTA);
		assertTrue(PillarBars.seedFull(60, 60));
		assertFalse(PillarBars.seedFull(59, 60));
		assertFalse(PillarBars.seedFull(0, 0));
	}

	@Test
	public void layoutSize()
	{
		assertEquals(64, PillarBarsOverlay.barWidth(40));
		assertEquals(74, PillarBarsOverlay.barWidth(70));
		assertEquals(5, PillarBarsOverlay.barHeight(4));
		assertEquals(9, PillarBarsOverlay.barHeight(24));
		assertEquals(7, PillarBarsOverlay.barHeight(7));
		assertEquals(22, PillarBarsOverlay.cellHeight(12, 9));
		assertEquals(new Dimension(134, 47), PillarBarsOverlay.gridSize(64, 22));
	}

	@Test
	public void filledWidth()
	{
		assertEquals(62, PillarBarsOverlay.filledWidth(62, 0.2f, true));
		assertEquals(31, PillarBarsOverlay.filledWidth(62, 0.5f, false));
		assertEquals(0, PillarBarsOverlay.filledWidth(62, 0f, false));
		assertEquals(62, PillarBarsOverlay.filledWidth(62, 1.5f, false));
	}
}
