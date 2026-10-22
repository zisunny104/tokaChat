package dev.toka.chat;

/**
 * Sorts the reason text the game core gives for a player leaving into a kind, so the wording can live in the language file.
 * The core's own English words are matched here once; everything shown to players comes from lang.
 */
public enum QuitReason {
    DISCONNECT, TIMEOUT, TRANSFER, KICKED, BANNED, BANNED_LOGIN, OTHER;

    public static QuitReason of(String raw) {
        if (raw == null) return OTHER;
        if (raw.equals("Disconnected from Server")) return DISCONNECT;
        if (raw.equals("Timed out")) return TIMEOUT;
        if (raw.startsWith("Transferred to ")) return TRANSFER;
        if (raw.startsWith("Kicked")) return KICKED;
        if (raw.startsWith("Banned")) return BANNED;
        if (raw.startsWith("You are banned")) return BANNED_LOGIN;
        return OTHER;
    }

    /** The administrator's reason after "Reason", or empty. */
    public static String detail(String raw) {
        if (raw == null) return "";
        int at = raw.indexOf("Reason");
        return at < 0 ? "" : raw.substring(at + "Reason".length()).replaceFirst("^[:\\s]+", "").strip();
    }

    /** The address a transfer went to, for example 1.2.3.4:19132. */
    public static String target(String raw) {
        return raw == null || !raw.startsWith("Transferred to ") ? "" : raw.substring("Transferred to ".length()).strip();
    }

    public String langKey() { return "quit." + name().toLowerCase(java.util.Locale.ROOT); }
}
