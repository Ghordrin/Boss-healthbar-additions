package com.ghordrin.bosshealthbar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NpcFilterTest
{
	private static boolean matches(String csv, String name, int id, int currentId)
	{
		return NpcFilter.parse(csv).matches(name, id, currentId);
	}

	@Test
	public void emptyListsMatchNothing()
	{
		assertTrue(NpcFilter.parse("").isEmpty());
		assertTrue(NpcFilter.parse(null).isEmpty());
		assertTrue(NpcFilter.parse(" , ,, ").isEmpty());
		assertFalse(matches("", "goblin", 1, 1));
	}

	@Test
	public void newLinesSeparateEntries()
	{
		assertTrue(matches("Hill Giant\r\nGoblin", "goblin", 1, 1));
		assertTrue(matches("Hill Giant\n123,\nGoblin", "hill giant", 1, 1));
		assertTrue(matches("Hill Giant\n123", "imp", 123, 123));
		assertTrue(NpcFilter.parse("\n\n ,\n").isEmpty());
	}

	@Test
	public void namesMatchIgnoringCaseAndSpaces()
	{
		assertTrue(matches("  Hill Giant , Goblin", "hill giant", 1, 1));
		assertTrue(matches("GOBLIN", "goblin", 1, 1));
		assertFalse(matches("Goblin", "hobgoblin", 1, 1));
	}

	@Test
	public void wildcardsMatchAnything()
	{
		assertTrue(matches("*goblin", "hobgoblin", 1, 1));
		assertTrue(matches("Hill*", "hill giant", 1, 1));
		assertTrue(matches("*giant*", "hill giant", 1, 1));
		assertTrue(matches("*", "anything", 1, 1));
		assertFalse(matches("Hill*", "moss giant", 1, 1));
	}

	@Test
	public void wildcardsTreatOtherCharactersLiterally()
	{
		assertTrue(matches("Dr. *", "dr. ford", 1, 1));
		assertFalse(matches("Dr. *", "drx ford", 1, 1));
	}

	@Test
	public void idsMatchEitherForm()
	{
		assertTrue(matches("1234", "someone", 1234, 99));
		assertTrue(matches("1234", "someone", 99, 1234));
		assertFalse(matches("1234", "someone", 99, 98));
	}

	@Test
	public void numbersAreNotNames()
	{
		assertFalse(matches("12", "12", 1, 1));
		assertTrue(matches("12a", "12a", 1, 1));
	}

	@Test
	public void ignoresIdsTooLongToParse()
	{
		assertTrue(NpcFilter.parse("99999999999999999999").isEmpty());
	}

	@Test
	public void neverBeatsAlso()
	{
		final NpcFilter never = NpcFilter.parse("Goblin");
		final NpcFilter also = NpcFilter.parse("Goblin, Hill giant");
		assertEquals(NpcFilter.Match.NEVER, NpcFilter.check(never, also, "goblin", 1, 1));
		assertEquals(NpcFilter.Match.ALSO, NpcFilter.check(never, also, "hill giant", 1, 1));
		assertEquals(NpcFilter.Match.NONE, NpcFilter.check(never, also, "moss giant", 1, 1));
	}

	@Test
	public void neverByIdBeatsAlsoByName()
	{
		assertEquals(NpcFilter.Match.NEVER,
			NpcFilter.check(NpcFilter.parse("500"), NpcFilter.parse("Goblin"), "goblin", 1, 500));
	}
}
