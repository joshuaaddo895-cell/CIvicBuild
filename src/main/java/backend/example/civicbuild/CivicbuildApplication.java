package backend.example.civicbuild;

import backend.example.civicbuild.config.AppProperties;
import backend.example.civicbuild.config.DotenvBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class CivicbuildApplication {

	public static void main(String[] args) {
		DotenvBootstrap.load();
		SpringApplication.run(CivicbuildApplication.class, args);
	}

}
