package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BarLayout.NOT_PLACED;
import static com.ghordrin.bosshealthbar.BarLayout.Spot.CENTER;
import static com.ghordrin.bosshealthbar.BarLayout.Spot.LEFT;
import static com.ghordrin.bosshealthbar.BarLayout.Spot.RIGHT;
import static com.ghordrin.bosshealthbar.BarTextPainter.DAMAGE_NUMBER;
import static com.ghordrin.bosshealthbar.BarTextPainter.DRAIN_CAP;
import static com.ghordrin.bosshealthbar.BarTextPainter.FIGHT_TIMER;
import static com.ghordrin.bosshealthbar.BarTextPainter.HITPOINTS;
import static com.ghordrin.bosshealthbar.BarTextPainter.ITEM_COUNT;
import static com.ghordrin.bosshealthbar.BarTextPainter.KILL_COUNT;
import static com.ghordrin.bosshealthbar.BarTextPainter.LEVEL_GAP;
import static com.ghordrin.bosshealthbar.BarTextPainter.MAGIC_DEFENCE;
import static com.ghordrin.bosshealthbar.BarTextPainter.NAME;
import static com.ghordrin.bosshealthbar.BarTextPainter.PARTY_DEFENCE;
import static com.ghordrin.bosshealthbar.BarTextPainter.SPECIAL_ATTACKS;
import static com.ghordrin.bosshealthbar.BarTextPainter.TEXT_INSET;
import static com.ghordrin.bosshealthbar.BarTextPainter.WEAKNESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import org.junit.Test;

public class BarLayoutTest
{
	private static final int WIDTH = 220;
	private static final int LEFT_EDGE = 10;
	private static final int RIGHT_EDGE = 210;
	private static final int GAP = 12;

	private final BarLayout layout = new BarLayout(ITEM_COUNT);

	private void layOut()
	{
		layout.layout(WIDTH, LEFT_EDGE, RIGHT_EDGE, GAP);
	}

	@Test
	public void leftItemsFillFromTheLeftEdgeInImportanceOrder()
	{
		layout.add(WEAKNESS, LEFT, 30);
		layout.add(KILL_COUNT, LEFT, 10);
		layout.add(PARTY_DEFENCE, LEFT, 20);
		layOut();

		assertEquals(10, layout.x(KILL_COUNT));
		assertEquals(32, layout.x(PARTY_DEFENCE));
		assertEquals(64, layout.x(WEAKNESS));
	}

	@Test
	public void rightItemsFillFromTheRightEdgeWithTheMostImportantAtTheEdge()
	{
		layout.add(NAME, RIGHT, 50);
		layout.add(DAMAGE_NUMBER, RIGHT, 30);
		layOut();

		assertEquals(160, layout.x(NAME));
		assertEquals(118, layout.x(DAMAGE_NUMBER));
	}

	@Test
	public void centreItemsAreCentredOnTheBarAndReadInImportanceOrder()
	{
		layout.add(KILL_COUNT, CENTER, 30);
		layout.add(HITPOINTS, CENTER, 20);
		layOut();

		// The group is 20 + 12 + 30 = 62 wide.
		assertEquals(79, layout.x(HITPOINTS));
		assertEquals(111, layout.x(KILL_COUNT));
	}

	@Test
	public void onceAnItemDoesNotFitTheRestOfItsSpotIsLeftOut()
	{
		layout.add(KILL_COUNT, LEFT, 100);
		layout.add(PARTY_DEFENCE, LEFT, 150);
		layout.add(SPECIAL_ATTACKS, LEFT, 10);
		layout.add(WEAKNESS, RIGHT, 10);
		layOut();

		assertEquals(10, layout.x(KILL_COUNT));
		assertEquals(NOT_PLACED, layout.x(PARTY_DEFENCE));
		assertEquals(NOT_PLACED, layout.x(SPECIAL_ATTACKS));
		// Other spots carry on.
		assertEquals(200, layout.x(WEAKNESS));
	}

