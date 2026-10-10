package com.ghordrin.bosshealthbar;

import com.google.common.annotations.VisibleForTesting;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import net.runelite.client.util.ImageUtil;

// Tells Better Party Defence's Magic defence box apart by its image. With its theme icon option on it draws its
// own 16x16 copy of the Magic icon (or of a resource pack's override), so those copies are rebuilt the same way
// here and compared pixel by pixel.
class MagicIconMatcher
{
	private static final int THEMED_ICON_SIZE = 16;
	private static final int MAX_VERDICTS = 8;

	private BufferedImage classicSource;
	private BufferedImage classic;
	private BufferedImage overrideSource;
	private BufferedImage override;
	private final Map<BufferedImage, Boolean> verdicts = new IdentityHashMap<>();

	boolean isMagic(BufferedImage image, BufferedImage skillImage, BufferedImage overrideImage)
	{
		if (image == null)
		{
			return false;
		}
		if (image == skillImage)
		{
			return true;
		}

		if (skillImage != classicSource)
		{
			classicSource = skillImage;
			classic = themed(skillImage);
			verdicts.clear();
		}
		if (overrideImage != overrideSource)
		{
			overrideSource = overrideImage;
			override = themed(overrideImage);
			verdicts.clear();
		}

		Boolean verdict = verdicts.get(image);
		if (verdict == null)
		{
			if (verdicts.size() >= MAX_VERDICTS)
			{
				verdicts.clear();
			}
			verdict = samePixels(image, classic) || samePixels(image, override);
			verdicts.put(image, verdict);
		}
		return verdict;
	}

	void reset()
	{
		classicSource = null;
		classic = null;
		overrideSource = null;
		override = null;
		verdicts.clear();
	}

	@VisibleForTesting
	static BufferedImage themed(BufferedImage source)
	{
		return source != null && source.getWidth() > 0 && source.getHeight() > 0
			? ImageUtil.resizeImage(source, THEMED_ICON_SIZE, THEMED_ICON_SIZE) : null;
	}

	@VisibleForTesting
	static boolean samePixels(BufferedImage a, BufferedImage b)
	{
		if (a == null || b == null || a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight())
		{
			return false;
		}
		final int width = a.getWidth();
		final int height = a.getHeight();
		return Arrays.equals(a.getRGB(0, 0, width, height, null, 0, width), b.getRGB(0, 0, width, height, null, 0, width));
	}
}
