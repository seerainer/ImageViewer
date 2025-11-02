package io.github.seerainer.imageviewer;

/**
 * Result codes for Rust image operations. Must match the Rust enum values.
 */
enum ImageResult {
    SUCCESS(0), ERROR_INVALID_PATH(1), ERROR_INVALID_HANDLE(2), ERROR_LOAD_FAILED(3), ERROR_SAVE_FAILED(4),
    ERROR_ALLOCATION(5), ERROR_UNSUPPORTED_FORMAT(6);

    /**
     * Convert a result code to an ImageResult enum value.
     *
     * @param code Result code from Rust
     * @return Corresponding ImageResult enum value
     */
    static ImageResult fromCode(final int code) {
	for (final var result : values()) {
	    if (result.code == code) {
		return result;
	    }
	}
	return ERROR_INVALID_HANDLE; // Default fallback
    }

    private final int code;

    ImageResult(final int code) {
	this.code = code;
    }

    int getCode() {
	return code;
    }

    boolean isSuccess() {
	return this == SUCCESS;
    }
}