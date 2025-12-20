package top.productivitytools.PdfTools.api.controllers;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ReversePagesController {

    @PostMapping("/reverse-pages-in-file")
    public ResponseEntity<Resource> reversePages(
            @RequestParam("file") MultipartFile file) throws IOException {

        byte[] processedData = processLogic(file);
        ByteArrayResource resource = new ByteArrayResource(processedData);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reversed.pdf\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(processedData.length)
                .body(resource);
    }

    private byte[] processLogic(MultipartFile file) throws IOException {
        try (PDDocument inputDoc = Loader.loadPDF(file.getBytes());
                PDDocument outputDoc = new PDDocument()) {

            int pageCount = inputDoc.getNumberOfPages();
            for (int i = pageCount - 1; i >= 0; i--) {
                outputDoc.addPage(inputDoc.getPage(i));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            outputDoc.save(baos);
            return baos.toByteArray();
        }
    }
}
