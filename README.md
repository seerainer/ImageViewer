# ImageViewer 🖼️

A small desktop image viewer application with a Rust-based image processing native library.

This repository contains two main parts:

- Java application (Gradle) — the main ImageViewer app
- Rust crate (`rs-image`) — native image processing library compiled to a shared library

This project uses the Java Foreign Function & Memory (FFM) API to call the Rust native library from the JVM. The build also uses GraalVM native-image tooling for optional native image builds.

---

## Key features ✨

- Fast native image processing using Rust called from Java via the FFM (Foreign Function & Memory) API
- Java/Gradle front-end with a small UI
- Cross-platform support (Windows, macOS, Linux)
- Integration tests and unit tests included

---

## Requirements 🧰

## Dependencies

Brief list of required components to build and run the project:

- Java JDK 21+ (JDK required; Gradle toolchain targets Java 25)
- Rust toolchain (rustup + cargo) to build the native `rs-image` crate
- Native build tools / linker: Visual Studio Build Tools on Windows, Xcode CLI on macOS, or GCC/Clang on Linux
- Gradle wrapper (included) — no system Gradle required

Runtime notes:

- SWT is pulled via Gradle (platform-specific artifact configured in `build.gradle`)
- The Rust native library must be present in `build/native` (Windows: `rs_image.dll`, macOS: `librs_image.dylib`, Linux: `librs_image.so`)

More install guidance: https://www.rust-lang.org/tools/install

---

## Quickstart — Cross-platform ⚡

The repository contains a Gradle task that builds the Rust crate and copies the produced native artifact into `build/native`:

- Gradle task: `copyRustLib` (runs `cargo build --release` in `rs-image` and copies the platform-specific library)

Build everything (recommended):

- Windows (cmd.exe):

```cmd
.\gradlew.bat build
```

- macOS / Linux (bash/zsh):

```bash
./gradlew build
```

If you prefer to only build the native library and copy it into place (no Java build):

- Windows (cmd.exe):

```cmd
.\gradlew.bat copyRustLib
```

- macOS / Linux:

```bash
./gradlew copyRustLib
```

Manual steps (if you want to run `cargo` manually):

- Build the Rust native library:

```bash
cd rs-image
cargo build --release
```

- The build produces a platform-specific artifact in `rs-image/target/release/`:
  - Windows: `rs_image.dll`
  - macOS: `librs_image.dylib`
  - Linux: `librs_image.so`

- Copy the resulting file into `build/native` (the Gradle task automates this):

Windows (cmd.exe):

```cmd
copy rs-image\target\release\rs_image.dll build\native\
```

macOS / Linux:

```bash
cp rs-image/target/release/librs_image.* build/native/
```

---

## Run the application ▶️

The `run` task depends on `copyRustLib` so it will ensure the native library is available.

- Windows (cmd.exe):

```cmd
.\gradlew.bat run
```

- macOS / Linux:

```bash
./gradlew run
```

You can also run the JAR directly after a build (the gradle distribution may place a runnable JAR in `build/libs`):

```bash
java -jar build/libs/ImageViewer-0.1.0.jar
```

If you run the JAR directly, ensure `build/native` is on `java.library.path` or copy the native library into a directory on the system library path.

---

## Tests ✅

Run unit and integration tests with Gradle. The Gradle test configuration ensures the native library is built and copied before tests run.

- Windows (cmd.exe):

```cmd
.\gradlew.bat test
```

- macOS / Linux:

```bash
./gradlew test
```

Integration tests that exercise the Rust library expect the native library to be present in `build/native` (the `copyRustLib` task ensures this).

---

## Contributing 🤝

Contributions welcome! Please open issues or pull requests.

---

## License 📄

This project is licensed under the MIT License. See `LICENSE` for details.