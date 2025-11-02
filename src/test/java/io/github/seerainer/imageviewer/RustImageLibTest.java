package io.github.seerainer.imageviewer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("RustImageLib Unit Tests")
class RustImageLibTest {

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("adjustBrightness should return error for null handle")
    void testAdjustBrightnessNullHandle() {
	final var result = RustImageLib.adjustBrightness(null, 50);
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("adjustContrast should return error for null handle")
    void testAdjustContrastNullHandle() {
	final var result = RustImageLib.adjustContrast(null, 1.5f);
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("blur should return error for null handle")
    void testBlurNullHandle() {
	final var result = RustImageLib.blur(null, 2.0f);
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("flipHorizontal should return error for null handle")
    void testFlipHorizontalNullHandle() {
	final var result = RustImageLib.flipHorizontal(null);
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("flipVertical should return error for null handle")
    void testFlipVerticalNullHandle() {
	final var result = RustImageLib.flipVertical(null);
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("freeImage should handle null handle gracefully")
    void testFreeImageNullHandle() {
	assertThatCode(() -> RustImageLib.freeImage(null)).doesNotThrowAnyException();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("freeImage should handle zero address gracefully")
    void testFreeImageZeroAddress() {
	final var nullSegment = MemorySegment.ofAddress(0);
	assertThatCode(() -> RustImageLib.freeImage(nullSegment)).doesNotThrowAnyException();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("getDataLen should return 0 for null handle")
    void testGetDataLenNullHandle() {
	assertThat(RustImageLib.getDataLen(null)).isEqualTo(0);
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("getData should return null for null handle")
    void testGetDataNullHandle() {
	assertThat(RustImageLib.getData(null)).satisfiesAnyOf(data -> assertThat(data).isNull(),
		data -> assertThat(data.address()).isEqualTo(0));
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("getHeight should return 0 for null handle")
    void testGetHeightNullHandle() {
	assertThat(RustImageLib.getHeight(null)).isEqualTo(0);
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("getWidth should return 0 for null handle")
    void testGetWidthNullHandle() {
	assertThat(RustImageLib.getWidth(null)).isEqualTo(0);
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("grayscale should return error for null handle")
    void testGrayscaleNullHandle() {
	final var result = RustImageLib.grayscale(null);
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("ImageResult enum should have correct codes")
    void testImageResultCodes() {
	assertThat(ImageResult.SUCCESS.getCode()).isEqualTo(0);
	assertThat(ImageResult.ERROR_INVALID_PATH.getCode()).isEqualTo(1);
	assertThat(ImageResult.ERROR_INVALID_HANDLE.getCode()).isEqualTo(2);
	assertThat(ImageResult.ERROR_LOAD_FAILED.getCode()).isEqualTo(3);
	assertThat(ImageResult.ERROR_SAVE_FAILED.getCode()).isEqualTo(4);
	assertThat(ImageResult.ERROR_ALLOCATION.getCode()).isEqualTo(5);
	assertThat(ImageResult.ERROR_UNSUPPORTED_FORMAT.getCode()).isEqualTo(6);
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("ImageResult.fromCode should return correct enum")
    void testImageResultFromCode() {
	assertThat(ImageResult.fromCode(0)).isEqualTo(ImageResult.SUCCESS);
	assertThat(ImageResult.fromCode(1)).isEqualTo(ImageResult.ERROR_INVALID_PATH);
	assertThat(ImageResult.fromCode(2)).isEqualTo(ImageResult.ERROR_INVALID_HANDLE);
	assertThat(ImageResult.fromCode(3)).isEqualTo(ImageResult.ERROR_LOAD_FAILED);
	assertThat(ImageResult.fromCode(4)).isEqualTo(ImageResult.ERROR_SAVE_FAILED);
	assertThat(ImageResult.fromCode(5)).isEqualTo(ImageResult.ERROR_ALLOCATION);
	assertThat(ImageResult.fromCode(6)).isEqualTo(ImageResult.ERROR_UNSUPPORTED_FORMAT);
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("ImageResult.fromCode should return ERROR_INVALID_HANDLE for unknown code")
    void testImageResultFromCodeUnknown() {
	assertThat(ImageResult.fromCode(999)).isEqualTo(ImageResult.ERROR_INVALID_HANDLE);
	assertThat(ImageResult.fromCode(-1)).isEqualTo(ImageResult.ERROR_INVALID_HANDLE);
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("ImageResult.isSuccess should return true only for SUCCESS")
    void testImageResultIsSuccess() {
	assertThat(ImageResult.SUCCESS.isSuccess()).isTrue();
	assertThat(ImageResult.ERROR_INVALID_PATH.isSuccess()).isFalse();
	assertThat(ImageResult.ERROR_INVALID_HANDLE.isSuccess()).isFalse();
	assertThat(ImageResult.ERROR_LOAD_FAILED.isSuccess()).isFalse();
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("invert should return error for null handle")
    void testInvertNullHandle() {
	final var result = RustImageLib.invert(null);
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("loadImage should return null for invalid path")
    void testLoadImageInvalidPath() {
	final var handle = RustImageLib.loadImage("nonexistent_file.png");
	assertThat(handle).satisfiesAnyOf(h -> assertThat(h).isNull(), h -> assertThat(h.address()).isEqualTo(0));
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("resize should return error for null handle")
    void testResizeNullHandle() {
	final var result = RustImageLib.resizeWithFilter(null, 100, 100, ResizeFilter.NEAREST.getCode());
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("rotateLeft should return error for null handle")
    void testRotateLeftNullHandle() {
	final var result = RustImageLib.rotateLeft(null);
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("rotateRight should return error for null handle")
    void testRotateRightNullHandle() {
	final var result = RustImageLib.rotateRight(null);
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("saveImage should return error for null handle")
    void testSaveImageNullHandle() {
	final var result = RustImageLib.saveImage(null, "output.png");
	assertThat(result).isNotEqualTo(ImageResult.SUCCESS.getCode());
    }
}
