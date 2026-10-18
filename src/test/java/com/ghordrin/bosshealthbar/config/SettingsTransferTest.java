package com.ghordrin.bosshealthbar.config;

import static org.junit.Assert.assertEquals;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class SettingsTransferTest
{
	@Test
	public void keepsEverythingTheFileNames()
	{
		assertEquals(Collections.emptyList(), SettingsTransfer.keysToUnset(
			Arrays.asList("barWidth", "font"),
			ImmutableSet.of("barWidth", "font"),
			ImmutableSet.of("barWidth", "font")));
	}

	// The reason this helper exists: a declared setting the file leaves out gets its default written, so it
	// must not be unset. With nothing saved for it, the client's own font editor reads null and throws.
	@Test
	public void leavesADeclaredSettingTheFileLeavesOut()
	{
		assertEquals(Collections.emptyList(), SettingsTransfer.keysToUnset(
			Arrays.asList("barWidth", "font"),
			ImmutableSet.of("barWidth", "font"),
			ImmutableSet.of("barWidth")));
	}

	@Test
	public void clearsASettingThisVersionNoLongerHas()
	{
		assertEquals(Collections.singletonList("ornamentStyle"), SettingsTransfer.keysToUnset(
			Arrays.asList("barWidth", "ornamentStyle"),
			ImmutableSet.of("barWidth"),
			ImmutableSet.of("barWidth")));
	}
}
