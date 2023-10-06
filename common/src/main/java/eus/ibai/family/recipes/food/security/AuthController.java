package eus.ibai.family.recipes.food.security;

import eus.ibai.family.recipes.food.util.Temporary;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@SecurityRequirements
@Slf4j
@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
@Temporary("Will be re-implemented in a dedicated microservice including RBAC")
public class AuthController {

    private final JwtService jwtService;

    private final ReactiveAuthenticationManager authenticationManager;

    @PostMapping("/login")
    public Mono<ResponseEntity<JwtResponseDto>> login(@Valid @RequestBody AuthenticationRequestDto authenticationRequest) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authenticationRequest.username(), authenticationRequest.password()))
                .flatMap(auth -> jwtService.create(auth.getName()))
                .map(jwt -> {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
                    httpHeaders.add(HttpHeaders.PRAGMA, "no-cache");
                    return new ResponseEntity<>(jwt, httpHeaders, HttpStatus.OK);
                });
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<JwtResponseDto>> refreshJwtToken(@Valid @RequestBody AuthenticationRefreshRequestDto refreshRequest) {
        return jwtService.refresh(refreshRequest.refreshToken())
                .map(jwt -> {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
                    httpHeaders.add(HttpHeaders.PRAGMA, "no-cache");
                    return new ResponseEntity<>(jwt, httpHeaders, HttpStatus.OK);
                });
    }

    @ExceptionHandler({BadCredentialsException.class, InvalidJwtTokenException.class})
    public ResponseEntity<Void> handleBadCredentialsException(Throwable t) {
        log.trace("Handling unauthorized exception: {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .build();
    }
}