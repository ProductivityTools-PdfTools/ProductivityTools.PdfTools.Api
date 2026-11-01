package top.productivitytools.PdfTools.api.controllers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import top.productivitytools.PdfTools.api.pdf.FileNames;
import top.productivitytools.PdfTools.api.pdf.ImagePages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * "Images to PDF": every image becomes its own PDF (photo.jpg -> photo.pdf),
 * and all PDFs are returned in a single ZIP file.
 */
@RestController
@RequestMapping("/api")
public class ImagesToPdfFilesController {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp");

    @PostMapping("/images-to-pdf-files")
    public ResponseEntity<Resource> imagesToPdfFiles(@RequestParam("files") List<MultipartFile> files)
            throws IOException {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No images provided");
        }

        byte[] zipData = convertToZip(files);
        ByteArrayResource resource = new ByteArrayResource(zipData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"images.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zipData.length)
                .body(resource);
    }

    private byte[] convertToZip(List<MultipartFile> files) throws IOException {
        FileNames.Unique uniqueNames = new FileNames.Unique();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (MultipartFile file : files) {
                String name = FileNames.sanitize(file.getOriginalFilename());
                if (!SUPPORTED_EXTENSIONS.contains(FileNames.extension(name))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Unsupported file: " + name + " (supported: JPG, PNG, GIF, BMP)");
                }

                zos.putNextEntry(new ZipEntry(uniqueNames.next(FileNames.baseName(name)) + ".pdf"));
                try (PDDocument document = new PDDocument()) {
                    ImagePages.addImagePage(document, file);
                    // PDDocument.save would close the ZIP stream, so write to a buffer first.
                    ByteArrayOutputStream pdf = new ByteArrayOutputStream();
                    document.save(pdf);
                    pdf.writeTo(zos);
                }
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        }
    }
}
