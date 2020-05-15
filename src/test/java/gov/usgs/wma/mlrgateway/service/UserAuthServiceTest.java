package gov.usgs.wma.mlrgateway.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes=UserAuthService.class)
public class UserAuthServiceTest {

    private final String TEST_CLIENT_ID = "test-client";
    private final String TEST_PRINCIPAL_NAME = "test-uid";

    @MockBean
    private OAuth2AuthorizedClientService mockOAuth2ClientService;

    @MockBean
	private OAuth2AuthorizedClientManager mockOAuth2ClientManager;

    @Autowired
    private UserAuthService userAuthService;
    
    @MockBean
    private OAuth2AuthenticationToken mockOAuthToken;

    @MockBean
    private OAuth2User mockOAuth2User;

    @MockBean
    private OAuth2AuthorizedClient mockOAuth2Client;

    @MockBean
    private OAuth2AccessToken mockAccessToken;

    @MockBean
    private OAuth2RefreshToken mockRefreshToken;

    private ClientRegistration clientReg;

    @BeforeEach
    public void setup() {
        clientReg = ClientRegistration.withRegistrationId(TEST_CLIENT_ID)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientId(TEST_CLIENT_ID)
            .clientSecret("secret")
            .redirectUriTemplate("template")
            .authorizationUri("test-authorization")
            .tokenUri("test-token").build();
        when(mockOAuthToken.getPrincipal()).thenReturn(mockOAuth2User);
        when(mockOAuthToken.getAuthorizedClientRegistrationId()).thenReturn(TEST_CLIENT_ID);
        when(mockOAuthToken.getName()).thenReturn(TEST_PRINCIPAL_NAME);
        when(mockOAuth2Client.getAccessToken()).thenReturn(mockAccessToken);
        when(mockOAuth2Client.getRefreshToken()).thenReturn(mockRefreshToken);
        when(mockOAuth2Client.getClientRegistration()).thenReturn(clientReg);
        when(mockAccessToken.getTokenValue()).thenReturn("mock-access");
        when(mockRefreshToken.getTokenValue()).thenReturn("mock-refresh");
        when(mockOAuth2ClientService.loadAuthorizedClient(TEST_CLIENT_ID, TEST_PRINCIPAL_NAME))
            .thenReturn(mockOAuth2Client);
    }
    
    @Test
    public void getOAuth2UserAttributeSuccessTest() {
        when(mockOAuth2User.getAttribute("test")).thenReturn("test-value");

        assertEquals("test-value", userAuthService.getOAuth2UserAttribute(mockOAuthToken, "test"));
        assertEquals(null, userAuthService.getOAuth2UserAttribute(mockOAuthToken, "undefined"));
    }

    @Test
    public void getOAuth2UserAttributeInvalidTest() {
        assertEquals(null, userAuthService.getOAuth2UserAttribute(null, "test"));
        assertEquals(null, userAuthService.getOAuth2UserAttribute(new UsernamePasswordAuthenticationToken("user", "password"), "test"));
    }

    @Test
    public void getUserEmailTest() {
        when(mockOAuth2User.getAttribute("email")).thenReturn("test@test.test");
        assertEquals("test@test.test", userAuthService.getUserEmail(mockOAuthToken));
    }

    @Test
    public void getUserNameTest() {
        when(mockOAuth2User.getAttribute("preferred_username")).thenReturn("test-user");
        assertEquals("test-user", userAuthService.getUserName(mockOAuthToken));
    }

    @Test
    public void getRefreshAuthorizedClientSuccessTest() {
        OAuth2AuthorizedClient mockRefreshedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken mockRefAccessToken = mock(OAuth2AccessToken.class);
        OAuth2RefreshToken mockRefRefreshToken = mock(OAuth2RefreshToken.class);
        when(mockRefAccessToken.getTokenValue()).thenReturn("mock-access-2");
        when(mockRefRefreshToken.getTokenValue()).thenReturn("mock-refresh-2");
        when(mockRefreshedClient.getAccessToken()).thenReturn(mockRefAccessToken);
        when(mockRefreshedClient.getRefreshToken()).thenReturn(mockRefRefreshToken);
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(mockRefreshedClient);

        OAuth2AuthorizedClient clientResult = userAuthService.getRefreshAuthorizedClient(mockOAuthToken);
        assertEquals(mockRefAccessToken.getTokenValue(), clientResult.getAccessToken().getTokenValue());
        assertEquals(mockRefRefreshToken.getTokenValue(), clientResult.getRefreshToken().getTokenValue());
    }

    @Test
    public void getRefreshAuthorizedErrorTest() {
        when(mockOAuth2ClientManager.authorize(any())).thenThrow(
            new ClientAuthorizationRequiredException(TEST_CLIENT_ID)
        );

        try {
            userAuthService.getRefreshAuthorizedClient(mockOAuthToken);
            fail("Expected ClientAuthorizationRequiredException but got no exception.");
        } catch(ClientAuthorizationRequiredException e) {
            assertTrue(e.getMessage().contains(TEST_CLIENT_ID));
        } catch(Exception e) {
            fail("Expected ClientAuthorizationRequiredException but got " + e.getClass().getName());
        }
    }

