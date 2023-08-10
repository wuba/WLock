package com.wuba.wlock.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableConfigurationProperties()
public class ServerApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication springApplication = new SpringApplication(ServerApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = springApplication.run(args);

        TestService testService = context.getBean(TestService.class);
        testService.test1();
        testService.test2();
        testService.test3();
        testService.test4();
        testService.test5();
        testService.test6();
        testService.test7("test");

        testService.test8();
        testService.test9("test");

        testService.test10();
        testService.test11("test");

        testService.test12(123, "test2");

        testService.test61(6, "b");
        testService.test81(6, "b");
        testService.test101(101, "b");
        testService.test121(121, "b");
    }
}