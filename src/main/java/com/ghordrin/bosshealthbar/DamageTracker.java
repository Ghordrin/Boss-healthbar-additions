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

	void recordHit(Hitsplat hitsplat, DamageNumberSource source)
	{
		if (hitsplat.getAmount() <= 0 || hitsplat.getHitsplatType() == HitsplatID.HEAL)
		{
			return;
		}

		final long now = System.currentTimeMillis();
		lastHitMillis = now;
		lastHitAmount = hitsplat.getAmount();

		if (countsTowardCombo(hitsplat.isMine(), hitsplat.isOthers(), source))
		{
			addToCombo(hitsplat.getAmount(), now);
		}
	}

	// Party members' hits arrive over the party connection instead, so their hitsplats aren't counted twice.
	static boolean countsTowardCombo(boolean mine, boolean others, DamageNumberSource source)
	{
		return mine || (others && source == DamageNumberSource.EVERYONE);
	}

	void recordPartyHit(int amount)
	{
		if (amount > 0)
		{
			addToCombo(amount, System.currentTimeMillis());
		}
	}

	private void addToCombo(int amount, long now)
	{
		comboDamage = nextComboDamage(comboDamage, lastDamageDealtMillis, now, amount);
		lastDamageDealtMillis = now;
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
