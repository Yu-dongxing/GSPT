package com.wzz.gspt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.wzz.gspt.mapper")
public class GSPTApplication {

    public static void main(String[] args) {
        SpringApplication.run(GSPTApplication.class, args);
    }

}
