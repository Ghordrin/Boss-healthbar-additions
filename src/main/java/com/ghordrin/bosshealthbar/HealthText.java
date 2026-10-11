package com.ghordrin.bosshealthbar;

final class HealthText
{
	private HealthText()
	{
	}

	static String hitpointsText(HitpointsTextMode mode, Health health)
	{
		if (mode == HitpointsTextMode.NONE)
		{
			return null;
		}

		String percentText = hitpointsPercent(health.ratio, health.scale) + "%";

		if (mode == HitpointsTextMode.PERCENTAGE || health.maxHealth == null || health.percentOnly)
		{
			return percentText;
		}

		int currentHealth = health.exactHealth ? health.ratio : estimateHealth(health.ratio, health.scale, health.maxHealth);
		String hpText = currentHealth + " / " + health.maxHealth;

		if (mode == HitpointsTextMode.HITPOINTS)
		{
			return hpText;
		}

		return hpText + "   " + percentText;
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
