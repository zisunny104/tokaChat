package dev.toka.chat;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Emoji the game's font cannot draw show up as boxes. The server cannot ask the client which glyphs it has, so every emoji is
 * written out as its name (":rose:") instead; Discord's own custom emoji already carry one.
 */
final class Emoji {
    private static final Pattern CUSTOM = Pattern.compile("<a?:([A-Za-z0-9_]{2,32}):\\d+>");

    private Emoji() {}

    static String plain(String text) {
        text = CUSTOM.matcher(text).replaceAll(":$1:");
        StringBuilder out = new StringBuilder();
        text.codePoints().forEach(code -> {
            if (invisible(code)) return;
            if (!pictograph(code)) { out.appendCodePoint(code); return; }
            String name = Character.getName(code);
            out.append(name == null ? ":emoji:" : ":" + name.toLowerCase(Locale.ROOT).replace(' ', '_') + ":");
        });
        return out.toString();
    }

    /** Joiners, variation selectors and skin tones only change how the emoji next to them looks. */
    private static boolean invisible(int code) {
        return code == 0x200D || code == 0xFE0F || code == 0xFE0E || code == 0x20E3 || (code >= 0x1F3FB && code <= 0x1F3FF) || (code >= 0xE0020 && code <= 0xE007F);
    }

    /** The emoji planes, and the few older symbols that Discord turns into emoji by themselves. */
    private static boolean pictograph(int code) {
        return (code >= 0x1F000 && code <= 0x1FAFF) || code == 0x2705 || code == 0x274C || code == 0x2764 || code == 0x2B50 || code == 0x2728 || code == 0x26A0 || code == 0x2757;
    }
}
