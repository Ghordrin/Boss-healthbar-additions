package com.ghordrin.bosshealthbar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

// Party messages are registered by class name, so this one is named to stay clear of other plugins.
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BossBarPartyHit extends PartyMemberMessage
{
	private int world;
	private int npcIndex;
	private int damage;
}
