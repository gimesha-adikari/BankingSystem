package com.bankingsystem.core.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.BaselineResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Official Flyway Baseline Operator CLI.
 *
 * Invokes Flyway's official baseline API to record an explicit schema baseline.
 * Enforces secure credential handling: passwords must NEVER be passed as CLI arguments.
 */
public class FlywayBaselineOperator {

    public static void main(String[] args) {
        String url = null;
        String user = null;
        String version = "1";
        String defaultsFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--url":
                    if (i + 1 < args.length) url = args[++i];
                    break;
                case "--user":
                    if (i + 1 < args.length) user = args[++i];
                    break;
                case "--version":
                    if (i + 1 < args.length) version = args[++i];
                    break;
                case "--defaults-file":
                case "--defaults-extra-file":
                    if (i + 1 < args.length) defaultsFile = args[++i];
                    break;
                default:
                    if (args[i].startsWith("--password")) {
                        System.err.println("FATAL: --password is prohibited on command line for security.");
                        System.exit(1);
                    }
                    break;
            }
        }

        if (url == null || user == null) {
            System.err.println("Usage: FlywayBaselineOperator --url <JDBC_URL> --user <DB_USER> [--version <VER>] [--defaults-file <PATH>]");
            System.exit(1);
        }

        String password = resolvePassword(defaultsFile);

        try {
            System.out.println(">>> Invoking official Flyway baseline API at version " + version + "...");
            Flyway flyway = Flyway.configure()
                    .dataSource(url, user, password)
                    .baselineVersion(version)
                    .baselineDescription("<< Flyway Baseline >>")
                    .load();

            BaselineResult result = flyway.baseline();

            if (result.successfullyBaselined) {
                System.out.println(">>> SUCCESS: Official Flyway baseline established at version " + result.baselineVersion + ".");
                System.exit(0);
            } else {
                System.err.println(">>> FAILURE: Flyway baseline could not be applied. Target version: " + result.baselineVersion);
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println(">>> ERROR: Exception during official Flyway baseline: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String resolvePassword(String defaultsFile) {
        // 1. Try defaults file if provided
        if (defaultsFile != null && !defaultsFile.isBlank()) {
            File f = new File(defaultsFile);
            if (f.exists()) {
                try {
                    // Check file permissions if posix
                    try {
                        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(f.toPath());
                        if (perms.contains(PosixFilePermission.OTHERS_READ) || perms.contains(PosixFilePermission.GROUP_READ)) {
                            System.err.println("WARNING: Credentials file " + defaultsFile + " has permissive permissions. Recommend 0600.");
                        }
                    } catch (UnsupportedOperationException ignored) {
                    }

                    try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.startsWith("password=") || line.startsWith("password =")) {
                                int eqIdx = line.indexOf('=');
                                String pass = line.substring(eqIdx + 1).trim();
                                if (pass.startsWith("\"") && pass.endsWith("\"") && pass.length() >= 2) {
                                    pass = pass.substring(1, pass.length() - 1);
                                }
                                return pass;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("WARNING: Could not parse defaults file: " + e.getMessage());
                }
            }
        }

        // 2. Try environment variables
        String envPass = System.getenv("DB_PASSWORD");
        if (envPass != null && !envPass.isBlank()) return envPass;

        envPass = System.getenv("SPRING_DATASOURCE_PASSWORD");
        if (envPass != null && !envPass.isBlank()) return envPass;

        envPass = System.getenv("MYSQL_PWD");
        if (envPass != null && !envPass.isBlank()) return envPass;

        // Default empty password if unconfigured
        return "";
    }
}
