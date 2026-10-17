package com.ghordrin.bosshealthbar.config;

import com.ghordrin.bosshealthbar.theme.HealthBarTheme;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;

// Opens the custom icon window and the fill texture window.
@Singleton
public class Pickers
{
	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final BossHealthBarConfig config;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;

	// Only touched on the EDT.
	private FillTexturePickerDialog texturePickerDialog;
	private CustomIconPickerDialog iconPickerDialog;

	// The theme the switch was declined for, so the prompt asks once per theme. Only touched on the EDT.
	private String declinedFor;

	@Inject
	Pickers(Client client, ClientThread clientThread, ConfigManager configManager, BossHealthBarConfig config,
		ItemManager itemManager, SpriteManager spriteManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.config = config;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
	}

	public void openIconPicker()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (iconPickerDialog == null || !iconPickerDialog.isDisplayable())
			{
				iconPickerDialog = new CustomIconPickerDialog(client, clientThread, configManager, config,
					itemManager, spriteManager);
			}
			iconPickerDialog.setVisible(true);
			iconPickerDialog.toFront();
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

	// Picking a theme writes its colors over the section again, so whatever was declined before is moot.
	public void themeChanged()
	{
		declinedFor = null;
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
			if (iconPickerDialog != null)
			{
				iconPickerDialog.dispose();
				iconPickerDialog = null;
			}
		});
	}
}
