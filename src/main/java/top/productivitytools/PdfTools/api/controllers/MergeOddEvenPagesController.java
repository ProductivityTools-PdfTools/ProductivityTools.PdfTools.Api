package top.productivitytools.PdfTools.api.controllers;

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

import lombok.RequiredArgsConstructor;
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
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"result.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(processedData.length)
                .body(resource);
    }

    private byte[] processLogic(MultipartFile f1, MultipartFile f2) {
        // Implementation of your file processing
        return "Processed content from Java".getBytes();
    }

}
