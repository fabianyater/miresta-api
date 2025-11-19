package com.miresta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MirestaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MirestaApplication.class, args);
    }

}
