package com.ghordrin.bosshealthbar.render;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.data.KnownBosses;
import com.ghordrin.bosshealthbar.theme.BarTheme;
import com.ghordrin.bosshealthbar.theme.HealthBarTheme;
import com.ghordrin.bosshealthbar.theme.ThemeIcon;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.ImageUtil;

// The icons the bar draws: the one in the crests and the one in front of the name. A sprite is kept at the
// size the crest wants, so it is only resized when the icon changes.
@Singleton
public class BarIcons
{
	private final BossHealthBarConfig config;
	private final SpriteManager spriteManager;
	private final ItemManager itemManager;

	private int spriteId = -1;
	private BufferedImage paddedSprite;

	@Inject
	private BarIcons(BossHealthBarConfig config, SpriteManager spriteManager, ItemManager itemManager)
	{
		this.config = config;
		this.spriteManager = spriteManager;
		this.itemManager = itemManager;
	}

	public BufferedImage nameIcon(BarTheme theme, KnownBosses.Icon icon, boolean showingPreview)
	{
		return placement(theme) == BarIconPlacement.BEFORE_NAME ? icon(theme, icon, showingPreview) : null;
	}

	public BufferedImage crestIcon(BarTheme theme, KnownBosses.Icon icon, boolean showingPreview)
	{
		return placement(theme).hasCrest() ? icon(theme, icon, showingPreview) : null;
	}

	private BarIconPlacement placement(BarTheme theme)
	{
		return config.iconPlacement().onTheme(theme.isFlat());
	}

	private BufferedImage icon(BarTheme theme, KnownBosses.Icon icon, boolean showingPreview)
	{
		if (config.useBossIcon() && !showingPreview && icon != null)
		{
			final BufferedImage bossIcon = image(icon.isItem(), icon.getId());
			if (bossIcon != null)
			{
				return bossIcon;
			}
		}

		if (theme == HealthBarTheme.CUSTOM)
		{
			final int itemId = config.customIconItemId();
			return itemId > 0 ? itemManager.getImage(itemId) : null;
		}

		final ThemeIcon themeIcon = theme.getIcon();
		return themeIcon != null ? image(themeIcon.isItem(), themeIcon.getId()) : null;
	}

	private BufferedImage image(boolean item, int id)
	{
		if (item)
		{
			return itemManager.getImage(id);
		}

		if (id != spriteId)
		{
			final BufferedImage sprite = spriteManager.getSprite(id, 0);
			if (sprite == null)
			{
				return null;
			}
			// The crest draws its icon into a square, so pad the sprite rather than stretch it.
			final int size = Math.max(sprite.getWidth(), sprite.getHeight());
			paddedSprite = ImageUtil.resizeCanvas(sprite, size, size);
			spriteId = id;
		}
		return paddedSprite;
	}
}
