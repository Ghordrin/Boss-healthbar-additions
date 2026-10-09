package com.ghordrin.bosshealthbar;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.game.NPCManager;
import net.runelite.client.util.Text;

// Bosses fought as two NPCs that take turns. Both count as one fight, and each keeps its last known state.
@Slf4j
@Singleton
class PairBosses
{
	static final String GUARDIANS = "grotesque guardians";
	// The members of a pair can be out of sight for a while, so they're only forgotten after this long.
	static final int CLEAR_TICKS = 10;

	static final class Member
	{
		final String pair;
		final int side;

		Member(String pair, int side)
		{
			this.pair = pair;
			this.side = side;
		}
	}

	static final class Slot
	{
		final Member member;
		NPC npc;
		boolean dead;
		int lastPresentTick;
		String name;
		int ratio = -1;
		int scale = -1;
		boolean exact;
		Integer maxHealth;
		BarState state;

		Slot(Member member)
		{
			this.member = member;
		}
	}

	private static final Map<Integer, Member> MEMBERS;

	static
	{
		final Map<Integer, Member> members = new HashMap<>();
		final Member dawn = new Member(GUARDIANS, 0);
		final Member dusk = new Member(GUARDIANS, 1);
		for (int id : new int[]{NpcID.GARGBOSS_DAWN_SPAWN, NpcID.GARGBOSS_DAWN_PHASE1,
			NpcID.GARGBOSS_DAWN_PHASE1_TRANSITION, NpcID.GARGBOSS_DAWN_PHASE3, NpcID.GARGBOSS_DAWN_DEATH})
		{
			members.put(id, dawn);
		}
		for (int id : new int[]{NpcID.GARGBOSS_DUSK_SPAWN, NpcID.GARGBOSS_DUSK_PHASE1_DEFENSIVE,
			NpcID.GARGBOSS_DUSK_PHASE1_TRANSITION, NpcID.GARGBOSS_DUSK_PHASE1_FLYTRANSITION,
			NpcID.GARGBOSS_DUSK_PHASE2_ATTACKING, NpcID.GARGBOSS_DUSK_PHASE3_DEFENSIVE,
			NpcID.GARGBOSS_DUSK_PHASE3_TRANSITION, NpcID.GARGBOSS_DUSK_PHASE4_SPAWN, NpcID.GARGBOSS_DUSK_PHASE4,
			NpcID.GARGBOSS_DUSK_DEATH})
		{
			members.put(id, dusk);
		}
		MEMBERS = Collections.unmodifiableMap(members);
	}

	// The forms the members spawn in when a fight starts, at full health.
	private static final Set<Integer> START_FORMS = ImmutableSet.of(NpcID.GARGBOSS_DAWN_SPAWN, NpcID.GARGBOSS_DUSK_SPAWN);
	// The forms the members change into as they die.
	private static final Set<Integer> DEATH_FORMS = ImmutableSet.of(NpcID.GARGBOSS_DAWN_DEATH, NpcID.GARGBOSS_DUSK_DEATH);

	private final Client client;
	private final GameBossBar gameBossBar;
	private final NPCManager npcManager;
	private final List<Slot> slots = new ArrayList<>();
	private final Set<String> ended = new HashSet<>();

	@Inject
	PairBosses(Client client, GameBossBar gameBossBar, NPCManager npcManager)
	{
		this.client = client;
		this.gameBossBar = gameBossBar;
		this.npcManager = npcManager;
	}

	static boolean isStartForm(int npcId)
	{
		return START_FORMS.contains(npcId);
	}

	static boolean isDeathForm(int npcId)
	{
		return DEATH_FORMS.contains(npcId);
	}

	static boolean isDying(NPC npc)
	{
		return npc.isDead() || isDeathForm(NpcUtil.currentId(npc));
	}

	static Member member(int npcId)
	{
		return MEMBERS.get(npcId);
	}

	static Member member(Actor actor)
	{
		return actor instanceof NPC ? member(NpcUtil.currentId((NPC) actor)) : null;
	}

	static boolean isPairMember(Actor actor)
	{
		return member(actor) != null;
	}

	static boolean isPairKey(String name)
	{
		return GUARDIANS.equals(name);
	}

	static Map<Integer, Member> members()
	{
		return MEMBERS;
	}

	// The name the fight timer goes by: the pair's, so swapping between its members carries on the fight.
	static String fightName(Actor actor)
	{
		final Member member = member(actor);
		return member != null ? member.pair : actor.getName();
	}

