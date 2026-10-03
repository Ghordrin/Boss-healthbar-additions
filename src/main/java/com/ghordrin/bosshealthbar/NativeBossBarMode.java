package com.ghordrin.bosshealthbar;

public enum NativeBossBarMode
{
	REPLACE("Replace it"),
	BOTH("Show both"),
	HIDE_OURS("Hide this bar");

	private final String label;

	NativeBossBarMode(String label)
	{
		this.label = label;
	}

	boolean hidesGameBar()
	{
		return this == REPLACE;
	}

	boolean hidesOurBar()
	{
		return this == HIDE_OURS;
	}

	// The old "Replace game's boss health bar" checkbox: on replaced the game's bar, off hid ours.
	static NativeBossBarMode fromReplaceSetting(String saved)
	{
		if (saved == null)
		{
			return null;
		}
		return Boolean.parseBoolean(saved) ? REPLACE : HIDE_OURS;
	}

	// RuneLite saves the default before the plugin starts, so a saved REPLACE may not be the user's choice.
	static NativeBossBarMode migrate(String oldSaved, String currentSaved)
	{
		final NativeBossBarMode old = fromReplaceSetting(oldSaved);
		if (old == null || (currentSaved != null && !REPLACE.name().equals(currentSaved)))
		{
			return null;
		}
		return old;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
