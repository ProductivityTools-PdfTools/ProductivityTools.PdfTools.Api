package top.productivitytools.PdfTools.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns upload limit errors (too many files, files too large) into HTTP 413
 * with a readable message instead of a generic HTTP 500. The body has the same
 * shape as Spring's default error response, so the web app can show "message".
 */
@Slf4j
@RestControllerAdvice
public class UploadLimitsExceptionHandler {

    static final String TOO_LARGE_MESSAGE = "Upload is too large. Maximum total size is 50 MB.";
    static final String TOO_MANY_FILES_MESSAGE = "Too many files or files too large in one upload (maximum 500 files, 50 MB in total).";

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("Upload rejected: {}", e.getMessage());
        return payloadTooLarge(TOO_LARGE_MESSAGE);
    }

    /** E.g. Tomcat's FileCountLimitExceededException ("Failed to parse multipart servlet request"). */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipart(MultipartException e) {
        log.warn("Upload rejected: {} (cause: {})", e.getMessage(),
                e.getCause() != null ? e.getCause().toString() : "none");
        return payloadTooLarge(TOO_MANY_FILES_MESSAGE);
    }

    private static ResponseEntity<Map<String, Object>> payloadTooLarge(String message) {
        HttpStatus status = HttpStatus.CONTENT_TOO_LARGE;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
