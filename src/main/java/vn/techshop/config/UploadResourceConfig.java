package vn.techshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.techshop.shared.FileStorageService;

/** Serve persisted uploads outside the packaged JAR. */
@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    private final FileStorageService storage;
    public UploadResourceConfig(FileStorageService storage) { this.storage = storage; }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storage.uploadDirectory().toUri().toString();
        registry.addResourceHandler("/assets/products/uploads/**")
            .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
