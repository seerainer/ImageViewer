package io.github.seerainer.imageviewer;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for Rust image processing library using image-rs. Provides
 * high-performance image operations via native code.
 */
final class RustImageLib {

    private static final String LIBRARY_NAME = "rs_image";
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup SYMBOL_LOOKUP;

    // Function handles
    private static final MethodHandle IMAGE_LOAD;
    private static final MethodHandle IMAGE_FROM_RGBA;
    private static final MethodHandle IMAGE_SAVE;
    private static final MethodHandle IMAGE_FREE;
    private static final MethodHandle IMAGE_ROTATE_90;
    private static final MethodHandle IMAGE_ROTATE_180;
    private static final MethodHandle IMAGE_ROTATE_270;
    private static final MethodHandle IMAGE_FLIP_HORIZONTAL;
    private static final MethodHandle IMAGE_FLIP_VERTICAL;
    private static final MethodHandle IMAGE_RESIZE_WITH_FILTER;
    private static final MethodHandle IMAGE_ADJUST_BRIGHTNESS;
    private static final MethodHandle IMAGE_ADJUST_CONTRAST;
    private static final MethodHandle IMAGE_BLUR;
    private static final MethodHandle IMAGE_GRAYSCALE;
    private static final MethodHandle IMAGE_INVERT;
    private static final MethodHandle IMAGE_GET_WIDTH;
    private static final MethodHandle IMAGE_GET_HEIGHT;
    private static final MethodHandle IMAGE_GET_DATA;
    private static final MethodHandle IMAGE_GET_DATA_LEN;