	@Test
	public void anItemThatOnlyPartlyFitsIsLeftOutWhole()
	{
		layout.add(SPECIAL_ATTACKS, LEFT, RIGHT_EDGE - LEFT_EDGE + 1);
		layOut();

		assertEquals(NOT_PLACED, layout.x(SPECIAL_ATTACKS));
		assertEquals(0, layout.room(SPECIAL_ATTACKS));
	}

	@Test
	public void moreImportantItemsGetTheSpaceFirst()
	{
		layout.add(HITPOINTS, LEFT, 150);
		layout.add(DAMAGE_NUMBER, RIGHT, 60);
		layout.add(KILL_COUNT, RIGHT, 5);
		layOut();

		assertEquals(10, layout.x(HITPOINTS));
		assertEquals(NOT_PLACED, layout.x(DAMAGE_NUMBER));
		assertEquals(NOT_PLACED, layout.x(KILL_COUNT));
	}

	@Test
	public void itemsKeepTheGapToTheOtherSide()
	{
		layout.add(HITPOINTS, RIGHT, 100);
		layout.add(KILL_COUNT, LEFT, 88);
		layout.add(PARTY_DEFENCE, LEFT, 0);
		layOut();

		// 10 + 88 + 12 == 110, the hitpoints' x.
		assertEquals(10, layout.x(KILL_COUNT));
		assertEquals(NOT_PLACED, layout.x(PARTY_DEFENCE));
	}

	@Test
	public void theCentreGroupDropsItsLaterItemsWhenTheyWouldHitTheSides()
	{
		layout.add(HITPOINTS, LEFT, 60);
		layout.add(DAMAGE_NUMBER, CENTER, 20);
		layout.add(KILL_COUNT, CENTER, 40);
		layout.add(PARTY_DEFENCE, CENTER, 10);
		layout.add(SPECIAL_ATTACKS, RIGHT, 100);
		layOut();

		assertEquals(10, layout.x(HITPOINTS));
		assertEquals(100, layout.x(DAMAGE_NUMBER));
		// With the kill count the group would start at 74, inside the hitpoints' gap.
		assertEquals(NOT_PLACED, layout.x(KILL_COUNT));
		// It would fit on its own, but nothing moves into an earlier item's place.
		assertEquals(NOT_PLACED, layout.x(PARTY_DEFENCE));
		// Side items placed later keep clear of the centre group.
		assertEquals(NOT_PLACED, layout.x(SPECIAL_ATTACKS));
	}

	@Test
	public void sideItemsPlacedAfterTheCentreGroupStopAtIt()
	{
		layout.add(HITPOINTS, CENTER, 40);
		layout.add(KILL_COUNT, LEFT, 68);
		layout.add(PARTY_DEFENCE, RIGHT, 68);
		layout.add(WEAKNESS, RIGHT, 1);
		layOut();

		assertEquals(90, layout.x(HITPOINTS));
		assertEquals(10, layout.x(KILL_COUNT));
		assertEquals(142, layout.x(PARTY_DEFENCE));
		assertEquals(NOT_PLACED, layout.x(WEAKNESS));
	}

	@Test
	public void aKeptItemIsPlacedEvenWhenItDoesNotFit()
	{
		layout.addKept(NAME, LEFT, 500);
		layOut();

		assertEquals(10, layout.x(NAME));
		assertEquals(0, layout.room(NAME));
	}

	@Test
	public void theNameGetsTheRoomLeftBeforeItemsInOtherSpots()
	{
		layout.addKept(NAME, LEFT, 30);
		layout.add(DAMAGE_NUMBER, RIGHT, 40);
		layOut();

		assertEquals(170, layout.x(DAMAGE_NUMBER));
		assertEquals(170 - GAP - 10 - 30, layout.room(NAME));
	}

