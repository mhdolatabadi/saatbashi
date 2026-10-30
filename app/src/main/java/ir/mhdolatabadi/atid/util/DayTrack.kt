package ir.mhdolatabadi.atid.util

/** The rows and marks of today's prayer page; a 1:1 port of web/src/lib/dayTrack.ts. */
object DayTracks {

    /** fajr..isha as in [PrayerTimes], plus "midnight" (Shar'i midnight). */
    private val labels = mapOf(
        "fajr" to ("اذان صبح" to "صبح"),
        "sunrise" to ("طلوع آفتاب" to "طلوع"),
        "dhuhr" to ("اذان ظهر" to "ظهر"),
        "asr" to ("اذان عصر" to "عصر"),
        "sunset" to ("غروب آفتاب" to "غروب"),
        "maghrib" to ("اذان مغرب" to "مغرب"),
        "isha" to ("اذان عشا" to "عشا"),
        "midnight" to ("نیمه‌شب شرعی" to "نیمه‌شب")
    )

    /** Today's rows in order, as the prayer page lists them. */
    val dayRows = listOf("fajr", "sunrise", "dhuhr", "asr", "sunset", "maghrib", "isha")

    /** Labels closer than this share no lane: about one label width on a phone. */
    const val MIN_LABEL_GAP = 0.16

    fun fullLabel(key: String): String = labels.getValue(key).first

    fun timeOf(times: PrayerTimes, key: String): String = when (key) {
        "fajr" -> times.fajr
        "sunrise" -> times.sunrise
        "dhuhr" -> times.dhuhr
        "asr" -> times.asr
        "sunset" -> times.sunset
        "maghrib" -> times.maghrib
        "isha" -> times.isha
        "midnight" -> midnight(times)
        else -> error("unknown row $key")
    }

    /** Shar'i midnight (Jafari): halfway between sunset and the next dawn. */
    fun midnight(times: PrayerTimes): String {
        val sunset = toMinutes(times.sunset)
        val nextFajr = toMinutes(times.fajr) + 1440
        return fromMinutes(sunset + (nextFajr - sunset) / 2.0)
    }

    /** The next row after [nowMinutes] (minute of the day at the place), or tomorrow's dawn after the last one. */
    fun nextRow(times: PrayerTimes, nowMinutes: Int): NextRow {
        val key = dayRows.firstOrNull { toMinutes(timeOf(times, it)) > nowMinutes }
        val tomorrow = key == null
        val k = key ?: "fajr"
        val at = toMinutes(timeOf(times, k)) + if (tomorrow) 1440 else 0
        return NextRow(k, labels.getValue(k).first, labels.getValue(k).second, timeOf(times, k), at - nowMinutes, tomorrow)
    }

    /**
     * The day track shown under the next prayer: from dawn to Isha with the five adhans during the day,
     * and from Isha through Shar'i midnight to the next dawn at night.
     */
    fun dayTrack(times: PrayerTimes, nowMinutes: Int): DayTrack {
        val next = nextRow(times, nowMinutes)
        val fajr = toMinutes(times.fajr)
        val isha = toMinutes(times.isha)
        val night = nowMinutes < fajr || nowMinutes >= isha

        // Night minutes are measured from Isha on a clock that runs past 24:00.
        fun nightClock(m: Int) = if (m < isha) m + 1440 else m
        val points: List<Triple<String, Int, String>> = if (night) {
            val mid = midnight(times)
            listOf(
                Triple("isha", isha, times.isha),
                Triple("midnight", nightClock(toMinutes(mid)), mid),
                Triple("fajr", fajr + 1440, times.fajr)
            )
        } else {
            listOf("fajr", "dhuhr", "asr", "maghrib", "isha").map { Triple(it, toMinutes(timeOf(times, it)), timeOf(times, it)) }
        }

        val start = points.first().second
        val span = (points.last().second - start).toDouble()
        val now = if (night) nightClock(nowMinutes) else nowMinutes
        val nextIndex = points.indexOfFirst { it.second > now }

        // Marks sit at their true place in time; a label that would run into the one before it moves above the bar.
        val lastInLane = doubleArrayOf(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
        val marks = points.mapIndexed { i, (key, at, time) ->
            val position = (at - start) / span
            val lane = if (position - lastInLane[0] < MIN_LABEL_GAP && position - lastInLane[1] >= MIN_LABEL_GAP) 1 else 0
            lastInLane[lane] = position
            TrackMark(
                key = key,
                label = labels.getValue(key).second,
                time = time,
                position = position,
                state = when {
                    i == nextIndex -> MarkState.NEXT
                    i < nextIndex || nextIndex == -1 -> MarkState.PAST
                    else -> MarkState.FUTURE
                },
                lane = lane
            )
        }

        return DayTrack(next, night, ((now - start) / span).coerceIn(0.0, 1.0), marks)
    }

    /** Each of today's rows plus Shar'i midnight, marked past, next or upcoming. Midnight is never dimmed. */
    fun dayTiles(times: PrayerTimes, nowMinutes: Int): List<DayTile> {
        val next = nextRow(times, nowMinutes)
        val rows = dayRows.map { key ->
            val time = timeOf(times, key)
            val state = when {
                key == next.key && !next.tomorrow -> MarkState.NEXT
                toMinutes(time) <= nowMinutes -> MarkState.PAST
                else -> MarkState.FUTURE
            }
            DayTile(key, labels.getValue(key).first, time, state)
        }.toMutableList()
        // After Isha the next row is tomorrow's dawn: today's dawn tile stands in for it.
        if (next.tomorrow) rows[0] = rows[0].copy(state = MarkState.NEXT)
        return rows + DayTile("midnight", labels.getValue("midnight").first, midnight(times), MarkState.FUTURE)
    }

    private fun toMinutes(hhmm: String): Int {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return h * 60 + m
    }

    private fun fromMinutes(total: Double): String {
        // Half a minute rounds up, as Math.round does on the web.
        val m = ((kotlin.math.floor(total + 0.5).toInt() % 1440) + 1440) % 1440
        return String.format(java.util.Locale.US, "%02d:%02d", m / 60, m % 60)
    }
}

enum class MarkState { PAST, NEXT, FUTURE }

data class NextRow(
    val key: String,
    val label: String,
    val shortLabel: String,
    val time: String,
    val minutesLeft: Int,
    val tomorrow: Boolean
)

data class TrackMark(
    val key: String,
    val label: String,
    val time: String,
    /** 0..1 along the track. */
    val position: Double,
    val state: MarkState,
    /** 0: label below the bar; 1: above it, when the mark sits too close to its neighbour below. */
    val lane: Int
)

data class DayTrack(
    val next: NextRow,
    /** True between Isha and dawn: the track then runs from Isha to the next dawn. */
    val night: Boolean,
    /** 0..1, how far along the track now is. */
    val progress: Double,
    val marks: List<TrackMark>
)

data class DayTile(val key: String, val label: String, val time: String, val state: MarkState)
