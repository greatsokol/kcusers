package org.gs.kcusers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class KCUsersApplication {
    private static final Logger logger = LoggerFactory.getLogger(KCUsersApplication.class);

    public static void main(String[] args) {
        try {
            SpringApplication.run(KCUsersApplication.class, args);
        } catch (UnsatisfiedDependencyException ex) {
            if ("securityFilterChain".equals(ex.getBeanName())) {
                logger.error("Error connecting to KeyCloak");
            }
        }
    }

}
