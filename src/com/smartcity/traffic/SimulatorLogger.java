package com.smartcity.traffic;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SimulatorLogger – File-based logging for the Smart City Traffic Simulator.
 *
 * <p>Installs a custom {@link PrintStream} that:
 * <ul>
 *   <li>Prefixes every printed line with a timestamp.</li>
 *   <li>Writes to a {@code simulator_YYYY-MM-DD_HH-mm-ss.log} file in the
 *       working directory (or the directory specified by
 *       {@link #DEFAULT_LOG_DIR}).</li>
 *   <li>Keeps the last {@link #MAX_BUFFER_LINES} lines in memory so the
 *       in-app "View Logs" popup can show them instantly without re-reading
 *       disk.</li>
 * </ul>
 *
 * <p>Usage – call {@code SimulatorLogger.install()} once before the GUI is
 * built (e.g., at the top of {@code Main.main()}).  After that every
 * {@code System.out.println(…)} call is automatically captured.
 */
public class SimulatorLogger {

    // ── Configuration ─────────────────────────────────────────────────────────

    /** Maximum number of log lines kept in the in-memory ring buffer. */
    public static final int MAX_BUFFER_LINES = 500;

    /**
     * App-specific log directory, rooted in the user's home folder so it is
     * always writable – even when the app is launched from a jpackage installer
     * (where the working directory may be inside a read-only bundle).
     *
     * <ul>
     *   <li>macOS / Linux: {@code ~/SmartCityTraffic/logs}</li>
     *   <li>Windows:       {@code %USERPROFILE%\SmartCityTraffic\logs}</li>
     * </ul>
     */
    private static Path resolveLogDir() {
        return Paths.get(System.getProperty("user.home"), "SmartCityTraffic", "logs");
    }

    // ── Singleton state ───────────────────────────────────────────────────────

    private static SimulatorLogger instance;

    private final Path logFile;
    private final SimpleDateFormat timestampFmt =
            new SimpleDateFormat("HH:mm:ss");

    /** Thread-safe ring buffer of the last MAX_BUFFER_LINES log lines. */
    private final CopyOnWriteArrayList<String> buffer =
            new CopyOnWriteArrayList<>();

    private BufferedWriter fileWriter;

    // ── Private constructor ───────────────────────────────────────────────────

    private SimulatorLogger() throws IOException {
        // Resolve a writable log directory under the user's home folder.
        Path logDir = resolveLogDir();
        Files.createDirectories(logDir);

        // Session log file name contains the startup timestamp.
        String sessionStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                .format(new Date());
        logFile = logDir.resolve("simulator_" + sessionStamp + ".log");

        fileWriter = new BufferedWriter(new FileWriter(logFile.toFile(), true));

        // Write a session header.
        String header = "=== Smart City Traffic Simulator – Session started "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                + " ===";
        writeLine(header);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Installs the logger and redirects {@code System.out} / {@code System.err}
     * through the timestamping tee stream.  Safe to call multiple times (no-op
     * after the first call).
     */
    public static synchronized void install() {
        if (instance != null) return;

        try {
            instance = new SimulatorLogger();
        } catch (IOException e) {
            System.err.println("[SimulatorLogger] Could not create log file: "
                    + e.getMessage());
            return;
        }

        // Replace System.out
        PrintStream teeOut = new TeePrintStream(System.out, false);
        System.setOut(teeOut);

        // Replace System.err
        PrintStream teeErr = new TeePrintStream(System.err, true);
        System.setErr(teeErr);
    }

    /** Returns the absolute path of the current session log file. */
    public static Path getLogFilePath() {
        return instance != null ? instance.logFile : null;
    }

    /**
     * Returns a snapshot of the in-memory log buffer (up to
     * {@link #MAX_BUFFER_LINES} recent lines).
     */
    public static List<String> getBufferedLines() {
        if (instance == null) return Collections.emptyList();
        return new ArrayList<>(instance.buffer);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private synchronized void writeLine(String line) {
        String stamped = "[" + timestampFmt.format(new Date()) + "] " + line;

        // Write to file.
        try {
            if (fileWriter != null) {
                fileWriter.write(stamped);
                fileWriter.newLine();
                fileWriter.flush();
            }
        } catch (IOException ignored) { }

        // Keep buffer bounded.
        buffer.add(stamped);
        while (buffer.size() > MAX_BUFFER_LINES) {
            buffer.remove(0);
        }
    }

    // ── Inner: Tee PrintStream ────────────────────────────────────────────────

    /**
     * A {@link PrintStream} that writes to the original stream AND forwards
     * every complete line to the {@link SimulatorLogger}.
     */
    private static final class TeePrintStream extends PrintStream {

        private final boolean isErr;
        private final StringBuilder lineBuffer = new StringBuilder();

        TeePrintStream(PrintStream wrapped, boolean isErr) {
            super(wrapped, true);
            this.isErr = isErr;
        }

        @Override
        public void println(String x) {
            super.println(x);
            capture(x == null ? "null" : x);
        }

        @Override
        public void println(Object x) {
            super.println(x);
            capture(String.valueOf(x));
        }

        @Override
        public void println() {
            super.println();
            capture("");
        }

        @Override
        public void print(String s) {
            super.print(s);
            if (s != null) {
                lineBuffer.append(s);
                flushLineBuffer();
            }
        }

        @Override
        public void print(Object obj) {
            print(String.valueOf(obj));
        }

        @Override
        public PrintStream printf(String format, Object... args) {
            String s = String.format(format, args);
            print(s);
            return this;
        }

        // ── helpers ───────────────────────────────────────────────────────────

        private void capture(String line) {
            if (instance == null) return;
            String tagged = isErr ? "[ERR] " + line : line;
            instance.writeLine(tagged);
        }

        private void flushLineBuffer() {
            if (instance == null) {
                lineBuffer.setLength(0);
                return;
            }
            int nl;
            while ((nl = lineBuffer.indexOf("\n")) != -1) {
                String line = lineBuffer.substring(0, nl)
                        .replace("\r", "");
                capture(line);
                lineBuffer.delete(0, nl + 1);
            }
        }
    }
}