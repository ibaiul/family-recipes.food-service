package eus.ibai.family.recipes.food.security;

import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.util.Collections;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private MapReactiveUserDetailsService userDetailsService;

    private JwtProperties jwtProperties;

    private JwtService jwtService;

    private User user;

    @BeforeEach
    void beforeEach() throws JOSEException {
        user = createUser();
        jwtProperties = createJwtProperties("ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789");
        jwtService = new JwtService(jwtProperties, userDetailsService);
        jwtService.init();
    }

    @Test
    void should_create_tokens_when_providing_username() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));

        jwtService.create(user.getUsername())
                .as(StepVerifier::create)
                .assertNext(tokens -> {
                    assertThat(tokens.accessToken()).isNotEmpty();
                    assertThat(tokens.refreshToken()).isNotEmpty();
                })
                .verifyComplete();
    }

    @Test
    void should_not_create_tokens_when_providing_non_existing_username() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.empty());

        jwtService.create(user.getUsername())
                .as(StepVerifier::create)
                .verifyError(UsernameNotFoundException.class);
    }

    @Test
    void should_decode_access_tokens() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        String accessToken = jwtService.create(user.getUsername()).block().accessToken();

        jwtService.getUserDetails(accessToken)
                .as(StepVerifier::create)
                .expectNext(Tuples.of(user.getUsername(), user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()))
                .verifyComplete();
    }

    @Test
    void should_decode_previously_issued_access_tokens_due_to_allowing_multiple_active_sessions() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        String initialAccessToken = jwtService.create(user.getUsername()).block().accessToken();
        jwtService.create(user.getUsername()).block().accessToken();

        jwtService.getUserDetails(initialAccessToken)
                .as(StepVerifier::create)
                .expectNext(Tuples.of(user.getUsername(), user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()))
                .verifyComplete();
    }

    @Test
    void should_not_decode_tokens_with_invalid_encryption() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        String accessToken = jwtService.create(user.getUsername()).block().accessToken();
        String invalidEncryptionToken = accessToken.substring(0, accessToken.length() - 2);

        jwtService.getUserDetails(invalidEncryptionToken)
                .as(StepVerifier::create)
                .verifyErrorMatches(t -> t instanceof InvalidJwtTokenException && t.getMessage().startsWith("Unable to decrypt token"));
    }

    @Test
    void should_not_decode_tokens_with_invalid_signature() throws JOSEException {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        JwtProperties otherJwtProperties = createJwtProperties("0000000000000000000000000000000000000000000000000000000000000000");
        JwtService otherJwtService = new JwtService(otherJwtProperties, userDetailsService);
        otherJwtService.init();
        String invalidSignatureToken = otherJwtService.create(user.getUsername()).block().accessToken();

        jwtService.getUserDetails(invalidSignatureToken)
                .as(StepVerifier::create)
                .verifyErrorMatches(t -> t instanceof InvalidJwtTokenException && t.getMessage().startsWith("Token signature is invalid"));
    }

    @Test
    void should_not_decode_tokens_that_have_expired() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        String accessToken = jwtService.create(user.getUsername()).block().accessToken();

        await().pollInterval(1, SECONDS).atLeast(jwtProperties.getAccessToken().expirationTime(), SECONDS).untilAsserted(() ->
                jwtService.getUserDetails(accessToken)
                        .as(StepVerifier::create)
                        .verifyErrorMatches(t -> t instanceof InvalidJwtTokenException && t.getMessage().startsWith("Token is expired")));
    }

    @Test
    void should_refresh_tokens() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        String refreshToken = jwtService.create(user.getUsername()).block().refreshToken();

        jwtService.refresh(refreshToken)
                .as(StepVerifier::create)
                .assertNext(tokens -> {
                    assertThat(tokens.accessToken()).isNotEmpty();
                    assertThat(tokens.refreshToken()).isNotEmpty();
                })
                .verifyComplete();
    }

    @Test
    void should_refresh_tokens_when_using_previously_issued_token_due_to_allowing_multiple_active_sessions() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        String initialRefreshToken = jwtService.create(user.getUsername()).block().refreshToken();
        jwtService.refresh(initialRefreshToken).block();

        jwtService.refresh(initialRefreshToken)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }


    @Test
    void should_not_refresh_tokens_when_using_expired_token() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        String refreshToken = jwtService.create(user.getUsername()).block().refreshToken();

        await().pollInterval(1, SECONDS).atLeast(jwtProperties.getRefreshToken().expirationTime(), SECONDS).untilAsserted(() ->
                jwtService.refresh(refreshToken)
                        .as(StepVerifier::create)
                        .verifyErrorMatches(t -> t instanceof InvalidJwtTokenException && t.getMessage().startsWith("Token is expired")));
    }

    private User createUser() {
        return new User("username", "password", Collections.singleton(new SimpleGrantedAuthority("ROLE_FAMILY_MEMBER")));
    }

    private JwtProperties createJwtProperties(String signatureKey) {
        JwtProperties.AccessToken accessTokenProperties = new JwtProperties.AccessToken(2);
        JwtProperties.RefreshToken refreshTokenProperties = new JwtProperties.RefreshToken(3);
        JwtProperties.Encryption encryptionProperties = new JwtProperties.Encryption("ABCDEF0123456789ABCDEF0123456789");
        JwtProperties.Signature signatureProperties = new JwtProperties.Signature(signatureKey);
        JwtProperties properties = new JwtProperties();
        properties.setAccessToken(accessTokenProperties);
        properties.setRefreshToken(refreshTokenProperties);
        properties.setEncryption(encryptionProperties);
        properties.setSignature(signatureProperties);
        return properties;
    }
}
