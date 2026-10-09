package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.BarAnimation.clamp01;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

// The game's pillar charge bars, read from the scripts that drive them so they can be drawn while the game's are hidden.
@Slf4j
@Singleton
class PillarBars
{
	// Client script ids, which have no gameval constants.
	static final int UPDATE_SCRIPT = 3315;
	static final int FADE_IN_SCRIPT = 3312;
	static final int FADE_OUT_SCRIPT = 3323;

	static final int CORNERS = 4;
	private static final int ARGS_PER_CORNER = 4;
	// The script id, four corners and a trailing flag.
	static final int UPDATE_ARGS = 1 + CORNERS * ARGS_PER_CORNER + 1;
	private static final int UNCHANGED = -1;
	private static final int TRANSPARENT = 255;

	// In drawing order: north west, north east, south west, south east.
	private static final int[] BAR_BACKS = {
		InterfaceID.NightmareTotems.TOTEM_NW_BAR_BACK,
		InterfaceID.NightmareTotems.TOTEM_NE_BAR_BACK,
		InterfaceID.NightmareTotems.TOTEM_SW_BAR_BACK,
		InterfaceID.NightmareTotems.TOTEM_SE_BAR_BACK,
	};
	private static final int[] BAR_REMAINING = {
		InterfaceID.NightmareTotems.TOTEM_NW_BAR_REMAINING,
		InterfaceID.NightmareTotems.TOTEM_NE_BAR_REMAINING,
		InterfaceID.NightmareTotems.TOTEM_SW_BAR_REMAINING,
		InterfaceID.NightmareTotems.TOTEM_SE_BAR_REMAINING,
	};

	private final Client client;

	private final float[] fractions = new float[CORNERS];
	private final boolean[] full = new boolean[CORNERS];
	private final boolean[] known = new boolean[CORNERS];
	private final int[] maxes = new int[CORNERS];
	private final int[] currents = {UNCHANGED, UNCHANGED, UNCHANGED, UNCHANGED};
	private boolean shown;
	private boolean shownKnown;
	private boolean seen;
	private boolean hidden;

	@Inject
	PillarBars(Client client)
	{
		this.client = client;
	}

	void onScript(int scriptId, Object[] args)
	{
		if (scriptId == FADE_IN_SCRIPT || scriptId == FADE_OUT_SCRIPT)
		{
			shown = scriptId == FADE_IN_SCRIPT;
			shownKnown = true;
			return;
		}
		if (scriptId != UPDATE_SCRIPT || args == null || args.length < UPDATE_ARGS)
		{
			return;
		}

		for (int group = 0; group < CORNERS; group++)
		{
			final int first = 1 + group * ARGS_PER_CORNER;
			final Object back = args[first];
			final Object max = args[first + 2];
			final Object current = args[first + 3];
			if (!(back instanceof Integer) || !(max instanceof Integer) || !(current instanceof Integer))
			{
				continue;
			}

			final int corner = corner((Integer) back);
			if (corner >= 0)
			{
				apply(corner, (Integer) max, (Integer) current);
			}
		}
	}

	// Either value can come as unchanged, so the last one the script sent stands in for it.
	private void apply(int corner, int maxValue, int currentValue)
	{
		final int max = maxValue == UNCHANGED ? maxes[corner] : maxValue;
		if (max <= 0)
		{
			return;
		}
		maxes[corner] = max;
		final int current = currentValue == UNCHANGED ? currents[corner] : currentValue;
		if (current == UNCHANGED)
		{
			return;
		}
		currents[corner] = current;
		fractions[corner] = fraction(current, max);
		full[corner] = current >= max;
		known[corner] = true;
		log.debug("Pillar bar {}: {}/{}", corner, current, max);
	}

	void update(boolean replace)
	{
		final Widget totems = client.getWidget(InterfaceID.NightmareTotems.TOTEMS);
		if (totems == null)
		{
			hidden = false;
			clear();
			return;
		}

		if (!seen)
		{
			seed();
			seen = true;
		}

		if (replace)
		{
			if (!totems.isSelfHidden())
			{
				totems.setHidden(true);
				hidden = true;
			}
		}
		else
		{
			release();
		}
	}

	// Hidden widgets don't run their timers, so the widths are only read before the bars are first hidden.
	private void seed()
	{
		for (int corner = 0; corner < CORNERS; corner++)
		{
			final Widget back = client.getWidget(BAR_BACKS[corner]);
			final Widget remaining = client.getWidget(BAR_REMAINING[corner]);
			if (known[corner] || back == null || remaining == null)
			{
				continue;
			}
			fractions[corner] = seedFraction(remaining.getWidth(), back.getWidth());
			full[corner] = seedFull(remaining.getWidth(), back.getWidth());
		}

		if (!shownKnown)
		{
			final Widget border = client.getWidget(InterfaceID.NightmareTotems.TOTEMS_BORDER);
			shown = border != null && border.getOpacity() < TRANSPARENT;
		}
		log.debug("Pillar bars seeded: {} {} shown={}", Arrays.toString(fractions), Arrays.toString(full), shown);
	}

	private void release()
	{
		if (hidden)
		{
			final Widget totems = client.getWidget(InterfaceID.NightmareTotems.TOTEMS);
			if (totems != null)
			{
				totems.setHidden(false);
			}
		}
		hidden = false;
	}

	void reset()
	{
		release();
		clear();
	}

	private void clear()
	{
		Arrays.fill(fractions, 0f);
		Arrays.fill(full, false);
		Arrays.fill(known, false);
		Arrays.fill(maxes, 0);
		Arrays.fill(currents, UNCHANGED);
		shown = false;
		shownKnown = false;
		seen = false;
	}

	boolean isVisible()
	{
		return hidden && shown;
	}

	float fraction(int corner)
	{
		return fractions[corner];
	}

	boolean isFull(int corner)
	{
		return full[corner];
	}

	static int corner(int barBackId)
	{
		for (int corner = 0; corner < CORNERS; corner++)
		{
			if (BAR_BACKS[corner] == barBackId)
			{
				return corner;
			}
		}
		return -1;
	}

	static float fraction(int current, int max)
	{
		return clamp01(current / (float) max);
	}

	static float seedFraction(int remainingWidth, int backWidth)
	{
		return backWidth > 0 ? clamp01(remainingWidth / (float) backWidth) : 0f;
	}

	static boolean seedFull(int remainingWidth, int backWidth)
	{
		return backWidth > 0 && remainingWidth == backWidth;
	}

	boolean isShown()
	{
		return shown;
	}
}
