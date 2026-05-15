package top.harrylei.bitlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "top.harrylei.bitlog")
@ConfigurationPropertiesScan(basePackages = "top.harrylei.bitlog")
public class BitlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BitlogApplication.class, args);
    }
}