	@Test
	public void laterItemsInTheNamesSpotMoveOutAsTheNameGrows()
	{
		layout.addKept(NAME, LEFT, 30);
		layout.add(KILL_COUNT, LEFT, 20);
		layOut();

		final int room = layout.room(NAME);
		assertEquals(RIGHT_EDGE - (10 + 30 + GAP + 20), room);

		layout.setWidth(NAME, 30 + room);
		layOut();
		assertEquals(10, layout.x(NAME));
		assertEquals(RIGHT_EDGE - 20, layout.x(KILL_COUNT));
	}

	@Test
	public void aNameOnTheRightGrowsLeftwardsAndPushesTheDamageNumberAlong()
	{
		layout.addKept(NAME, RIGHT, 30);
		layout.add(DAMAGE_NUMBER, RIGHT, 40);
		layOut();

		assertEquals(180, layout.x(NAME));
		assertEquals(128, layout.x(DAMAGE_NUMBER));
		final int room = layout.room(NAME);
		assertEquals(128 - LEFT_EDGE, room);

		layout.setWidth(NAME, 30 + room);
		layOut();
		assertEquals(RIGHT_EDGE - 30 - room, layout.x(NAME));
		assertEquals(LEFT_EDGE, layout.x(DAMAGE_NUMBER));
	}

	@Test
	public void aCentredNameGrowsEvenlyUpToTheCloserSide()
	{
		layout.addKept(NAME, CENTER, 20);
		layout.add(KILL_COUNT, LEFT, 50);
		layOut();

		final int room = layout.room(NAME);
		// The kill count ends at 60, so the name can start at 72 and, centred, be 220 - 2 * 72 = 76 wide.
		assertEquals(76 - 20, room);

		layout.setWidth(NAME, 20 + room);
		layOut();
		assertEquals(72, layout.x(NAME));
		assertEquals(10, layout.x(KILL_COUNT));
	}

	@Test
	public void roomNeverGoesNegative()
	{
		layout.addKept(NAME, LEFT, 300);
		layout.add(DAMAGE_NUMBER, RIGHT, 40);
		layOut();

		assertEquals(0, layout.room(NAME));
	}

	@Test
	public void damageNumberSitsAgainstItsSpotsSide()
	{
		assertEquals(100, BarTextPainter.damageNumberX(LEFT, 100, 40, 20));
		assertEquals(110, BarTextPainter.damageNumberX(CENTER, 100, 40, 20));
		assertEquals(120, BarTextPainter.damageNumberX(RIGHT, 100, 40, 20));
	}

	@Test
	public void positionsDefaultToTheClassicLayout()
	{
		final BossHealthBarConfig defaults = new BossHealthBarConfig()
		{
		};
		assertEquals(BarPosition.TOP_LEFT, defaults.namePosition());
		assertEquals(BarPosition.TOP_RIGHT, defaults.damageNumberPosition());
		assertEquals(BarPosition.BOTTOM_RIGHT, defaults.hitpointsPosition());
		assertEquals(BarPosition.BOTTOM_LEFT, defaults.killCountPosition());
		assertEquals(BarPosition.BOTTOM_LEFT, defaults.partyDefencePosition());
		assertEquals(BarPosition.BOTTOM_LEFT, defaults.specialAttackCountsPosition());
		assertEquals(BarPosition.BOTTOM_LEFT, defaults.weaknessPosition());
		assertEquals(BarPosition.BOTTOM_LEFT, defaults.drainCapPosition());
		assertEquals(BarPosition.TOP_CENTER, defaults.fightTimerPosition());
		assertTrue(defaults.namePosition().isTop());
		assertFalse(defaults.hitpointsPosition().isTop());
	}

	// The checks below lay out the default positions the way the painter does and compare them with how the
	// header and footer were placed before positions could be chosen.
	private static final int BAR_WIDTH = 600;
	private static final int CAP_WIDTH = 9;
	private static final int BAR_LEFT = CAP_WIDTH + TEXT_INSET;
	private static final int BAR_RIGHT = BAR_WIDTH - CAP_WIDTH - TEXT_INSET;

