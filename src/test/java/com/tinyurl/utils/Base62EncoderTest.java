package com.tinyurl.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Base62EncoderTest {

    @Test
    public void testEncode() {
        Base62Encoder base62Encoder = new Base62Encoder();
        long value = 653436189499457547L;
        String encoded = base62Encoder.encode(value);
        assertEquals("mGkAYBHPwp", encoded);
    }

    @Test
    public void testEncode_whenArgumentIsNegative() {
        Base62Encoder base62Encoder = new Base62Encoder();
        long value = -653436189499457547L;
        assertThrows(IllegalArgumentException.class, () -> base62Encoder.encode(value));
    }
}
