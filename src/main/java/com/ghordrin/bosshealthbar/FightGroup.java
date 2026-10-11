package com.ghordrin.bosshealthbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.client.game.NPCManager;
import net.runelite.client.util.Text;

// The NPCs that have been the opponent during the current fight, each with its last known health, so another boss
// fought at the same time can be shown under the main bar.
@Singleton
class FightGroup
{
	static final int MAX_MEMBERS = 8;
	static final int NO_INDEX = -1;

	static final class Member
	{
		// The NPC's index while it's in the scene, NO_INDEX while it's away.
		int index = NO_INDEX;
		NPC npc;
		String name;
		boolean bossLike;
		int lastEngagedTick;
		int lastSeenTick;
		int ratio = -1;
		int scale = -1;
		boolean exact;
		Integer maxHealth;
		BarState state;
		boolean dead;

		boolean isAway()
		{
			return index == NO_INDEX;
		}
	}

	private final GameBossBar gameBossBar;
	private final NPCManager npcManager;
	private final DebugLog debugLog;
	private final List<Member> members = new ArrayList<>(MAX_MEMBERS);
	private int nextId;
	private int id;
	private int bossLikeJoins;
	private Member partner;

	@Inject
	FightGroup(GameBossBar gameBossBar, NPCManager npcManager, DebugLog debugLog)
	{
		this.gameBossBar = gameBossBar;
		this.npcManager = npcManager;
		this.debugLog = debugLog;
	}

	int id()
	{
		return id;
	}

	void startFight(Actor actor, boolean bossLike, int tick)
	{
		clear();
		debugLog.add("Fight group: new fight");
		join(actor, bossLike, tick);
	}

	void join(Actor actor, boolean bossLike, int tick)
	{
		if (!(actor instanceof NPC))
		{
			return;
		}
		final NPC npc = (NPC) actor;
		final String name = name(npc);
		final boolean known = find(npc.getIndex()) != null;
		final boolean back = !known && awayByName(name) != null;
		final Member member = join(npc.getIndex(), name, bossLike, tick);
		member.npc = npc;
		if (back)
		{
			debugLog.add("Fight group: {} back", DebugLog.describe(npc));
		}
		else if (!known)
		{
			debugLog.add("Fight group: {} joined ({} members)", DebugLog.describe(npc), members.size());
		}
	}

	// An opponent back from out of sight carries on the fight it was part of.
	void restore(int fightId, Actor actor, boolean bossLike, int tick)
	{
		if (fightId != id)
		{
			clearMembers();
			id = fightId;
		}
		join(actor, bossLike, tick);
	}

	// A returning NPC gets a new index, so a single away member with its name takes it back.
	Member join(int index, String name, boolean bossLike, int tick)
	{
		Member member = find(index);
		if (member == null)
		{
			member = awayByName(name);
			if (member != null)
			{
				member.index = index;
			}
		}
		if (member == null)
		{
			if (members.size() >= MAX_MEMBERS)
			{
				remove(leastRecentlyEngaged());
			}
			member = new Member();
			member.index = index;
			member.name = name;
			members.add(member);
		}
		if (bossLike && !member.bossLike)
		{
			member.bossLike = true;
			bossLikeJoins++;
		}
		member.lastEngagedTick = tick;
		member.lastSeenTick = tick;
		return member;
	}

	boolean isAliveMember(Actor actor)
	{
		if (!(actor instanceof NPC))
		{
			return false;
		}
		final NPC npc = (NPC) actor;
		return isAliveMember(npc.getIndex(), name(npc), npc.isDead());
	}

	boolean isAliveMember(int index, String name, boolean npcDead)
	{
		if (npcDead)
		{
			return false;
		}
		Member member = find(index);
		if (member == null)
		{
			member = awayByName(name);
		}
		return member != null && !member.dead;
	}

	void onNpcDespawned(NPC npc)
	{
		final Member member = find(npc.getIndex());
		if (member == null)
		{
			return;
		}
		despawned(member, npc.isDead() || diedReading(member, NpcUtil.isAttackable(npc)));
		if (member.isAway())
		{
			debugLog.add("Fight group: {} away, kept with its last health", DebugLog.describe(npc));
		}
	}

	// One that dies is gone, one that leaves alive is kept with its last health.
	void despawned(Member member, boolean died)
	{
		if (died || member.dead)
		{
			remove(member);
		}
		else
		{
			member.index = NO_INDEX;
			member.npc = null;
		}
	}

	// Some turn into a form that can't be attacked as they die, before their bar reaches 0.
	static boolean diedReading(Member member, boolean attackable)
	{
		return member.scale > 0 && (member.ratio == 0 || !member.exact && member.ratio == 1 && !attackable);
	}

	void onGameTick(Actor opponent, int tick)
	{
		for (Member member : members)
		{
			final NPC npc = member.npc;
			if (npc == null)
			{
				continue;
			}
			member.lastSeenTick = tick;
			rename(member, npc);
			final boolean gameBar = gameBossBar.isTracking(npc) && gameBossBar.hasHealth();
			read(member, gameBar ? gameBossBar.health() : 0, gameBar ? gameBossBar.maxHealth() : 0,
				npc.getHealthRatio(), npc.getHealthScale(), npcManager.getHealth(NpcUtil.currentId(npc)));
			updateDead(member, npc.isDead());
		}
		expireAway(tick);
		final Member own = opponent instanceof NPC ? find(((NPC) opponent).getIndex()) : null;
		if (own != null)
		{
			own.lastEngagedTick = tick;
		}
		updatePartner(own, opponent != null);
	}

