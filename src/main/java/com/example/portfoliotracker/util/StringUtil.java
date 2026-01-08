package com.example.portfoliotracker.util;

import java.nio.ByteBuffer;
import java.nio.charset.*;

public class StringUtil {
    public static Charset detectCharset(byte[] data) {

        if (data == null || data.length == 0) {
            return StandardCharsets.UTF_8;
        }

        // 1. UTF-8 BOM check
        if (data.length >= 3 &&
                (data[0] & 0xFF) == 0xEF &&
                (data[1] & 0xFF) == 0xBB &&
                (data[2] & 0xFF) == 0xBF) {

            return StandardCharsets.UTF_8;
        }

        // 2. Strict UTF-8 validation
        CharsetDecoder utf8Decoder =
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            utf8Decoder.decode(ByteBuffer.wrap(data));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException ignored) {
            // not UTF-
            throw new RuntimeException("Failed to decode data as UTF-8", ignored);
        }
    }
}
