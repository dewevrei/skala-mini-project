package com.dewevrei.aikanban.user;

import java.time.OffsetDateTime;

import com.dewevrei.aikanban.common.time.SeoulTimeMapper;
import com.dewevrei.aikanban.domain.User;

public record UserResponse(Long id, String name, String email, String nickname,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getNickname(),
                SeoulTimeMapper.toApiTimestamp(user.getCreatedAt()),
                SeoulTimeMapper.toApiTimestamp(user.getUpdatedAt()));
    }
}
