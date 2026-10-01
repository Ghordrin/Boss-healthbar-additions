package com.ghordrin.bosshealthbar;

import com.google.gson.Gson;
import java.awt.Color;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class HealthIndicatorMarkersTest
{
	private static final Gson GSON = new Gson();

	// java.awt.Color written field by field, as Gson does without a color type adapter.
	private static final String SAVED = "[{\"bossName\":\"Big Boss\",\"entries\":["
		+ "{\"percentage\":0.5,\"color\":{\"value\":-65536,\"falpha\":0.0},\"notify\":true},"
		+ "{\"percentage\":0.25,\"color\":{\"value\":-16711936,\"falpha\":0.0},\"notify\":false}]},"
		+ "{\"bossName\":\"Big.*\",\"entries\":[{\"percentage\":0.75,\"color\":{\"value\":-16776961},\"notify\":false}]}]";

	@Test
	public void readsMarkersForMatchingBosses()
	{
		final HealthIndicatorMarkers.Marker[] markers = HealthIndicatorMarkers.markersFor(GSON, SAVED, "Big Boss");

		assertEquals(3, markers.length);
		assertEquals(new HealthIndicatorMarkers.Marker(0.5f, Color.RED), markers[0]);
		assertEquals(new HealthIndicatorMarkers.Marker(0.25f, Color.GREEN), markers[1]);
		assertEquals(new HealthIndicatorMarkers.Marker(0.75f, Color.BLUE), markers[2]);
	}

	@Test
	public void readsHexColorsAsSavedThroughRuneLitesGson()
	{
		final String saved = "[{\"bossName\":\"Zulrah\",\"entries\":["
			+ "{\"percentage\":0.7,\"color\":\"#FFF53B3B\",\"notify\":true},"
			+ "{\"percentage\":0.3,\"color\":\"#FFFFFFFF\",\"notify\":true}]},"
			+ "{\"bossName\":\"Yama\",\"entries\":[]}]";

		final HealthIndicatorMarkers.Marker[] markers = HealthIndicatorMarkers.markersFor(GSON, saved, "Zulrah");
		assertEquals(2, markers.length);
		assertEquals(new HealthIndicatorMarkers.Marker(0.7f, new Color(0xF53B3B)), markers[0]);
		assertEquals(new HealthIndicatorMarkers.Marker(0.3f, Color.WHITE), markers[1]);

		assertEquals(0, HealthIndicatorMarkers.markersFor(GSON, saved, "Yama").length);
	}

	@Test
	public void namesAreMatchedAsAWholeRegex()
	{
		assertEquals(1, HealthIndicatorMarkers.markersFor(GSON, SAVED, "Bigger Boss").length);
		assertEquals(0, HealthIndicatorMarkers.markersFor(GSON, SAVED, "A Big Boss").length);
	}

	@Test
	public void skipsAnythingUnexpected()
	{
		assertEquals(0, HealthIndicatorMarkers.markersFor(GSON, null, "Big Boss").length);
		assertEquals(0, HealthIndicatorMarkers.markersFor(GSON, "not json {", "Big Boss").length);
		assertEquals(0, HealthIndicatorMarkers.markersFor(GSON, "{\"bossName\":\"Big Boss\"}", "Big Boss").length);
		assertEquals(0, HealthIndicatorMarkers.markersFor(GSON,
			"[{\"bossName\":\"[\",\"entries\":[{\"percentage\":0.5,\"color\":{\"value\":-1}}]}]", "[").length);

		final String mixed = "[{\"bossName\":\"Big Boss\",\"entries\":["
			+ "{\"percentage\":1.5,\"color\":{\"value\":-1}},"
			+ "{\"percentage\":0.5},"
			+ "{\"percentage\":\"half\",\"color\":{\"value\":-1}},"
			+ "{\"percentage\":0.4,\"color\":{\"value\":-1}}]}]";
		assertEquals(1, HealthIndicatorMarkers.markersFor(GSON, mixed, "Big Boss").length);
	}
}
