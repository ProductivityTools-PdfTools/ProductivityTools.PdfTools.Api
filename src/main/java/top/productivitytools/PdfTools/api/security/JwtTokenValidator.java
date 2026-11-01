package top.productivitytools.PdfTools.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Validates Firebase Authentication ID tokens issued by the "ptprojectsweb"
 * Firebase project.
 *
 * A token is accepted only if:
 * - it is signed by Google (keys fetched from the securetoken JWKS endpoint),
 * - it is not expired,
 * - its issuer is https://securetoken.google.com/{projectId},
 * - its audience is {projectId},
 * - it contains a verified email that is on the allowed list.
 */
@Component
@Slf4j
public class JwtTokenValidator {

    private static final String FIREBASE_PROJECT_ID = "ptprojectsweb";
    private static final String ISSUER = "https://securetoken.google.com/" + FIREBASE_PROJECT_ID;
    private static final String JWK_SET_URI =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    private static final Set<String> ALLOWED_EMAILS = Set.of(
            "pwujczyk@gmail.com",
            "malgorzata.wujczyk@gmail.com");

    private final JwtDecoder jwtDecoder;

    public JwtTokenValidator() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(FIREBASE_PROJECT_ID)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token audience is not " + FIREBASE_PROJECT_ID, null));

        // createDefaultWithIssuer checks the issuer as well as exp/nbf timestamps.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                audienceValidator));

        this.jwtDecoder = decoder;
    }

    /**
     * Validates the JWT token and checks if the email is in the allowed list
     *
     * @param token The JWT token to validate
     * @return true if the token is valid and contains an allowed email, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);

            String email = jwt.getClaimAsString("email");
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            if (email == null || !Boolean.TRUE.equals(emailVerified)) {
                log.warn("Token does not contain a verified email");
                return false;
            }

            boolean isAllowed = ALLOWED_EMAILS.contains(email);
            if (!isAllowed) {
                log.warn("Email {} is not in the allowed list", email);
            }
            return isAllowed;
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the email from the token. Call only after validateToken returned true.
     */
    public String getEmailFromToken(String token) {
        try {
            return jwtDecoder.decode(token).getClaimAsString("email");
        } catch (JwtException e) {
            log.error("Error getting email from token: {}", e.getMessage());
            return null;
        }
    }
}
