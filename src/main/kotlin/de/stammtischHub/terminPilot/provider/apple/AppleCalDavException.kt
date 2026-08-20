package de.stammtischHub.terminPilot.provider.apple

sealed class AppleCalDavException(
  message: String,
  val errorCode: String,
  val userMessage: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)

class AppleAuthenticationException(
  val httpStatusCode: Int? = null,
  cause: Throwable? = null,
) : AppleCalDavException(
    message =
      if (httpStatusCode != null) {
        "Authentication with Apple CalDAV failed (HTTP $httpStatusCode)"
      } else {
        "Authentication with Apple CalDAV failed"
      },
    errorCode = "APPLE_AUTHENTICATION_FAILED",
    userMessage =
      "Die Anmeldung bei Apple ist fehlgeschlagen. " +
        "Bitte prüfe deine Apple-ID und dein app-spezifisches Passwort.",
    cause = cause,
  )

class AppleCalDavUnavailableException(
  val httpStatusCode: Int? = null,
  cause: Throwable? = null,
) : AppleCalDavException(
    message =
      if (httpStatusCode != null) {
        "Apple CalDAV server is unavailable (HTTP $httpStatusCode)"
      } else {
        "Apple CalDAV server is unavailable"
      },
    errorCode = "APPLE_CALDAV_UNAVAILABLE",
    userMessage = "Apple Kalender ist momentan nicht erreichbar. Bitte versuche es später erneut.",
    cause = cause,
  )

class AppleCalendarDiscoveryException(
  detail: String = "",
  cause: Throwable? = null,
) : AppleCalDavException(
    message = if (detail.isEmpty()) "Apple calendar discovery failed" else "Apple calendar discovery failed: $detail",
    errorCode = "APPLE_CALENDAR_DISCOVERY_FAILED",
    userMessage = "Die Apple-Kalender konnten nicht gefunden werden.",
    cause = cause,
  )

class AppleNoCalendarsFoundException :
  AppleCalDavException(
    message = "No Apple calendars found for this account",
    errorCode = "APPLE_NO_CALENDARS_FOUND",
    userMessage = "Es wurden keine Apple-Kalender gefunden.",
  )

class AppleCalendarParseException(
  detail: String = "",
  cause: Throwable? = null,
) : AppleCalDavException(
    message = if (detail.isEmpty()) "Apple calendar parsing failed" else "Apple calendar parsing failed: $detail",
    errorCode = "APPLE_CALENDAR_PARSE_FAILED",
    userMessage = "Ein Apple-Kalender konnte nicht verarbeitet werden.",
    cause = cause,
  )

class AppleCalendarConflictException(
  cause: Throwable? = null,
) : AppleCalDavException(
    message = "Apple calendar rejected the event target or reported a calendar conflict",
    errorCode = "APPLE_CALENDAR_CONFLICT",
    userMessage = "Der Apple-Kalender konnte den Termin nicht anlegen. Bitte versuche es erneut.",
    cause = cause,
  )

class AppleCalendarResourceConflictException(
  cause: Throwable? = null,
) : AppleCalDavException(
    message = "Apple calendar rejected the event because the resource already exists",
    errorCode = "APPLE_CALENDAR_RESOURCE_CONFLICT",
    userMessage =
      "Der Termin konnte nicht angelegt werden, " +
        "da bereits ein Konflikt mit dieser Apple-Ressource besteht.",
    cause = cause,
  )

internal class AppleFreeBusyNotSupportedException(
  val httpStatusCode: Int? = null,
  cause: Throwable? = null,
) : AppleCalDavException(
    message =
      if (httpStatusCode != null) {
        "free-busy-query not supported by this CalDAV server (HTTP $httpStatusCode)"
      } else {
        "free-busy-query not supported by this CalDAV server"
      },
    errorCode = "FREE_BUSY_NOT_SUPPORTED",
    userMessage = "",
    cause = cause,
  )

internal class AppleFreeBusyBadRequestException(
  cause: Throwable? = null,
) : AppleCalDavException(
    message = "free-busy-query returned HTTP 400 (Bad Request), falling back to calendar-query",
    errorCode = "FREE_BUSY_BAD_REQUEST",
    userMessage = "",
    cause = cause,
  )
