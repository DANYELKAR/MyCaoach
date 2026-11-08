package com.spark.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SparkApplication {
  public static void main(String[] args) {
    SpringApplication.run(SparkApplication.class, args);
  }
}
