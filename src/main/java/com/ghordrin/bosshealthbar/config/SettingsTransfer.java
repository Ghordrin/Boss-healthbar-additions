package com.ghordrin.bosshealthbar.config;

import com.ghordrin.bosshealthbar.BossHealthBarPlugin;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Filepath;

// Saves every setting to a file you can keep or pass on, and puts one back. The file is plain JSON, so a
// setup can also be shared as text through the clipboard.
@Slf4j
@Singleton
public class SettingsTransfer
{
	private static final String FILE_PREFIX = "boss-health-bar-settings-";
	private static final String FILE_SUFFIX = ".json";
	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private static final DateTimeFormatter SAVED_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String KEY_PREFIX = BossHealthBarConfig.GROUP + ".";

	// Buttons that do something the moment they are ticked, and bookkeeping that says which one-off changes
	// have already been made to this installation. Neither belongs to a saved look, so neither travels: a
	// button would fire on import, and another installation's bookkeeping would skip or redo a change here.
	private static final Set<String> NOT_SETTINGS = ImmutableSet.of(
		BossHealthBarConfig.SAVE_DEBUG_LOG_KEY,
		BossHealthBarConfig.CHOOSE_FILL_TEXTURE_KEY,
		BossHealthBarConfig.CHOOSE_CUSTOM_ICON_KEY,
		BossHealthBarConfig.EXPORT_SETTINGS_KEY,
		BossHealthBarConfig.IMPORT_SETTINGS_KEY,
		BossHealthBarConfig.SHOW_PREVIEW_KEY,
		ConfigMigrations.FONT_DEFAULT_MIGRATED_KEY,
		ConfigMigrations.PIXEL_FONT_SIZE_MIGRATED_KEY,
		ConfigMigrations.ITEM_FONTS_MIGRATED_KEY,
		ConfigMigrations.FIGHT_TIMER_FONT_MIGRATED_KEY);

	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final BossHealthBarConfig config;
	private final ChatMessageManager chatMessageManager;
	private final ScheduledExecutorService executor;
	private final BossHealthBarPlugin plugin;
	private final Gson gson;

	// While this is on, the settings are being written in bulk. The plugin leaves them alone until it is
	// over: the theme would copy itself over the custom colors, every color would ask about the theme, and
	// the Oldschool toggle would write its companions, all on top of settings that were just chosen.
	@Getter
	private volatile boolean importing;

