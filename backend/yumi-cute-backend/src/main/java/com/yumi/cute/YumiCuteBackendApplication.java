package com.yumi.cute;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.yumi.cute.mapper")
public class YumiCuteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YumiCuteBackendApplication.class, args);
    }

}
