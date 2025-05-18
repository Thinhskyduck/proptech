package com.example.proptech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // Thêm import

@SpringBootApplication
@EnableAsync // Kích hoạt xử lý bất đồng bộ
public class ProptechApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProptechApplication.class, args);
	}

}