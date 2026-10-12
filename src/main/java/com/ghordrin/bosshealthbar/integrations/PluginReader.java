package com.ghordrin.bosshealthbar.integrations;

import com.ghordrin.bosshealthbar.debug.DebugLog;
import java.util.Objects;
import net.runelite.client.plugins.PluginManager;
import org.slf4j.Logger;

// Whether another plugin this one reads from is on. Checked once, then kept until invalidate.
class PluginReader
{
	private final PluginManager pluginManager;
	private final String name;
	private Boolean active;

	PluginReader(PluginManager pluginManager, String name)
	{
		this.pluginManager = pluginManager;
		this.name = name;
	}

	boolean isActive()
	{
		if (active == null)
		{
			active = PluginState.isActive(pluginManager, name);
		}
		return active;
	}

	void invalidate()
	{
		active = null;
	}

	// A broken info box fails every tick, so the debug log gets it once until a read works again.
	static class ReadFailure
	{
		private final DebugLog debugLog;
		private final String message;
		private boolean logged;

		ReadFailure(DebugLog debugLog, String message)
		{
			this.debugLog = debugLog;
			this.message = message;
		}

		void failed(Logger log, RuntimeException e)
		{
			log.debug(message, e);
			if (!logged)
			{
				logged = true;
				debugLog.add(message + ": {}", e.toString());
			}
		}

		void succeeded()
		{
			logged = false;
		}
	}

	// The other plugin's info boxes can be gone for a tick while they're rebuilt, so the last reading is kept
	// for one tick, as long as it's still for the same key.
	static class HeldReading<T>
	{
		private Object key;
		private T reading;
		private boolean missedTick;

		// A null reading means none was found this tick.
		void accept(Object key, T found)
		{
			if (found != null)
			{
				// An unchanged reading keeps the old instance, so callers can compare by identity.
				if (!found.equals(reading))
				{
					reading = found;
				}
				this.key = key;
				missedTick = false;
			}
			else if (reading != null && !missedTick && Objects.equals(key, this.key))
			{
				missedTick = true;
			}
			else
			{
				clear();
			}
		}

		T reading()
		{
			return reading;
		}

		T readingFor(Object key)
		{
			return key != null && key.equals(this.key) ? reading : null;
		}

		void clear()
		{
			key = null;
			reading = null;
			missedTick = false;
		}
	}
}