	void onNpcSpawned(NPC npc)
	{
		final Member member = member(npc);
		if (member == null)
		{
			return;
		}
		log.debug("Pair member {} spawned (id {})", npc.getName(), NpcUtil.currentId(npc));
		final Slot slot = slot(slots, member, true);
		final int id = NpcUtil.currentId(npc);
		slot.npc = npc;
		slot.dead = isDeathForm(id);
		slot.lastPresentTick = client.getTickCount();
		rename(slot, npc);
		if (isStartForm(id))
		{
			log.debug("Pair member {} spawned in its start form (id {}), seeding it at full health", npc.getName(), id);
			seedFull(slot, npcManager.getHealth(id));
		}
	}

	void onNpcChanged(NPC npc)
	{
		final Member member = member(npc);
		final Slot previous = slotOf(npc);
		if (previous != null && previous.member != member)
		{
			previous.npc = null;
		}
		if (member != null)
		{
			final Slot slot = slot(slots, member, true);
			slot.npc = npc;
			slot.lastPresentTick = client.getTickCount();
			if (isDeathForm(NpcUtil.currentId(npc)))
			{
				slot.dead = true;
			}
			rename(slot, npc);
		}
	}

	void onNpcDespawned(NPC npc)
	{
		final Slot slot = slotOf(npc);
		if (slot != null)
		{
			despawned(slots, slot, isDying(npc));
			if (!hasPair(slot.member.pair))
			{
				// Both members died, so the next ones that spawn start a new fight.
				ended.add(slot.member.pair);
			}
		}
	}

	// Returns the pairs that were forgotten this tick.
	Set<String> onGameTick(Actor opponent)
	{
		final int tick = client.getTickCount();
		adopt(opponent, tick);
		adopt(gameBossBar.findNpc(opponent), tick);
		for (Slot slot : slots)
		{
			final NPC npc = slot.npc;
			if (npc != null)
			{
				slot.lastPresentTick = tick;
				final boolean gameBar = gameBossBar.isTracking(npc);
				read(slot, gameBar ? gameBossBar.health() : 0, gameBar ? gameBossBar.maxHealth() : 0,
					npc.getHealthRatio(), npc.getHealthScale(), npcManager.getHealth(NpcUtil.currentId(npc)));
				updateDead(slot, isDying(npc));
			}
		}
		final Set<String> cleared = clearGone(slots, tick);
		if (ended.isEmpty())
		{
			return cleared;
		}
		final Set<String> over = new HashSet<>(cleared);
		over.addAll(ended);
		ended.clear();
		return over;
	}

