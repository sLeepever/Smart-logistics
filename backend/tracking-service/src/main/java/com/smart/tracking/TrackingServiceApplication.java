package com.smart.tracking;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.smart")
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.smart.tracking.mapper")
public class TrackingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrackingServiceApplication.class, args);
    }
}
