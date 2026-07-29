package de.stammtischHub.terminPilot.provider.apple

import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import javax.xml.parsers.DocumentBuilderFactory

@Component
class CalDavXmlParser {
  companion object {
    const val DAV_NS = "DAV:"
    const val CALDAV_NS = "urn:ietf:params:xml:ns:caldav"
  }

  private val factory: DocumentBuilderFactory by lazy {
    DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
      setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
      setFeature("http://xml.org/sax/features/external-general-entities", false)
      setFeature("http://xml.org/sax/features/external-parameter-entities", false)
      setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
      isExpandEntityReferences = false
      isXIncludeAware = false
    }
  }

  fun extractPrincipalHref(body: String): String {
    val doc = parse(body)
    return childHref(doc, DAV_NS, "current-user-principal")
      ?: throw AppleCalendarDiscoveryException("No current-user-principal href in PROPFIND response")
  }

  fun extractCalendarHomeHref(body: String): String {
    val doc = parse(body)
    return childHref(doc, CALDAV_NS, "calendar-home-set")
      ?: throw AppleCalendarDiscoveryException("No calendar-home-set href in PROPFIND response")
  }

  fun extractCalendarCollections(body: String): List<CalendarCollection> {
    val doc = parse(body)
    val responses = doc.getElementsByTagNameNS(DAV_NS, "response")
    val result = mutableListOf<CalendarCollection>()

    for (i in 0 until responses.length) {
      val resp = responses.item(i) as? Element ?: continue
      val href = textOf(resp, DAV_NS, "href") ?: continue

      val resourceType = firstDescendant(resp, DAV_NS, "resourcetype") ?: continue
      val isCalendar = resourceType.getElementsByTagNameNS(CALDAV_NS, "calendar").length > 0
      if (!isCalendar) continue

      val compSet = firstDescendant(resp, CALDAV_NS, "supported-calendar-component-set")
      val supportsVEvent = supportsComponent(compSet, "VEVENT")
      if (!supportsVEvent) continue

      val displayName = textOf(resp, DAV_NS, "displayname")?.takeIf { it.isNotBlank() } ?: href
      result += CalendarCollection(href = href.trim(), displayName = displayName.trim())
    }

    return result
  }

  fun extractCalendarData(body: String): List<String> {
    val doc = parse(body)
    val nodes = doc.getElementsByTagNameNS(CALDAV_NS, "calendar-data")
    return (0 until nodes.length)
      .mapNotNull {
        nodes
          .item(it)
          ?.textContent
          ?.trim()
          ?.takeIf { s -> s.isNotEmpty() }
      }
  }

  internal fun parse(body: String): Document =
    factory
      .newDocumentBuilder()
      .apply { setErrorHandler(null) }
      .parse(InputSource(body.byteInputStream(Charsets.UTF_8)))

  private fun childHref(
    doc: Document,
    parentNs: String,
    parentLocalName: String,
  ): String? {
    val parents = doc.getElementsByTagNameNS(parentNs, parentLocalName)
    for (i in 0 until parents.length) {
      val parent = parents.item(i) as? Element ?: continue
      val hrefs = parent.getElementsByTagNameNS(DAV_NS, "href")
      val text = hrefs.item(0)?.textContent?.trim()
      if (!text.isNullOrEmpty()) return text
    }
    return null
  }

  private fun textOf(
    parent: Element,
    ns: String,
    localName: String,
  ): String? = firstDescendant(parent, ns, localName)?.textContent?.trim()

  private fun firstDescendant(
    parent: Element,
    ns: String,
    localName: String,
  ): Element? = parent.getElementsByTagNameNS(ns, localName).item(0) as? Element

  private fun supportsComponent(
    compSet: Element?,
    name: String,
  ): Boolean {
    compSet ?: return true // if server omits the property, assume it supports VEVENT
    val comps = compSet.getElementsByTagNameNS(CALDAV_NS, "comp")
    for (i in 0 until comps.length) {
      val comp = comps.item(i) as? Element ?: continue
      if (comp.getAttribute("name").uppercase() == name.uppercase()) return true
    }
    return false
  }

  data class CalendarCollection(
    val href: String,
    val displayName: String,
  )
}
