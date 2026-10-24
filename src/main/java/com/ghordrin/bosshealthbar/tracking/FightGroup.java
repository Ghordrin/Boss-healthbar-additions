package com.ghordrin.bosshealthbar.tracking;

import com.ghordrin.bosshealthbar.data.BarState;
import com.ghordrin.bosshealthbar.data.Health;
import com.ghordrin.bosshealthbar.data.NpcUtil;
import com.ghordrin.bosshealthbar.debug.DebugLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.client.util.Text;

// The NPCs that have been the opponent during the current fight, each with its last known health, so another boss
// fought at the same time can be shown under the main bar.
@Singleton
public class FightGroup
{
	static final int MAX_MEMBERS = 8;
	static final int NO_INDEX = -1;

	public static final class Member
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
		// The last reading above 0, kept for a moment where the NPC has no reading or reads a 0 it can come back from.
		Health lastAboveZero;
		public BarState state;
		final HealthReader.DownState downState = new HealthReader.DownState();
		final HealthReader.Watch watch = new HealthReader.Watch();

		boolean isAway()
		{
			return index == NO_INDEX;
		}

		// For what the bar has to ask the game about this boss, such as the color its own bar paints it in.
		public NPC npc()
		{
			return npc;
		}

		// A confirmed kill, not just a 0 it can still come back from.
		public boolean isDead()
		{
			return downState.defeated;
		}
	}

	private final HealthReader healthReader;
	private final DebugLog debugLog;
	private final List<Member> members = new ArrayList<>(MAX_MEMBERS);
	private int nextId;
	private int id;
	private int bossLikeJoins;
	private Member partner;

	@Inject
	FightGroup(HealthReader healthReader, DebugLog debugLog)
	{
		this.healthReader = healthReader;
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
				member.watch.clear();
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

	public boolean isAliveMember(Actor actor)
	{
		if (!(actor instanceof NPC))
		{
			return false;
		}
		final NPC npc = (NPC) actor;
		return isAliveMember(npc.getIndex(), name(npc));
	}

	// A member at 0 still counts until its kill is confirmed.
	public boolean isAliveMember(int index, String name)
	{
		Member member = find(index);
		if (member == null)
		{
			member = awayByName(name);
		}
		return member != null && !member.isDead();
	}

	public void onNpcDespawned(NPC npc)
	{
		final Member member = find(npc.getIndex());
		if (member == null)
		{
			return;
		}
		despawned(member, member.downState.down || npc.isDead() || diedReading(member, NpcUtil.isAttackable(npc)));
		if (member.isAway())
		{
			debugLog.add("Fight group: {} away, kept with its last health", DebugLog.describe(npc));
		}
	}

	// One that dies is gone, one that leaves alive is kept with its last health.
	void despawned(Member member, boolean died)
	{
		if (died || member.isDead())
		{
			remove(member);
		}
		else
		{
			member.index = NO_INDEX;
			member.npc = null;
		}
	}

	// A member back up after its kill was confirmed is the next one of its kind, so it starts without the dead one's
	// health or state.
	static boolean startNewLife(Member member)
	{
		if (!member.downState.takeRevived())
		{
			return false;
		}
		member.downState.clear();
		member.ratio = -1;
		member.scale = -1;
		member.exact = false;
		member.maxHealth = null;
		member.lastAboveZero = null;
		member.state = null;
		return true;
	}

	// Some turn into a form that can't be attacked as they die, before their bar reaches 0.
	static boolean diedReading(Member member, boolean attackable)
	{
		return member.scale > 0 && (member.ratio == 0 || !member.exact && member.ratio == 1 && !attackable);
	}

	public void onGameTick(Actor opponent, int tick)
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
			final HealthReader.Reading reading = healthReader.read(npc);
			final boolean npcDead = npc.isDead();
			final boolean attackable = NpcUtil.isAttackable(npc);
			member.watch.log(debugLog, npc, reading, npcDead, attackable);
			member.downState.update(reading, npcDead, true, attackable, tick, debugLog, "Fight group: ", npc);
			startNewLife(member);
			read(member, reading != null ? reading.health : null);
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
			: partner != null && members.contains(partner) && !partner.isDead() ? partner : null;
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
			if (member != own && member.bossLike && !member.isDead() && member.scale > 0
				&& (best == null || member.lastEngagedTick > best.lastEngagedTick))
			{
				best = member;
			}
		}
		return best;
	}

	public Member partner()
	{
		return withHealth(partner);
	}

	// Two bosses fought in the same fight. Kept until the fight ends, so the bar doesn't move in the middle of it.
	public boolean isGroupFight()
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

	// The last health above 0 read for one NPC of this fight. Same NPC first, and since an NPC that left and came back
	// gets a new index, a single member with its name counts too.
	public Health rememberedHealth(int index, String name)
	{
		if (name == null)
		{
			return null;
		}
		Health byIndex = null;
		Member byName = null;
		int sameName = 0;
		for (Member member : members)
		{
			if (!name.equals(member.name))
			{
				continue;
			}
			if (member.index == index && index != NO_INDEX)
			{
				byIndex = member.lastAboveZero;
			}
			else
			{
				byName = member;
				sameName++;
			}
		}
		if (byIndex != null)
		{
			return byIndex;
		}
		return sameName == 1 ? byName.lastAboveZero : null;
	}

	// An NPC picked up again from memory brings its remembered health back, in case the fight it belonged to was
	// cleared while another one was fought.
	void restoreHealth(int index, Health health)
	{
		final Member member = find(index);
		if (member != null && member.lastAboveZero == null)
		{
			member.lastAboveZero = health;
		}
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
			if (member.isAway() && member.bossLike && !member.isDead() && name.equals(member.name))
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
			member.state = BarState.healthOnly(member.name,
				new Health(member.ratio, member.scale, member.exact, member.maxHealth, false));
		}
		return member;
	}

	// Without a reading the last one is kept, since the overhead bar of an NPC that isn't being hit soon goes away.
	static boolean read(Member member, Health health)
	{
		if (health == null || health.scale <= 0 || health.ratio < 0)
		{
			return false;
		}
		if (health.ratio > 0)
		{
			member.lastAboveZero = health;
		}
		if (member.ratio == health.ratio && member.scale == health.scale && member.exact == health.exactHealth
			&& Objects.equals(member.maxHealth, health.maxHealth))
		{
			return false;
		}
		member.ratio = health.ratio;
		member.scale = health.scale;
		member.exact = health.exactHealth;
		member.maxHealth = health.maxHealth;
		member.state = null;
		return true;
	}
}
