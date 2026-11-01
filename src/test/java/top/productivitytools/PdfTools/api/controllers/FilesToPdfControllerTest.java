package top.productivitytools.PdfTools.api.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the controller logic with a fake "soffice" script that behaves like
 * LibreOffice: it writes OUTDIR/&lt;name&gt;.pdf for every input file and skips
 * files it cannot load. The real LibreOffice conversion is not exercised here.
 */
@DisabledOnOs(OS.WINDOWS)
class FilesToPdfControllerTest {

    private static final String FAKE_SOFFICE = """
            #!/bin/sh
            sleep "${FAKE_SLEEP:-0}"
            while [ "$1" != "--outdir" ]; do shift; done
            out="$2"; shift 2
            for f in "$@"; do
              if head -c 15 "$f" | grep -q "not a word file"; then
                echo "Error: source file could not be loaded"; continue
              fi
              b=$(basename "$f"); b="${b%.*}"
              printf '%%PDF-1.4 fake %s\\n' "$b" > "$out/$b.pdf"
            done
            """;

    @TempDir
    Path tempDir;

    private FilesToPdfController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws IOException {
        controller = new FilesToPdfController();
        ReflectionTestUtils.setField(controller, "sofficePath", writeScript("soffice", FAKE_SOFFICE).toString());
        ReflectionTestUtils.setField(controller, "timeoutSeconds", 30L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void convertsAllFilesIntoZipKeepingNames() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/files-to-pdf")
                        .file(word("Raport Q3.docx"))
                        .file(word("umowa.doc"))
                        .file(word("umowa.docx")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("converted.zip")))
                .andReturn();

        // "umowa.doc" and "umowa.docx" would both become "umowa.pdf", so the second gets a suffix.
        assertThat(zipEntries(result.getResponse().getContentAsByteArray()))
                .containsExactly("Raport Q3.pdf", "umowa.pdf", "umowa (2).pdf");
    }

    @Test
    void stripsDirectoriesFromFileNames() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/files-to-pdf").file(word("../../evil.docx")))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(zipEntries(result.getResponse().getContentAsByteArray())).containsExactly("evil.pdf");
    }

    @Test
    void rejectsUnsupportedFiles() throws Exception {
        mockMvc.perform(multipart("/api/files-to-pdf").file(word("notes.txt")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("notes.txt")));
    }

    @Test
    void reportsFilesThatCouldNotBeConverted() throws Exception {
        MockMultipartFile broken = new MockMultipartFile("files", "broken.docx", null,
                "not a word file".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/files-to-pdf").file(word("ok.docx")).file(broken))
                .andExpect(status().isUnprocessableContent())
                .andExpect(status().reason(containsString("broken.docx")));
    }

    @Test
    void returns503WhenLibreOfficeIsMissing() throws Exception {
        ReflectionTestUtils.setField(controller, "sofficePath", tempDir.resolve("missing-soffice").toString());

        mockMvc.perform(multipart("/api/files-to-pdf").file(word("a.docx")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void returns504WhenConversionTimesOut() throws Exception {
        Path slow = writeScript("slow-soffice", "#!/bin/sh\nsleep 5\n");
        ReflectionTestUtils.setField(controller, "sofficePath", slow.toString());
        ReflectionTestUtils.setField(controller, "timeoutSeconds", 1L);

        mockMvc.perform(multipart("/api/files-to-pdf").file(word("a.docx")))
                .andExpect(status().isGatewayTimeout());
    }

    private static MockMultipartFile word(String name) {
        return new MockMultipartFile("files", name, null, ("content of " + name).getBytes(StandardCharsets.UTF_8));
    }

    private Path writeScript(String name, String content) throws IOException {
        Path script = tempDir.resolve(name);
        Files.writeString(script, content);
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
        return script;
    }

    private static List<String> zipEntries(byte[] zip) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            for (ZipEntry entry; (entry = zis.getNextEntry()) != null; ) {
                assertThat(zis.readAllBytes()).startsWith("%PDF".getBytes(StandardCharsets.UTF_8));
                names.add(entry.getName());
            }
        }
        return names;
    }
}
