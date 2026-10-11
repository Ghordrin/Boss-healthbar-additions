package com.ghordrin.bosshealthbar;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

// A game widget hidden while this plugin draws its own version, and shown again when it stops.
class HiddenWidget
{
	private final Client client;
	private final DebugLog debugLog;
	private final int widgetId;

	private boolean hidden;

	HiddenWidget(Client client, DebugLog debugLog, int widgetId)
	{
		this.client = client;
		this.debugLog = debugLog;
		this.widgetId = widgetId;
	}

	Widget widget()
	{
		return client.getWidget(widgetId);
	}

	void hide(Widget widget, String message, Object... args)
	{
		if (!widget.isSelfHidden())
		{
			widget.setHidden(true);
			if (!hidden)
			{
				debugLog.add(message, args);
			}
			hidden = true;
		}
	}

	void release(String message)
	{
		release(message, true);
	}

	void release(String message, boolean unhide)
	{
		if (hidden)
		{
			debugLog.add(message);
			final Widget widget = widget();
			if (widget != null && unhide)
			{
				widget.setHidden(false);
			}
		}
		hidden = false;
	}

	// For when the widget is gone, so there is nothing left to show again.
	void forget()
	{
		hidden = false;
	}

	boolean isHidden()
	{
		return hidden;
	}
}
