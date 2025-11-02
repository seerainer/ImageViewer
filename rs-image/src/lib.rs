use image::{DynamicImage, ImageBuffer, ImageReader};
use std::ffi::{c_char, CStr};
use std::ptr;
use std::slice;

/// Represents an image handle that can be passed across FFI boundary
#[repr(C)]
pub struct ImageHandle {
    width: u32,
    height: u32,
    data: *mut u8,
    data_len: usize,
}

/// Image operation result codes
#[repr(C)]
#[derive(Debug, PartialEq)]
pub enum ImageResult {
    Success = 0,
    ErrorInvalidPath = 1,
    ErrorInvalidHandle = 2,
    ErrorLoadFailed = 3,
    ErrorSaveFailed = 4,
    ErrorAllocation = 5,
    ErrorUnsupportedFormat = 6,
}

/// Load image from file path
/// Returns null on error
#[no_mangle]
pub unsafe extern "C" fn image_load(path: *const c_char) -> *mut ImageHandle {
    if path.is_null() {
        return ptr::null_mut();
    }

    let c_str = CStr::from_ptr(path);
    let path_str = match c_str.to_str() {
        Ok(s) => s,
        Err(_) => return ptr::null_mut(),
    };

    let img = match ImageReader::open(path_str) {
        Ok(reader) => match reader.decode() {
            Ok(img) => img,
            Err(_) => return ptr::null_mut(),
        },
        Err(_) => return ptr::null_mut(),
    };

    create_image_handle(img)
}

/// Create image from raw RGBA data
/// Returns null on error
#[no_mangle]
pub unsafe extern "C" fn image_from_rgba(
    data: *const u8,
    width: u32,
    height: u32,
) -> *mut ImageHandle {
    if data.is_null() || width == 0 || height == 0 {
        return ptr::null_mut();
    }

    let data_len = (width * height * 4) as usize;
    let data_slice = slice::from_raw_parts(data, data_len);
    
    let buffer = match ImageBuffer::from_raw(width, height, data_slice.to_vec()) {
        Some(buf) => buf,
        None => return ptr::null_mut(),
    };

    let img = DynamicImage::ImageRgba8(buffer);
    create_image_handle(img)
}

/// Save image to file path
#[no_mangle]
pub unsafe extern "C" fn image_save(
    handle: *const ImageHandle,
    path: *const c_char,
) -> ImageResult {
    if handle.is_null() || path.is_null() {
        return ImageResult::ErrorInvalidHandle;
    }

    let c_str = CStr::from_ptr(path);
    let path_str = match c_str.to_str() {
        Ok(s) => s,
        Err(_) => return ImageResult::ErrorInvalidPath,
    };

    let img = match handle_to_image(handle) {
        Some(img) => img,
        None => return ImageResult::ErrorInvalidHandle,
    };

    match img.save(path_str) {
        Ok(_) => ImageResult::Success,
        Err(_) => ImageResult::ErrorSaveFailed,
    }
}

/// Rotate image 90 degrees clockwise
#[no_mangle]
pub unsafe extern "C" fn image_rotate_90(handle: *mut ImageHandle) -> ImageResult {
    transform_image(handle, |img| img.rotate90())
}

/// Rotate image 180 degrees
#[no_mangle]
pub unsafe extern "C" fn image_rotate_180(handle: *mut ImageHandle) -> ImageResult {
    transform_image(handle, |img| img.rotate180())
}

/// Rotate image 270 degrees clockwise (90 counter-clockwise)
#[no_mangle]
pub unsafe extern "C" fn image_rotate_270(handle: *mut ImageHandle) -> ImageResult {
    transform_image(handle, |img| img.rotate270())
}

/// Flip image horizontally
#[no_mangle]
pub unsafe extern "C" fn image_flip_horizontal(handle: *mut ImageHandle) -> ImageResult {
    transform_image(handle, |img| img.fliph())
}

/// Flip image vertically
#[no_mangle]
pub unsafe extern "C" fn image_flip_vertical(handle: *mut ImageHandle) -> ImageResult {
    transform_image(handle, |img| img.flipv())
}

/// Resize image using specified filter
#[no_mangle]
pub unsafe extern "C" fn image_resize_with_filter(
    handle: *mut ImageHandle,
    width: u32,
    height: u32,
    filter: u32,
) -> ImageResult {
    let filter_type = match filter {
        0 => image::imageops::FilterType::Nearest,
        1 => image::imageops::FilterType::Triangle,
        2 => image::imageops::FilterType::CatmullRom,
        3 => image::imageops::FilterType::Gaussian,
        4 => image::imageops::FilterType::Lanczos3,
        _ => image::imageops::FilterType::Nearest, // Default fallback
    };
    
    transform_image(handle, |img| {
        img.resize(width, height, filter_type)
    })
}

/// Adjust image brightness (-100 to 100)
#[no_mangle]
pub unsafe extern "C" fn image_adjust_brightness(
    handle: *mut ImageHandle,
    value: i32,
) -> ImageResult {
    transform_image(handle, |img| img.brighten(value))
}

/// Adjust image contrast (-100.0 to 100.0)
#[no_mangle]
pub unsafe extern "C" fn image_adjust_contrast(
    handle: *mut ImageHandle,
    contrast: f32,
) -> ImageResult {
    transform_image(handle, |img| img.adjust_contrast(contrast))
}

