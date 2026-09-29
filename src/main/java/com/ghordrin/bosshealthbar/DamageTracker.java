package com.ghordrin.bosshealthbar;

import java.time.Duration;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;

@Singleton
class DamageTracker
{
	static final Duration COMBO_WINDOW = Duration.ofMillis(2500);

	@Getter(AccessLevel.PACKAGE)
	private long lastHitMillis;

	@Getter(AccessLevel.PACKAGE)
	private int lastHitAmount;

	@Getter(AccessLevel.PACKAGE)
	private int comboDamage;

	@Getter(AccessLevel.PACKAGE)
	private long lastDamageDealtMillis;

	void recordHit(Hitsplat hitsplat)
	{
		if (hitsplat.getAmount() <= 0 || hitsplat.getHitsplatType() == HitsplatID.HEAL)
		{
			return;
		}

		final long now = System.currentTimeMillis();
		lastHitMillis = now;
		lastHitAmount = hitsplat.getAmount();

		if (hitsplat.isMine())
		{
			comboDamage = nextComboDamage(comboDamage, lastDamageDealtMillis, now, hitsplat.getAmount());
			lastDamageDealtMillis = now;
		}
	}

	void resetCombo()
	{
		comboDamage = 0;
		lastDamageDealtMillis = 0;
	}

	void reset()
	{
		lastHitMillis = 0;
		resetCombo();
	}

	static int nextComboDamage(int previousCombo, long previousDamageMillis, long now, int hitAmount)
	{
		final boolean withinWindow = previousDamageMillis != 0 && now - previousDamageMillis <= COMBO_WINDOW.toMillis();
		return (withinWindow ? previousCombo : 0) + hitAmount;
	}
}
