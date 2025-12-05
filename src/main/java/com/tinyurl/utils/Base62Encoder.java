package com.tinyurl.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.tinyurl.ApplicationConstants.BASE;
import static com.tinyurl.ApplicationConstants.BASE62_ALPHABET;

/**
 * Base62Encoder
 * * A utility class to encode integers/longs into Base62 strings and decode them back.
 * Base62 encoding is commonly used for URL shortening (e.g., bit.ly) to convert
 * database IDs into short, URL-friendly strings.
 * * Character Set: 0-9, A-Z, a-z
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
        log.info("Encoding value={}", value);
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative.");
        }

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(BASE62_ALPHABET.charAt(remainder));
            value /= BASE;
        }

        // The loop builds the string in reverse order (least significant digit first),
        // so we reverse it to get the correct Base62 representation.
        return sb.reverse().toString();
    }

    /**
     * Main method for demonstration and testing.
     */
//    public static void main(String[] args) {
//        System.out.println("--- Base62 Encoder/Decoder Test ---\n");
//
//        // Test Cases
//        long[] testValues = {0, 1, 61, 62, 12345, 652024909219758082L, Long.MAX_VALUE};
//
//        for (long original : testValues) {
//            try {
//                // Encode
//                String encoded = encode(original);
//
//                // Decode
//
//                System.out.printf("Original: %-20d | Encoded: %-10s\n", original, encoded);
//
//            } catch (Exception e) {
//                System.err.println("Error processing " + original + ": " + e.getMessage());
//            }
//        }
//    }
}