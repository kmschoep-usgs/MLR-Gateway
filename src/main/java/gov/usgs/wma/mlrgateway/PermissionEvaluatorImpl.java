package gov.usgs.wma.mlrgateway;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

	private static final Logger LOG = LoggerFactory.getLogger(PermissionEvaluator.class);
	@Value("${security.maintenanceRoles}")
	private String[] roles;

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		if (null == authentication || null == authentication.getAuthorities() || null == roles) {
			return false;
		}
		
		
		Set<String> blessedRoles = new HashSet<>(Arrays.asList(roles));
		LOG.debug("Spring says these are the Blessed Roles: {}", blessedRoles);

		Set<String> usersRoles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
		LOG.debug("User's Roles: {}", usersRoles);
		
		Set<String> intersectingRoles = new HashSet<>(blessedRoles);
		intersectingRoles.retainAll(usersRoles);
		LOG.debug("Intersecting Roles: {}", intersectingRoles);
		
		if (intersectingRoles.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
			Object permission) {
		return false;
	}

}
