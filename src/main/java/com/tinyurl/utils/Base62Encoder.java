package com.tinyurl.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.tinyurl.ApplicationConstants.BASE;
import static com.tinyurl.ApplicationConstants.BASE62_ALPHABET;

/**
 * A utility class to encode longs into Base62 strings
 * Character Set: 0-9, A-Z, a-z
 */
@Component
@Slf4j
public class Base62Encoder {

    /**
     * Encodes a positive long identifier into a Base62 string.
     *
     * @param value The non-negative long value to encode.
     * @return The Base62 encoded string.
     * @throws IllegalArgumentException if the value is negative.
     */
    public String encode(long value) {
        log.debug("Encoding value={}", value);
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative.");
        }

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(BASE62_ALPHABET.charAt(remainder));
            value /= BASE;
        }

        return sb.reverse().toString();
    }

}
