package de.stammtischHub.terminPilot.provider.apple

import de.stammtischHub.terminPilot.config.AppleCalDavProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

@Component
class CalDavHttpClient(
  private val properties: AppleCalDavProperties,
) {
  private val log = LoggerFactory.getLogger(CalDavHttpClient::class.java)

  private val httpClient: HttpClient by lazy {
    HttpClient
      .newBuilder()
      .connectTimeout(properties.connectTimeout)
      .build()
  }

  fun propfind(
    url: String,
    email: String,
    password: String,
    depth: String,
    body: String,
  ): String = execute("PROPFIND", url, email, password, depth, body)

  fun report(
    url: String,
    email: String,
    password: String,
    depth: String = "1",
    body: String,
  ): String = execute("REPORT", url, email, password, depth, body)

  fun putCalendarObject(
    url: String,
    email: String,
    password: String,
    body: String,
    ifNoneMatch: String = "*",
  ) {
    executePut(url = url, email = email, password = password, body = body, ifNoneMatch = ifNoneMatch)
  }

  fun resolveUrl(
    baseUrl: String,
    href: String,
  ): String {
    val trimmed = href.trim()
    return when {
      trimmed.startsWith("https://") || trimmed.startsWith("http://") -> {
        trimmed
      }

      trimmed.startsWith("/") -> {
        val uri = URI.create(baseUrl)
        val port = if (uri.port in listOf(80, 443, -1)) "" else ":${uri.port}"
        "${uri.scheme}://${uri.host}$port$trimmed"
      }

      else -> {
        "$baseUrl/$trimmed"
      }
    }
  }

  private fun execute(
    method: String,
    url: String,
    email: String,
    password: String,
    depth: String,
    body: String,
  ): String {
    val request =
      HttpRequest
        .newBuilder()
        .uri(URI.create(url))
        .header("Authorization", buildBasicAuth(email, password))
        .header("Content-Type", "application/xml; charset=utf-8")
        .header("Depth", depth)
        .method(method, HttpRequest.BodyPublishers.ofString(body, Charsets.UTF_8))
        .timeout(properties.readTimeout)
        .build()

    val response: HttpResponse<String> =
      try {
        httpClient.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
      } catch (e: IOException) {
        throw AppleCalDavUnavailableException(cause = e)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw AppleCalDavUnavailableException(cause = e)
      }

    return mapResponse(response)
  }

  private fun executePut(
    url: String,
    email: String,
    password: String,
    body: String,
    ifNoneMatch: String,
  ) {
    val request =
      HttpRequest
        .newBuilder()
        .uri(URI.create(url))
        .header("Authorization", buildBasicAuth(email, password))
        .header("Content-Type", "text/calendar; charset=utf-8")
        .header("If-None-Match", ifNoneMatch)
        .method("PUT", HttpRequest.BodyPublishers.ofString(body, Charsets.UTF_8))
        .timeout(properties.readTimeout)
        .build()

    val response: HttpResponse<String> =
      try {
        httpClient.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
      } catch (e: IOException) {
        throw AppleCalDavUnavailableException(cause = e)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw AppleCalDavUnavailableException(cause = e)
      }

    when (response.statusCode()) {
      in 200..299 -> return
      401, 403 -> throw AppleAuthenticationException(httpStatusCode = response.statusCode())
      409 -> throw AppleCalendarConflictException()
      412 -> throw AppleCalendarResourceConflictException()
      in 500..599 -> throw AppleCalDavUnavailableException(httpStatusCode = response.statusCode())
      else -> throw AppleCalDavUnavailableException(httpStatusCode = response.statusCode())
    }
  }

  private fun mapResponse(response: HttpResponse<String>): String =
    when (response.statusCode()) {
      200, 207 -> response.body() ?: ""
      401, 403 -> throw AppleAuthenticationException()
      405, 501, 415 -> throw AppleFreeBusyNotSupportedException(httpStatusCode = response.statusCode())
      409, 412 -> throw AppleCalendarConflictException()
      in 500..599 -> throw AppleCalDavUnavailableException(httpStatusCode = response.statusCode())
      else -> throw AppleCalDavUnavailableException(httpStatusCode = response.statusCode())
    }

  private fun buildBasicAuth(
    email: String,
    password: String,
  ): String = "Basic " + Base64.getEncoder().encodeToString("$email:$password".toByteArray(Charsets.UTF_8))
}
