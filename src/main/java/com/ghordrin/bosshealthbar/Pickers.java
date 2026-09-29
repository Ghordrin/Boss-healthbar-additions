package com.ghordrin.bosshealthbar;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.chatbox.ChatboxItemSearch;

// Opens the custom icon search and the fill texture window.
@Singleton
class Pickers
{
	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final BossHealthBarConfig config;
	private final ChatboxItemSearch itemSearch;

	// Only touched on the EDT.
	private FillTexturePickerDialog texturePickerDialog;

	@Inject
	Pickers(Client client, ClientThread clientThread, ConfigManager configManager, BossHealthBarConfig config,
		ChatboxItemSearch itemSearch)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.config = config;
		this.itemSearch = itemSearch;
	}

	// Must be called on the client thread.
	void openIconPicker()
	{
		itemSearch
			.tooltipText("Custom icon")
			.onItemSelected(itemId -> configManager.setConfiguration(
				BossHealthBarConfig.GROUP, BossHealthBarConfig.CUSTOM_ICON_ITEM_ID_KEY, itemId))
			.build();
	}

	// The item search lives in the chatbox, so it only opens while logged in.
	void openIconPickerIfLoggedIn()
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				openIconPicker();
			}
		});
	}

	void openFillTexturePicker()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (texturePickerDialog == null || !texturePickerDialog.isDisplayable())
			{
				texturePickerDialog = new FillTexturePickerDialog(client, clientThread, configManager, config);
			}
			texturePickerDialog.setVisible(true);
			texturePickerDialog.toFront();
		});
	}

	void close()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (texturePickerDialog != null)
			{
				texturePickerDialog.dispose();
				texturePickerDialog = null;
			}
		});
	}
}
