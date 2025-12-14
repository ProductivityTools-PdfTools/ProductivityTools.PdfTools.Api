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
public class MergeOddEvenPagesController {

    @PostMapping("/merge-odd-even-pages")
    public ResponseEntity<Resource> handleFileUpload(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2) throws IOException {

        // 1. Logic to process files
        byte[] processedData = processLogic(file1, file2);

        // 2. Wrap result in a Resource
        ByteArrayResource resource = new ByteArrayResource(processedData);

        // 3. Return response with headers for file download
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"merged.pdf\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(processedData.length)
                .body(resource);
    }

    private byte[] processLogic(MultipartFile f1, MultipartFile f2) throws IOException {
        try (PDDocument doc1 = Loader.loadPDF(f1.getBytes());
                PDDocument doc2 = Loader.loadPDF(f2.getBytes());
                PDDocument outDoc = new PDDocument()) {

            int pages1 = doc1.getNumberOfPages();
            int pages2 = doc2.getNumberOfPages();
            int maxPages = Math.max(pages1, pages2);

            for (int i = 0; i < maxPages; i++) {
                if (i < pages1) {
                    outDoc.addPage(doc1.getPage(i));
                }
                if (i < pages2) {
                    outDoc.addPage(doc2.getPage(i));
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            outDoc.save(baos);
            return baos.toByteArray();
        }
    }

}
