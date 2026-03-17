package org.example.bt4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.example.bt4.repository.sql")
@EnableMongoRepositories(basePackages = "org.example.bt4.repository.mongo")
@EnableRedisRepositories(basePackages = "org.example.bt4.repository.redis")
public class Bt4Application {
    public static void main(String[] args) {
        SpringApplication.run(Bt4Application.class, args);
    }
}