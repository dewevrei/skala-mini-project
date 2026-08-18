package com.dewevrei.aikanban.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NicknameGeneratorTest {

    private final NicknameGenerator generator = new NicknameGenerator();

    @Test
    void 전체_소문자_UUID를_붙이고_Unicode_문자_기준으로_255자를_지킨다() {
        String result = generator.generate("😀".repeat(300));

        assertThat(result.codePointCount(0, result.length())).isEqualTo(255);
        assertThat(result).matches("😀{218}-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
