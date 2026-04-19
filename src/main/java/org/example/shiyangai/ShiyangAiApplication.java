package org.example.shiyangai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@MapperScan("org.example.shiyangai.mapper")
public class ShiyangAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShiyangAiApplication.class, args);
		System.out.println("=== 食养AI启动成功 ===");
		System.out.println("API文档: http://localhost:8080/doc.html");
		System.out.println("H2控制台: http://localhost:8080/h2-console");
	}

}
