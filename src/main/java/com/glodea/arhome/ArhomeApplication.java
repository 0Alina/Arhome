package com.glodea.arhome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.UserRepository;

@SpringBootApplication
public class ArhomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArhomeApplication.class, args);
	}

	@Bean
	org.springframework.boot.CommandLineRunner adminAccountInitializer(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder
	) {
		return args -> {
			if (userRepository.findByEmail("admin").isPresent()) {
				return;
			}

			User admin = new User();
			admin.setFullName("Administrator");
			admin.setEmail("admin");
			admin.setPasswordHash(passwordEncoder.encode("admin"));
			admin.setRole("ADMIN");
			admin.setCategory("Administration");
			userRepository.save(admin);
		};
	}

}
