package top.productivitytools.PdfTools.api.controllers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

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
     * Creates a PDF in which every image becomes a separate page. The page has
     * exactly the size of the image (1 pixel = 1 point), so the image is not
     * scaled or cropped. Images are added in the order they were sent.
     */
    private byte[] convert(List<MultipartFile> files) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (MultipartFile file : files) {
                PDImageXObject image = loadImage(document, file);

                PDRectangle pageSize = new PDRectangle(image.getWidth(), image.getHeight());
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.drawImage(image, 0, 0, pageSize.getWidth(), pageSize.getHeight());
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private PDImageXObject loadImage(PDDocument document, MultipartFile file) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        try {
            byte[] bytes = file.getBytes();
            if (isJpeg(bytes)) {
                // JPEG data is embedded as-is (no re-compression, no quality loss).
                return JPEGFactory.createFromByteArray(document, bytes);
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IllegalArgumentException("ImageIO could not decode the file");
            }
            return LosslessFactory.createFromImage(document, toDirectColor(image));
        } catch (IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported or corrupted image: " + name + " (supported: JPG, PNG, GIF, BMP)", e);
        }
    }

    private static boolean isJpeg(byte[] bytes) {
        return bytes.length > 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
    }

    /**
     * PDFBox treats 1-bit palette images (e.g. GIFs or paletted PNGs with two
     * colours) as black and white, which loses the colours. Converting palette
     * images to plain RGB/ARGB first keeps the colours intact.
     */
    private static BufferedImage toDirectColor(BufferedImage image) {
        if (!(image.getColorModel() instanceof IndexColorModel)) {
            return image;
        }
        int type = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), type);
        Graphics2D g = converted.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return converted;
    }
}
