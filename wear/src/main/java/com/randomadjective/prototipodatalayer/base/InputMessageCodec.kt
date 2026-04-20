package com.randomadjective.prototipodatalayer.base

object InputMessageCodec {

    private const val SEP = "|"

    data class Ack(
        val eventId: String,
        val phoneModel: String
    )

    fun buildMeasuredEvent(
        eventId: String,
        inputFamily: String,
        rawMessage: String
    ): String {
        return "EVT$SEP$eventId$SEP$inputFamily$SEP$rawMessage"
    }

    fun buildRawEvent(
        inputFamily: String,
        rawMessage: String
    ): String {
        return "RAW$SEP$inputFamily$SEP$rawMessage"
    }

    fun buildAck(eventId: String, phoneModel: String): String {
        return "ACK$SEP$eventId$SEP$phoneModel"
    }

    fun parseAck(message: String): Ack? {
        val parts = message.split(SEP)
        if (parts.size < 3) return null
        if (parts[0] != "ACK") return null

        return Ack(parts[1], parts[2])
    }
}