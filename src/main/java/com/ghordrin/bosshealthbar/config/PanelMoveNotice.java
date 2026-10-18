package com.ghordrin.bosshealthbar.config;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;

// Says once where the settings that used to be on the settings screen went.
@Singleton
public class PanelMoveNotice
{
	// Set once the notice has been shown. Not a config item, so neither Reset nor an import brings it back.
	static final String TOLD_KEY = "panelMoveTold";

	private static final String MESSAGE = "Boss Health Bar: the look, text, boss info, animations and layout"
		+ " settings are now in the plugin's own panel. Open it from the sidebar icon, or right-click the bar"
		+ " and pick \"Open panel\".";

	private final ConfigManager configManager;
	private final ChatMessageManager chatMessageManager;

	private boolean told;

	@Inject
	PanelMoveNotice(ConfigManager configManager, ChatMessageManager chatMessageManager)
	{
		this.configManager = configManager;
		this.chatMessageManager = chatMessageManager;
	}

	// A chat message before login goes nowhere, so this waits for one, and every login after the first is
	// one the notice has already been shown on.
	public void onLoggedIn()
	{
		if (!shouldTell(configManager.getConfiguration(BossHealthBarConfig.GROUP, TOLD_KEY), told))
		{
			return;
		}

		told = true;
		ConfigWrites.write(configManager, TOLD_KEY, true);
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(MESSAGE)
			.build());
	}

	static boolean shouldTell(String saved, boolean toldThisSession)
	{
		return !toldThisSession && saved == null;
	}
}
