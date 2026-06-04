package com.fantasyrealm.server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.fantasyrealm")
@EnableScheduling
public class FantasyRealmApplication {
    private static final Logger log = LoggerFactory.getLogger(FantasyRealmApplication.class);

    public static void main(String[] args) throws Exception {
        log.info("=== Fantasy Realm Online starting ===");
        ConfigurableApplicationContext ctx =
            SpringApplication.run(FantasyRealmApplication.class, args);

        GameServer gameServer = ctx.getBean(GameServer.class);
        gameServer.start();
        log.info("=== Fantasy Realm Online is LIVE ===");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> gameServer.shutdown()));
    }
}
