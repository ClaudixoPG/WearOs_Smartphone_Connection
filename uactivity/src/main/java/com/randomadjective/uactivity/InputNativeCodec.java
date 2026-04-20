package com.randomadjective.uactivity;

public final class InputNativeCodec {

    private static final String SEP_REGEX = "\\|";
    private static final String SEP = "|";

    public static final class MeasuredEvent {
        public final String eventId;
        public final String inputFamily;
        public final String rawMessage;

        public MeasuredEvent(String eventId, String inputFamily, String rawMessage) {
            this.eventId = eventId;
            this.inputFamily = inputFamily;
            this.rawMessage = rawMessage;
        }
    }

    public static final class RawEvent {
        public final String inputFamily;
        public final String rawMessage;

        public RawEvent(String inputFamily, String rawMessage) {
            this.inputFamily = inputFamily;
            this.rawMessage = rawMessage;
        }
    }

    private InputNativeCodec() { }

    public static MeasuredEvent parseMeasuredEvent(String message) {
        String[] parts = message.split(SEP_REGEX, 4);
        if (parts.length != 4) return null;
        if (!"EVT".equals(parts[0])) return null;

        return new MeasuredEvent(parts[1], parts[2], parts[3]);
    }

    public static RawEvent parseRawEvent(String message) {
        String[] parts = message.split(SEP_REGEX, 3);
        if (parts.length != 3) return null;
        if (!"RAW".equals(parts[0])) return null;

        return new RawEvent(parts[1], parts[2]);
    }

    public static String buildAck(String eventId, String phoneModel) {
        return "ACK" + SEP + eventId + SEP + phoneModel;
    }

    public static boolean isAck(String message) {
        return message != null && message.startsWith("ACK" + SEP);
    }
}