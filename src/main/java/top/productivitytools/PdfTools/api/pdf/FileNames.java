package top.productivitytools.PdfTools.api.pdf;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** File name helpers for converting uploads into PDFs packed in a ZIP. */
public final class FileNames {

    private FileNames() {
    }

    /** Drops any directory part sent by the browser and characters that are unsafe in file names. */
    public static String sanitize(String originalName) {
        String name = originalName == null ? "" : originalName;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}:*?\"<>|]", "_").trim();
        return name.isEmpty() || name.startsWith(".") ? "document" + name : name;
    }

    /** Lower-case extension without the dot ("" if none). */
    public static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    public static String baseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * Makes base names unique (case-insensitive): "photo.png" and "photo.jpg"
     * would both produce "photo.pdf", so the second becomes "photo (2)".
     */
    public static final class Unique {
        private final Set<String> used = new HashSet<>();

        public String next(String baseName) {
            String candidate = baseName;
            for (int i = 2; !used.add(candidate.toLowerCase(Locale.ROOT)); i++) {
                candidate = baseName + " (" + i + ")";
            }
            return candidate;
        }
    }
}
