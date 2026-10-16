package com.ghordrin.bosshealthbar.render;

import com.ghordrin.bosshealthbar.data.Health;

public final class HealthText
{
	private HealthText()
	{
	}

	public static String hitpointsText(HitpointsTextMode mode, Health health)
	{
		if (mode == HitpointsTextMode.NONE)
		{
			return null;
		}

		final String percentText = hitpointsPercent(health.ratio, health.scale) + "%";
		final String hpText = hitpointsPart(mode, health);

		if (hpText == null)
		{
			return percentText;
		}
		return mode.showsPercentage() ? hpText + "   " + percentText : hpText;
	}

	// Null when there is no hitpoints part to show, which a boss that only reports a percentage also means.
	private static String hitpointsPart(HitpointsTextMode mode, Health health)
	{
		if (mode.hitpoints() == HitpointsTextMode.Hitpoints.OFF
			|| health.maxHealth == null || health.percentOnly)
		{
			return null;
		}

		final int currentHealth = health.exactHealth
			? health.ratio : estimateHealth(health.ratio, health.scale, health.maxHealth);
		return mode.hitpoints() == HitpointsTextMode.Hitpoints.LEFT
			? String.valueOf(currentHealth) : currentHealth + " / " + health.maxHealth;
	}

	// Like the game's own bar, 0% and 100% are only shown when the opponent is really dead or full.
	static int hitpointsPercent(int ratio, int scale)
	{
		int percent = (int) Math.round(100.0 * ratio / scale);
		if (ratio > 0 && ratio < scale)
		{
			percent = Math.max(1, Math.min(99, percent));
		}
		return percent;
	}

	// The game sends ratio = 1 + (scale - 1) * health / maxHealth, rounded down, for health above 0.
	// This returns the middle of the range of health values that give that ratio.
	static int estimateHealth(int ratio, int healthScale, int maxHealth)
	{
		if (ratio <= 0)
		{
			return 0;
		}

		int minHealth = 1;
		int maxHealthForRatio;
		if (healthScale > 1)
		{
			if (ratio > 1)
			{
				minHealth = (maxHealth * (ratio - 1) + healthScale - 2) / (healthScale - 1);
			}
			maxHealthForRatio = (maxHealth * ratio - 1) / (healthScale - 1);
			if (maxHealthForRatio > maxHealth)
			{
				maxHealthForRatio = maxHealth;
			}
		}
		else
		{
			maxHealthForRatio = maxHealth;
		}

		return (minHealth + maxHealthForRatio + 1) / 2;
	}
}
