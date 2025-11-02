package io.github.seerainer.imageviewer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("ImageService Unit Tests")
class ImageServiceTest {

    private static Display display;

    @BeforeAll
    static void setupDisplay() {
	// Create display for SWT operations
	display = Display.getDefault();
    }

    @AfterAll
    static void tearDownDisplay() {
	if (display != null && !display.isDisposed()) {
	    display.dispose();
	}
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("adjustBrightness should return null for null image")
    void testAdjustBrightnessNullImage() {
	final var result = ImageService.adjustBrightness(display, null, 50);
	assertThat(result).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("adjustContrast should return null for null image")
    void testAdjustContrastNullImage() {
	final var result = ImageService.adjustContrast(display, null, 1.5f);
	assertThat(result).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("blur should return null for null image")
    void testBlurNullImage() {
	final var result = ImageService.blur(display, null, 2.0f);
	assertThat(result).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("flipHorizontal should return null for null image")
    void testFlipHorizontalNullImage() {
	final var result = ImageService.flipHorizontal(display, null);
	assertThat(result).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("flipVertical should return null for null image")
    void testFlipVerticalNullImage() {
	final var result = ImageService.flipVertical(display, null);
	assertThat(result).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("grayscale should return null for null image")
    void testGrayscaleNullImage() {
	final var result = ImageService.grayscale(display, null);
	assertThat(result).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("invert should return null for null image")
    void testInvertNullImage() {
	final var result = ImageService.invert(display, null);
	assertThat(result).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("loadImage should return null for invalid path")
    void testLoadImageInvalidPath() {
	final var image = ImageService.loadImage(display, "");
	assertThat(image).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("loadImage should return null for non-existent file")
    void testLoadImageNonExistentFile() {
	final var image = ImageService.loadImage(display, "nonexistent_file.png");
	assertThat(image).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("loadImage should return null for null path")
    void testLoadImageNullPath() {
	assertThatCode(() -> {
	    final var image = ImageService.loadImage(display, null);
	    assertThat(image).isNull();
	}).doesNotThrowAnyException();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Operations should handle disposed images gracefully")
    void testOperationsWithDisposedImage() {
	// Create a small test image
	final var image = new Image(display, 10, 10);
	image.dispose();

	assertThat(ImageService.rotateRight(display, image)).isNull();
	assertThat(ImageService.rotateLeft(display, image)).isNull();
	assertThat(ImageService.flipHorizontal(display, image)).isNull();
	assertThat(ImageService.flipVertical(display, image)).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("rotateLeft should return null for null image")
    void testRotateLeftNullImage() {
	final var result = ImageService.rotateLeft(display, null);
	assertThat(result).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("rotateRight should return null for null image")
    void testRotateRightNullImage() {
	final var result = ImageService.rotateRight(display, null);
	assertThat(result).isNull();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("saveImage should return false for null image")
    void testSaveImageNullImage() {
	final var result = ImageService.saveImage(null, "output.png");
	assertThat(result).isFalse();
    }
}
