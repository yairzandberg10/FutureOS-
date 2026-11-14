package com.future.navigation.data.siri

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * בניית בקשת SIRI 2.0 StopMonitoringRequest ופענוח התשובה, לפי התקן הרשמי
 * (OASIS SIRI - ר' התיעוד ב-SiriConfig.kt). נבחר XmlPullParser (מובנה
 * באנדרואיד, org.xmlpull.v1) על פני ספריית XML/SOAP חיצונית - כמו בשאר
 * הפרויקט (NetworkModule) הגישה היא מינימום תלויות, ופה זה פירוק שדות בודדים
 * מתוך עץ מקונן, לא מיפוי סכמה מלא.
 */
object SiriXml {
    private const val NAMESPACE = "http://www.siri.org.uk/siri"

    fun buildStopMonitoringRequest(stopId: String, apiKey: String, lineRef: String? = null): String {
        val timestamp = OffsetDateTime.now().toString()
        val lineRefXml = if (lineRef.isNullOrBlank()) "" else "<LineRef>${escape(lineRef)}</LineRef>"
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Siri xmlns="$NAMESPACE" version="2.0">
              <ServiceRequest>
                <RequestTimestamp>$timestamp</RequestTimestamp>
                <RequestorRef>${escape(apiKey)}</RequestorRef>
                <StopMonitoringRequest version="2.0">
                  <RequestTimestamp>$timestamp</RequestTimestamp>
                  <MonitoringRef>${escape(stopId)}</MonitoringRef>
                  $lineRefXml
                  <MaximumStopVisits>20</MaximumStopVisits>
                </StopMonitoringRequest>
              </ServiceRequest>
            </Siri>
        """.trimIndent()
    }

    /**
     * מפענח את כל MonitoredStopVisit בתשובה. עוקב אחרי הנתיב בעץ בעזרת מחסנית
     * שמות תגים (בלי namespace-prefix, ר' FEATURE_PROCESS_NAMESPACES) כדי
     * לדעת אם טקסט עלה שייך ל-MonitoredCall או לחלק אחר של ה-MonitoredVehicleJourney.
     */
    fun parseStopMonitoringResponse(xml: String): List<RealtimeArrival> {
        val results = mutableListOf<RealtimeArrival>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(xml))

        var inMonitoredCall = false
        var lineRef: String? = null
        var vehicleRef: String? = null
        var aimedDeparture: String? = null
        var expectedDeparture: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "MonitoredStopVisit" -> {
                        lineRef = null; vehicleRef = null; aimedDeparture = null; expectedDeparture = null
                    }
                    "MonitoredCall" -> inMonitoredCall = true
                    "LineRef" -> lineRef = readText(parser) ?: lineRef
                    "VehicleRef" -> vehicleRef = readText(parser) ?: vehicleRef
                    "AimedDepartureTime" -> if (inMonitoredCall) aimedDeparture = readText(parser)
                    "ExpectedDepartureTime" -> if (inMonitoredCall) expectedDeparture = readText(parser)
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "MonitoredCall" -> inMonitoredCall = false
                    "MonitoredStopVisit" -> {
                        results += RealtimeArrival(
                            lineRef = lineRef,
                            vehicleRef = vehicleRef,
                            aimedDepartureSeconds = aimedDeparture?.let(::secondsSinceMidnightJerusalem),
                            expectedDepartureSeconds = expectedDeparture?.let(::secondsSinceMidnightJerusalem)
                        )
                    }
                }
            }
            event = parser.next()
        }
        return results
    }

    /** קורא את התוכן הטקסטואלי של התג הנוכחי ומקדם את הפענוח לתג הסוגר שלו. */
    private fun readText(parser: XmlPullParser): String? {
        if (parser.isEmptyElementTag) return null
        var text: String? = null
        var event = parser.next()
        while (event != XmlPullParser.END_TAG) {
            if (event == XmlPullParser.TEXT) text = parser.text
            event = parser.next()
        }
        return text?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun secondsSinceMidnightJerusalem(isoDateTime: String): Int? = try {
        val zoned = OffsetDateTime.parse(isoDateTime).atZoneSameInstant(ZoneId.of("Asia/Jerusalem"))
        zoned.hour * 3600 + zoned.minute * 60 + zoned.second
    } catch (e: DateTimeParseException) {
        null
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
