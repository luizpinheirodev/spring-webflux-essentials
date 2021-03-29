package com.luiz.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.blockhound.BlockHound;

@SpringBootApplication
public class SpringWebFluxEssentialsApplication {

    static {
        BlockHound.install();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringWebFluxEssentialsApplication.class, args);
    }

}
