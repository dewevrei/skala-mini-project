package com.dewevrei.aikanban.user;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dewevrei.aikanban.auth.AuthenticatedUser;
import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.api.SuccessCode;
import com.dewevrei.aikanban.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> me(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return success(SuccessCode.USER_READ, principal.userId());
    }

    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> updateNickname(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody UpdateNicknameRequest request) {
        UserResponse user = UserResponse.from(userService.updateNickname(principal.userId(),
                request == null ? null : request.nickname()));
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.USER_UPDATED, Map.of("user", user)));
    }

    private ResponseEntity<ApiResponse<Map<String, UserResponse>>> success(ApiCode code, long userId) {
        UserResponse user = UserResponse.from(userService.getUser(userId));
        return ResponseEntity.ok(ApiResponse.success(code, Map.of("user", user)));
    }
}
