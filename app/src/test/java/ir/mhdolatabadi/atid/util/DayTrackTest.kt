package ir.mhdolatabadi.atid.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Same reference values as web/src/lib/__tests__/dayTrack.test.ts. */
class DayTrackTest {
    private val times = PrayerTimes(
        fajr = "04:30", sunrise = "06:00", dhuhr = "12:00", asr = "15:30",
        sunset = "18:00", maghrib = "18:20", isha = "19:30"
    )

    @Test fun nextRowIncludesSunriseAndSunset() {
        val sunrise = DayTracks.nextRow(times, 5 * 60)
        assertEquals("sunrise", sunrise.key)
        assertEquals("طلوع", sunrise.shortLabel)
        assertEquals(60, sunrise.minutesLeft)
        assertFalse(sunrise.tomorrow)
        val asr = DayTracks.nextRow(times, 15 * 60)
        assertEquals("asr", asr.key)
        assertEquals("اذان عصر", asr.label)
        assertEquals("15:30", asr.time)
        assertEquals(30, asr.minutesLeft)
    }

    @Test fun countsToTomorrowsDawnAfterIsha() {
        val next = DayTracks.nextRow(times, 23 * 60)
        assertEquals("fajr", next.key)
        assertEquals(330, next.minutesLeft)
        assertTrue(next.tomorrow)
    }

    @Test fun runsFromDawnToIshaThroughTheFiveAdhansDuringTheDay() {
        val track = DayTracks.dayTrack(times, 15 * 60)
        assertFalse(track.night)
        assertEquals(listOf("fajr", "dhuhr", "asr", "maghrib", "isha"), track.marks.map { it.key })
        assertEquals(
            listOf(MarkState.PAST, MarkState.PAST, MarkState.NEXT, MarkState.FUTURE, MarkState.FUTURE),
            track.marks.map { it.state }
        )
        assertEquals((15 * 60 - 270) / 900.0, track.progress, 1e-9)
    }

    @Test fun placesMarksAtTheirTrueTimeAndLiftsACrowdedLabel() {
        val track = DayTracks.dayTrack(times, 15 * 60)
        val expected = listOf(0.0, 0.5, 660 / 900.0, 830 / 900.0, 1.0)
        track.marks.zip(expected).forEach { (mark, position) -> assertEquals(position, mark.position, 1e-9) }
        // Maghrib (18:20) and Isha (19:30) are 70 minutes apart, so Isha's label goes above.
        assertEquals(listOf(0, 0, 0, 0, 1), track.marks.map { it.lane })
    }

    @Test fun runsFromIshaThroughMidnightToTheNextDawnAtNight() {
        // Shar'i midnight is halfway from sunset (18:00) to the next dawn (04:30): 23:15.
        val evening = DayTracks.dayTrack(times, 21 * 60)
        assertTrue(evening.night)
        assertEquals(
            listOf(
                Triple("isha", "19:30", MarkState.PAST),
                Triple("midnight", "23:15", MarkState.NEXT),
                Triple("fajr", "04:30", MarkState.FUTURE)
            ),
            evening.marks.map { Triple(it.key, it.time, it.state) }
        )
        val beforeDawn = DayTracks.dayTrack(times, 2 * 60)
        assertEquals(listOf(MarkState.PAST, MarkState.PAST, MarkState.NEXT), beforeDawn.marks.map { it.state })
        assertEquals((2 * 60 + 1440 - 1170) / (270 + 1440 - 1170).toDouble(), beforeDawn.progress, 1e-9)
    }

    @Test fun tilesDimElapsedRowsAndMarkTheNextOne() {
        val tiles = DayTracks.dayTiles(times, 15 * 60)
        assertEquals(
            listOf("past", "past", "past", "next", "future", "future", "future", "future"),
            tiles.map { it.state.name.lowercase() }
        )
        assertEquals("midnight", tiles[7].key)
        assertEquals("23:15", tiles[7].time)
    }

    @Test fun dawnTileIsNextAfterIsha() {
        assertEquals(
            listOf("next", "past", "past", "past", "past", "past", "past", "future"),
            DayTracks.dayTiles(times, 23 * 60).map { it.state.name.lowercase() }
        )
    }

    @Test fun midnightRoundsHalfAMinuteUpAsTheWebDoes() {
        // Sunset 17:11 to dawn 05:00 is 709 minutes; half of it lands on 23:05:30, shown as 23:06.
        val autumn = times.copy(fajr = "05:00", sunset = "17:11")
        assertEquals("23:06", DayTracks.midnight(autumn))
    }
}
