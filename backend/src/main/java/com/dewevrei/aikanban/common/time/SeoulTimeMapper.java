package com.dewevrei.aikanban.common.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class SeoulTimeMapper {

    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    public static final ZoneOffset OFFSET = ZoneOffset.ofHours(9);

    private SeoulTimeMapper() {
    }

    public static OffsetDateTime toApiTimestamp(LocalDateTime databaseTimestamp) {
        return databaseTimestamp == null ? null : databaseTimestamp.atZone(ZONE_ID).toOffsetDateTime();
    }

    public static LocalDateTime toDatabaseTimestamp(OffsetDateTime apiTimestamp) {
        return apiTimestamp == null ? null : apiTimestamp.atZoneSameInstant(ZONE_ID).toLocalDateTime();
    }
}
