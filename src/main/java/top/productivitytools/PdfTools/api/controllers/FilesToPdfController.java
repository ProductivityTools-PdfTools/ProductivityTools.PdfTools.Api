package top.productivitytools.PdfTools.api.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Converts Word documents (.doc, .docx) to PDF using LibreOffice in headless
 * mode and returns all PDFs in a single ZIP file. Each PDF has the same name
 * as the source document (report.docx -> report.pdf).
 *
 * Requires LibreOffice (the "soffice" binary) to be installed on the server.
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class FilesToPdfController {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("doc", "docx");

    @Value("${pdftools.soffice-path:soffice}")
    private String sofficePath;

    @Value("${pdftools.conversion-timeout-seconds:180}")
    private long timeoutSeconds;

    @PostMapping("/files-to-pdf")
    public ResponseEntity<Resource> filesToPdf(@RequestParam("files") List<MultipartFile> files)
            throws IOException, InterruptedException {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided");
        }

        byte[] zipData = convertToZip(files);
        ByteArrayResource resource = new ByteArrayResource(zipData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zipData.length)
                .body(resource);
    }

    private byte[] convertToZip(List<MultipartFile> files) throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("files-to-pdf-");
        try {
            Path inputDir = Files.createDirectory(workDir.resolve("in"));
            Path outputDir = Files.createDirectory(workDir.resolve("out"));

            List<Path> inputs = saveInputs(files, inputDir);
            runLibreOffice(inputs, outputDir, workDir.resolve("profile"));

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                List<String> failed = new ArrayList<>();
                for (Path input : inputs) {
                    String pdfName = baseName(input.getFileName().toString()) + ".pdf";
                    Path pdf = outputDir.resolve(pdfName);
                    if (!Files.isRegularFile(pdf) || Files.size(pdf) == 0) {
                        failed.add(input.getFileName().toString());
                        continue;
                    }
                    zos.putNextEntry(new ZipEntry(pdfName));
                    Files.copy(pdf, zos);
                    zos.closeEntry();
                }
                if (!failed.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Could not convert: " + String.join(", ", failed));
                }
                zos.finish();
                return baos.toByteArray();
            }
        } finally {
            FileSystemUtils.deleteRecursively(workDir);
        }
    }

    /**
     * Saves uploaded files to disk. File names are sanitized and made unique
     * (e.g. "a.doc" and "a.docx" would both produce "a.pdf", so the second one
     * is saved as "a (2).docx" and produces "a (2).pdf").
     */
    private List<Path> saveInputs(List<MultipartFile> files, Path inputDir) throws IOException {
        List<Path> inputs = new ArrayList<>();
        Set<String> usedBaseNames = new HashSet<>();

        for (MultipartFile file : files) {
            String name = sanitize(file.getOriginalFilename());
            String extension = extension(name);
            if (!SUPPORTED_EXTENSIONS.contains(extension)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported file: " + name + " (supported: .doc, .docx)");
            }

            String base = baseName(name);
            String uniqueBase = base;
            for (int i = 2; !usedBaseNames.add(uniqueBase.toLowerCase(Locale.ROOT)); i++) {
                uniqueBase = base + " (" + i + ")";
            }

            Path target = inputDir.resolve(uniqueBase + "." + extension);
            file.transferTo(target);
            inputs.add(target);
        }
        return inputs;
    }

    private void runLibreOffice(List<Path> inputs, Path outputDir, Path profileDir)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(
                sofficePath,
                "--headless", "--norestore", "--nolockcheck", "--nodefault", "--nofirststartwizard",
                // A separate profile per request lets several conversions run in parallel.
                "-env:UserInstallation=" + profileDir.toUri(),
                "--convert-to", "pdf",
                "--outdir", outputDir.toString()));
        inputs.forEach(input -> command.add(input.toString()));

        Path logFile = outputDir.getParent().resolve("soffice.log");
        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();
        } catch (IOException e) {
            log.error("Could not start LibreOffice ({}): {}", sofficePath, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Document conversion is not available on the server", e);
        }

        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Document conversion timed out");
        }
        if (process.exitValue() != 0) {
            log.warn("LibreOffice exited with code {}: {}", process.exitValue(), Files.readString(logFile));
        }
    }

    private static String sanitize(String originalName) {
        String name = originalName == null ? "" : originalName;
        // Drop any directory part sent by the browser and characters that are unsafe in file names.
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}:*?\"<>|]", "_").trim();
        return name.isEmpty() || name.startsWith(".") ? "document" + name : name;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private static String baseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
