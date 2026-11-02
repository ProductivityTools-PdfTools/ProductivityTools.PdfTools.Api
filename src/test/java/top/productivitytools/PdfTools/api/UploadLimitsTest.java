package top.productivitytools.PdfTools.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real embedded Tomcat to check the multipart limits. Uses the public
 * /api/hello endpoint (no Firebase token needed): Tomcat parses the multipart
 * body before the request reaches any controller, so the limit applies anyway.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadLimitsTest {

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void acceptsManyFilesInOneUpload() throws Exception {
        HttpResponse<String> response = postFiles(70);

        // Parsing succeeds; /api/hello itself does not accept multipart (415), but it must not be a limit error.
        assertThat(response.statusCode()).isNotIn(413, 500);
    }

    @Test
    void returns413WithMessageWhenThereAreTooManyFiles() throws Exception {
        HttpResponse<String> response = postFiles(600);

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains(UploadLimitsExceptionHandler.TOO_MANY_FILES_MESSAGE);
    }

    private HttpResponse<String> postFiles(int count) throws Exception {
        String boundary = "----pdftools-test-boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (int i = 0; i < count; i++) {
            body.writeBytes(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"files\"; filename=\"image" + i + ".png\"\r\n"
                    + "Content-Type: image/png\r\n\r\n"
                    + "fake image " + i + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        body.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/hello"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
