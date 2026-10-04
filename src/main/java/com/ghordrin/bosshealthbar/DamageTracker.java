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
	static final Duration DISPLAY_HOLD = Duration.ofMillis(2500);
	static final Duration PARTY_WINDOW = Duration.ofMillis(2500);
	static final int NO_TICK = -1;

	// Hits on the same or the next tick belong to one attack (multi-hit weapons and specs land a tick apart).
	private static final int MAX_TICK_GAP = 1;

	@Getter(AccessLevel.PACKAGE)
	private long lastHitMillis;

	@Getter(AccessLevel.PACKAGE)
	private int lastHitAmount;

	@Getter(AccessLevel.PACKAGE)
	private int comboDamage;

	@Getter(AccessLevel.PACKAGE)
	private long lastDamageDealtMillis;

	private int comboStartTick = NO_TICK;

	void recordHit(Hitsplat hitsplat, int tick, boolean partyTotal)
	{
		if (hitsplat.getAmount() <= 0 || hitsplat.getHitsplatType() == HitsplatID.HEAL)
		{
			return;
		}

		final long now = System.currentTimeMillis();
		lastHitMillis = now;
		lastHitAmount = hitsplat.getAmount();

		// Party members' hits arrive over the party connection instead, so other players' hitsplats never count.
		if (hitsplat.isMine())
		{
			addToCombo(hitsplat.getAmount(), now, tick, partyTotal);
		}
	}

	void recordPartyHit(int amount, int tick)
	{
		if (amount > 0)
		{
			addToCombo(amount, System.currentTimeMillis(), tick, true);
		}
	}

	// A party total keeps adding while hits keep coming; otherwise each attack gets its own number.
	void addToCombo(int amount, long now, int tick, boolean partyTotal)
	{
		final boolean continues = partyTotal
			? continuesPartyTotal(lastDamageDealtMillis, now)
			: isSameAttack(comboStartTick, tick);
		if (!continues)
		{
			comboDamage = 0;
			comboStartTick = tick;
		}
		comboDamage += amount;
		lastDamageDealtMillis = now;
	}

	static boolean continuesPartyTotal(long lastDamageMillis, long now)
	{
		return lastDamageMillis != 0 && now - lastDamageMillis <= PARTY_WINDOW.toMillis();
	}

	void resetCombo()
	{
		comboDamage = 0;
		lastDamageDealtMillis = 0;
		comboStartTick = NO_TICK;
	}

	void reset()
	{
		lastHitMillis = 0;
		resetCombo();
	}

	// Measured from the first hit, so hits that keep landing a tick apart can't chain into one long total.
	static boolean isSameAttack(int startTick, int tick)
	{
		final int gap = tick - startTick;
		return startTick != NO_TICK && gap >= 0 && gap <= MAX_TICK_GAP;
	}
}
