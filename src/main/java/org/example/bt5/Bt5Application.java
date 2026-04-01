package org.example.bt5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.example.bt5.repository.sql")
@EnableMongoRepositories(basePackages = "org.example.bt5.repository.mongo")
@EnableRedisRepositories(basePackages = "org.example.bt5.repository.redis")
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class Bt5Application {
    public static void main(String[] args) {
        SpringApplication.run(Bt5Application.class, args);
    }
}