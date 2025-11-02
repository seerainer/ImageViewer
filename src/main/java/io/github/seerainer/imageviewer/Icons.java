package io.github.seerainer.imageviewer;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Centralized icon manager with proper resource management. Icons are loaded
 * once and shared across all windows.
 */
public class Icons {
    public static final String APP_ICON = """
    	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
    	rkJggg==""";

    private static Icons instance;

    private final Map<String, Image> imageCache = new HashMap<>();

    private final Display display;

    private Icons(final Display display) {
	this.display = display;
    }

    /**
     * Dispose all cached images. Call this when the application shuts down.
     */
    public static void dispose() {
	if (instance == null) {
	    return;
	}
	instance.disposeAll();
	instance = null;
    }

    /**
     * Get a shared image instance. The image is cached and reused. Do NOT dispose
     * images returned by this method.
     *
     * @param imageKey The image key (e.g., APP_ICON)
     * @return The cached image instance
     */
    public static Image getImage(final String imageKey) {
	if (instance == null) {
	    throw new IllegalStateException("Icons not initialized. Call Icons.initialize() first.");
	}
	return instance.loadImage(imageKey);
    }

    /**
     * Initialize the icon manager. Must be called before using getImage().
     *
     * @param display The display to use for creating images
     */
    public static void initialize(final Display display) {
	if (instance == null) {
	    instance = new Icons(display);
	}
    }

    private void disposeAll() {
	imageCache.values().stream().filter((final var image) -> image != null && !image.isDisposed())
		.forEach(Image::dispose);
	imageCache.clear();
    }

    private Image loadImage(final String imageKey) {
	// Return cached image if available
	if (imageCache.containsKey(imageKey)) {
	    return imageCache.get(imageKey);
	}

	// Load and cache the image
	final var bytes = Base64.getMimeDecoder().decode(imageKey.getBytes(UTF_8));
	final var bais = new ByteArrayInputStream(bytes);
	final var image = new Image(display, bais);
	image.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

	imageCache.put(imageKey, image);
	return image;
    }
}