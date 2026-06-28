package com.tdfpoule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration is excluded because this app has no login system to
// autoconfigure a default user for -- SecurityConfig already permits every request and
// disables httpBasic/formLogin, so that default user was unused and only existed to print a
// "Using generated security password" line on every startup.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TdfPouleApplication {
    public static void main(String[] args) {
        SpringApplication.run(TdfPouleApplication.class, args);
    }
}
