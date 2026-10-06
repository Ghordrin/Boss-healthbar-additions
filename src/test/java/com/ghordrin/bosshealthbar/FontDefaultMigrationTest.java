package com.ghordrin.bosshealthbar;

import java.awt.Font;
import net.runelite.client.config.FontType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

	@Test
	public void runescapeFontsGetTheirNativeSize()
	{
		final FontType regular = FontDefaultMigration.withNativePixelSize(FontType.REGULAR.withSize(17));
		assertEquals(FontType.REGULAR.getFamily(), regular.getFamily());
		assertEquals(16, regular.getSize());

		final FontType small = FontDefaultMigration.withNativePixelSize(FontType.SMALL.withSize(17));
		assertEquals(FontType.SMALL.getFamily(), small.getFamily());
		assertEquals(16, small.getSize());

		final FontType bold = FontDefaultMigration.withNativePixelSize(
			FontType.REGULAR.withBold(true).withItalic(true).withSize(20));
		assertEquals(16, bold.getSize());
		assertTrue(bold.isBold());
		assertTrue(bold.isItalic());
	}

	@Test
	public void otherFontsKeepTheirSize()
	{
		assertNull(FontDefaultMigration.withNativePixelSize(FontType.REGULAR));
		assertNull(FontDefaultMigration.withNativePixelSize(FontType.SMALL));
		assertNull(FontDefaultMigration.withNativePixelSize(OLD_DEFAULT));
		assertNull(FontDefaultMigration.withNativePixelSize(OLD_DEFAULT.withSize(24)));
		assertNull(FontDefaultMigration.withNativePixelSize(null));
	}
}
