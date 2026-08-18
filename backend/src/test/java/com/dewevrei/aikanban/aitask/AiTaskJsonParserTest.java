package com.dewevrei.aikanban.aitask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AiTaskJsonParserTest {
    private final AiTaskJsonParser parser = new AiTaskJsonParser();

    @Test
    void exact_JSON을_trim하고_배열_순서대로_변환한다() {
        var result = parser.parse("""
                {"tasks":[
                  {"title":" first ","description":" detail one ","priority":2},
                  {"title":"second","description":"detail two","priority":5}
                ]}
                """, "original");

        assertThat(result).containsExactly(
                new AiTaskItem("first", "detail one", 2),
                new AiTaskItem("second", "detail two", 5));
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    void missing_extra_empty_type_range_length_partial_truncated를_모두_거부한다(String json) {
        assertThatThrownBy(() -> parser.parse(json, "original"))
                .isInstanceOf(InvalidAiResponseException.class);
    }

    static Stream<String> invalidResponses() {
        String valid = "{\"title\":\"ok\",\"description\":\"detail\",\"priority\":1}";
        return Stream.of(
                "{}",
                "{\"tasks\":[],\"extra\":1}",
                "{\"tasks\":[]}",
                "{\"tasks\":[{\"title\":\"x\",\"description\":\"d\"}]}",
                "{\"tasks\":[{\"title\":\"x\",\"description\":\"d\",\"priority\":1,\"extra\":true}]}",
                "{\"tasks\":[{\"title\":\" \",\"description\":\"d\",\"priority\":1}]}",
                "{\"tasks\":[{\"title\":\"x\",\"description\":\" \",\"priority\":1}]}",
                "{\"tasks\":[{\"title\":\"x\",\"description\":\"d\",\"priority\":\"1\"}]}",
                "{\"tasks\":[{\"title\":\"x\",\"description\":\"d\",\"priority\":1.0}]}",
                "{\"tasks\":[{\"title\":\"x\",\"description\":\"d\",\"priority\":0}]}",
                "{\"tasks\":[{\"title\":\"x\",\"description\":\"d\",\"priority\":6}]}",
                "{\"tasks\":[{\"title\":\"" + "x".repeat(201) + "\",\"description\":\"d\",\"priority\":1}]}",
                "{\"tasks\":[{\"title\":\"x\",\"description\":\"" + "d".repeat(4990) + "\",\"priority\":1}]}",
                "{\"tasks\":[" + valid + ",{" + "\"title\":\"bad\"}]}",
                "{\"tasks\":[" + valid);
    }
}
