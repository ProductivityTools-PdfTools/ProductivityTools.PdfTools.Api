package top.productivitytools.PdfTools.api.controllers;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImagesToPdfFilesControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ImagesToPdfFilesController()).build();

    @Test
    void createsOnePdfPerImageInZip() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/images-to-pdf-files")
                        .file(image("photo.png", "png", 300, 200))
                        .file(image("photo.jpg", "jpg", 640, 480))
                        .file(image("Wakacje 01.JPEG", "jpg", 100, 150)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("images.zip")))
                .andReturn();

        Map<String, byte[]> entries = unzip(result.getResponse().getContentAsByteArray());
        // "photo.png" and "photo.jpg" would both become "photo.pdf", so the second gets a suffix.
        assertThat(entries).containsOnlyKeys("photo.pdf", "photo (2).pdf", "Wakacje 01.pdf");

        assertSinglePage(entries.get("photo.pdf"), 300, 200);
        assertSinglePage(entries.get("photo (2).pdf"), 640, 480);
        assertSinglePage(entries.get("Wakacje 01.pdf"), 100, 150);
    }

    @Test
    void rejectsUnsupportedFiles() throws Exception {
        mockMvc.perform(multipart("/api/images-to-pdf-files")
                        .file(new MockMultipartFile("files", "notes.txt", null, "x".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("notes.txt")));
    }

    @Test
    void rejectsCorruptedImages() throws Exception {
        mockMvc.perform(multipart("/api/images-to-pdf-files")
                        .file(new MockMultipartFile("files", "broken.png", null, "not an image".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("broken.png")));
    }

    private static MockMultipartFile image(String name, String format, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return new MockMultipartFile("files", name, null, out.toByteArray());
    }

    private static void assertSinglePage(byte[] pdf, int width, int height) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            PDRectangle size = document.getPage(0).getMediaBox();
            assertThat(size.getWidth()).isEqualTo(width);
            assertThat(size.getHeight()).isEqualTo(height);
        }
    }

    private static Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            for (ZipEntry entry; (entry = zis.getNextEntry()) != null; ) {
                entries.put(entry.getName(), zis.readAllBytes());
            }
        }
        return entries;
    }
}