	@Inject
	SettingsTransfer(Client client, ClientThread clientThread, ConfigManager configManager,
		BossHealthBarConfig config, ChatMessageManager chatMessageManager, ScheduledExecutorService executor,
		BossHealthBarPlugin plugin, Gson gson)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.config = config;
		this.chatMessageManager = chatMessageManager;
		this.executor = executor;
		this.plugin = plugin;
		this.gson = gson;
	}

	public void export()
	{
		executor.execute(() ->
		{
			final LocalDateTime now = LocalDateTime.now();
			final String json = gson.toJson(new Saved(BossHealthBarPlugin.NAME, BossHealthBarPlugin.VERSION,
				SAVED_TIME.format(now), settings()));
			final String fileName = FILE_PREFIX + FILE_TIME.format(now) + FILE_SUFFIX;
			try
			{
				final Filepath directory = plugin.dataDirectory();
				directory.createDirectories();
				directory.joinSegment(fileName).write(json.getBytes(StandardCharsets.UTF_8));
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("Couldn't save the settings", e);
				chat("Couldn't save your settings to a file.");
				return;
			}
			copyToClipboard(json);
			chat("Settings saved to .runelite/plugin-data/" + BossHealthBarPlugin.INTERNAL_NAME + "/" + fileName
				+ " and copied to your clipboard.");
		});
	}

	private Map<String, String> settings()
	{
		final Map<String, String> settings = new TreeMap<>();
		for (String key : keys())
		{
			final String value = configManager.getConfiguration(BossHealthBarConfig.GROUP, key);
			if (value != null)
			{
				settings.put(key, value);
			}
		}
		return settings;
	}

	// The keys this installation has a saved value for, without the group in front and without the ones that
	// aren't part of a look. A setting left at its default has no saved value and so isn't one of them.
	private List<String> keys()
	{
		final List<String> keys = new ArrayList<>();
		for (String full : configManager.getConfigurationKeys(KEY_PREFIX))
		{
			final String key = full.substring(KEY_PREFIX.length());
			if (!key.isEmpty() && !NOT_SETTINGS.contains(key))
			{
				keys.add(key);
			}
		}
		return keys;
	}

	private void copyToClipboard(String json)
	{
		SwingUtilities.invokeLater(() ->
		{
			try
			{
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
			}
			catch (RuntimeException e)
			{
				log.warn("Couldn't copy the settings to the clipboard", e);
			}
		});
	}

	public void startImport()
	{
		SwingUtilities.invokeLater(() ->
		{
			final Filepath.Chooser chooser = new Filepath.Chooser()
				.setIsOpen()
				.setAcceptsFiles()
				.setDialogTitle("Import settings")
				.addExtensionFilter("Settings file (*.json)", "json");
			startIn(chooser);
			final List<Filepath> chosen = chooser.showDialog(parent());
			if (chosen == null || chosen.isEmpty())
			{
				return;
			}

			final Filepath file = chosen.get(0);
			executor.execute(() -> read(file));
		});
	}

	// Settings shared as text rather than as a file, the way a setup is passed on.
	public void importFromClipboard()
	{
		SwingUtilities.invokeLater(() ->
		{
			final String text;
			try
			{
				text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			}
			catch (UnsupportedFlavorException | IOException | RuntimeException e)
			{
				log.debug("Couldn't read the clipboard", e);
				error("There is no text on your clipboard to read settings from.");
				return;
			}
			executor.execute(() -> use(text, "Your clipboard holds no settings for this plugin."));
		});
	}

	private void startIn(Filepath.Chooser chooser)
	{
		try
		{
			final Filepath directory = plugin.dataDirectory();
			if (directory.isDirectory())
			{
				chooser.setCurrentDirectory(directory);
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Couldn't start the file chooser in the plugin's folder", e);
		}
	}

	private void read(Filepath file)
	{
		final String text;
		try (Reader reader = file.openReader())
		{
			text = CharStreams.toString(reader);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Couldn't read the settings file", e);
			error("That file couldn't be read.");
			return;
		}
		use(text, "That file holds no settings for this plugin.");
	}

	private void use(String text, String emptyMessage)
	{
		final Saved saved;
		try
		{
			saved = gson.fromJson(text, Saved.class);
		}
		catch (RuntimeException e)
		{
			log.debug("Couldn't read the settings", e);
			error(emptyMessage);
			return;
		}

		if (saved == null || saved.settings == null || saved.settings.isEmpty())
		{
			error(emptyMessage);
			return;
		}

		final Map<String, String> wanted = keep(saved.settings);
		if (wanted.isEmpty())
		{
			error(emptyMessage);
			return;
		}

		final int dropped = saved.settings.size() - wanted.size();
		confirm(wanted, dropped);
	}

	// A file is someone else's text, so only the settings this plugin actually has are taken from it, and
	// only where the value is one that setting can hold. Anything else would either mean nothing or would
	// break the setting it landed in the first time the bar tried to read it.
	private Map<String, String> keep(Map<String, String> settings)
	{
		final Map<String, Class<?>> declared = declared();
		final Map<String, String> wanted = new TreeMap<>();
		for (Map.Entry<String, String> setting : settings.entrySet())
		{
			final String key = setting.getKey();
			final Class<?> type = declared.get(key);
			if (type == null || NOT_SETTINGS.contains(key) || setting.getValue() == null)
			{
				log.debug("Ignoring {}, which isn't one of this plugin's settings", key);
				continue;
			}
			if (!holds(type, setting.getValue()))
			{
				log.debug("Ignoring {}, which can't hold \"{}\"", key, setting.getValue());
				continue;
			}
			wanted.put(key, setting.getValue());
		}
		return wanted;
	}

	// Every setting this plugin declares, and what each one holds. Taken from the config itself, so a setting
	// added later is known here without being listed twice.
	private Map<String, Class<?>> declared()
	{
		final Map<String, Class<?>> declared = new HashMap<>();
		for (ConfigItemDescriptor item : configManager.getConfigDescriptor(config).getItems())
		{
			// What a setting holds is the class behind it. A setting whose type isn't a plain class, such as
			// a list of something, has no one value a saved line could be read as, so it is left out here
			// and never taken from a file.
			if (item.getType() instanceof Class)
			{
				declared.put(item.key(), (Class<?>) item.getType());
			}
		}
		return declared;
	}

	// The saved keys to clear outright: the ones the file doesn't name and this version no longer declares,
	// such as a setting a later version dropped. Everything still declared is covered by the defaults.
	static List<String> keysToUnset(Collection<String> saved, Set<String> declared, Set<String> named)
	{
		final List<String> unset = new ArrayList<>();
		for (String key : saved)
		{
			if (!named.contains(key) && !declared.contains(key))
			{
				unset.add(key);
			}
		}
		return unset;
	}

	// The same readings the client makes of a saved value, asked beforehand rather than when the bar is drawn.
	private boolean holds(Class<?> type, String value)
	{
		try
		{
			if (type == boolean.class || type == Boolean.class)
			{
				return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
			}
			if (type == int.class || type == Integer.class)
			{
				Integer.parseInt(value);
				return true;
			}
			if (type == double.class || type == Double.class)
			{
				Double.parseDouble(value);
				return true;
			}
			if (type == Color.class)
			{
				return ColorUtil.fromString(value) != null;
			}
			if (type.isEnum())
			{
				for (Object constant : type.getEnumConstants())
				{
					if (((Enum<?>) constant).name().equals(value))
					{
						return true;
					}
				}
				return false;
			}
			// A plain string setting, such as the lists of bosses to always or never show.
			return type == String.class;
		}
		catch (RuntimeException e)
		{
			return false;
		}
	}

	// Loading puts back everything the file names and returns the rest to its default, so it is worth saying
	// so first. The count is what the file turned out to be worth, not how long it was.
	private void confirm(Map<String, String> settings, int dropped)
	{
		SwingUtilities.invokeLater(() ->
		{
			final String ignored = dropped == 0 ? ""
				: "\n" + dropped + (dropped == 1 ? " entry was" : " entries were") + " ignored.";
			final int answer = JOptionPane.showConfirmDialog(parent(),
				"Load " + settings.size() + (settings.size() == 1 ? " setting" : " settings")
					+ "? Everything the file doesn't name goes back to its default." + ignored,
				"Import settings", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (answer == JOptionPane.YES_OPTION)
			{
				apply(settings);
			}
		});
	}

	// Written on the client thread so the writes and everything they set off happen while importing is on,
	// and the plugin sees one finished set of settings rather than a half-applied one.
	private void apply(Map<String, String> settings)
	{
		clientThread.invoke(() ->
		{
			int written = 0;
			importing = true;
			try
			{
				// A setting the file doesn't name goes back to its default, so what is loaded is what was
				// saved, and not what was saved on top of whatever was here before. The defaults are
				// written rather than the keys unset: a setting with nothing saved for it reads as null,
				// and the client's own editor for a font type doesn't expect that, so the font settings
				// couldn't be opened at all after an import that left them out.
				configManager.setDefaultConfiguration(config, true);
				for (String key : keysToUnset(keys(), declared().keySet(), settings.keySet()))
				{
					configManager.unsetConfiguration(BossHealthBarConfig.GROUP, key);
				}
				for (Map.Entry<String, String> setting : settings.entrySet())
				{
					configManager.setConfiguration(BossHealthBarConfig.GROUP, setting.getKey(), setting.getValue());
					written++;
				}
			}
			finally
			{
				importing = false;
			}
			chat(written + (written == 1 ? " setting" : " settings") + " loaded.");
		});
	}

	private void error(String message)
	{
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent(), message, "Import settings",
			JOptionPane.WARNING_MESSAGE));
	}

	private Window parent()
	{
		return SwingUtilities.getWindowAncestor(client.getCanvas());
	}

	private void chat(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	// The file's shape. Everything but the settings is there to read, so a file can be told apart by eye.
	// The empty constructor is the one a file is read back into.
	private static class Saved
	{
		private String plugin;
		private String version;
		private String saved;
		private Map<String, String> settings;

		Saved()
		{
		}

		Saved(String plugin, String version, String saved, Map<String, String> settings)
		{
			this.plugin = plugin;
			this.version = version;
			this.saved = saved;
			this.settings = settings;
		}
	}
}