    @Test
    public void getRefreshAuthorizedNullResultTest() {
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(null);

        try {
            userAuthService.getRefreshAuthorizedClient(mockOAuthToken);
            fail("Expected ClientAuthorizationRequiredException but got no exception.");
        } catch(ClientAuthorizationRequiredException e) {
            assertTrue(e.getMessage().contains(TEST_CLIENT_ID));
        } catch(Exception e) {
            fail("Expected ClientAuthorizationRequiredException but got " + e.getClass().getName());
        }
    }

    @Test
    public void getRefreshAuthorizedNullClientTest() {
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(mockOAuth2Client);
        when(mockOAuth2ClientService.loadAuthorizedClient(TEST_CLIENT_ID, TEST_PRINCIPAL_NAME))
            .thenReturn(null);

        try {
            userAuthService.getRefreshAuthorizedClient(mockOAuthToken);
            fail("Expected ClientAuthorizationRequiredException but got no exception.");
        } catch(ClientAuthorizationRequiredException e) {
            assertTrue(e.getMessage().contains(TEST_CLIENT_ID));
        } catch(Exception e) {
            fail("Expected ClientAuthorizationRequiredException but got " + e.getClass().getName());
        }
    }

    @Test
    public void getRefreshAuthorizedNullAuthTest() {
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(mockOAuth2Client);

        assertEquals(null, userAuthService.getRefreshAuthorizedClient(null));
    }

    @Test
    public void validateTokenSuccessTest() {
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(mockOAuth2Client);

        try {
            userAuthService.validateToken(mockOAuthToken);
        } catch(Exception e) {
            fail("Expected no exception but got " + e.getClass().getName());
        }
    }

    @Test
    public void validateTokenInvalidTest() {
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(mockOAuth2Client);

        try {
            userAuthService.validateToken(null);
            fail("Expected ClientAuthorizationRequiredException but got no exception.");
        } catch(ClientAuthorizationRequiredException e) {
            assertTrue(e.getMessage().contains("Current user context is not oauth2 authenticated"));
        } catch(Exception e) {
            fail("Expected ClientAuthorizationRequiredException but got " + e.getClass().getName());
        }

        try {
            userAuthService.validateToken(new UsernamePasswordAuthenticationToken("test-user", "test-pass"));
            fail("Expected ClientAuthorizationRequiredException but got no exception.");
        } catch(ClientAuthorizationRequiredException e) {
            assertTrue(e.getMessage().contains("Current user context is not oauth2 authenticated"));
        } catch(Exception e) {
            fail("Expected ClientAuthorizationRequiredException but got " + e.getClass().getName());
        }
    }


    @Test
    public void validateTokenErrorTest() {
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(null);

        try {
            userAuthService.validateToken(mockOAuthToken);
            fail("Expected ClientAuthorizationRequiredException but got no exception.");
        } catch(ClientAuthorizationRequiredException e) {
            assertTrue(e.getMessage().contains(TEST_CLIENT_ID));
        } catch(Exception e) {
            fail("Expected ClientAuthorizationRequiredException but got " + e.getClass().getName());
        }
    }

    @Test
    public void getTokenValueSuccessTest() {
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(mockOAuth2Client);

        try {
            assertEquals("mock-access", userAuthService.getTokenValue(mockOAuthToken));
        } catch(Exception e) {
            fail("Expected no exception but got " + e.getClass().getName());
        }
    }

    @Test
    public void getTokenValueSuccessRefTest() {
        OAuth2AuthorizedClient mockRefreshedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken mockRefAccessToken = mock(OAuth2AccessToken.class);
        when(mockRefAccessToken.getTokenValue()).thenReturn("mock-access-2");
        when(mockRefreshedClient.getAccessToken()).thenReturn(mockRefAccessToken);
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(mockRefreshedClient);

        try {
            assertEquals("mock-access-2", userAuthService.getTokenValue(mockOAuthToken));
        } catch(Exception e) {
            fail("Expected no exception but got " + e.getClass().getName());
        }
    }

    @Test
    public void getTokenValueInvalidTest() {
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(mockOAuth2Client);

        assertEquals(null, userAuthService.getTokenValue(null));
        assertEquals(null, userAuthService.getTokenValue(new UsernamePasswordAuthenticationToken("test-user", "test-pass")));
    }

    @Test
    public void getTokenValueErrorTest() {
        when(mockOAuth2ClientManager.authorize(any())).thenReturn(null);

        try {
            userAuthService.getTokenValue(mockOAuthToken);
            fail("Expected ClientAuthorizationRequiredException but got no exception.");
        } catch(ClientAuthorizationRequiredException e) {
            assertTrue(e.getMessage().contains(TEST_CLIENT_ID));
        } catch(Exception e) {
            fail("Expected ClientAuthorizationRequiredException but got " + e.getClass().getName());
        }
    }
}