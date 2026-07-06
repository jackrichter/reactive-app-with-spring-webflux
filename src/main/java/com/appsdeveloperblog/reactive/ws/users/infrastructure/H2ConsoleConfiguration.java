package com.appsdeveloperblog.reactive.ws.users.infrastructure;

import org.h2.tools.Server;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import java.sql.SQLException;

@Configuration
//@Profile({"dev", "test"})
@Profile("!prod & !production")
public class H2ConsoleConfiguration {

    private Server webServer;       // Utility class to start and stop h2 database servers programmatically

    // Method to run when spring boot application has fully started
    @EventListener(ApplicationStartedEvent.class)
    public void start() throws SQLException {
        String WEB_PORT = "8082";
        this.webServer = Server.createWebServer("-webPort", WEB_PORT).start();
    }

    // Method to close the web server when the application shuts down
    @EventListener(ContextClosedEvent.class)
    public void stop() {
        this.webServer.stop();
    }
}
