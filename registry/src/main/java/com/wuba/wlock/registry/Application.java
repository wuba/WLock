/*
 * Copyright (C) 2005-present, 58.com.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wuba.wlock.registry;

import com.wuba.wlock.registry.config.Environment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan({"com.wuba.wlock.registry", "com.wuba.wlock.repository"})
public class Application {
    @Value("${registry.env}")
    String env;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        log.error("----------------------------- Application start finish! --------------------------");
    }

    @Bean
    public Environment initEnvironment() throws Exception {
        Environment environment = new Environment();
        environment.setEnv(env);
        return environment;
    }
}
