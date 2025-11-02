package io.github.seerainer.imageviewer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("Rust Image Processing Integration Tests")
class RustImageIntegrationTest {

    private static Display display;
    private static Path testImagePath;
    private static Path tempDir;

    private static Path createTestImage() {
	// Create a simple 100x100 red square image
	final var width = 100;
	final var height = 100;
	final var palette = new PaletteData(0xFF0000, 0x00FF00, 0x0000FF);
	final var imageData = new ImageData(width, height, 24, palette);

	// Fill with red color
	for (var y = 0; y < height; y++) {
	    for (var x = 0; x < width; x++) {
		imageData.setPixel(x, y, 0xFF0000);
	    }
	}

	final var image = new Image(display, imageData);
	final var path = tempDir.resolve("test_image.png");

	final var loader = new ImageLoader();
	loader.data = new ImageData[] { imageData };
	loader.save(path.toString(), SWT.IMAGE_PNG);

	image.dispose();
	return path;
    }

    @BeforeAll
    static void setupTest() throws IOException {
	display = Display.getDefault();
	tempDir = Files.createTempDirectory("imageviewer-test");
	testImagePath = createTestImage();
    }

    @AfterAll
    static void tearDownTest() throws IOException {
	if (display != null && !display.isDisposed()) {
	    display.dispose();
	}
	if (tempDir != null && Files.exists(tempDir)) {
	    try (var walk = Files.walk(tempDir)) {
		walk.sorted(Comparator.reverseOrder()).forEach(path -> {
		    try {
			Files.deleteIfExists(path);
		    } catch (final IOException e) {
			// Ignore cleanup errors
		    }
		});
	    }
	}
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should handle brightness adjustment")
    void testAdjustBrightness() {
	final var original = new Image(display, testImagePath.toString());

	final var brightened = ImageService.adjustBrightness(display, original, 50);

	assertThat(brightened).isNotNull();
	assertThat(brightened.isDisposed()).isFalse();

	final var bounds = brightened.getBounds();
	assertThat(bounds.width).isEqualTo(100);
	assertThat(bounds.height).isEqualTo(100);

	original.dispose();
	brightened.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should handle contrast adjustment")
    void testAdjustContrast() {
	final var original = new Image(display, testImagePath.toString());

	final var adjusted = ImageService.adjustContrast(display, original, 1.5f);

	assertThat(adjusted).isNotNull();
	assertThat(adjusted.isDisposed()).isFalse();

	final var bounds = adjusted.getBounds();
	assertThat(bounds.width).isEqualTo(100);
	assertThat(bounds.height).isEqualTo(100);

	original.dispose();
	adjusted.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should handle blur filter")
    void testBlur() {
	final var original = new Image(display, testImagePath.toString());

	final var blurred = ImageService.blur(display, original, 2.0f);

	assertThat(blurred).isNotNull();
	assertThat(blurred.isDisposed()).isFalse();

	final var bounds = blurred.getBounds();
	assertThat(bounds.width).isEqualTo(100);
	assertThat(bounds.height).isEqualTo(100);

	original.dispose();
	blurred.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should handle extreme brightness values")
    void testExtremeBrightness() {
	final var original = new Image(display, testImagePath.toString());

	// Test maximum brightness
	var adjusted = ImageService.adjustBrightness(display, original, 100);
	assertThat(adjusted).isNotNull();
	adjusted.dispose();

	// Test minimum brightness
	adjusted = ImageService.adjustBrightness(display, original, -100);
	assertThat(adjusted).isNotNull();
	adjusted.dispose();

	original.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should flip image horizontally")
    void testFlipHorizontal() {
	final var original = new Image(display, testImagePath.toString());
	final var originalBounds = original.getBounds();

	final var flipped = ImageService.flipHorizontal(display, original);

	assertThat(flipped).isNotNull();
	assertThat(flipped.isDisposed()).isFalse();

	final var flippedBounds = flipped.getBounds();
	// Dimensions should remain the same after flip
	assertThat(flippedBounds.width).isEqualTo(originalBounds.width);
	assertThat(flippedBounds.height).isEqualTo(originalBounds.height);

	original.dispose();
	flipped.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should flip image vertically")
    void testFlipVertical() {
	final var original = new Image(display, testImagePath.toString());
	final var originalBounds = original.getBounds();

	final var flipped = ImageService.flipVertical(display, original);

	assertThat(flipped).isNotNull();
	assertThat(flipped.isDisposed()).isFalse();

	final var flippedBounds = flipped.getBounds();
	// Dimensions should remain the same after flip
	assertThat(flippedBounds.width).isEqualTo(originalBounds.width);
	assertThat(flippedBounds.height).isEqualTo(originalBounds.height);

	original.dispose();
	flipped.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should handle 360° rotation (4x90°) returning to original dimensions")
    void testFullRotation() {
	var image = new Image(display, testImagePath.toString());
	final var originalBounds = image.getBounds();

	// Rotate 4 times (360°)
	for (var i = 0; i < 4; i++) {
	    final var rotated = ImageService.rotateRight(display, image);
	    assertThat(rotated).isNotNull();
	    image.dispose();
	    image = rotated;
	}

	final var finalBounds = image.getBounds();
	assertThat(finalBounds.width).isEqualTo(originalBounds.width);
	assertThat(finalBounds.height).isEqualTo(originalBounds.height);

	image.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should convert to grayscale")
    void testGrayscale() {
	final var original = new Image(display, testImagePath.toString());

	final var grayscale = ImageService.grayscale(display, original);

	assertThat(grayscale).isNotNull();
	assertThat(grayscale.isDisposed()).isFalse();

	final var bounds = grayscale.getBounds();
	assertThat(bounds.width).isEqualTo(100);
	assertThat(bounds.height).isEqualTo(100);

	original.dispose();
	grayscale.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should invert colors")
    void testInvert() {
	final var original = new Image(display, testImagePath.toString());

	final var inverted = ImageService.invert(display, original);

	assertThat(inverted).isNotNull();
	assertThat(inverted.isDisposed()).isFalse();

	final var bounds = inverted.getBounds();
	assertThat(bounds.width).isEqualTo(100);
	assertThat(bounds.height).isEqualTo(100);

	original.dispose();
	inverted.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should load valid PNG image via Rust")
    void testLoadImage() {
	final var image = ImageService.loadImage(display, testImagePath.toString());

	assertThat(image).isNotNull();
	assertThat(image.isDisposed()).isFalse();

	final var bounds = image.getBounds();
	assertThat(bounds.width).isEqualTo(100);
	assertThat(bounds.height).isEqualTo(100);

	image.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should perform multiple transformations in sequence")
    void testMultipleTransformations() {
	var image = new Image(display, testImagePath.toString());

	// Rotate right
	var transformed = ImageService.rotateRight(display, image);
	assertThat(transformed).isNotNull();
	image.dispose();
	image = transformed;

	// Flip horizontal
	transformed = ImageService.flipHorizontal(display, image);
	assertThat(transformed).isNotNull();
	image.dispose();
	image = transformed;

	// Rotate left
	transformed = ImageService.rotateLeft(display, image);
	assertThat(transformed).isNotNull();
	image.dispose();
	image = transformed;

	// Final image should still be valid
	assertThat(image.isDisposed()).isFalse();
	final var bounds = image.getBounds();
	assertThat(bounds.width).isGreaterThan(0);
	assertThat(bounds.height).isGreaterThan(0);

	image.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should resize image")
    void testResize() {
	final var original = new Image(display, testImagePath.toString());

	final var resized = ImageService.resizeWithFilter(display, original, 50, 50, ResizeFilter.LANCZOS3);

	assertThat(resized).isNotNull();
	assertThat(resized.isDisposed()).isFalse();

	final var resizedBounds = resized.getBounds();
	assertThat(resizedBounds.width).isLessThanOrEqualTo(50);
	assertThat(resizedBounds.height).isLessThanOrEqualTo(50);

	original.dispose();
	resized.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should rotate image left (90° counter-clockwise)")
    void testRotateLeft() {
	final var original = new Image(display, testImagePath.toString());
	final var originalBounds = original.getBounds();

	final var rotated = ImageService.rotateLeft(display, original);

	assertThat(rotated).isNotNull();
	assertThat(rotated.isDisposed()).isFalse();

	final var rotatedBounds = rotated.getBounds();
	// After 90° rotation, width and height should be swapped
	assertThat(rotatedBounds.width).isEqualTo(originalBounds.height);
	assertThat(rotatedBounds.height).isEqualTo(originalBounds.width);

	original.dispose();
	rotated.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should rotate image right (90° clockwise)")
    void testRotateRight() {
	final var original = new Image(display, testImagePath.toString());
	final var originalBounds = original.getBounds();

	final var rotated = ImageService.rotateRight(display, original);

	assertThat(rotated).isNotNull();
	assertThat(rotated.isDisposed()).isFalse();

	final var rotatedBounds = rotated.getBounds();
	// After 90° rotation, width and height should be swapped
	assertThat(rotatedBounds.width).isEqualTo(originalBounds.height);
	assertThat(rotatedBounds.height).isEqualTo(originalBounds.width);

	original.dispose();
	rotated.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should save image via Rust")
    void testSaveImage() throws IOException {
	final var original = new Image(display, testImagePath.toString());
	final var outputPath = tempDir.resolve("output.png");

	final var result = ImageService.saveImage(original, outputPath.toString());

	assertThat(result).isTrue();
	assertThat(Files.exists(outputPath)).isTrue();
	assertThat(Files.size(outputPath)).isGreaterThan(0);

	original.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should handle small blur sigma")
    void testSmallBlur() {
	final var original = new Image(display, testImagePath.toString());

	final var blurred = ImageService.blur(display, original, 0.5f);

	assertThat(blurred).isNotNull();
	assertThat(blurred.isDisposed()).isFalse();

	original.dispose();
	blurred.dispose();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Should handle resize to larger dimensions")
    void testUpscale() {
	final var original = new Image(display, testImagePath.toString());

	final var upscaled = ImageService.resizeWithFilter(display, original, 200, 200, ResizeFilter.LANCZOS3);

	assertThat(upscaled).isNotNull();
	assertThat(upscaled.isDisposed()).isFalse();

	final var bounds = upscaled.getBounds();
	assertThat(bounds.width).isGreaterThanOrEqualTo(100);
	assertThat(bounds.height).isGreaterThanOrEqualTo(100);

	original.dispose();
	upscaled.dispose();
    }
}
