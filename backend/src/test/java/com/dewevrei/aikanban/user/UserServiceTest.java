package com.dewevrei.aikanban.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.domain.User;
import com.dewevrei.aikanban.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock NicknameGenerator nicknameGenerator;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, nicknameGenerator);
        lenient().when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 최초_로그인은_Google_profile을_정규화해_사용자를_생성한다() {
        when(userRepository.findByGoogleId("google-1")).thenReturn(Optional.empty());
        when(nicknameGenerator.generate("홍길동")).thenReturn("홍길동-550e8400-e29b-41d4-a716-446655440000");

        User result = userService.synchronizeGoogleUser(
                new GoogleProfile(" google-1 ", " 홍길동 ", " USER@Example.com ", true));

        assertThat(result.getGoogleId()).isEqualTo("google-1");
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getEmail()).isEqualTo("USER@Example.com");
        assertThat(result.getNickname()).isEqualTo("홍길동-550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void 재로그인은_name과_email만_갱신하고_nickname은_보존한다() {
        User existing = user(1L, "google-1", "이전 이름", "old@example.com", "내 닉네임");
        when(userRepository.findByGoogleId("google-1")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());

        User result = userService.synchronizeGoogleUser(
                new GoogleProfile("google-1", "새 이름", "new@example.com", true));

        assertThat(result.getName()).isEqualTo("새 이름");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getNickname()).isEqualTo("내 닉네임");
        verify(nicknameGenerator, never()).generate(any());
    }

    @Test
    void 검증되지_않은_Google_email은_거부한다() {
        assertCode(ApiCode.OAUTH_EMAIL_INVALID, () -> userService.synchronizeGoogleUser(
                new GoogleProfile("google-1", "이름", "user@example.com", false)));
    }

    @Test
    void 기존_사용자와_email이_대소문자만_달라도_거부한다() {
        when(userRepository.findByGoogleId("google-1")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("USED@example.com")).thenReturn(true);

        assertCode(ApiCode.DUPLICATE_EMAIL, () -> userService.synchronizeGoogleUser(
                new GoogleProfile("google-1", "이름", "USED@example.com", true)));
    }

    @Test
    void nickname은_trim한_Unicode_문자열로_변경한다() {
        User current = user(1L, "google-1", "이름", "user@example.com", "이전");
        when(userRepository.findById(1L)).thenReturn(Optional.of(current));
        when(userRepository.findByNicknameIgnoreCase("새 닉네임😀")).thenReturn(Optional.empty());

        User result = userService.updateNickname(1L, "  새 닉네임😀  ");

        assertThat(result.getNickname()).isEqualTo("새 닉네임😀");
    }

    @Test
    void nickname의_대소문자_무시_중복을_거부한다() {
        User current = user(1L, "google-1", "이름", "user@example.com", "이전");
        User another = user(2L, "google-2", "다른 이름", "other@example.com", "NickName");
        when(userRepository.findById(1L)).thenReturn(Optional.of(current));
        when(userRepository.findByNicknameIgnoreCase("nickname")).thenReturn(Optional.of(another));

        assertCode(ApiCode.DUPLICATE_NICKNAME, () -> userService.updateNickname(1L, "nickname"));
    }

    @Test
    void 자신의_nickname과_대소문자가_같은_값은_허용한다() {
        User current = user(1L, "google-1", "이름", "user@example.com", "NickName");
        when(userRepository.findById(1L)).thenReturn(Optional.of(current));
        when(userRepository.findByNicknameIgnoreCase("nickname")).thenReturn(Optional.of(current));

        User result = userService.updateNickname(1L, "nickname");

        assertThat(result.getNickname()).isEqualTo("nickname");
    }

    @Test
    void nickname의_제어문자와_100자를_넘는_입력은_거부한다() {
        assertCode(ApiCode.INVALID_NICKNAME, () -> userService.updateNickname(1L, "잘못\n된 이름"));
        assertCode(ApiCode.INVALID_NICKNAME, () -> userService.updateNickname(1L, "가".repeat(101)));
    }

    @Test
    void nickname_후보가_계속_충돌하면_생성_실패로_처리한다() {
        when(userRepository.findByGoogleId("google-1")).thenReturn(Optional.empty());
        when(nicknameGenerator.generate("이름")).thenReturn("이미-사용중");
        when(userRepository.existsByNicknameIgnoreCase("이미-사용중")).thenReturn(true);

        assertCode(ApiCode.NICKNAME_GENERATION_FAILED, () -> userService.synchronizeGoogleUser(
                new GoogleProfile("google-1", "이름", "user@example.com", true)));
    }

    private User user(long id, String googleId, String name, String email, String nickname) {
        User user = new User(googleId, name, email, nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void assertCode(ApiCode code, Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(DomainException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
