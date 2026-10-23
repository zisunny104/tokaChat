package dev.toka.chat;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EmojiTest {
    @Test public void anEmojiBecomesItsName() { assertEquals("hi :rose:", Emoji.plain("hi 🌹")); }

    @Test public void joinersAndSelectorsDisappear() { assertEquals(":rose:", Emoji.plain("🌹️")); }

    @Test public void customDiscordEmojiKeepTheirName() { assertEquals("a :pepe: b", Emoji.plain("a <:pepe:123456789> b")); }

    @Test public void ordinaryTextAndChineseAreLeftAlone() { assertEquals("好的 ok →", Emoji.plain("好的 ok →")); }
}
