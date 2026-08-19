package com.dewevrei.aikanban.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.api.SuccessCode;
import com.dewevrei.aikanban.common.api.ApiResponse;

import tools.jackson.databind.ObjectMapper;

class SeoulTimeMapperTest {

    @Test
    void DB_local_datetime을_서울_offset으로_변환한다() {
        OffsetDateTime result = SeoulTimeMapper.toApiTimestamp(LocalDateTime.of(2026, 8, 18, 12, 30, 15));

        assertThat(result.toString()).isEqualTo("2026-08-18T12:30:15+09:00");
    }

    @Test
    void 다른_offset의_API_시간을_같은_순간의_서울_DB_시간으로_변환한다() {
        LocalDateTime result = SeoulTimeMapper.toDatabaseTimestamp(
                OffsetDateTime.parse("2026-08-18T03:30:15Z"));

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 8, 18, 12, 30, 15));
    }

    @Test
    void null은_null로_유지한다() {
        assertThat(SeoulTimeMapper.toApiTimestamp(null)).isNull();
        assertThat(SeoulTimeMapper.toDatabaseTimestamp(null)).isNull();
    }

    @Test
    void ApiResponse의_OffsetDateTime은_Jackson3_JSON에_서울_offset을_보존한다() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        OffsetDateTime timestamp = SeoulTimeMapper.toApiTimestamp(
                LocalDateTime.of(2026, 8, 18, 12, 30, 15, 123_456_000));

        String json = objectMapper.writeValueAsString(
                ApiResponse.success(SuccessCode.TASK_READ, new TimestampPayload(timestamp)));

        assertThat(json).contains("\"timestamp\":\"2026-08-18T12:30:15.123456+09:00\"");
    }

    private record TimestampPayload(OffsetDateTime timestamp) {
    }
}