	private static void layOutDefaults(BarLayout layout)
	{
		layout.layout(BAR_WIDTH, BAR_LEFT, BAR_RIGHT, LEVEL_GAP);
	}

	@Test
	public void defaultHeaderMatchesTheClassicLayout()
	{
		final BossHealthBarConfig defaults = new BossHealthBarConfig()
		{
		};
		final int iconAdvance = 18;
		final int levelWidth = LEVEL_GAP + 30;
		final int ellipsisWidth = 14;
		final int sizingWidth = 36;
		final int nameWidth = 120;

		final BarLayout header = new BarLayout(ITEM_COUNT);
		header.addKept(NAME, defaults.namePosition().getSpot(), iconAdvance + levelWidth + ellipsisWidth);
		header.add(DAMAGE_NUMBER, defaults.damageNumberPosition().getSpot(), sizingWidth);
		layOutDefaults(header);

		final int nameMaxWidth = ellipsisWidth + header.room(NAME);
		final int classicNameMaxWidth = BAR_RIGHT - (BAR_LEFT + iconAdvance) - (sizingWidth + LEVEL_GAP) - levelWidth;
		assertEquals(classicNameMaxWidth, nameMaxWidth);

		header.setWidth(NAME, iconAdvance + nameWidth + levelWidth);
		layOutDefaults(header);
		assertEquals(BAR_LEFT, header.x(NAME));
		assertEquals(BAR_RIGHT - 25, BarTextPainter.damageNumberX(header.spot(DAMAGE_NUMBER), header.x(DAMAGE_NUMBER),
			header.width(DAMAGE_NUMBER), 25));
	}

	@Test
	public void defaultFightTimerSitsCentredAboveTheBarAndLimitsTheName()
	{
		final BossHealthBarConfig defaults = new BossHealthBarConfig()
		{
		};
		final int ellipsisWidth = 14;
		final int timerWidth = 30;

		final BarLayout header = new BarLayout(ITEM_COUNT);
		header.addKept(NAME, defaults.namePosition().getSpot(), ellipsisWidth);
		header.add(DAMAGE_NUMBER, defaults.damageNumberPosition().getSpot(), 36);
		header.add(FIGHT_TIMER, defaults.fightTimerPosition().getSpot(), timerWidth);
		layOutDefaults(header);

		final int timerX = (BAR_WIDTH - timerWidth) / 2;
		assertEquals(timerX, header.x(FIGHT_TIMER));
		assertEquals(timerX - LEVEL_GAP - BAR_LEFT - ellipsisWidth, header.room(NAME));
	}

	@Test
	public void defaultFooterMatchesTheClassicLayout()
	{
		final BossHealthBarConfig defaults = new BossHealthBarConfig()
		{
		};
		final int hitpointsWidth = 80;
		final int[] widths = {0, hitpointsWidth, 0, 0, 40, 50, 45, 70, 35, 30};

		final BarLayout footer = new BarLayout(ITEM_COUNT);
		footer.add(HITPOINTS, defaults.hitpointsPosition().getSpot(), hitpointsWidth);
		footer.add(KILL_COUNT, defaults.killCountPosition().getSpot(), widths[KILL_COUNT]);
		footer.add(PARTY_DEFENCE, defaults.partyDefencePosition().getSpot(), widths[PARTY_DEFENCE]);
		footer.add(MAGIC_DEFENCE, defaults.partyDefencePosition().getSpot(), widths[MAGIC_DEFENCE]);
		footer.add(SPECIAL_ATTACKS, defaults.specialAttackCountsPosition().getSpot(), widths[SPECIAL_ATTACKS]);
		footer.add(WEAKNESS, defaults.weaknessPosition().getSpot(), widths[WEAKNESS]);
		footer.add(DRAIN_CAP, defaults.drainCapPosition().getSpot(), widths[DRAIN_CAP]);
		layOutDefaults(footer);

		assertEquals(BAR_RIGHT - hitpointsWidth, footer.x(HITPOINTS));
		int left = BAR_LEFT;
		for (int item = KILL_COUNT; item <= DRAIN_CAP; item++)
		{
			assertEquals(left, footer.x(item));
			left += widths[item] + LEVEL_GAP;
		}
	}

