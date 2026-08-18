package com.dewevrei.aikanban.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class AppOAuth2User implements OAuth2User, AuthenticatedUser, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long userId;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public AppOAuth2User(long userId, OAuth2User delegate) {
        this.userId = userId;
        this.attributes = new LinkedHashMap<>(delegate.getAttributes());
        this.authorities = delegate.getAuthorities();
    }

    @Override public long userId() { return userId; }
    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getName() { return String.valueOf(attributes.get("sub")); }
}
