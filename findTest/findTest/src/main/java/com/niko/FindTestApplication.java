package com.niko;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FindTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(FindTestApplication.class, args);
        System.out.println("http://localhost:80");

    }
}
