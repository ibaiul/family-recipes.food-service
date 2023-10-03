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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

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
        jwtProperties = createJwtProperties();
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
    void should_create_tokens_when_providing_username_and_roles() {
        jwtService.create(user.getUsername(), user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .as(StepVerifier::create)
                .assertNext(tokens -> {
                    assertThat(tokens.accessToken()).isNotEmpty();
                    assertThat(tokens.refreshToken()).isNotEmpty();
                })
                .verifyComplete();
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
    void should_not_decode_tokens_with_invalid_encryption() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        String accessToken = jwtService.create(user.getUsername()).block().accessToken();
        String invalidSignatureToken = accessToken.substring(0, accessToken.length() - 2);

        jwtService.getUserDetails(invalidSignatureToken)
                .as(StepVerifier::create)
                .verifyErrorMatches(t -> t instanceof InvalidJwtTokenException && t.getMessage().startsWith("Unable to decrypt token"));
    }

    @Test
    void should_fail_to_decode_access_token_when_revoked_due_to_allowing_single_active_session() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        String accessToken = jwtService.create(user.getUsername()).block().accessToken();

        await().atMost(jwtProperties.getAccessToken().expirationTime(), TimeUnit.SECONDS).untilAsserted(() -> {
            jwtService.create(user.getUsername()).block().accessToken();
            jwtService.getUserDetails(accessToken)
                    .as(StepVerifier::create)
                    .verifyErrorMatches(t -> t instanceof InvalidJwtTokenException && t.getMessage().startsWith("Token is revoked"));
        });
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
    void should_fail_to_refresh_token_when_revoked_due_to_allowing_single_active_session() {
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(user));
        String refreshToken = jwtService.create(user.getUsername()).block().refreshToken();
        jwtService.refresh(refreshToken).block();

        await().atMost(jwtProperties.getAccessToken().expirationTime(), TimeUnit.SECONDS).untilAsserted(() -> {
            jwtService.refresh(refreshToken)
                    .as(StepVerifier::create)
                    .verifyErrorMatches(t -> t instanceof InvalidJwtTokenException && t.getMessage().startsWith("Token is revoked"));
        });
    }

    private User createUser() {
        return new User("username", "password", Collections.singleton(new SimpleGrantedAuthority("ROLE_FAMILY_MEMBER")));
    }

    private JwtProperties createJwtProperties() {
        JwtProperties.AccessToken accessTokenProperties = new JwtProperties.AccessToken(2000);
        JwtProperties.RefreshToken refreshTokenProperties = new JwtProperties.RefreshToken(5000);
        JwtProperties.Encryption encryptionProperties = new JwtProperties.Encryption("ABCDEF0123456789ABCDEF0123456789");
        JwtProperties.Signature signatureProperties = new JwtProperties.Signature("ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789");
        JwtProperties properties = new JwtProperties();
        properties.setAccessToken(accessTokenProperties);
        properties.setRefreshToken(refreshTokenProperties);
        properties.setEncryption(encryptionProperties);
        properties.setSignature(signatureProperties);
        return properties;
    }
}
