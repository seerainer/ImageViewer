package io.github.seerainer.imageviewer;

/**
 * Resize filter types supported by the image library. Maps to
 * image::imageops::FilterType from Rust's image crate.
 */
public enum ResizeFilter {
    /** Nearest Neighbor - Fast but low quality */
    NEAREST(0, "Nearest (Fast)"),

    /** Triangle (Bilinear) - Good quality, fast */
    TRIANGLE(1, "Bilinear"),

    /** Catmull-Rom (Bicubic) - High quality */
    CATMULL_ROM(2, "Bicubic"),

    /** Gaussian - Smooth, high quality */
    GAUSSIAN(3, "Gaussian"),

    /** Lanczos3 - Best quality, slower */
    LANCZOS3(4, "Lanczos3 (Best)");

    private final int code;
    private final String displayName;

    ResizeFilter(final int code, final String displayName) {
	this.code = code;
	this.displayName = displayName;
    }

    public int getCode() {
	return code;
    }

    public String getDisplayName() {
	return displayName;
    }
}
