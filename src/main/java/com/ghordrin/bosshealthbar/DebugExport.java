package com.ghordrin.bosshealthbar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.util.Filepath;

// Writes the debug log, with the plugin's state and settings, to a text file for bug reports.
@Slf4j
@Singleton
class DebugExport
{
	static final int KEEP_FILES = 10;
	private static final String FILE_PREFIX = "boss-health-bar-debug-";
	private static final String FILE_SUFFIX = ".txt";
	private static final Pattern FILE_NAME = Pattern.compile("boss-health-bar-debug-\\d{8}-\\d{6}\\.txt");
	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private static final DateTimeFormatter HEADER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String CONFIG_PREFIX = BossHealthBarConfig.GROUP + ".";
	private static final int MAX_VALUE_LENGTH = 200;
	private static final String[] RELATED_PLUGINS = {
		"Boss Health Indicators",
		"Opponent Information",
		"Party Defence Tracker",
		"Better Party Defence",
		"Special Attack Counter",
		"Health Threshold Indicators",
	};

	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final PluginManager pluginManager;
	private final ChatMessageManager chatMessageManager;
	private final ScheduledExecutorService executor;
	private final DebugLog debugLog;
	private final BossHealthBarPlugin plugin;
	private final OpponentTracker opponentTracker;
	private final GameBossBar gameBossBar;
	private final TobBossBar tobBossBar;
	private final PillarBars pillarBars;
	private final FightTimer fightTimer;

	@Inject
	DebugExport(Client client, ClientThread clientThread, ConfigManager configManager, PluginManager pluginManager,
		ChatMessageManager chatMessageManager, ScheduledExecutorService executor, DebugLog debugLog,
		BossHealthBarPlugin plugin, OpponentTracker opponentTracker, GameBossBar gameBossBar, TobBossBar tobBossBar,
		PillarBars pillarBars, FightTimer fightTimer)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.pluginManager = pluginManager;
		this.chatMessageManager = chatMessageManager;
		this.executor = executor;
		this.debugLog = debugLog;
		this.plugin = plugin;
		this.opponentTracker = opponentTracker;
		this.gameBossBar = gameBossBar;
		this.tobBossBar = tobBossBar;
		this.pillarBars = pillarBars;
		this.fightTimer = fightTimer;
	}

	void save()
	{
		clientThread.invoke(() ->
		{
			final LocalDateTime now = LocalDateTime.now();
			final String text = DebugLog.render(header(now), debugLog.snapshot());
			executor.execute(() -> write(fileName(now), text));
		});
	}

	private void write(String fileName, String text)
	{
		try
		{
			final Filepath directory = plugin.dataDirectory();
			directory.createDirectories();
			final Filepath file = directory.joinSegment(fileName);
			file.write(text.getBytes(StandardCharsets.UTF_8));
			removeOldFiles(directory);
			chat("Debug log saved to .runelite/plugin-data/" + BossHealthBarPlugin.INTERNAL_NAME + "/" + fileName);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Couldn't save the debug log", e);
			chat("Couldn't save the debug log.");
		}
	}

	private void removeOldFiles(Filepath directory) throws IOException
	{
		final List<Filepath> files;
		try (Stream<Filepath> walk = directory.walk(1))
		{
			files = walk.filter(Filepath::isFile).collect(Collectors.toList());
		}
		final List<String> names = new ArrayList<>();
		for (Filepath file : files)
		{
			names.add(file.getFileName());
		}
		for (String name : namesToDelete(names, KEEP_FILES))
		{
			directory.joinSegment(name).deleteIfExists();
		}
	}

	private void chat(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	private List<String> header(LocalDateTime now)
	{
		final List<String> lines = new ArrayList<>();
		lines.add(BossHealthBarPlugin.NAME + " debug log");
		lines.add("Saved at " + HEADER_TIME.format(now) + " (local time)");
		lines.add("Plugin " + BossHealthBarPlugin.VERSION);
		lines.add("RuneLite " + RuneLiteProperties.getVersion());
		lines.add("Client revision " + client.getRevision());
		lines.add("Game state " + client.getGameState());
		lines.add("Game tick " + client.getTickCount());

		lines.add("");
		lines.add("Related plugins");
		for (String name : RELATED_PLUGINS)
		{
			lines.add("  " + name + ": " + pluginState(name));
		}

		lines.add("");
		lines.add("Current state");
		lines.add("  Opponent: " + DebugLog.describe(opponentTracker.getOpponent()));
		final int trackedId = gameBossBar.trackedNpcId();
		lines.add("  Game boss bar: " + (trackedId == -1 ? "none"
			: "NPC id " + trackedId + ", health " + gameBossBar.health() + "/" + gameBossBar.maxHealth())
			+ ", hidden " + gameBossBar.isHidden());
		lines.add("  Raid boss bar: " + DebugLog.describe(tobBossBar.cachedBoss()) + ", hidden " + tobBossBar.isHidden());
		lines.add("  Pillar bars: present " + pillarBars.isSeen() + ", hidden " + pillarBars.isHidingGameBars()
			+ ", shown " + pillarBars.isShown());
		final String fightTime = fightTimer.getText();
		lines.add("  Fight timer: " + (fightTime != null ? fightTime : "none"));

		lines.add("");
		lines.add("Settings");
		final List<String> keys = new ArrayList<>(configManager.getConfigurationKeys(CONFIG_PREFIX));
		Collections.sort(keys);
		for (String fullKey : keys)
		{
			final String key = fullKey.substring(CONFIG_PREFIX.length());
			lines.add("  " + key + " = "
				+ DebugLog.cut(configManager.getConfiguration(BossHealthBarConfig.GROUP, key), MAX_VALUE_LENGTH));
		}
		return lines;
	}

	private String pluginState(String name)
	{
		for (Plugin other : pluginManager.getPlugins())
		{
			if (name.equals(other.getName()))
			{
				return pluginManager.isPluginActive(other) ? "on" : "off";
			}
		}
		return "not installed";
	}

	static String fileName(LocalDateTime time)
	{
		return FILE_PREFIX + FILE_TIME.format(time) + FILE_SUFFIX;
	}

	// The time in the name sorts the files oldest first. Only files with our own name pattern are touched.
	static List<String> namesToDelete(List<String> names, int keep)
	{
		final List<String> ours = names.stream()
			.filter(name -> FILE_NAME.matcher(name).matches())
			.sorted()
			.collect(Collectors.toList());
		return ours.size() <= keep ? Collections.emptyList() : ours.subList(0, ours.size() - keep);
	}
}
