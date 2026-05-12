package de.stammtischHub.terminPilot.provider.apple

import de.stammtischHub.terminPilot.domain.Appointment
import de.stammtischHub.terminPilot.provider.CalendarProvider
import java.time.LocalDateTime

class AppleCalendarProvider : CalendarProvider {
    override fun getCalendarForTimespan(start: LocalDateTime, end: LocalDateTime): List<Appointment> {
        TODO("Not yet implemented")
    }

    override fun writeToCalendar(appointment: Appointment) {
        TODO("Not yet implemented")
    }
}
