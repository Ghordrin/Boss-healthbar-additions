package com.ghordrin.bosshealthbar;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;

// Shares your hits on the current opponent with your RuneLite party, so members who count party
// damage can add them to their number.
@Singleton
class PartyDamage
{
	private final Client client;
	private final BossHealthBarConfig config;
	private final PartyService partyService;
	private final WSClient wsClient;
	private final OpponentTracker opponentTracker;
	private final DamageTracker damageTracker;

	@Inject
	PartyDamage(Client client, BossHealthBarConfig config, PartyService partyService, WSClient wsClient,
		OpponentTracker opponentTracker, DamageTracker damageTracker)
	{
		this.client = client;
		this.config = config;
		this.partyService = partyService;
		this.wsClient = wsClient;
		this.opponentTracker = opponentTracker;
		this.damageTracker = damageTracker;
	}

	void startUp()
	{
		wsClient.registerMessage(BossBarPartyHit.class);
	}

	void shutDown()
	{
		wsClient.unregisterMessage(BossBarPartyHit.class);
	}

	void sendHit(Actor target, int damage)
	{
		if (damage <= 0 || !(target instanceof NPC) || !partyService.isInParty())
		{
			return;
		}
		partyService.send(new BossBarPartyHit(client.getWorld(), ((NPC) target).getIndex(), damage));
	}

	boolean countsPartyHits()
	{
		return config.damageNumberSource() == DamageNumberSource.PARTY && partyService.isInParty();
	}

	void onPartyHit(BossBarPartyHit hit)
	{
		if (!countsPartyHits())
		{
			return;
		}

		final PartyMember local = partyService.getLocalMember();
		if (local != null && local.getMemberId() == hit.getMemberId())
		{
			return;
		}

		final Actor opponent = opponentTracker.getOpponent();
		if (opponent instanceof NPC && ((NPC) opponent).getIndex() == hit.getNpcIndex() && client.getWorld() == hit.getWorld())
		{
			damageTracker.recordPartyHit(hit.getDamage(), client.getTickCount());
		}
	}
}
