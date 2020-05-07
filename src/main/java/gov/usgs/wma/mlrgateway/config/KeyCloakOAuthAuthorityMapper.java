package gov.usgs.wma.mlrgateway.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

@Component
@SuppressWarnings("unchecked")
public class KeyCloakOAuthAuthorityMapper implements GrantedAuthoritiesMapper {
	public static final String AUTHORITIES_CLAIM = "authorities";

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
		Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

		authorities.forEach(authority -> {
			if (OidcUserAuthority.class.isInstance(authority)) {
				OidcUserAuthority oidcUserAuthority = (OidcUserAuthority)authority;
				OidcIdToken idToken = oidcUserAuthority.getIdToken();
				List<String> authList = idToken.getClaim(AUTHORITIES_CLAIM);
				
				if(authList != null && !authList.isEmpty()) {
					for(String auth : authList) {
						mappedAuthorities.add(new SimpleGrantedAuthority(auth));
					}
				}
			} else if (OAuth2UserAuthority.class.isInstance(authority)) {
				OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority)authority;

				Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();
				Object authObject = userAttributes.get(AUTHORITIES_CLAIM);

				if(authObject != null && authObject instanceof Collection) {
					List<String> authList = new ArrayList<String>((Collection<String>)authObject);
					if(authList != null && !authList.isEmpty()) {
						for(String auth : authList) {
							mappedAuthorities.add(new SimpleGrantedAuthority(auth));
						}
					}
				}				
			}
		});

		return mappedAuthorities;
	}

}