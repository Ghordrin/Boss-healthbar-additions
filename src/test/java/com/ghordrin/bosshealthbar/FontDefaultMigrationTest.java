package com.ghordrin.bosshealthbar;

import java.awt.Font;
import net.runelite.client.config.FontType;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FontDefaultMigrationTest
{
	private static final FontType OLD_DEFAULT = new FontType().withFamily(Font.SERIF).withSize(17);

	@Test
	public void oldDefaultIsMigrated()
	{
		assertTrue(FontDefaultMigration.isOldDefault(OLD_DEFAULT));
	}

	@Test
	public void otherFontsAreKept()
	{
		assertFalse(FontDefaultMigration.isOldDefault(null));
		assertFalse(FontDefaultMigration.isOldDefault(OLD_DEFAULT.withSize(18)));
		assertFalse(FontDefaultMigration.isOldDefault(OLD_DEFAULT.withBold(true)));
		assertFalse(FontDefaultMigration.isOldDefault(OLD_DEFAULT.withItalic(true)));
		assertFalse(FontDefaultMigration.isOldDefault(OLD_DEFAULT.withFamily(Font.SANS_SERIF)));
		assertFalse(FontDefaultMigration.isOldDefault(FontType.REGULAR));
		assertFalse(FontDefaultMigration.isOldDefault(FontType.BOLD));
		assertFalse(FontDefaultMigration.isOldDefault(FontType.SMALL));
	}
}
