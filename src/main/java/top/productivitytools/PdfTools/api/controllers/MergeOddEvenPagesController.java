package top.productivitytools.PdfTools.api.controllers;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class MergeOddEvenPagesController {

    @QueryMapping
    public String helloQuery() {
        return "Hello World";
    }

    @MutationMapping
    public String Hello(@Argument("name") String name) {
        var response = "Hello " + name;
        return response;
    }

    @MutationMapping("MergeFiles")
    public String mergeFilesGraphQL(@Argument("file1") MultipartFile file1, @Argument("file2") MultipartFile file2) {
        return "Merged " + (file1 != null ? file1.getOriginalFilename() : "null") + " and "
                + (file2 != null ? file2.getOriginalFilename() : "null");
    }

    @PostMapping(value = "/MergeFiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> mergeFilesRest(@RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2) throws IOException {

        byte[] zipBytes = file1.getBytes();

        ByteArrayResource resource = new ByteArrayResource(zipBytes);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=merged.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(zipBytes.length)
                .body(resource);
    }

}
