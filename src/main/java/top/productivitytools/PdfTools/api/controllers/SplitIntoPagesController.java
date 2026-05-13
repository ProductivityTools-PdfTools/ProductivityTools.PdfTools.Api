package top.productivitytools.PdfTools.api.controllers;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api")
public class SplitIntoPagesController {

    @PostMapping("/split-into-pages")
    public ResponseEntity<Resource> splitPages(
            @RequestParam("file") MultipartFile file) throws IOException {

        byte[] zipData = splitIntoZip(file);
        ByteArrayResource resource = new ByteArrayResource(zipData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pages.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zipData.length)
                .body(resource);
    }

    private byte[] splitIntoZip(MultipartFile file) throws IOException {
        try (PDDocument inputDoc = Loader.loadPDF(file.getBytes());
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            int pageCount = inputDoc.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                try (PDDocument singlePageDoc = new PDDocument()) {
                    singlePageDoc.addPage(inputDoc.getPage(i));

                    ByteArrayOutputStream pageOutputStream = new ByteArrayOutputStream();
                    singlePageDoc.save(pageOutputStream);

                    ZipEntry zipEntry = new ZipEntry(getOriginalFilenameWithoutExtension(file) + "_page_" + (i + 1) + ".pdf");
                    zos.putNextEntry(zipEntry);
                    zos.write(pageOutputStream.toByteArray());
                    zos.closeEntry();
                }
            }
            zos.finish();
            return baos.toByteArray();
        }
    }
    
    private String getOriginalFilenameWithoutExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.lastIndexOf('.') > 0) {
            return originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        }
        return "file";
    }
}