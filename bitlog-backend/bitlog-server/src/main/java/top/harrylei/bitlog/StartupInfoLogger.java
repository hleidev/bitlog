package top.harrylei.bitlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logs a structured startup summary after the application is fully ready.
 *
 * @author Harry
 * @since 2026-05-16
 */
@Component
public class StartupInfoLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupInfoLogger.class);

    private static final Pattern JDBC_PATTERN = Pattern.compile("jdbc:[^:]+://([^/?]+)(?:[/?]([^?]*).*)?");

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        RuntimeMXBean runtimeMx = ManagementFactory.getRuntimeMXBean();

        String appName = env.getProperty("spring.application.name", "bitlog-server");
        String[] activeProfiles = env.getActiveProfiles();
        String profiles = activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default";
        String port = env.getProperty("server.port", "8080");
        String host = resolveHost();
        String db = parseJdbcTarget(env.getProperty("spring.datasource.url", ""));
        String redisHost = env.getProperty("spring.data.redis.host", "localhost");
        String redisPort = env.getProperty("spring.data.redis.port", "6379");
        String redisDb = env.getProperty("spring.data.redis.database", "0");
        String storageEndpoint = env.getProperty("storage.endpoint", "N/A");
        String jvmVersion = System.getProperty("java.version");
        String jvmName = System.getProperty("java.vm.name");
        long uptimeMs = runtimeMx.getUptime();
        String pid = String.valueOf(ProcessHandle.current().pid());

        log.info("""
            \n==========================================================
              Application : {}
              Profile     : {}
              URL         : http://{}:{}
              Database    : {}
              Redis       : {}:{} / db{}
              Storage     : {}
              JVM         : {} | {}
              PID         : {}
              Started in  : {} ms
            ==========================================================""", appName, profiles, host, port, db, redisHost,
            redisPort, redisDb, storageEndpoint, jvmVersion, jvmName, pid, uptimeMs);
    }

    private String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }

    /**
     * Extracts "host:port/database" from a JDBC URL, omitting credentials and parameters.
     */
    private String parseJdbcTarget(String jdbcUrl) {
        if (jdbcUrl.isBlank()) {
            return "N/A";
        }
        Matcher m = JDBC_PATTERN.matcher(jdbcUrl);
        if (!m.find()) {
            return jdbcUrl;
        }
        String hostPort = m.group(1);
        String database = m.group(2);
        return database != null && !database.isBlank() ? hostPort + "/" + database : hostPort;
    }
}
