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
import top.productivitytools.PdfTools.api.pdf.ImagePages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** "Merge Images to PDF": all images become pages of a single PDF. */
@RestController
@RequestMapping("/api")
public class ImagesToPdfController {

    @PostMapping("/images-to-pdf")
    public ResponseEntity<Resource> imagesToPdf(@RequestParam("files") List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No images provided");
        }

        byte[] pdfData = convert(files);
        ByteArrayResource resource = new ByteArrayResource(pdfData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"images.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfData.length)
                .body(resource);
    }

    /**
     * Creates a PDF in which every image becomes a separate page, in the order
     * the images were sent.
     */
    private byte[] convert(List<MultipartFile> files) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (MultipartFile file : files) {
                ImagePages.addImagePage(document, file);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