	// Away members are dropped once BossMemory would have forgotten them too.
	void expireAway(int tick)
	{
		for (int i = members.size() - 1; i >= 0; i--)
		{
			final Member member = members.get(i);
			if (member.isAway() && BossMemory.isExpired(member.lastSeenTick, tick))
			{
				remove(member);
			}
		}
	}

	// With no opponent for a moment, such as between two NPCs, the last partner stays.
	void updatePartner(Member own, boolean hasOpponent)
	{
		final Member next = hasOpponent ? choosePartner(own)
			: partner != null && members.contains(partner) && !partner.dead ? partner : null;
		if (next != partner)
		{
			partner = next;
			if (next != null)
			{
				debugLog.add("Second bar shows {}", next.npc != null ? DebugLog.describe(next.npc) : next.name + " (away)");
			}
		}
	}

	// The boss fought most recently besides the opponent, while it's alive and has health to show, even while away.
	Member choosePartner(Member own)
	{
		if (own == null || !own.bossLike)
		{
			return null;
		}
		Member best = null;
		for (Member member : members)
		{
			if (member != own && member.bossLike && !member.dead && member.scale > 0
				&& (member.npc == null || !member.npc.isDead())
				&& (best == null || member.lastEngagedTick > best.lastEngagedTick))
			{
				best = member;
			}
		}
		return best;
	}

	Member partner()
	{
		return withHealth(partner);
	}

	// Two bosses fought in the same fight. Kept until the fight ends, so the bar doesn't move in the middle of it.
	boolean isGroupFight()
	{
		return bossLikeJoins >= 2;
	}

	void clear()
	{
		clearMembers();
		id = ++nextId;
	}

	private void clearMembers()
	{
		members.clear();
		partner = null;
		bossLikeJoins = 0;
	}

	int size()
	{
		return members.size();
	}

	Member find(int index)
	{
		if (index == NO_INDEX)
		{
			return null;
		}
		for (Member member : members)
		{
			if (member.index == index)
			{
				return member;
			}
		}
		return null;
	}

	// Only a boss is taken back by name, and only when the name leaves no doubt which one it is.
	Member awayByName(String name)
	{
		if (name == null)
		{
			return null;
		}
		Member found = null;
		for (Member member : members)
		{
			if (member.isAway() && member.bossLike && !member.dead && name.equals(member.name))
			{
				if (found != null)
				{
					return null;
				}
				found = member;
			}
		}
		return found;
	}

	private Member leastRecentlyEngaged()
	{
		Member oldest = null;
		for (Member member : members)
		{
			if (oldest == null || member.lastEngagedTick < oldest.lastEngagedTick)
			{
				oldest = member;
			}
		}
		return oldest;
	}

	private void remove(Member member)
	{
		members.remove(member);
		if (member == partner)
		{
			partner = null;
		}
	}

	private static String name(NPC npc)
	{
		return npc.getName() != null ? Text.removeTags(npc.getName()) : null;
	}

	private static void rename(Member member, NPC npc)
	{
		final String name = name(npc);
		if (name != null && !name.equals(member.name))
		{
			member.name = name;
			member.state = null;
		}
	}

	private static Member withHealth(Member member)
	{
		if (member == null || member.scale <= 0)
		{
			return null;
		}
		if (member.state == null)
		{
			member.state = new BarState(member.name, 0, member.maxHealth, member.ratio, member.scale, member.exact, false,
				BarState.NO_PHASE_MARKERS, HealthIndicatorMarkers.NONE, null, null, Collections.emptyList(), null);
		}
		return member;
	}

	static void updateDead(Member member, boolean npcDead)
	{
		if (npcDead || (member.scale > 0 && member.ratio == 0))
		{
			member.dead = true;
		}
		else if (member.scale > 0 && member.ratio > 0)
		{
			member.dead = false;
		}
	}

	// The game's boss bar has exact health, then a fresh overhead bar. Without either, the last reading is kept,
	// since the overhead bar of an NPC that isn't being hit soon goes away.
	static boolean read(Member member, int barHealth, int barMaxHealth, int ratio, int scale, Integer maxHealth)
	{
		if (barMaxHealth > 0)
		{
			return set(member, barHealth, barMaxHealth, true, barMaxHealth);
		}
		if (scale > 0 && ratio >= 0)
		{
			return set(member, ratio, scale, false, maxHealth);
		}
		return false;
	}

	private static boolean set(Member member, int ratio, int scale, boolean exact, Integer maxHealth)
	{
		if (member.ratio == ratio && member.scale == scale && member.exact == exact
			&& Objects.equals(member.maxHealth, maxHealth))
		{
			return false;
		}
		member.ratio = ratio;
		member.scale = scale;
		member.exact = exact;
		member.maxHealth = maxHealth;
		member.state = null;
		return true;
	}
}
