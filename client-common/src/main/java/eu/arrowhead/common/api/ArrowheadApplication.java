package eu.arrowhead.common.api;

import eu.arrowhead.common.api.clients.EventHandlerClient;
import eu.arrowhead.common.api.clients.ServiceRegistryClient;
import eu.arrowhead.common.misc.ArrowheadProperties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class ArrowheadApplication {
    protected final Logger log = Logger.getLogger(getClass());
    private boolean isDaemon;
    private boolean isSecure;
    private ArrowheadProperties props;

    public ArrowheadApplication(String[] args) {
        setProperties(ArrowheadProperties.loadDefault());

        boolean daemon = false;
        for (String arg : args) {
            switch (arg) {
                case "-daemon":
                    daemon = true;
                    log.info("Starting server as daemon!");
                    break;
                case "-d":
                    setDebug(true);
                    log.info("Starting server in debug mode!");
                    break;
                case "-tls":
                    log.warn("-------------------------------------------------------");
                    log.warn("  WARNING: -TLS IS NO LONGER ACCEPTED AS ARGUMENT");
                    log.warn("  Use 'secure=true' in the configuration file instead");
                    log.warn("-------------------------------------------------------");
            }
        }
        isDaemon = daemon;
    }

    public ArrowheadApplication setProperties(ArrowheadProperties props) {
        if (props != null) {
            if (!props.contains("log4j.rootLogger")) {
                props.putIfAbsent("log4j.rootLogger", "INFO, CONSOLE");
                props.putIfAbsent("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
                props.putIfAbsent("log4j.appender.CONSOLE.target", "System.err");
                props.putIfAbsent("log4j.appender.CONSOLE.ImmediateFlush", "true");
                props.putIfAbsent("log4j.appender.CONSOLE.Threshold", "debug");
                props.putIfAbsent("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
                props.putIfAbsent("log4j.appender.CONSOLE.layout.conversionPattern", "%d{yyyy-MM-dd HH:mm:ss}  %c{1}.%M(%F:%L)  %p  %m%n");
            }
            PropertyConfigurator.configure(props);

            isSecure = props.isSecure();
        }
        this.props = props;

        return this;
    }

    public ArrowheadProperties getProps() {
        return props;
    }

    public ArrowheadApplication setDebug(boolean debug) {
        System.setProperty("debug_mode", debug ? "true" : "false");
        return this;
    }

    public boolean isDebug() {
        return System.getProperty("debug_mode", "false").equals("true");
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public ArrowheadApplication setSecure(boolean secure) {
        isSecure = secure;
        return this;
    }

    public void setDaemon(boolean daemon) {
        isDaemon = daemon;
    }

    protected void shutdown() {
        EventHandlerClient.unsubscribeAll();
        ServiceRegistryClient.unregisterAll();
        ArrowheadServer.stopAll();
        onStop();
        System.exit(0);
    }

    protected void listenForInput() {
        if (isDaemon) {
            log.info("In daemon mode, process will terminate for TERM signal...");
        } else {
            log.info("Type \"stop\" to shutdown...");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String input = "";
            try {
                while (!input.equals("stop")) {
                    input = br.readLine();
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            shutdown();
        }
    }

    protected void start(boolean listen) {
        log.info("Working directory: " + System.getProperty("user.dir"));
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Received TERM signal, shutting down...");
                shutdown();
            }));

            onStart();

            if (listen) listenForInput();
        } catch (Throwable e) {
            log.error("Starting client failed", e);
        }
    }

    protected void start() {
        start(true);
    }

    protected abstract void onStart();

    protected abstract void onStop();
}