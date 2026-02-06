package com.skillstorm.finsight.mq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
    }
)
public class MqGatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MqGatewayServiceApplication.class, args);
    }
}