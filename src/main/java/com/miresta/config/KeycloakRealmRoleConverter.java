package com.miresta.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

@Component
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        var authorities = new ArrayList<GrantedAuthority>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            var roles = (Collection<?>) realmAccess.get("roles");
            if (roles != null) {
                roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
            }
        }

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.forEach((clientId, data) -> {
                Map<?, ?> d = (Map<?, ?>) data;
                var roles = (Collection<?>) d.get("roles");
                if (roles != null) {
                    roles.forEach(r -> authorities.add(
                            new SimpleGrantedAuthority(clientId + ":" + r)
                    ));
                }
            });
        }

        // Optional: scopes como authorities
        var scopes = jwt.getClaimAsString("scope");
        if (scopes != null) {
            Arrays.stream(scopes.split(" "))
                    .forEach(s -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + s)));
        }

        return authorities;
    }
}