	@Test
	public void defaultFooterKeepsTheHitpointsSlotAndCentresDefeated()
	{
		final int defeatedWidth = 51;
		final BarLayout footer = new BarLayout(ITEM_COUNT);
		footer.add(HITPOINTS, BarLayout.Spot.RIGHT, 80);
		footer.add(KILL_COUNT, BarLayout.Spot.LEFT, 40);
		footer.add(PARTY_DEFENCE, BarLayout.Spot.LEFT, 50);
		layOutDefaults(footer);

		assertEquals(BAR_RIGHT - 80, footer.x(HITPOINTS));
		assertEquals(BAR_LEFT, footer.x(KILL_COUNT));
		assertEquals(BAR_LEFT + 40 + LEVEL_GAP, footer.x(PARTY_DEFENCE));
		assertEquals(Math.floorDiv(BAR_WIDTH - defeatedWidth, 2), footer.centredX(defeatedWidth, HITPOINTS));
	}

	@Test
	public void defeatedIsLeftOutWhenTheLeftItemsReachTheMiddle()
	{
		final BarLayout footer = new BarLayout(ITEM_COUNT);
		footer.add(HITPOINTS, BarLayout.Spot.RIGHT, 80);
		footer.add(KILL_COUNT, BarLayout.Spot.LEFT, 260);
		layOutDefaults(footer);

		// The kill count ends at 271, within a gap of where "Defeated" would start (274).
		assertEquals(BAR_LEFT, footer.x(KILL_COUNT));
		assertEquals(NOT_PLACED, footer.centredX(51, HITPOINTS));
	}

	@Test
	public void defeatedIsLeftOutWhenAnotherItemIsInTheCentreOfItsRow()
	{
		final BarLayout footer = new BarLayout(ITEM_COUNT);
		footer.add(HITPOINTS, BarLayout.Spot.RIGHT, 80);
		footer.add(KILL_COUNT, CENTER, 40);
		layOutDefaults(footer);

		assertEquals(280, footer.x(KILL_COUNT));
		assertEquals(NOT_PLACED, footer.centredX(51, HITPOINTS));
	}

	@Test
	public void defeatedShowsWithHitpointsAndKillCountBothOnTheRight()
	{
		final BarLayout footer = new BarLayout(ITEM_COUNT);
		footer.add(HITPOINTS, BarLayout.Spot.RIGHT, 80);
		footer.add(KILL_COUNT, BarLayout.Spot.RIGHT, 40);
		layOutDefaults(footer);

		// The kill count stays where it was while the opponent was alive.
		assertEquals(BAR_RIGHT - 80, footer.x(HITPOINTS));
		assertEquals(BAR_RIGHT - 80 - LEVEL_GAP - 40, footer.x(KILL_COUNT));
		assertEquals(274, footer.centredX(51, HITPOINTS));
	}

	@Test
	public void defeatedReplacesCentredHitpointsRatherThanAvoidingThem()
	{
		final BarLayout footer = new BarLayout(ITEM_COUNT);
		footer.add(HITPOINTS, CENTER, 80);
		layOutDefaults(footer);

		assertEquals(260, footer.x(HITPOINTS));
		assertEquals(274, footer.centredX(51, HITPOINTS));
	}

	private static final int LARGE_ROW = 24;
	private static final int SMALL_ROW = 16;

	private static BossHealthBarConfig rowConfig(boolean showName, boolean showDamage, boolean showDefeat,
		boolean showKillCount)
	{
		return new BossHealthBarConfig()
		{
			@Override
			public boolean showBossName()
			{
				return showName;
			}

			@Override
			public boolean showDamageNumber()
			{
				return showDamage;
			}

			@Override
			public boolean showDefeatAnimation()
			{
				return showDefeat;
			}

			@Override
			public boolean showKillCount()
			{
				return showKillCount;
			}
		};
	}

