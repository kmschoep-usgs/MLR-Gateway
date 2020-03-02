package gov.usgs.wma.mlrgateway;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
public class PermissionEvaluatorImplTest {

	PermissionEvaluatorImpl permissionEvaluatorImpl = new PermissionEvaluatorImpl();
	//The @WithMockUser way of setting roles prefixes them with "ROLE_"
	String[] roles = Stream.of("ROLE_DOG", "ROLE_COW").toArray(String[]::new);

	@Test
	@WithMockUser(username="mlr",roles={"LDM"})
	public void npeTestRoles() {
		ReflectionTestUtils.setField(permissionEvaluatorImpl, "roles", null);
		assertFalse(permissionEvaluatorImpl.hasPermission(SecurityContextHolder.getContext().getAuthentication(), null, null));
	}

	@Test
	public void npeTestAuthentication() {
		ReflectionTestUtils.setField(permissionEvaluatorImpl, "roles", roles);
		assertFalse(permissionEvaluatorImpl.hasPermission(null, null, null));
	}

	@Test
	@WithMockUser(username="mlr")
	public void npeTestAuthorities() {
		ReflectionTestUtils.setField(permissionEvaluatorImpl, "roles", roles);
		assertFalse(permissionEvaluatorImpl.hasPermission(SecurityContextHolder.getContext().getAuthentication(), null, null));
	}

	@Test
	public void npeTestAuthoritiesAndRoles() {
		ReflectionTestUtils.setField(permissionEvaluatorImpl, "roles", null);
		assertFalse(permissionEvaluatorImpl.hasPermission(SecurityContextHolder.getContext().getAuthentication(), null, null));
	}

	@Test
	@WithMockUser(username="mlr",roles={"LDM","DONKEY"})
	public void notAuthorizedTest() {
		ReflectionTestUtils.setField(permissionEvaluatorImpl, "roles", roles);
		assertFalse(permissionEvaluatorImpl.hasPermission(SecurityContextHolder.getContext().getAuthentication(), null, null));
	}

	@Test
	@WithMockUser(username="mlr",roles={"LDM","DONKEY","COW"})
	public void oneMatchTest() {
		ReflectionTestUtils.setField(permissionEvaluatorImpl, "roles", roles);
		assertTrue(permissionEvaluatorImpl.hasPermission(SecurityContextHolder.getContext().getAuthentication(), null, null));
	}

	@Test
	@WithMockUser(username="mlr",roles={"LDM","DONKEY","cow"})
	public void DoNotMatchOneWrongCaseTest() {
		ReflectionTestUtils.setField(permissionEvaluatorImpl, "roles", roles);
		assertFalse(permissionEvaluatorImpl.hasPermission(SecurityContextHolder.getContext().getAuthentication(), null, null));
	}
	
	@Test
	@WithMockUser(username="mlr",roles={"LDM","cow","DONKEY","dog"})
	public void DoNotMatchTwoWrongCaseTest() {
		ReflectionTestUtils.setField(permissionEvaluatorImpl, "roles", roles);
		assertFalse(permissionEvaluatorImpl.hasPermission(SecurityContextHolder.getContext().getAuthentication(), null, null));
	}
	
	@Test
	@WithMockUser(username="mlr",roles={"LDM","COW","DONKEY","DOG"})
	public void twoMatchTest() {
		ReflectionTestUtils.setField(permissionEvaluatorImpl, "roles", roles);
		assertTrue(permissionEvaluatorImpl.hasPermission(SecurityContextHolder.getContext().getAuthentication(), null, null));
	}

	@Test
	public void alwaysFalse() {
		assertFalse(permissionEvaluatorImpl.hasPermission(null, null, null, null));
	}

}
