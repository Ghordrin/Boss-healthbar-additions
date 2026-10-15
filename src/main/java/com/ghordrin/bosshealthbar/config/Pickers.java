package com.ghordrin.bosshealthbar.config;

import com.ghordrin.bosshealthbar.theme.HealthBarTheme;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.chatbox.ChatboxItemSearch;

// Opens the custom icon search and the fill texture window.
@Singleton
public class Pickers
{
	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final BossHealthBarConfig config;
	private final ChatboxItemSearch itemSearch;

	// Only touched on the EDT.
	private FillTexturePickerDialog texturePickerDialog;

	// The theme the switch was declined for, so the prompt asks once per theme. Only touched on the EDT.
	private String declinedFor;

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
	public void openIconPicker()
	{
		itemSearch
			.tooltipText("Custom icon")
			.onItemSelected(itemId -> configManager.setConfiguration(
				BossHealthBarConfig.GROUP, BossHealthBarConfig.CUSTOM_ICON_ITEM_ID_KEY, itemId))
			.build();
	}

	// The item search lives in the chatbox, so it only opens while logged in.
	public void openIconPickerIfLoggedIn()
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				openIconPicker();
			}
		});
	}

	public void openFillTexturePicker()
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

	// A picked color only shows on the Custom theme, so offer the switch rather than change nothing. The theme
	// it asked about is kept so a decline doesn't ask again for every other color of the same theme.
	public void confirmCustomTheme()
	{
		if (config.oldschoolTheme() || config.theme() == HealthBarTheme.CUSTOM
			|| config.theme().name().equals(declinedFor))
		{
			return;
		}

		final String asked = config.theme().name();
		SwingUtilities.invokeLater(() ->
		{
			final int choice = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(client.getCanvas()),
				"The Custom colors are only used by the Custom theme, which starts from the colors of the theme"
					+ " you have now. Switch the theme to Custom so your color shows?",
				"Switch to the Custom theme?", JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.YES_OPTION)
			{
				configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.THEME_KEY,
					HealthBarTheme.CUSTOM);
			}
			else
			{
				declinedFor = asked;
			}
		});
	}

	public void close()
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
