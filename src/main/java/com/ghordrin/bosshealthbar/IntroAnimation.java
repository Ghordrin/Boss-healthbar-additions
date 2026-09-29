package com.ghordrin.bosshealthbar;

public enum IntroAnimation
{
	FADE("Fade", false, false),
	SLIDE("Slide in", true, false),
	EXPAND("Expand", false, true),
	SLIDE_AND_EXPAND("Slide in and expand", true, true);

	private final String label;
	final boolean slide;
	final boolean expand;

	IntroAnimation(String label, boolean slide, boolean expand)
	{
		this.label = label;
		this.slide = slide;
		this.expand = expand;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
