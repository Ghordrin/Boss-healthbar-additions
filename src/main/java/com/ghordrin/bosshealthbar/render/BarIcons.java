package com.ghordrin.bosshealthbar.render;

import com.ghordrin.bosshealthbar.config.BossHealthBarConfig;
import com.ghordrin.bosshealthbar.data.KnownBosses;
import com.ghordrin.bosshealthbar.theme.BarTheme;
import com.ghordrin.bosshealthbar.theme.HealthBarTheme;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.ImageUtil;

// The icons the bar draws: the one in the crests and the one in front of the name. The boss sprite is kept at the
// size the crest wants, so it is only resized when the boss changes.
@Singleton
public class BarIcons
{
	private final BossHealthBarConfig config;
	private final SpriteManager spriteManager;
	private final ItemManager itemManager;

	private int bossSpriteId = -1;
	private BufferedImage bossSprite;

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
			final BufferedImage bossIcon = bossIcon(icon);
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

		final Integer godIconSpriteId = theme.getGodIconSpriteId();
		return godIconSpriteId != null ? spriteManager.getSprite(godIconSpriteId, 0) : null;
	}

	private BufferedImage bossIcon(KnownBosses.Icon icon)
	{
		if (icon.isItem())
		{
			return itemManager.getImage(icon.getId());
		}

		if (icon.getId() != bossSpriteId)
		{
			final BufferedImage sprite = spriteManager.getSprite(icon.getId(), 0);
			if (sprite == null)
			{
				return null;
			}
			// The crest draws its icon into a square, so pad the sprite rather than stretch it.
			final int size = Math.max(sprite.getWidth(), sprite.getHeight());
			bossSprite = ImageUtil.resizeCanvas(sprite, size, size);
			bossSpriteId = icon.getId();
		}
		return bossSprite;
	}
}
