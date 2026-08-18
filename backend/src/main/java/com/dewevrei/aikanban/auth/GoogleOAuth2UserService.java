package com.dewevrei.aikanban.auth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.dewevrei.aikanban.domain.User;
import com.dewevrei.aikanban.user.GoogleProfile;
import com.dewevrei.aikanban.user.UserService;

@Component
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserService userService;

    public GoogleOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User google = delegate.loadUser(request);
        try {
            User user = userService.synchronizeGoogleUser(profile(google));
            return new AppOAuth2User(user.getId(), google);
        } catch (RuntimeException exception) {
            throw new OAuth2AuthenticationException(new OAuth2Error("oauth_user_sync_failed"),
                    "Google 사용자 정보를 처리하지 못했습니다.", exception);
        }
    }

    private GoogleProfile profile(OAuth2User google) {
        return new GoogleProfile(google.getAttribute("sub"), google.getAttribute("name"),
                google.getAttribute("email"), Boolean.TRUE.equals(google.getAttribute("email_verified")));
    }
}
