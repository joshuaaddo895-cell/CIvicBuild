package backend.example.civicbuild.config;

import com.cloudinary.Cloudinary;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class CloudinaryConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryConfig.class);

    @Bean
    public Cloudinary cloudinary(AppProperties properties) {
        AppProperties.Cloudinary cloudinary = properties.cloudinary();
        if (!StringUtils.hasText(cloudinary.cloudName())
                || !StringUtils.hasText(cloudinary.apiKey())
                || !StringUtils.hasText(cloudinary.apiSecret())) {
            throw new IllegalStateException(
                    "Cloudinary credentials are not fully configured (CLOUDINARY_CLOUD_NAME, "
                            + "CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET)");
        }
        log.info("Cloudinary client configured (cloud: {})", cloudinary.cloudName());
        return new Cloudinary(Map.of(
                "cloud_name", cloudinary.cloudName(),
                "api_key", cloudinary.apiKey(),
                "api_secret", cloudinary.apiSecret(),
                "secure", true));
    }
}