    static {
	System.loadLibrary(LIBRARY_NAME);
	SYMBOL_LOOKUP = SymbolLookup.loaderLookup();

	IMAGE_LOAD = findFunction("image_load", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
	IMAGE_FROM_RGBA = findFunction("image_from_rgba", FunctionDescriptor.of(ValueLayout.ADDRESS,
		ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
	IMAGE_SAVE = findFunction("image_save",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
	IMAGE_FREE = findFunction("image_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
	IMAGE_ROTATE_90 = findFunction("image_rotate_90",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
	IMAGE_ROTATE_180 = findFunction("image_rotate_180",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
	IMAGE_ROTATE_270 = findFunction("image_rotate_270",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
	IMAGE_FLIP_HORIZONTAL = findFunction("image_flip_horizontal",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
	IMAGE_FLIP_VERTICAL = findFunction("image_flip_vertical",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
	IMAGE_RESIZE_WITH_FILTER = findFunction("image_resize_with_filter", FunctionDescriptor.of(ValueLayout.JAVA_INT,
		ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
	IMAGE_ADJUST_BRIGHTNESS = findFunction("image_adjust_brightness",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
	IMAGE_ADJUST_CONTRAST = findFunction("image_adjust_contrast",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT));
	IMAGE_BLUR = findFunction("image_blur",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT));
	IMAGE_GRAYSCALE = findFunction("image_grayscale",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
	IMAGE_INVERT = findFunction("image_invert", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
	IMAGE_GET_WIDTH = findFunction("image_get_width",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
	IMAGE_GET_HEIGHT = findFunction("image_get_height",
		FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
	IMAGE_GET_DATA = findFunction("image_get_data",
		FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
	IMAGE_GET_DATA_LEN = findFunction("image_get_data_len",
		FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
    }

    private RustImageLib() {
	// Utility class
    }

    static int adjustBrightness(final MemorySegment handle, final int value) {
	if (handle == null || handle.address() == 0) {
	    return ImageResult.ERROR_INVALID_HANDLE.getCode();
	}
	try {
	    return (int) IMAGE_ADJUST_BRIGHTNESS.invoke(handle, value);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to adjust brightness", e);
	}
    }

    static int adjustContrast(final MemorySegment handle, final float contrast) {
	if (handle == null || handle.address() == 0) {
	    return ImageResult.ERROR_INVALID_HANDLE.getCode();
	}
	try {
	    return (int) IMAGE_ADJUST_CONTRAST.invoke(handle, contrast);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to adjust contrast", e);
	}
    }

    static int blur(final MemorySegment handle, final float sigma) {
	if (handle == null || handle.address() == 0) {
	    return ImageResult.ERROR_INVALID_HANDLE.getCode();
	}
	try {
	    return (int) IMAGE_BLUR.invoke(handle, sigma);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to blur image", e);
	}
    }

    private static MethodHandle findFunction(final String name, final FunctionDescriptor descriptor) {
	return SYMBOL_LOOKUP.find(name).map(addr -> LINKER.downcallHandle(addr, descriptor))
		.orElseThrow(() -> new UnsatisfiedLinkError("Failed to find function: " + name));
    }

    static int flipHorizontal(final MemorySegment handle) {
	return invokeTransform(IMAGE_FLIP_HORIZONTAL, handle);
    }

    static int flipVertical(final MemorySegment handle) {
	return invokeTransform(IMAGE_FLIP_VERTICAL, handle);
    }

    /**
     * Free an image handle.
     *
     * @param handle Image handle to free
     */
    static void freeImage(final MemorySegment handle) {
	if (handle == null || handle.address() == 0) {
	    return;
	}
	try {
	    IMAGE_FREE.invoke(handle);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to free image", e);
	}
    }

    /**
     * Create an image from raw RGBA data.
     *
     * @param rgbaData RGBA pixel data (4 bytes per pixel)
     * @param width    Image width
     * @param height   Image height
     * @return Image handle or null on error
     */
    static MemorySegment fromRgbaData(final byte[] rgbaData, final int width, final int height) {
	if (rgbaData == null || width <= 0 || height <= 0) {
	    return null;
	}
	if (rgbaData.length != width * height * 4) {
	    throw new IllegalArgumentException("RGBA data size must match width * height * 4");
	}
	try (final var arena = Arena.ofConfined()) {
	    final var dataSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, rgbaData);
	    return (MemorySegment) IMAGE_FROM_RGBA.invoke(dataSegment, width, height);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to create image from RGBA data", e);
	}
    }

    static MemorySegment getData(final MemorySegment handle) {
	if (handle == null || handle.address() == 0) {
	    return null;
	}
	try {
	    return (MemorySegment) IMAGE_GET_DATA.invoke(handle);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to get image data", e);
	}
    }

    static long getDataLen(final MemorySegment handle) {
	if (handle == null || handle.address() == 0) {
	    return 0;
	}
	try {
	    return (long) IMAGE_GET_DATA_LEN.invoke(handle);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to get image data length", e);
	}
    }

    static int getHeight(final MemorySegment handle) {
	if (handle == null || handle.address() == 0) {
	    return 0;
	}
	try {
	    return (int) IMAGE_GET_HEIGHT.invoke(handle);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to get image height", e);
	}
    }

    static int getWidth(final MemorySegment handle) {
	if (handle == null || handle.address() == 0) {
	    return 0;
	}
	try {
	    return (int) IMAGE_GET_WIDTH.invoke(handle);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to get image width", e);
	}
    }

    static int grayscale(final MemorySegment handle) {
	return invokeTransform(IMAGE_GRAYSCALE, handle);
    }

    static int invert(final MemorySegment handle) {
	return invokeTransform(IMAGE_INVERT, handle);
    }

    private static int invokeTransform(final MethodHandle handle, final MemorySegment imageHandle) {
	if (imageHandle == null || imageHandle.address() == 0) {
	    return ImageResult.ERROR_INVALID_HANDLE.getCode();
	}
	try {
	    return (int) handle.invoke(imageHandle);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to transform image", e);
	}
    }

    /**
     * Load an image from file path.
     *
     * @param path File path to the image
     * @return Image handle or null on error
     */
    static MemorySegment loadImage(final String path) {
	if (path == null || path.isEmpty()) {
	    return null;
	}
	try (final var arena = Arena.ofConfined()) {
	    final var pathSegment = arena.allocateFrom(path);
	    return (MemorySegment) IMAGE_LOAD.invoke(pathSegment);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to load image", e);
	}
    }

    static int resizeWithFilter(final MemorySegment handle, final int width, final int height, final int filter) {
	if (handle == null || handle.address() == 0) {
	    return ImageResult.ERROR_INVALID_HANDLE.getCode();
	}
	try {
	    return (int) IMAGE_RESIZE_WITH_FILTER.invoke(handle, width, height, filter);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to resize image with filter", e);
	}
    }

    static int rotate180(final MemorySegment handle) {
	return invokeTransform(IMAGE_ROTATE_180, handle);
    }

    static int rotateLeft(final MemorySegment handle) {
	return invokeTransform(IMAGE_ROTATE_270, handle);
    }

    static int rotateRight(final MemorySegment handle) {
	return invokeTransform(IMAGE_ROTATE_90, handle);
    }

    /**
     * Save an image to file path.
     *
     * @param handle Image handle
     * @param path   File path to save to
     * @return Result code (0 = success)
     */
    static int saveImage(final MemorySegment handle, final String path) {
	if (handle == null || handle.address() == 0) {
	    return ImageResult.ERROR_INVALID_HANDLE.getCode();
	}
	try (final var arena = Arena.ofConfined()) {
	    final var pathSegment = arena.allocateFrom(path);
	    return (int) IMAGE_SAVE.invoke(handle, pathSegment);
	} catch (final Throwable e) {
	    throw new RuntimeException("Failed to save image", e);
	}
    }
}
