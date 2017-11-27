package gov.usgs.wma.mlrgateway;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

	@Value("${security.maintenanceRoles}")
	private String[] roles;

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		if (null == authentication || null == authentication.getAuthorities() || null == roles) {
			return false;
		}

		//This little bit of Java 8 voodoo compares the acceptable roles with the user's authorities and returns
		// true if there is a match on any of them.
		List<?> intersect = authentication.getAuthorities().stream()
				.filter(auth -> Arrays.stream(roles).anyMatch(role -> role.equalsIgnoreCase(auth.getAuthority())))
				.collect(Collectors.toList());

		if (intersect.isEmpty()) {
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