/// Apply blur filter
#[no_mangle]
pub unsafe extern "C" fn image_blur(handle: *mut ImageHandle, sigma: f32) -> ImageResult {
    transform_image(handle, |img| img.blur(sigma))
}

/// Convert to grayscale
#[no_mangle]
pub unsafe extern "C" fn image_grayscale(handle: *mut ImageHandle) -> ImageResult {
    transform_image(handle, |img| DynamicImage::ImageLuma8(img.to_luma8()))
}

/// Invert colors
#[no_mangle]
pub unsafe extern "C" fn image_invert(handle: *mut ImageHandle) -> ImageResult {
    if handle.is_null() {
        return ImageResult::ErrorInvalidHandle;
    }

    let mut img = match handle_to_image(handle) {
        Some(img) => img,
        None => return ImageResult::ErrorInvalidHandle,
    };

    img.invert();
    update_handle_from_image(handle, img)
}

/// Get image width
#[no_mangle]
pub unsafe extern "C" fn image_get_width(handle: *const ImageHandle) -> u32 {
    if handle.is_null() {
        return 0;
    }
    (*handle).width
}

/// Get image height
#[no_mangle]
pub unsafe extern "C" fn image_get_height(handle: *const ImageHandle) -> u32 {
    if handle.is_null() {
        return 0;
    }
    (*handle).height
}

/// Get raw RGBA pixel data
#[no_mangle]
pub unsafe extern "C" fn image_get_data(handle: *const ImageHandle) -> *const u8 {
    if handle.is_null() {
        return ptr::null();
    }
    (*handle).data
}

/// Get data length in bytes
#[no_mangle]
pub unsafe extern "C" fn image_get_data_len(handle: *const ImageHandle) -> usize {
    if handle.is_null() {
        return 0;
    }
    (*handle).data_len
}

/// Free image handle and associated memory
#[no_mangle]
pub unsafe extern "C" fn image_free(handle: *mut ImageHandle) {
    if handle.is_null() {
        return;
    }

    let handle_ref = &mut *handle;
    if !handle_ref.data.is_null() && handle_ref.data_len > 0 {
        drop(Vec::from_raw_parts(
            handle_ref.data,
            handle_ref.data_len,
            handle_ref.data_len,
        ));
        handle_ref.data = ptr::null_mut();
        handle_ref.data_len = 0;
    }

    drop(Box::from_raw(handle));
}

// Helper functions

unsafe fn create_image_handle(img: DynamicImage) -> *mut ImageHandle {
    let rgba = img.to_rgba8();
    let width = rgba.width();
    let height = rgba.height();
    let pixels = rgba.into_raw();
    let data_len = pixels.len();

    let mut boxed_pixels = pixels.into_boxed_slice();
    let data = boxed_pixels.as_mut_ptr();
    std::mem::forget(boxed_pixels);

    let handle = Box::new(ImageHandle {
        width,
        height,
        data,
        data_len,
    });

    Box::into_raw(handle)
}

unsafe fn handle_to_image(handle: *const ImageHandle) -> Option<DynamicImage> {
    if handle.is_null() {
        return None;
    }

    let handle_ref = &*handle;
    if handle_ref.data.is_null() || handle_ref.data_len == 0 {
        return None;
    }

    let data_slice = slice::from_raw_parts(handle_ref.data, handle_ref.data_len);
    let buffer = ImageBuffer::from_raw(handle_ref.width, handle_ref.height, data_slice.to_vec())?;

    Some(DynamicImage::ImageRgba8(buffer))
}

unsafe fn transform_image<F>(handle: *mut ImageHandle, transform: F) -> ImageResult
where
    F: FnOnce(DynamicImage) -> DynamicImage,
{
    if handle.is_null() {
        return ImageResult::ErrorInvalidHandle;
    }

    let img = match handle_to_image(handle) {
        Some(img) => img,
        None => return ImageResult::ErrorInvalidHandle,
    };

    let transformed = transform(img);
    update_handle_from_image(handle, transformed)
}

unsafe fn update_handle_from_image(
    handle: *mut ImageHandle,
    img: DynamicImage,
) -> ImageResult {
    let handle_ref = &mut *handle;

    // Free old data
    if !handle_ref.data.is_null() && handle_ref.data_len > 0 {
        drop(Vec::from_raw_parts(
            handle_ref.data,
            handle_ref.data_len,
            handle_ref.data_len,
        ));
    }

    // Convert to RGBA and update handle
    let rgba = img.to_rgba8();
    let width = rgba.width();
    let height = rgba.height();
    let pixels = rgba.into_raw();
    let data_len = pixels.len();

    let mut boxed_pixels = pixels.into_boxed_slice();
    let data = boxed_pixels.as_mut_ptr();
    std::mem::forget(boxed_pixels);

    handle_ref.width = width;
    handle_ref.height = height;
    handle_ref.data = data;
    handle_ref.data_len = data_len;

    ImageResult::Success
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_image_result_values() {
        assert_eq!(ImageResult::Success as i32, 0);
        assert_eq!(ImageResult::ErrorInvalidPath as i32, 1);
    }
}
