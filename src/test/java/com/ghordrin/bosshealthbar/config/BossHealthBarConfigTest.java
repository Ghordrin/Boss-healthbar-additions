package com.ghordrin.bosshealthbar.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.ghordrin.bosshealthbar.data.DamageNumberSource;
import com.ghordrin.bosshealthbar.render.BarPosition;
import com.ghordrin.bosshealthbar.render.HitpointsTextMode;
import com.ghordrin.bosshealthbar.render.IntroAnimation;
import org.junit.Test;

public class BossHealthBarConfigTest
{
	private final BossHealthBarConfig defaults = new BossHealthBarConfig()
	{
	};

	// The settings the panel writes are saved under these names, so a changed one would reset them for everyone.
	@Test
	public void panelKeysAreUnchanged()
	{
		assertEquals("showBossName", BossHealthBarConfig.SHOW_BOSS_NAME_KEY);
		assertEquals("showCombatLevel", BossHealthBarConfig.SHOW_COMBAT_LEVEL_KEY);
		assertEquals("hitpointsTextMode", BossHealthBarConfig.HITPOINTS_TEXT_MODE_KEY);
		assertEquals("showDamageNumber", BossHealthBarConfig.SHOW_DAMAGE_NUMBER_KEY);
		assertEquals("damageNumberSource", BossHealthBarConfig.DAMAGE_NUMBER_SOURCE_KEY);
		assertEquals("showKillCount", BossHealthBarConfig.SHOW_KILL_COUNT_KEY);
		assertEquals("showPartyDefence", BossHealthBarConfig.SHOW_PARTY_DEFENCE_KEY);
		assertEquals("showMagicDefence", BossHealthBarConfig.SHOW_MAGIC_DEFENCE_KEY);
		assertEquals("showSpecialAttackCounts", BossHealthBarConfig.SHOW_SPECIAL_ATTACK_COUNTS_KEY);
		assertEquals("showWeakness", BossHealthBarConfig.SHOW_WEAKNESS_KEY);
		assertEquals("showDrainCap", BossHealthBarConfig.SHOW_DRAIN_CAP_KEY);
		assertEquals("showFightTimer", BossHealthBarConfig.SHOW_FIGHT_TIMER_KEY);
		assertEquals("showPartnerBar", BossHealthBarConfig.SHOW_PARTNER_BAR_KEY);
		assertEquals("secondBarSeparate", BossHealthBarConfig.SECOND_BAR_SEPARATE_KEY);
		assertEquals("secondBarScale", BossHealthBarConfig.SECOND_BAR_SCALE_KEY);
		assertEquals("showDamageTrail", BossHealthBarConfig.SHOW_DAMAGE_TRAIL_KEY);
		assertEquals("animationSpeed", BossHealthBarConfig.ANIMATION_SPEED_KEY);
		assertEquals("lowHealthEffect", BossHealthBarConfig.LOW_HEALTH_EFFECT_KEY);
		assertEquals("lowHealthThreshold", BossHealthBarConfig.LOW_HEALTH_THRESHOLD_KEY);
		assertEquals("flashOnBigHits", BossHealthBarConfig.FLASH_ON_BIG_HITS_KEY);
		assertEquals("introAnimation", BossHealthBarConfig.INTRO_ANIMATION_KEY);
		assertEquals("showDefeatAnimation", BossHealthBarConfig.SHOW_DEFEAT_ANIMATION_KEY);
		assertEquals("burnAwayDefeat", BossHealthBarConfig.BURN_AWAY_DEFEAT_KEY);
		assertEquals("fortifyPhase", BossHealthBarConfig.FORTIFY_PHASE_KEY);
		assertEquals("namePosition", BossHealthBarConfig.NAME_POSITION_KEY);
		assertEquals("damageNumberPosition", BossHealthBarConfig.DAMAGE_NUMBER_POSITION_KEY);
		assertEquals("hitpointsPosition", BossHealthBarConfig.HITPOINTS_POSITION_KEY);
		assertEquals("killCountPosition", BossHealthBarConfig.KILL_COUNT_POSITION_KEY);
		assertEquals("partyDefencePosition", BossHealthBarConfig.PARTY_DEFENCE_POSITION_KEY);
		assertEquals("specialAttackCountsPosition", BossHealthBarConfig.SPECIAL_ATTACK_COUNTS_POSITION_KEY);
		assertEquals("weaknessPosition", BossHealthBarConfig.WEAKNESS_POSITION_KEY);
		assertEquals("drainCapPosition", BossHealthBarConfig.DRAIN_CAP_POSITION_KEY);
		assertEquals("fightTimerPosition", BossHealthBarConfig.FIGHT_TIMER_POSITION_KEY);
	}

	// The signpost on the settings screen is a button, so it must start, and stay, unticked.
	@Test
	public void openPanelIsAnUntickedButton()
	{
		assertEquals("openPanel", BossHealthBarConfig.OPEN_PANEL_KEY);
		assertFalse(defaults.openPanel());
	}

	@Test
	public void textDefaultsAreUnchanged()
	{
		assertTrue(defaults.showBossName());
		assertFalse(defaults.showCombatLevel());
		assertSame(HitpointsTextMode.NONE, defaults.hitpointsTextMode());
		assertTrue(defaults.showDamageNumber());
		assertSame(DamageNumberSource.ME, defaults.damageNumberSource());
	}

	@Test
	public void bossInfoDefaultsAreUnchanged()
	{
		assertFalse(defaults.showKillCount());
		assertTrue(defaults.showPartyDefence());
		assertTrue(defaults.showMagicDefence());
		assertTrue(defaults.showSpecialAttackCounts());
		assertTrue(defaults.showWeakness());
		assertTrue(defaults.showDrainCap());
		assertTrue(defaults.showFightTimer());
		assertTrue(defaults.showPartnerBar());
		assertFalse(defaults.secondBarSeparate());
		assertEquals(50, defaults.secondBarScale());
	}

	@Test
	public void animationDefaultsAreUnchanged()
	{
		assertTrue(defaults.showDamageTrail());
		assertEquals(6, defaults.animationSpeed());
		assertTrue(defaults.lowHealthEffect());
		assertEquals(25, defaults.lowHealthThreshold());
		assertFalse(defaults.flashOnBigHits());
		assertSame(IntroAnimation.SLIDE_AND_EXPAND, defaults.introAnimation());
		assertTrue(defaults.showDefeatAnimation());
		assertFalse(defaults.burnAwayDefeat());
		assertFalse(defaults.fortifyPhase());
	}

	@Test
	public void layoutDefaultsAreUnchanged()
	{
		assertSame(BarPosition.TOP_LEFT, defaults.namePosition());
		assertSame(BarPosition.TOP_RIGHT, defaults.damageNumberPosition());
		assertSame(BarPosition.BOTTOM_RIGHT, defaults.hitpointsPosition());
		assertSame(BarPosition.BOTTOM_LEFT, defaults.killCountPosition());
		assertSame(BarPosition.BOTTOM_LEFT, defaults.partyDefencePosition());
		assertSame(BarPosition.BOTTOM_LEFT, defaults.specialAttackCountsPosition());
		assertSame(BarPosition.BOTTOM_LEFT, defaults.weaknessPosition());
		assertSame(BarPosition.BOTTOM_LEFT, defaults.drainCapPosition());
		assertSame(BarPosition.TOP_CENTER, defaults.fightTimerPosition());
	}
}
