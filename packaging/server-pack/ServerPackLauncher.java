import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DxD Craft server pack launcher.
 *
 * 1. Downloads every mod listed in pack.json from its official Modrinth URL (skips files that are
 *    already present with the right SHA-512) and verifies the hash.
 * 2. Installs the NeoForge server the first time (official NeoForge installer).
 * 3. Starts the NeoForge server, passing on the memory flags this launcher was started with.
 *
 * Use it as your server jar:  java -Xmx6G -jar dxdcraft-server-launcher.jar
 */
public final class ServerPackLauncher {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(30)).build();

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String pack = new String(ServerPackLauncher.class.getResourceAsStream("/pack.json").readAllBytes(), StandardCharsets.UTF_8);
        String neoVersion = field(pack, "neoforge");
        log("DxD Craft server pack  (Minecraft " + field(pack, "minecraft") + ", NeoForge " + neoVersion + ")");

        // ---- 1. mods
        Path mods = Files.createDirectories(root.resolve("mods"));
        // Each mod is one {...} object in the "mods" array; read its fields independently of their order.
        Matcher m = Pattern.compile("\\{[^{}]*\"file\"[^{}]*}").matcher(pack);
        int ok = 0, fetched = 0, listed = 0;
        while (m.find()) {
            String obj = m.group();
            String file = field(obj, "file"), url = field(obj, "url"), sha = field(obj, "sha512");
            listed++;
            Path target = mods.resolve(file);
            if (Files.exists(target) && sha.equals(sha512(Files.readAllBytes(target)))) {
                ok++;
                continue;
            }
            log("Downloading " + file);
            byte[] data = download(url);
            if (!sha.equals(sha512(data))) throw new IOException("Hash mismatch for " + file + " - download corrupted, try again");
            Files.write(target, data);
            fetched++;
        }
        if (listed == 0) throw new IllegalStateException("pack.json lists no mods - the launcher is broken, please report it");
        log("Mods ready: " + (ok + fetched) + " (" + fetched + " downloaded)");

        // Updating the pack: an older DxD Craft jar left in mods/ would crash the server (duplicate mod).
        Matcher dxd = Pattern.compile("\"dxdcraft\"\\s*:\\s*\"([^\"]+)\"").matcher(pack);
        if (dxd.find()) {
            String current = dxd.group(1);
            try (var list = Files.list(mods)) {
                for (Path old : list.toList()) {
                    String n = old.getFileName().toString();
                    if (n.startsWith("dxdcraft-") && n.endsWith(".jar") && !n.equals(current) && Files.exists(mods.resolve(current))) {
                        Files.delete(old);
                        log("Removed old " + n + " (replaced by " + current + ")");
                    }
                }
            }
        }

        // ---- 2. NeoForge
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path argsFile = root.resolve("libraries/net/neoforged/neoforge/" + neoVersion + (windows ? "/win_args.txt" : "/unix_args.txt"));
        Path java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
        if (!Files.exists(argsFile)) {
            log("Installing NeoForge " + neoVersion + " server (first start only, takes a minute)...");
            Path installer = root.resolve("neoforge-" + neoVersion + "-installer.jar");
            Files.write(installer, download(field(pack, "installer")));
            int code = new ProcessBuilder(java.toString(), "-jar", installer.toString(), "--installServer")
                    .directory(root.toFile()).inheritIO().start().waitFor();
            Files.deleteIfExists(installer);
            Files.deleteIfExists(root.resolve(installer.getFileName() + ".log"));
            if (code != 0 || !Files.exists(argsFile)) throw new IOException("NeoForge installer failed (exit " + code + ")");
        }

        // ---- 3. launch, forwarding -Xmx / -Xms / -XX flags this launcher was given
        List<String> cmd = new ArrayList<>();
        cmd.add(java.toString());
        for (String a : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (a.startsWith("-Xm") || a.startsWith("-XX") || a.startsWith("-D")) cmd.add(a);
        }
        Path jvmArgs = root.resolve("user_jvm_args.txt");
        if (Files.exists(jvmArgs)) {
            for (String line : Files.readAllLines(jvmArgs)) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) cmd.addAll(List.of(line.split("\\s+")));
            }
        }
        cmd.add("@" + root.relativize(argsFile).toString().replace('\\', '/'));
        cmd.add("nogui");
        cmd.addAll(List.of(args));
        log("Starting server...");
        Process server = new ProcessBuilder(cmd).directory(root.toFile()).inheritIO().start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::destroy));
        System.exit(server.waitFor());
    }

    private static byte[] download(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "dxdcraft-server-pack/1.0")
                .timeout(Duration.ofMinutes(5)).build();
        HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) throw new IOException("HTTP " + res.statusCode() + " for " + url);
        try (InputStream in = res.body()) {
            return in.readAllBytes();
        }
    }

    private static String sha512(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(data));
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!m.find()) throw new IllegalStateException("pack.json missing " + name);
        return m.group(1);
    }

    private static void log(String s) {
        System.out.println("[DxD Pack] " + s);
    }

    @SuppressWarnings("unused")
    private static void move(Path a, Path b) throws IOException {
        Files.move(a, b, StandardCopyOption.REPLACE_EXISTING);
    }
}
