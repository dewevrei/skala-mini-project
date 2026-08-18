package com.dewevrei.aikanban.user;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.common.validation.UserInputValidator;
import com.dewevrei.aikanban.domain.User;
import com.dewevrei.aikanban.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int NICKNAME_ATTEMPTS = 10;

    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;

    public UserService(UserRepository userRepository, NicknameGenerator nicknameGenerator) {
        this.userRepository = userRepository;
        this.nicknameGenerator = nicknameGenerator;
    }

    @Transactional
    public User synchronizeGoogleUser(GoogleProfile profile) {
        if (profile == null) {
            throw new DomainException(ApiCode.OAUTH_PROFILE_INVALID);
        }
        String googleId = required(profile.googleId(), 255, ApiCode.OAUTH_PROFILE_INVALID);
        String name = required(profile.name(), 255, ApiCode.OAUTH_PROFILE_INVALID);
        String email = validateEmail(profile.email(), profile.emailVerified());

        return userRepository.findByGoogleId(googleId)
                .map(user -> updateExisting(user, name, email))
                .orElseGet(() -> createUser(googleId, name, email));
    }

    public User getUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ApiCode.AUTHENTICATION_REQUIRED));
    }

    @Transactional
    public User updateNickname(long userId, String requestedNickname) {
        String nickname = required(requestedNickname, 100, ApiCode.INVALID_NICKNAME);
        User user = getUser(userId);
        userRepository.findByNicknameIgnoreCase(nickname)
                .filter(found -> !found.getId().equals(user.getId()))
                .ifPresent(found -> { throw new DomainException(ApiCode.DUPLICATE_NICKNAME); });
        user.updateNickname(nickname);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException(ApiCode.DUPLICATE_NICKNAME, exception);
        }
    }

    private User updateExisting(User user, String name, String email) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(found -> !found.getId().equals(user.getId()))
                .ifPresent(found -> { throw new DomainException(ApiCode.DUPLICATE_EMAIL); });
        user.updateGoogleProfile(name, email);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException(ApiCode.DUPLICATE_EMAIL, exception);
        }
    }

    private User createUser(String googleId, String name, String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DomainException(ApiCode.DUPLICATE_EMAIL);
        }
        String nickname = null;
        for (int attempt = 0; attempt < NICKNAME_ATTEMPTS; attempt++) {
            String candidate = nicknameGenerator.generate(name);
            if (!userRepository.existsByNicknameIgnoreCase(candidate)) {
                nickname = candidate;
                break;
            }
        }
        if (nickname == null) {
            throw new DomainException(ApiCode.NICKNAME_GENERATION_FAILED);
        }
        try {
            return userRepository.saveAndFlush(new User(googleId, name, email, nickname));
        } catch (DataIntegrityViolationException exception) {
            String details = constraintDetails(exception);
            if (details.contains("uk_users_google_id") || details.contains("google_id")) {
                throw new DomainException(ApiCode.DUPLICATE_GOOGLE_ID, exception);
            }
            if (details.contains("uk_users_email") || details.contains("email")) {
                throw new DomainException(ApiCode.DUPLICATE_EMAIL, exception);
            }
            throw new DomainException(ApiCode.NICKNAME_GENERATION_FAILED, exception);
        }
    }

    private String validateEmail(String value, boolean verified) {
        if (!verified) {
            throw new DomainException(ApiCode.OAUTH_EMAIL_INVALID);
        }
        String email = required(value, 320, ApiCode.OAUTH_EMAIL_INVALID);
        if (!EMAIL.matcher(email).matches()) {
            throw new DomainException(ApiCode.OAUTH_EMAIL_INVALID);
        }
        return email;
    }

    private String required(String value, int maxCodePoints, ApiCode code) {
        return UserInputValidator.required(value, maxCodePoints, code);
    }

    private String constraintDetails(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        return String.valueOf(cause == null ? exception.getMessage() : cause.getMessage()).toLowerCase(Locale.ROOT);
    }
}
