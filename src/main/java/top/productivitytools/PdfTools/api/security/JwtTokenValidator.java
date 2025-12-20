package top.productivitytools.PdfTools.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class JwtTokenValidator {

    private static final Set<String> ALLOWED_EMAILS = Set.of(
            "pwujczyk@gmail.com",
            "malgorzata.wujczyk@gmail.com");

    /**
     * Validates the JWT token and checks if the email is in the allowed list
     * 
     * @param token The JWT token to validate
     * @return true if the token contains an allowed email, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            // Parse the token without signature verification (for tokens signed by external
            // providers like Google/Firebase)
            // This extracts the claims from the payload
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid token format - expected 3 parts");
                return false;
            }

            // Decode the payload (second part of JWT)
            String payload = parts[1];
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            log.debug("Token payload: {}", decodedPayload);

            // Parse the JSON to get email claim
            // Using a simple approach to extract email
            if (decodedPayload.contains("\"email\"")) {
                String email = extractEmail(decodedPayload);
                log.debug("Extracted email: {}", email);

                boolean isAllowed = ALLOWED_EMAILS.contains(email);
                if (!isAllowed) {
                    log.warn("Email {} is not in the allowed list", email);
                }
                return isAllowed;
            }

            log.warn("Token does not contain email claim");
            return false;

        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract email from JSON payload
     */
    private String extractEmail(String payload) {
        try {
            // Find the email field in JSON
            int emailStart = payload.indexOf("\"email\"");
            if (emailStart == -1) {
                return null;
            }

            // Find the value after "email":"
            int valueStart = emailStart + 8;
            // valueStart = payload.indexOf("\"", valueStart + 1);
            int valueEnd = payload.indexOf("\"", valueStart + 1);

            if (valueStart != -1 && valueEnd != -1) {
                String email = payload.substring(valueStart + 1, valueEnd);
                return email;
            }

            return null;
        } catch (Exception e) {
            log.error("Error extracting email: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the email from the token
     */
    public String getEmailFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payload = parts[1];
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            return extractEmail(decodedPayload);
        } catch (Exception e) {
            log.error("Error getting email from token: {}", e.getMessage());
            return null;
        }
    }
}
