package com.ejemplo.vitsync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves uploaded files from the configured external directory.
 *
 * <p>The location is {@code vitsync.upload.dir} (outside the repo), not the
 * old {@code user.dir/uploads} inside the project. Access to {@code /uploads/**}
 * requires authentication (see {@code SecurityConfig}): the files are medical
 * documents/avatars (audit finding V09).</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${vitsync.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(uploadDir).toFile().getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolute + "/");
    }
}
