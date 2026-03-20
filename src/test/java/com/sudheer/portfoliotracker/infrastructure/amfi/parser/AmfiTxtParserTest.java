package com.sudheer.portfoliotracker.infrastructure.amfi.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class AmfiTxtParserTest {
    private final AmfiTxtParser parser = new AmfiTxtParser();

    @Test
    void shouldParseValidAmfiNavFile() {
        String payload =
                "Scheme Code|ISIN Div Payout|ISIN Div Reinvestment|Scheme Name|Net Asset Value|Repurchase Price|Sale Price|Date\n" +
                        "100033|INF109K01AH8|INF109K01AI6|Sample Fund|123.4567|123.4567|123.4567|10-Jan-2026\n" +
                        "100034|INF109K01AJ4|INF109K01AK2|Sample Fund 2|98.1234|98.1234|98.1234|10-Jan-2026\n";

        ParsedAmfiData data = parser.parse(stream(payload));
        assertEquals(2, data.getNavRecords().size());

        ParsedNavRecord first = data.getNavRecords().get(0);
        assertEquals("100033", first.getSchemeCode());
        assertEquals("123.4567", first.getNavValue());
        assertEquals("10-Jan-2026", first.getNavDate());

    }

    @Test
    void shouldFailIfNoValidNavRecordsFound() {
        String payload =
                "Scheme Code|ISIN|ISIN|Scheme Name|NAV|Rep|Sale|Date\n" +
                        "|||||||\n" +
                        "BAD|DATA|ONLY\n";

        IllegalStateException ex = assertThrows(IllegalStateException.class, ()->
                parser.parse(stream(payload))
        );
        assertTrue(ex.getMessage().contains("no valid NAV records"));
    }

    @Test
    void shouldIgnoreRowsWithInvalidDataOrNav() {
        String payload = "Scheme Code|ISIN Div Payout|ISIN Div Reinvestment|Scheme Name|Net Asset Value|Repurchase Price|Sale Price|Date\n" +
                "100033|INF|INF|Bad Nav|ABC|110|110|10-Jan-2026\n" +
                "100034|INF109K01AJ4|INF109K01AK2|Sample Fund 2|98.1234|98.1234|98.1234|BAD-DATE\n";
        IllegalStateException ex = assertThrows(IllegalStateException.class, ()->
                parser.parse(stream(payload))
        );
        assertTrue(ex.getMessage().contains("no valid NAV records"));
    }

    private ByteArrayInputStream stream(String payload){
        return new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
    }
}
