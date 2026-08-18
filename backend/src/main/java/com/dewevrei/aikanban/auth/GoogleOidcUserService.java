package com.dewevrei.aikanban.auth;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import com.dewevrei.aikanban.domain.User;
import com.dewevrei.aikanban.user.GoogleProfile;
import com.dewevrei.aikanban.user.UserService;

@Component
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final UserService userService;

    public GoogleOidcUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser google = delegate.loadUser(request);
        try {
            User user = userService.synchronizeGoogleUser(new GoogleProfile(google.getSubject(), google.getFullName(),
                    google.getEmail(), Boolean.TRUE.equals(google.getEmailVerified())));
            return new AppOidcUser(user.getId(), google);
        } catch (RuntimeException exception) {
            throw new OAuth2AuthenticationException(new OAuth2Error("oauth_user_sync_failed"),
                    "Google 사용자 정보를 처리하지 못했습니다.", exception);
        }
    }
}
