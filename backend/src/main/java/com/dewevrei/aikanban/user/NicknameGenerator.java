package com.dewevrei.aikanban.user;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class NicknameGenerator {

    private static final int MAX_LENGTH = 255;
    private static final int UUID_LENGTH_WITH_SEPARATOR = 37;

    public String generate(String normalizedName) {
        String suffix = "-" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
        int maxNameCodePoints = MAX_LENGTH - UUID_LENGTH_WITH_SEPARATOR;
        int end = normalizedName.offsetByCodePoints(0,
                Math.min(normalizedName.codePointCount(0, normalizedName.length()), maxNameCodePoints));
        return normalizedName.substring(0, end) + suffix;
    }
}
