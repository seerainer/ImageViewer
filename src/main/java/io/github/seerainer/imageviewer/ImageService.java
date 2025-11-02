package io.github.seerainer.imageviewer;

import java.lang.foreign.MemorySegment;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

/**
 * Service for image operations using Rust image-rs library. Handles conversion
 * between SWT Images and Rust image handles.
 */
final class ImageService {

    private ImageService() {
	// Utility class
    }

    /**
     * Adjust brightness.
     *
     * @param display SWT Display
     * @param image   Source image
     * @param value   Brightness adjustment (-100 to 100)
     * @return Adjusted image or null on error
     */
    static Image adjustBrightness(final Display display, final Image image, final int value) {
	return transformImage(display, image, handle -> RustImageLib.adjustBrightness(handle, value));
    }

    /**
     * Adjust contrast.
     *
     * @param display  SWT Display
     * @param image    Source image
     * @param contrast Contrast value
     * @return Adjusted image or null on error
     */
    static Image adjustContrast(final Display display, final Image image, final float contrast) {
	return transformImage(display, image, handle -> RustImageLib.adjustContrast(handle, contrast));
    }

    /**
     * Apply blur effect.
     *
     * @param display SWT Display
     * @param image   Source image
     * @param sigma   Blur strength
     * @return Blurred image or null on error
     */
    static Image blur(final Display display, final Image image, final float sigma) {
	return transformImage(display, image, handle -> RustImageLib.blur(handle, sigma));
    }

    private static MemorySegment convertToRustHandle(final Image image) {
	final var imageData = image.getImageData();
	final var width = imageData.width;
	final var height = imageData.height;

	// Convert SWT image data to RGBA format
	final var rgbaData = new byte[width * height * 4];
	final var palette = imageData.palette;

	var rgbaIndex = 0;
	for (var y = 0; y < height; y++) {
	    for (var x = 0; x < width; x++) {
		final var pixel = imageData.getPixel(x, y);
		final var rgb = palette.getRGB(pixel);

		rgbaData[rgbaIndex++] = (byte) rgb.red;
		rgbaData[rgbaIndex++] = (byte) rgb.green;
		rgbaData[rgbaIndex++] = (byte) rgb.blue;

		// Alpha
		if (imageData.alphaData != null) {
		    rgbaData[rgbaIndex++] = imageData.alphaData[y * width + x];
		} else {
		    rgbaData[rgbaIndex++] = (byte) 0xFF;
		}
	    }
	}

	return RustImageLib.fromRgbaData(rgbaData, width, height);
    }

    private static Image convertToSwtImage(final Display display, final MemorySegment handle) {
	final var width = RustImageLib.getWidth(handle);
	final var height = RustImageLib.getHeight(handle);
	final var dataPtr = RustImageLib.getData(handle);
	final var dataLen = RustImageLib.getDataLen(handle);

	if (dataPtr == null || dataPtr.address() == 0 || dataLen == 0) {
	    return null;
	}

	// RGBA format from Rust
	final var data = new byte[(int) dataLen];
	MemorySegment.ofAddress(dataPtr.address()).reinterpret(dataLen).asByteBuffer().get(data);

	// Create SWT ImageData with RGBA palette
	final var palette = new PaletteData(0xFF000000, 0x00FF0000, 0x0000FF00);
	final var imageData = new ImageData(width, height, 32, palette);

	// Copy RGBA data
	imageData.data = data;

	// Handle alpha channel
	final var alphaData = new byte[width * height];
	for (var i = 0; i < alphaData.length; i++) {
	    alphaData[i] = data[i * 4 + 3]; // Extract alpha from RGBA
	}
	imageData.alphaData = alphaData;

	return new Image(display, imageData);
    }

    /**
     * Flip image horizontally.
     *
     * @param display SWT Display
     * @param image   Source image
     * @return Flipped image or null on error
     */
    static Image flipHorizontal(final Display display, final Image image) {
	return transformImage(display, image, RustImageLib::flipHorizontal);
    }

    /**
     * Flip image vertically.
     *
     * @param display SWT Display
     * @param image   Source image
     * @return Flipped image or null on error
     */
    static Image flipVertical(final Display display, final Image image) {
	return transformImage(display, image, RustImageLib::flipVertical);
    }

    /**
     * Convert to grayscale.
     *
     * @param display SWT Display
     * @param image   Source image
     * @return Grayscale image or null on error
     */
    static Image grayscale(final Display display, final Image image) {
	return transformImage(display, image, RustImageLib::grayscale);
    }

    /**
     * Invert colors.
     *
     * @param display SWT Display
     * @param image   Source image
     * @return Inverted image or null on error
     */
    static Image invert(final Display display, final Image image) {
	return transformImage(display, image, RustImageLib::invert);
    }

    /**
     * Load an image from file using Rust.
     *
     * @param display  SWT Display
     * @param filePath Path to image file
     * @return SWT Image or null on error
     */
    static Image loadImage(final Display display, final String filePath) {
	final var handle = RustImageLib.loadImage(filePath);
	if (handle == null || handle.address() == 0) {
	    return null;
	}

	try {
	    return convertToSwtImage(display, handle);
	} finally {
	    RustImageLib.freeImage(handle);
	}
    }

    /**
     * Resize image maintaining aspect ratio with specified filter quality.
     *
     * @param display SWT Display
     * @param image   Source image
     * @param width   Target width
     * @param height  Target height
     * @param filter  Resize filter to use
     * @return Resized image or null on error
     */
    static Image resizeWithFilter(final Display display, final Image image, final int width, final int height,
	    final ResizeFilter filter) {
	return transformImage(display, image,
		handle -> RustImageLib.resizeWithFilter(handle, width, height, filter.getCode()));
    }

    /**
     * Rotate image 90 degrees counter-clockwise.
     *
     * @param display SWT Display
     * @param image   Source image
     * @return Rotated image or null on error
     */
    static Image rotateLeft(final Display display, final Image image) {
	return transformImage(display, image, RustImageLib::rotateLeft);
    }

    /**
     * Rotate image 90 degrees clockwise.
     *
     * @param display SWT Display
     * @param image   Source image
     * @return Rotated image or null on error
     */
    static Image rotateRight(final Display display, final Image image) {
	return transformImage(display, image, RustImageLib::rotateRight);
    }

    /**
     * Save an SWT image to file using Rust.
     *
     * @param image    SWT Image to save
     * @param filePath Destination file path
     * @return true if successful
     */
    static boolean saveImage(final Image image, final String filePath) {
	if (image == null || image.isDisposed()) {
	    return false;
	}

	final var handle = convertToRustHandle(image);
	if (handle == null || handle.address() == 0) {
	    return false;
	}

	try {
	    final var result = RustImageLib.saveImage(handle, filePath);
	    return ImageResult.fromCode(result).isSuccess();
	} finally {
	    RustImageLib.freeImage(handle);
	}
    }

    private static Image transformImage(final Display display, final Image image, final ImageTransform transform) {
	if (image == null || image.isDisposed()) {
	    return null;
	}

	final var handle = convertToRustHandle(image);
	if (handle == null || handle.address() == 0) {
	    return null;
	}

	try {
	    final var result = transform.apply(handle);
	    if (!ImageResult.fromCode(result).isSuccess()) {
		return null;
	    }
	    return convertToSwtImage(display, handle);
	} finally {
	    RustImageLib.freeImage(handle);
	}
    }

    @FunctionalInterface
    private interface ImageTransform {
	int apply(MemorySegment handle);
    }
}
