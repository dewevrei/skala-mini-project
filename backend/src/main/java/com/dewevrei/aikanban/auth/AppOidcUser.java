package com.dewevrei.aikanban.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public final class AppOidcUser implements OidcUser, AuthenticatedUser, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long userId;
    private final Map<String, Object> claims;
    private final Collection<? extends GrantedAuthority> authorities;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public AppOidcUser(long userId, OidcUser delegate) {
        this.userId = userId;
        this.claims = new LinkedHashMap<>(delegate.getClaims());
        this.authorities = delegate.getAuthorities();
        this.idToken = delegate.getIdToken();
        this.userInfo = delegate.getUserInfo();
    }

    @Override public long userId() { return userId; }
    @Override public Map<String, Object> getClaims() { return claims; }
    @Override public Map<String, Object> getAttributes() { return claims; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public OidcIdToken getIdToken() { return idToken; }
    @Override public OidcUserInfo getUserInfo() { return userInfo; }
    @Override public String getName() { return String.valueOf(claims.get("sub")); }
}
