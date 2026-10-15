package com.ghordrin.bosshealthbar.data;

import com.ghordrin.bosshealthbar.theme.ThemeColors;
import com.google.common.base.Strings;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ParamID;
import net.runelite.client.util.Text;

// What the bar needs to know about the current opponent beyond its health: the name to show, the kill count key,
// the boss stats, and the icon and colors of a known boss. Looked up once per opponent and kept until it changes.
@Singleton
public class OpponentInfo
{
	private Actor actor;
	private int npcId = -1;
	private String name;
	private String shortName;
	private String killCountKey;
	private BossStats.Info stats;
	private KnownBosses.Icon bossIcon;
	private ThemeColors bossColors;

	public String name()
	{
		return name;
	}

	public String shortName()
	{
		return shortName;
	}

	public String killCountKey()
	{
		return killCountKey;
	}

	public BossStats.Info stats()
	{
		return stats;
	}

	public KnownBosses.Icon bossIcon()
	{
		return bossIcon;
	}

	public ThemeColors bossColors()
	{
		return bossColors;
	}

	public void reset()
	{
		actor = null;
	}

	public void update(Actor opponent)
	{
		final NPCComposition composition = opponent instanceof NPC
			? ((NPC) opponent).getTransformedComposition() : null;
		final int id = composition != null ? composition.getId() : -1;
		if (opponent == actor && id == npcId)
		{
			return;
		}

		String shown = Text.removeTags(opponent.getName());
		boolean complete = true;
		bossIcon = null;
		bossColors = null;
		killCountKey = null;
		shortName = null;
		stats = null;
		if (opponent instanceof NPC)
		{
			final KnownBosses.Boss boss = KnownBosses.find(shown);
			if (boss != null)
			{
				bossIcon = boss.getIcon();
				bossColors = boss.getColors();
				killCountKey = boss.getKillCountKey();
				shortName = boss.getShortName();
			}
			stats = BossStats.info(NpcUtil.currentId((NPC) opponent));
			if (composition != null)
			{
				final String longName = composition.getStringValue(ParamID.NPC_HP_NAME);
				if (!Strings.isNullOrEmpty(longName) && longName.contains(shown))
				{
					shown = longName;
				}
			}
			else
			{
				complete = false;
			}
		}

		name = shown;
		actor = complete ? opponent : null;
		npcId = id;
	}
}
