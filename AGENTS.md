# AGENTS.md

## Project Overview
This is a hybrid Java/Rust desktop application. The Java part uses SWT for the GUI, while performance-critical image processing is handled by a Rust library (`rs-image`). The build system is Gradle.

## Build & Test Commands

### General
- **Build All:** `./gradlew build` (compiles Java, builds Rust lib, runs checks)
- **Run Application:** `./gradlew run`
- **Clean:** `./gradlew clean`

### Testing
- **Run All Tests:** `./gradlew test` (or `./gradlew allTests`)
- **Run Unit Tests Only:** `./gradlew unitTest`
- **Run Integration Tests Only:** `./gradlew integrationTest`
- **Run a Single Test Class:**
  ```bash
  ./gradlew test --tests "io.github.seerainer.imageviewer.MyTestClass"
  ```
- **Run a Specific Test Method:**
  ```bash
  ./gradlew test --tests "io.github.seerainer.imageviewer.MyTestClass.myTestMethod"
  ```

### Rust Library
- **Build Rust Lib:** `./gradlew buildRustLib` (or manually in `rs-image/`: `cargo build --release`)
- **Copy Native Libs:** `./gradlew copyRustLib` (Automatically runs with build/test tasks)

### Native Image
- **Build Native Image:** `./gradlew nativeCompile` (Requires GraalVM)

## Code Style & Conventions

### Java (SWT)
- **Framework:** SWT (Standard Widget Toolkit).
- **Style:**
  - **Types:** Use `var` for local variables where type is obvious.
  - **Final:** Mark local variables and parameters as `final` where possible (see `Main.java`).
  - **Visibility:** Explicitly define visibility (private/public).
  - **Imports:** Organize imports. Avoid wildcard imports (e.g., `import java.util.*`).
- **Structure:**
  - Main class: `io.github.seerainer.imageviewer.Main`
  - UI Logic: `MainWindow.java`
  - Native Interface: `RustImageLib.java` (JNI/FFI bridge)
- **Error Handling:** Use try-catch-finally blocks, ensuring SWT resources (Display, Shell, Images) are disposed in `finally`.

### Rust (rs-image)
- **Style:** Standard Rust idioms (fmt, clippy).
- **Crate Type:** `cdylib` for FFI compatibility.
- **Dependencies:** Minimal. Currently uses `image` crate.
- **Profile:** Release profile optimized for size (`opt-level = "z"`, `strip = true`).

### Integration (JNI/FFI)
- **Loading:** Native library is loaded from `build/native`.
- **System Property:** `java.library.path` must point to `build/native` during run/test. This is handled automatically by Gradle tasks.

## Key Files & Directories
- `build.gradle`: Main build script. Handles platform-specific SWT artifacts and Rust compilation.
- `src/main/java/`: Java source code.
- `rs-image/`: Rust library source code.
- `rs-image/Cargo.toml`: Rust dependency configuration.

## Environment Details
- **Java Version:** 25
- **Gradle:** Wrapper provided (`gradlew`).
- **Rust:** Cargo required for building the native library.
