package ir.mhdolatabadi.atid.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ir.mhdolatabadi.atid.data.Occasion
import ir.mhdolatabadi.atid.data.Occasions
import ir.mhdolatabadi.atid.util.DateLabels
import ir.mhdolatabadi.atid.util.HijriDates
import ir.mhdolatabadi.atid.util.PersianDateUtils
import java.util.Calendar
import java.util.Date

/** One cell of the month grid. */
data class DayCellState(
    val date: Date,
    val solarDay: Int,
    val hijriDay: Int,
    val gregorianDay: Int,
    val isToday: Boolean,
    val isHoliday: Boolean,
    val isFriday: Boolean
)

data class DayDetails(
    val solarLabel: String,
    val hijriLabel: String,
    val gregorianLabel: String,
    val occasions: List<Occasion>
)

data class MonthOccasion(val dayLabel: String, val occasion: Occasion)

data class CalendarMonthUiState(
    val monthLabel: String,
    /** The month's name and year apart, for the sky hero ("آبان" and "۱۴۰۵"). */
    val monthName: String,
    val yearLabel: String,
    /** True when the displayed month holds today and today is the selected day. */
    val showingToday: Boolean,
    /** Lunar and Gregorian months the solar month spans, e.g. "ربیع‌الثانی – جمادی‌الاول ۱۴۴۸". */
    val hijriRange: String,
    val gregorianRange: String,
    val todayLabel: String,
    /** One entry per grid cell; null renders as a blank leading cell. */
    val days: List<DayCellState?>,
    val selectedDate: Date,
    val selected: DayDetails,
    val monthOccasions: List<MonthOccasion>,
    /** year * 12 + month, so the screen can tell which way the month moved. */
    val monthIndex: Int
)

class DashboardViewModel(private val now: () -> Date = { Date() }) : ViewModel() {

    private var today = PersianDateUtils.fromDate(now())
    private var todayDate = PersianDateUtils.toGregorianDate(today.year, today.month, today.day)
    private var displayedYear = today.year
    private var displayedMonth = today.month
    private var selectedDate = todayDate

    private val _uiState = MutableLiveData<CalendarMonthUiState>()
    val uiState: LiveData<CalendarMonthUiState> = _uiState

    init {
        refresh()
    }

    fun goToPreviousMonth() {
        displayedMonth--
        if (displayedMonth < 1) {
            displayedMonth = 12
            displayedYear--
        }
        selectedDate = PersianDateUtils.toGregorianDate(displayedYear, displayedMonth, 1)
        refresh()
    }

    fun goToNextMonth() {
        displayedMonth++
        if (displayedMonth > 12) {
            displayedMonth = 1
            displayedYear++
        }
        selectedDate = PersianDateUtils.toGregorianDate(displayedYear, displayedMonth, 1)
        refresh()
    }

    fun goToToday() {
        today = PersianDateUtils.fromDate(now())
        todayDate = PersianDateUtils.toGregorianDate(today.year, today.month, today.day)
        displayedYear = today.year
        displayedMonth = today.month
        selectedDate = todayDate
        refresh()
    }

    fun select(date: Date) {
        selectedDate = date
        refresh()
    }

    private fun sameDay(a: Date, b: Date): Boolean {
        val ca = Calendar.getInstance().apply { time = a }
        val cb = Calendar.getInstance().apply { time = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    private fun refresh() {
        val count = PersianDateUtils.daysInMonth(displayedYear, displayedMonth)
        val leadingBlanks = PersianDateUtils.weekdayIndexSaturdayFirst(
            PersianDateUtils.firstDayOfWeek(displayedYear, displayedMonth)
        )

        val cells = mutableListOf<DayCellState?>()
        repeat(leadingBlanks) { cells.add(null) }
        val monthOccasions = mutableListOf<MonthOccasion>()
        for (day in 1..count) {
            val date = PersianDateUtils.toGregorianDate(displayedYear, displayedMonth, day)
            val calendar = Calendar.getInstance().apply { time = date }
            val occasions = Occasions.on(date)
            val friday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            cells.add(
                DayCellState(
                    date = date,
                    solarDay = day,
                    hijriDay = HijriDates.of(date).day,
                    gregorianDay = calendar.get(Calendar.DAY_OF_MONTH),
                    isToday = sameDay(date, todayDate),
                    isHoliday = friday || occasions.any { it.holiday },
                    isFriday = friday
                )
            )
            val dayLabel = "${PersianDateUtils.toPersianDigits(day)} ${PersianDateUtils.monthName(displayedMonth)}"
            occasions.forEach { monthOccasions.add(MonthOccasion(dayLabel, it)) }
        }

        val first = PersianDateUtils.toGregorianDate(displayedYear, displayedMonth, 1)
        val last = PersianDateUtils.toGregorianDate(displayedYear, displayedMonth, count)
        val h1 = HijriDates.of(first)
        val h2 = HijriDates.of(last)
        val hijriRange = if (h1.month == h2.month) {
            "${HijriDates.monthName(h1.month)} ${PersianDateUtils.toPersianDigits(h1.year)}"
        } else {
            "${HijriDates.monthName(h1.month)} – ${HijriDates.monthName(h2.month)} ${PersianDateUtils.toPersianDigits(h2.year)}"
        }
        val g1 = Calendar.getInstance().apply { time = first }
        val g2 = Calendar.getInstance().apply { time = last }
        val gregorianRange = if (g1.get(Calendar.MONTH) == g2.get(Calendar.MONTH)) {
            "${PersianDateUtils.gregorianMonthNames[g1.get(Calendar.MONTH)]} ${PersianDateUtils.toPersianDigits(g1.get(Calendar.YEAR))}"
        } else {
            "${PersianDateUtils.gregorianMonthNames[g1.get(Calendar.MONTH)]} – " +
                "${PersianDateUtils.gregorianMonthNames[g2.get(Calendar.MONTH)]} ${PersianDateUtils.toPersianDigits(g2.get(Calendar.YEAR))}"
        }

        _uiState.value = CalendarMonthUiState(
            monthLabel = "${PersianDateUtils.monthName(displayedMonth)} ${PersianDateUtils.toPersianDigits(displayedYear)}",
            monthName = PersianDateUtils.monthName(displayedMonth),
            yearLabel = PersianDateUtils.toPersianDigits(displayedYear),
            showingToday = sameDay(selectedDate, todayDate),
            hijriRange = hijriRange,
            gregorianRange = gregorianRange,
            todayLabel = "امروز ${DateLabels.solar(todayDate)}",
            days = cells,
            selectedDate = selectedDate,
            selected = DayDetails(
                solarLabel = DateLabels.solar(selectedDate),
                hijriLabel = DateLabels.hijri(selectedDate),
                gregorianLabel = DateLabels.gregorian(selectedDate),
                occasions = Occasions.on(selectedDate)
            ),
            monthOccasions = monthOccasions,
            monthIndex = displayedYear * 12 + displayedMonth
        )
    }
}
