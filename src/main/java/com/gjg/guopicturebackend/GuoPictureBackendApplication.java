package com.gjg.guopicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.gjg.guopicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class GuoPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuoPictureBackendApplication.class, args);
    }

}