	private static BarPosition[] defaultPositions()
	{
		final BarPosition[] positions = new BarPosition[ITEM_COUNT];
		positions[NAME] = BarPosition.TOP_LEFT;
		positions[DAMAGE_NUMBER] = BarPosition.TOP_RIGHT;
		positions[HITPOINTS] = BarPosition.BOTTOM_RIGHT;
		positions[FIGHT_TIMER] = BarPosition.TOP_CENTER;
		for (int item = KILL_COUNT; item <= DRAIN_CAP; item++)
		{
			positions[item] = BarPosition.BOTTOM_LEFT;
		}
		return positions;
	}

	private static BarPosition[] allAt(BarPosition position)
	{
		final BarPosition[] positions = new BarPosition[ITEM_COUNT];
		Arrays.fill(positions, position);
		return positions;
	}

	private static int[] rowHeights(BossHealthBarConfig config, BarPosition[] positions, boolean others)
	{
		final boolean[] available = new boolean[ITEM_COUNT];
		BarTextPainter.itemsAvailable(config, others, others, others, others, others, available);
		// The default RuneScape fonts: the name and damage number reach 18 above the baseline and 6 below,
		// the small text 12 above and 4 below.
		final int[] above = new int[ITEM_COUNT];
		final int[] below = new int[ITEM_COUNT];
		Arrays.fill(above, 12);
		Arrays.fill(below, SMALL_ROW - 12);
		above[NAME] = above[DAMAGE_NUMBER] = 18;
		below[NAME] = below[DAMAGE_NUMBER] = LARGE_ROW - 18;
		return new int[]{
			BarTextPainter.rowHeight(positions, available, true, above, below),
			BarTextPainter.rowHeight(positions, available, false, above, below)};
	}

	@Test
	public void hitpointsNoneWithoutTheDefeatAnimationLeavesTheBottomRowEmpty()
	{
		final int[] heights = rowHeights(rowConfig(true, true, false, false), defaultPositions(), false);
		assertEquals(LARGE_ROW, heights[0]);
		assertEquals(0, heights[1]);
	}

	@Test
	public void theDefeatAnimationReservesTheHitpointsRowEvenWithHitpointsNone()
	{
		final int[] heights = rowHeights(rowConfig(true, true, true, false), defaultPositions(), false);
		assertEquals(LARGE_ROW, heights[0]);
		assertEquals(SMALL_ROW, heights[1]);

		final BarPosition[] hitpointsOnTop = defaultPositions();
		hitpointsOnTop[HITPOINTS] = BarPosition.TOP_CENTER;
		final int[] moved = rowHeights(rowConfig(false, false, true, false), hitpointsOnTop, false);
		assertEquals(SMALL_ROW, moved[0]);
		assertEquals(0, moved[1]);
	}

	@Test
	public void everythingOnTopLeavesNoBottomRow()
	{
		final int[] heights = rowHeights(rowConfig(true, true, true, true), allAt(BarPosition.TOP_RIGHT), true);
		assertEquals(LARGE_ROW, heights[0]);
		assertEquals(0, heights[1]);
	}

	@Test
	public void everythingOnTheBottomLeavesNoTopRow()
	{
		final int[] heights = rowHeights(rowConfig(true, true, true, true), allAt(BarPosition.BOTTOM_LEFT), true);
		assertEquals(0, heights[0]);
		assertEquals(LARGE_ROW, heights[1]);
	}

	@Test
	public void aRowWithOnlySmallItemsUsesTheSmallHeight()
	{
		final BarPosition[] positions = defaultPositions();
		positions[KILL_COUNT] = BarPosition.TOP_LEFT;
		final int[] heights = rowHeights(rowConfig(false, false, false, true), positions, false);
		assertEquals(SMALL_ROW, heights[0]);
		assertEquals(0, heights[1]);
	}
}