	// A member that was already there when the plugin started never spawned for us. It's picked up once it's the
	// opponent or on the game's boss bar, and its partner with a single look through the scene.
	private void adopt(Actor actor, int tick)
	{
		final Member member = member(actor);
		if (member == null || slot(slots, member, false) != null)
		{
			return;
		}
		final boolean pairKnown = hasPair(slots, member.pair);
		adopt((NPC) actor, member, tick);
		if (pairKnown || client.getTopLevelWorldView() == null)
		{
			return;
		}
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			final Member other = member(npc);
			if (other != null && other.pair.equals(member.pair) && slot(slots, other, false) == null)
			{
				adopt(npc, other, tick);
			}
		}
	}

	// Health is unknown here, so the slot stays empty until a reading comes in.
	private void adopt(NPC npc, Member member, int tick)
	{
		log.debug("Pair member {} found without a spawn (id {})", npc.getName(), NpcUtil.currentId(npc));
		final Slot slot = slot(slots, member, true);
		slot.npc = npc;
		slot.dead = isDying(npc);
		slot.lastPresentTick = tick;
		rename(slot, npc);
	}

	static void updateDead(Slot slot, boolean dying)
	{
		if (dying || (slot.scale > 0 && slot.ratio == 0))
		{
			slot.dead = true;
		}
		else if (slot.scale > 0 && slot.ratio > 0)
		{
			slot.dead = false;
		}
	}

	// Held from the first member seen until both have died or been out of sight for CLEAR_TICKS.
	boolean hasPair(String pair)
	{
		return hasPair(slots, pair);
	}

	static boolean hasPair(List<Slot> slots, String pair)
	{
		for (Slot slot : slots)
		{
			if (slot.member.pair.equals(pair))
			{
				return true;
			}
		}
		return false;
	}

	// The other member of the actor's pair, if it has health to show.
	Slot partnerOf(Actor actor)
	{
		final Member member = member(actor);
		final Slot slot = member != null ? partner(slots, member) : null;
		return withHealth(slot);
	}

	private static Slot withHealth(Slot slot)
	{
		if (slot == null || slot.scale <= 0)
		{
			return null;
		}
		if (slot.state == null)
		{
			slot.state = new BarState(slot.name, 0, slot.maxHealth, slot.ratio, slot.scale, slot.exact, false,
				BarState.NO_PHASE_MARKERS, HealthIndicatorMarkers.NONE, null, null, Collections.emptyList(), null);
		}
		return slot;
	}

	// Whether the other member of the actor's pair is still in the fight, even while it's out of sight.
	boolean otherMemberAlive(Actor actor)
	{
		final Member member = member(actor);
		if (member == null)
		{
			return false;
		}
		for (Slot slot : slots)
		{
			if (slot.member.pair.equals(member.pair) && slot.member.side != member.side && isAlive(slot))
			{
				return true;
			}
		}
		return false;
	}

	void reset()
	{
		slots.clear();
		ended.clear();
	}

	private Slot slotOf(NPC npc)
	{
		for (Slot slot : slots)
		{
			if (slot.npc == npc)
			{
				return slot;
			}
		}
		return null;
	}

	private static void rename(Slot slot, NPC npc)
	{
		final String name = npc.getName() != null ? Text.removeTags(npc.getName()) : null;
		if (name != null && !name.equals(slot.name))
		{
			slot.name = name;
			slot.state = null;
		}
	}

	// The game's boss bar has exact health, then a fresh overhead bar. Without either, the last reading is kept,
	// since the overhead bar of a member that isn't being hit soon goes away.
	static boolean read(Slot slot, int barHealth, int barMaxHealth, int ratio, int scale, Integer maxHealth)
	{
		if (barMaxHealth > 0)
		{
			return set(slot, barHealth, barMaxHealth, true, barMaxHealth);
		}
		if (scale > 0 && ratio >= 0)
		{
			return set(slot, ratio, scale, false, maxHealth);
		}
		return false;
	}

	static void seedFull(Slot slot, Integer maxHealth)
	{
		if (maxHealth != null && maxHealth > 0)
		{
			set(slot, maxHealth, maxHealth, true, maxHealth);
		}
		else
		{
			set(slot, 1, 1, false, null);
		}
	}

	private static boolean set(Slot slot, int ratio, int scale, boolean exact, Integer maxHealth)
	{
		if (slot.ratio == ratio && slot.scale == scale && slot.exact == exact && Objects.equals(slot.maxHealth, maxHealth))
		{
			return false;
		}
		slot.ratio = ratio;
		slot.scale = scale;
		slot.exact = exact;
		slot.maxHealth = maxHealth;
		slot.state = null;
		return true;
	}

	static Slot partner(List<Slot> slots, Member member)
	{
		for (Slot slot : slots)
		{
			if (slot.member.pair.equals(member.pair) && slot.member.side != member.side)
			{
				return slot;
			}
		}
		return null;
	}

	static boolean isAlive(Slot slot)
	{
		return !slot.dead && (slot.npc == null || !isDying(slot.npc));
	}

	static Slot slot(List<Slot> slots, Member member, boolean create)
	{
		for (Slot slot : slots)
		{
			if (slot.member == member)
			{
				return slot;
			}
		}
		if (!create)
		{
			return null;
		}
		final Slot slot = new Slot(member);
		slots.add(slot);
		return slot;
	}

	// A member that dies is gone for good, one that leaves alive is kept with its last state.
	static void despawned(List<Slot> slots, Slot slot, boolean npcDead)
	{
		if (npcDead || slot.dead)
		{
			slots.remove(slot);
		}
		else
		{
			slot.npc = null;
		}
	}

	static Set<String> clearGone(List<Slot> slots, int tick)
	{
		if (slots.isEmpty())
		{
			return Collections.emptySet();
		}
		final Map<String, Integer> lastPresent = new HashMap<>();
		for (Slot slot : slots)
		{
			lastPresent.merge(slot.member.pair, slot.npc != null ? Integer.MAX_VALUE : slot.lastPresentTick, Math::max);
		}
		lastPresent.values().removeIf(lastTick -> tick - lastTick <= CLEAR_TICKS);
		if (lastPresent.isEmpty())
		{
			return Collections.emptySet();
		}
		slots.removeIf(slot -> lastPresent.containsKey(slot.member.pair));
		return lastPresent.keySet();
	}
}
