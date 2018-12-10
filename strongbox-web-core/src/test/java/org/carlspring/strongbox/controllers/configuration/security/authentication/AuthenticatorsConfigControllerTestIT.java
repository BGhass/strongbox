package org.carlspring.strongbox.controllers.configuration.security.authentication;

import org.carlspring.strongbox.authentication.api.Authenticator;
import org.carlspring.strongbox.authentication.api.impl.xml.JwtAuthenticator;
import org.carlspring.strongbox.authentication.api.impl.xml.PasswordAuthenticator;
import org.carlspring.strongbox.authentication.api.impl.xml.SecurityTokenAuthenticator;
import org.carlspring.strongbox.authentication.registry.AuthenticatorsRegistry;
import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.carlspring.strongbox.CustomMatchers.equalByToString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Przemyslaw Fusik
 * @author Pablo Tirado
 */
@IntegrationTest
@ExtendWith(SpringExtension.class)
@Execution(CONCURRENT)
public class AuthenticatorsConfigControllerTestIT
        extends RestAssuredBaseTest
{

    private static List<Authenticator> originalRegistryList;

    private static final List<Authenticator> registryList = Arrays.asList(new OrientDbAuthenticator(),
                                                                          new LdapAuthenticator());

    @Inject
    private AuthenticatorsRegistry authenticatorsRegistry;

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();
        setContextBaseUrl(getContextBaseUrl() + "/api/configuration");
        
        Iterator<Authenticator> iterator = authenticatorsRegistry.iterator();
        originalRegistryList = Lists.newArrayList(iterator);
        authenticatorsRegistry.reload(registryList);
    }

    @AfterEach
    public void afterEveryTest() {
        authenticatorsRegistry.reload(originalRegistryList);
    }

    @Test
    public void registryShouldReturnExpectedInitialArray()
    {
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl() + "/authenticators/")
               .peek()
               .then()
               .body("authenticators.authenticator[0].index",
                     equalByToString(0))
               .body("authenticators.authenticator[0].name",
                     CoreMatchers.equalTo(OrientDbAuthenticator.class.getName()))
               .body("authenticators.authenticator[1].index",
                     equalByToString(1))
               .body("authenticators.authenticator[1].name",
                     CoreMatchers.equalTo(LdapAuthenticator.class.getName()))
               .body("authenticators.authenticator.size()", is(2))
               .statusCode(HttpStatus.OK.value());
    }

    private void registryShouldBeReloadable(String acceptHeader)
    {
        // Registry should have form setup in test, first
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl() + "/authenticators/")
               .peek()
               .then()
               .body("authenticators.authenticator[0].index",
                     equalByToString(0))
               .body("authenticators.authenticator[0].name",
                     CoreMatchers.equalTo(OrientDbAuthenticator.class.getName()))
               .body("authenticators.authenticator[1].index",
                     equalByToString(1))
               .body("authenticators.authenticator[1].name",
                     CoreMatchers.equalTo(LdapAuthenticator.class.getName()))
               .body("authenticators.authenticator.size()", is(2))
               .statusCode(HttpStatus.OK.value());

        // Reorder elements
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(acceptHeader)
               .when()
               .put(getContextBaseUrl() + "/authenticators/reorder/0/1")
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(containsString(AuthenticatorsConfigController.SUCCESSFUL_REORDER));

        // Confirm they are re-ordered
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl() + "/authenticators/")
               .peek()
               .then()
               .body("authenticators.authenticator[0].index",
                     equalByToString(0))
               .body("authenticators.authenticator[0].name",
                     CoreMatchers.equalTo(LdapAuthenticator.class.getName()))
               .body("authenticators.authenticator[1].index",
                     equalByToString(1))
               .body("authenticators.authenticator[1].name",
                     CoreMatchers.equalTo(OrientDbAuthenticator.class.getName()))
               .body("authenticators.authenticator.size()", is(2))
               .statusCode(HttpStatus.OK.value());

        // Reload registry
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(acceptHeader)
               .when()
               .put(getContextBaseUrl() + "/authenticators/reload")
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(containsString(AuthenticatorsConfigController.SUCCESSFUL_RELOAD));

        // Registry should be reloaded
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl() + "/authenticators/")
               .peek()
               .then()
               .body("authenticators.authenticator[0].index",
                     equalByToString(0))
               .body("authenticators.authenticator[0].name",
                     CoreMatchers.equalTo(PasswordAuthenticator.class.getName()))
               .body("authenticators.authenticator[1].index",
                     equalByToString(1))
               .body("authenticators.authenticator[1].name",
                     CoreMatchers.equalTo(JwtAuthenticator.class.getName()))
               .body("authenticators.authenticator[2].name",
                     CoreMatchers.equalTo(SecurityTokenAuthenticator.class.getName()))
               .body("authenticators.authenticator[3].name",
                     CoreMatchers.equalTo(org.carlspring.strongbox.authentication.api.impl.ldap.LdapAuthenticator.class.getName()))               
               .body("authenticators.authenticator.size()", is(4))
               .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void registryShouldBeReloadableWithResponseInJson()
    {
        registryShouldBeReloadable(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    public void registryShouldBeReloadableWithResponseInText()
    {
        registryShouldBeReloadable(MediaType.TEXT_PLAIN_VALUE);
    }

    private void registryShouldBeAbleToReorderElement(String acceptHeader)
    {
        // when
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .accept(acceptHeader)
               .when()
               .put(getContextBaseUrl() + "/authenticators/reorder/0/1")
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(containsString(AuthenticatorsConfigController.SUCCESSFUL_REORDER));

        // then
        given().contentType(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl() + "/authenticators/")
               .peek()
               .then()
               .body("authenticators.authenticator[0].index",
                     equalByToString(0))
               .body("authenticators.authenticator[0].name",
                     CoreMatchers.equalTo(LdapAuthenticator.class.getName()))
               .body("authenticators.authenticator[1].index",
                     equalByToString(1))
               .body("authenticators.authenticator[1].name",
                     CoreMatchers.equalTo(OrientDbAuthenticator.class.getName()))
               .body("authenticators.authenticator.size()", is(2))
               .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void registryShouldBeAbleToReorderElementsWithResponseInJson()
    {
        registryShouldBeAbleToReorderElement(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    public void registryShouldBeAbleToReorderElementsWithResponseInText()
    {
        registryShouldBeAbleToReorderElement(MediaType.TEXT_PLAIN_VALUE);
    }

    private static class OrientDbAuthenticator
            implements Authenticator
    {

        @Nonnull
        @Override
        public AuthenticationProvider getAuthenticationProvider()
        {
            return new AuthenticationProvider()
            {
                @Override
                public Authentication authenticate(Authentication authentication)
                        throws AuthenticationException
                {
                    return authentication;
                }

                @Override
                public boolean supports(Class<?> authentication)
                {
                    return true;
                }
            };

        }
    }

    private static class LdapAuthenticator
            implements Authenticator
    {

        @Nonnull
        @Override
        public AuthenticationProvider getAuthenticationProvider()
        {
            return new AuthenticationProvider()
            {
                @Override
                public Authentication authenticate(Authentication authentication)
                        throws AuthenticationException
                {
                    return authentication;
                }

                @Override
                public boolean supports(Class<?> authentication)
                {
                    return true;
                }
            };
        }
    }

}
