package eus.ibai.family.recipes.food.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.nimbusds.jose.JOSEObjectType.JWT;
import static com.nimbusds.jose.JWSAlgorithm.HS512;
import static eus.ibai.family.recipes.food.util.Utils.maskUsername;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtService {

    private static final String TOKEN_TYPE = "Bearer";

    private static final String ROLE_CLAIM = "roles";

    @Autowired
    private final JwtProperties jwtProperties;

    @Autowired
    private final MapReactiveUserDetailsService userDetailsService;

    private JWSHeader signatureHeader;

    private JWSSigner jwsSigner;

    private JWSVerifier jwsVerifier;

    private DirectEncrypter encrypter;

    private DirectDecrypter decrypter;

    private JWEHeader encryptionHeader;

    @PostConstruct
    public void init() throws JOSEException {
        String signatureSecret = jwtProperties.getSignature().secret();
        log.debug("Signing key length: {}", signatureSecret.getBytes().length * 8);
        signatureHeader = new JWSHeader.Builder(HS512).type(JWT).build();
        jwsSigner = new MACSigner(signatureSecret);
        jwsVerifier = new MACVerifier(signatureSecret);
        String encryptionSecret = jwtProperties.getEncryption().secret();
        log.debug("Signing key length: {}", encryptionSecret.getBytes().length * 8);
        encrypter = new DirectEncrypter(encryptionSecret.getBytes());
        decrypter = new DirectDecrypter(jwtProperties.getEncryption().secret().getBytes());
        encryptionHeader = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM).contentType("JWT").build();
    }

    public Mono<JwtResponseDto> refresh(String refreshToken) {
        return getUserDetails(refreshToken)
                .flatMap(userDetails -> create(userDetails.getT1()));
    }

    public Mono<JwtResponseDto> create(String accountId) {
        return userDetailsService.findByUsername(accountId)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("Cannot issue tokens for unknown user: " + accountId)))
                .map(UserDetails::getAuthorities)
                .flatMapMany(Flux::fromIterable)
                .map(GrantedAuthority::getAuthority)
                .collectList()
                .flatMap(roles -> create(accountId, roles));
    }

    private Mono<JwtResponseDto> create(String accountId, List<String> roles) {
        return Mono.fromCallable(() -> {
            String newAccessToken = createToken(accountId, roles, jwtProperties.getAccessToken().expirationTime());
            String newRefreshToken = createToken(accountId, Collections.emptyList(), jwtProperties.getRefreshToken().expirationTime());
            return new JwtResponseDto(newAccessToken, TOKEN_TYPE, jwtProperties.getAccessToken().expirationTime(), newRefreshToken);
        });
    }

    public Mono<Tuple2<String, List<String>>> getUserDetails(String token) {
        return decodeToken(token)
                .map(claims -> Tuples.of(claims.getSubject(), (List<String>) claims.getClaim(ROLE_CLAIM)));
    }

    private Mono<JWTClaimsSet> decodeToken(String token) {

        JWEObject jweObject;
        try {
            jweObject = JWEObject.parse(token);

            jweObject.decrypt(decrypter);
        } catch (ParseException | JOSEException e) {
            log.error("Failed to decrypt token: {}", token, e);
            return Mono.error(new InvalidJwtTokenException("Unable to decrypt token: " + token, e));
        }

        SignedJWT signedJwt = jweObject.getPayload().toSignedJWT();

        JWTClaimsSet claims;
        try {
            if (!signedJwt.verify(jwsVerifier)) {
                return Mono.error(new InvalidJwtTokenException("Token signature is invalid: " + token));
            }
            claims = signedJwt.getJWTClaimsSet();
        } catch (ParseException | IllegalStateException | JOSEException e) {
            log.error("Invalid token: {}", token, e);
            return Mono.error(new InvalidJwtTokenException("Unable to decode token: " + token, e));
        }

        if (claims.getExpirationTime().before(new Date())) {
            return Mono.error(new InvalidJwtTokenException("Token is expired"));
        }

        return Mono.just(claims);
    }

    private String createToken(String accountId, List<String> roles, long duration) {
        Instant issuedAt = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(accountId)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plusSeconds(duration)))
                .claim(ROLE_CLAIM, roles)
                .issuer("family-recipes")
                .build();
        SignedJWT signedJWT = new SignedJWT(signatureHeader, claims);
        try {
            signedJWT.sign(jwsSigner);
        } catch (JOSEException e) {
            log.error("Unable to sign token: {}", signedJWT);
            throw new IllegalStateException("Unable to create a token.", e);
        }

        JWEObject jweObject = new JWEObject(encryptionHeader, new Payload(signedJWT));
        try {
            jweObject.encrypt(encrypter);
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to encrypt token", e);
        }

        return jweObject.serialize();
    }
}

