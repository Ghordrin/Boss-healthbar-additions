package com.ghordrin.bosshealthbar;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import org.slf4j.helpers.MessageFormatter;

// The plugin's recent activity, kept in memory so it can be saved for a bug report.
@Slf4j
@Singleton
class DebugLog
{
	static final int CAPACITY = 1000;
	static final int NO_TICK = -1;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

	static final class Entry
	{
		final long millis;
		final int tick;
		final String message;
		final int repeats;

		Entry(long millis, int tick, String message, int repeats)
		{
			this.millis = millis;
			this.tick = tick;
			this.message = message;
			this.repeats = repeats;
		}
	}

	private final Client client;
	private final Deque<Entry> entries = new ArrayDeque<>();

	@Inject
	DebugLog(Client client)
	{
		this.client = client;
	}

	void add(String format, Object... args)
	{
		final String message = MessageFormatter.arrayFormat(format, args).getMessage();
		log.debug("{}", message);
		final int tick = client != null && client.isClientThread() ? client.getTickCount() : NO_TICK;
		add(System.currentTimeMillis(), tick, message);
	}

	synchronized void add(long millis, int tick, String message)
	{
		final Entry last = entries.peekLast();
		if (last != null && last.message.equals(message))
		{
			entries.pollLast();
			entries.addLast(new Entry(millis, tick, message, last.repeats + 1));
			return;
		}
		if (entries.size() >= CAPACITY)
		{
			entries.pollFirst();
		}
		entries.addLast(new Entry(millis, tick, message, 1));
	}

	synchronized List<Entry> snapshot()
	{
		return new ArrayList<>(entries);
	}

	// Players are never named, so nothing about other people ends up in a saved log.
	static String describe(Actor actor)
	{
		if (actor == null)
		{
			return "none";
		}
		if (actor instanceof Player)
		{
			return "a player";
		}
		if (actor instanceof NPC)
		{
			final NPC npc = (NPC) actor;
			return describeNpc(npc.getName(), npc.getId(), NpcUtil.currentId(npc), npc.getIndex());
		}
		return "another actor";
	}

	static String describeNpc(String name, int id, int form, int index)
	{
		return name + " (id " + id + ", form " + form + ", index " + index + ")";
	}

	static String render(List<String> header, List<Entry> entries)
	{
		return render(header, entries, ZoneId.systemDefault());
	}

	static String render(List<String> header, List<Entry> entries, ZoneId zone)
	{
		final StringBuilder builder = new StringBuilder();
		for (String line : header)
		{
			builder.append(line).append('\n');
		}
		builder.append('\n').append("Recent events (oldest first)").append('\n');
		for (Entry entry : entries)
		{
			builder.append(TIME.format(Instant.ofEpochMilli(entry.millis).atZone(zone)))
				.append("  tick ").append(entry.tick == NO_TICK ? "-" : Integer.toString(entry.tick))
				.append("  ").append(entry.message);
			if (entry.repeats > 1)
			{
				builder.append(" (x ").append(entry.repeats).append(')');
			}
			builder.append('\n');
		}
		return builder.toString();
	}

	static String cut(String value, int max)
	{
		if (value == null)
		{
			return "null";
		}
		return value.length() <= max ? value : value.substring(0, max) + "...";
	}
}
