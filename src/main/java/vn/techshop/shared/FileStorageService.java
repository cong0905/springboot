package vn.techshop.shared;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:./data/uploads}") String uploadPath) {
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể khởi tạo thư mục lưu trữ ảnh: " + this.uploadDir, e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.invalid("Tệp tải lên không được rỗng.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.invalid("Kích thước tệp vượt quá giới hạn cho phép (tối đa 5MB).");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw BusinessException.invalid("Định dạng tệp không được hỗ trợ. Chỉ chấp nhận JPG, PNG hoặc WEBP.");
        }

        try {
            byte[] bytes = file.getBytes();
            boolean png = bytes.length >= 8 && bytes[0] == (byte)137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71
                && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10;
            boolean jpeg = bytes.length >= 3 && bytes[0] == (byte)255 && bytes[1] == (byte)216 && bytes[2] == (byte)255;
            boolean webp = bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            String type = contentType.toLowerCase(Locale.ROOT);
            if (!(png && type.equals("image/png") || jpeg && type.equals("image/jpeg") || webp && type.equals("image/webp"))) {
                throw BusinessException.invalid("Nội dung tệp không khớp định dạng ảnh khai báo.");
            }
            // Validate dimensions before allocating decoded pixels. Never trust a magic header alone.
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw BusinessException.invalid("Không đọc được nội dung ảnh.");
                var reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 8192 || height > 8192 || (long)width * height > 16_000_000)
                        throw BusinessException.invalid("Ảnh tối đa 8192 px mỗi chiều và 16 triệu pixel.");
                    if (reader.read(0) == null) throw BusinessException.invalid("Ảnh bị hỏng hoặc không hợp lệ.");
                } finally { reader.dispose(); }
            } catch (IOException | IllegalArgumentException ex) {
                throw BusinessException.invalid("Ảnh bị hỏng hoặc không hợp lệ.");
            }
            String safeFilename = UUID.randomUUID() + (png ? ".png" : jpeg ? ".jpg" : ".webp");
            Files.write(this.uploadDir.resolve(safeFilename), bytes);
            return "/assets/products/uploads/" + safeFilename;
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu trữ ảnh tải lên.", e);
        }
    }

    public Path uploadDirectory() { return uploadDir; }

    public boolean delete(String assetPath) {
        if (assetPath == null || !assetPath.startsWith("/assets/products/uploads/")) {
            return false;
        }
        String filename = assetPath.substring("/assets/products/uploads/".length());
        Path targetLocation = this.uploadDir.resolve(filename).normalize();
        if (!targetLocation.startsWith(this.uploadDir)) {
            return false; // Path traversal guard
        }
        try {
            return Files.deleteIfExists(targetLocation);
        } catch (IOException e) {
            return false;
        }
    }
}
