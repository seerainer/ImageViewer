package io.github.seerainer.imageviewer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class MainWindow {

    private static final String APP_TITLE = "ImageViewer";
    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 600;
    private static final double ZOOM_STEP = 0.1;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 10.0;
    private static final String[] IMAGE_EXTENSIONS = { ".png", ".jpg", ".jpeg", ".jpe", ".jfif", ".gif", ".webp",
	    ".tiff", ".tif", ".bmp", ".dib", ".tga", ".ico", ".hdr", ".exr", ".pbm", ".pgm", ".ppm", ".pam", ".dds",
	    ".ff" };
    private final Display display;
    private Shell shell;
    private Canvas canvas;
    private Menu menuBar;
    private ToolBar toolBar;
    private Composite statusBar;
    private Label statusLabel;
    private Image currentImage;
    private Image originalImage;
    private final String initialFilePath;
    private String currentFilePath;
    private double currentZoom = 1.0;
    private boolean isFullScreen = false;
    private ResizeFilter currentResizeFilter = ResizeFilter.TRIANGLE;
    private final List<String> folderImages = new ArrayList<>();
    private int currentImageIndex = -1;
    private Color backgroundColor;

    public MainWindow(final Display display, final String filePath) {
	this.display = display;
	this.initialFilePath = filePath;
	Icons.initialize(display);
	this.shell = createShell();
	initializeUI();
	shell.open();
    }

    private static Rectangle calculateCenteredRectangle(final Rectangle source, final Rectangle container) {
	final var x = (container.width - source.width) / 2;
	final var y = (container.height - source.height) / 2;
	return new Rectangle(x, y, source.width, source.height);
    }

    private static void createMenuItem(final Menu menu, final String text, final int accelerator,
	    final Runnable action) {
	final var item = new MenuItem(menu, SWT.PUSH);
	item.setText(text);
	if (accelerator != SWT.NONE) {
	    item.setAccelerator(accelerator);
	}
	item.addListener(SWT.Selection, _ -> action.run());
    }

    private static void createToolItem(final ToolBar toolBar, final String icon, final String text,
	    final Runnable action) {
	final var item = new ToolItem(toolBar, SWT.PUSH);
	item.setText(icon);
	item.setToolTipText(text);
	item.addListener(SWT.Selection, _ -> action.run());
    }

    private static MenuItem menuSeparator(final Menu editMenu) {
	return new MenuItem(editMenu, SWT.SEPARATOR);
    }

    private static ToolItem toolSeparator(final ToolBar toolBar) {
	return new ToolItem(toolBar, SWT.SEPARATOR);
    }

    private void applyZoom(final double zoom) {
	if (originalImage == null || originalImage.isDisposed()) {
	    return;
	}

	try {
	    final var bounds = originalImage.getBounds();
	    final var newWidth = (int) (bounds.width * zoom);
	    final var newHeight = (int) (bounds.height * zoom);

	    if (newWidth < 1 || newHeight < 1) {
		updateStatus("Image too small to zoom");
		return;
	    }

	    final var resized = ImageService.resizeWithFilter(display, originalImage, newWidth, newHeight,
		    currentResizeFilter);
	    if (resized != null) {
		if (currentImage != null && !currentImage.isDisposed() && currentImage != originalImage) {
		    currentImage.dispose();
		}
		currentImage = resized;
		currentZoom = zoom;
		canvas.redraw();
		updateStatus("Zoom: %.0f%%".formatted(Double.valueOf(zoom * 100)));
	    } else {
		updateStatus("Failed to zoom image");
	    }
	} catch (final Exception e) {
	    updateStatus("Error zooming image: " + e.getMessage());
	}
    }

    private void configureShellLayout() {
	final var layout = new GridLayout(1, false);
	layout.marginWidth = 0;
	layout.marginHeight = 0;
	layout.verticalSpacing = 0;
	shell.setLayout(layout);
    }

    private void createCanvas() {
	canvas = new Canvas(shell, SWT.BORDER | SWT.DOUBLE_BUFFERED);
	canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	canvas.addPaintListener(this::paintCanvas);
	canvas.addListener(SWT.Resize, _ -> canvas.redraw());
	canvas.addListener(SWT.KeyDown, event -> {
	    switch (event.keyCode) {
	    case SWT.CR, SWT.KEYPAD_CR -> handleToggleFullScreen();
	    case SWT.ARROW_LEFT -> handlePreviousImage();
	    case SWT.ARROW_RIGHT -> handleNextImage();
	    default -> {
		// No action
	    }
	    }
	});
	canvas.setFocus();
    }

    private void createEditMenu() {
	final var editMenuItem = new MenuItem(menuBar, SWT.CASCADE);
	editMenuItem.setText("&Edit");
	final var editMenu = new Menu(shell, SWT.DROP_DOWN);
	editMenuItem.setMenu(editMenu);

	createMenuItem(editMenu, "&Rotate Right\tCtrl+R", SWT.MOD1 | 'R', this::handleRotateRight);
	createMenuItem(editMenu, "Rotate &Left\tCtrl+L", SWT.MOD1 | 'L', this::handleRotateLeft);
	menuSeparator(editMenu);
	createMenuItem(editMenu, "Flip &Horizontal", SWT.NONE, this::handleFlipHorizontal);
	createMenuItem(editMenu, "Flip &Vertical", SWT.NONE, this::handleFlipVertical);
    }

    private void createFileMenu() {
	final var fileMenuItem = new MenuItem(menuBar, SWT.CASCADE);
	fileMenuItem.setText("&File");
	final var fileMenu = new Menu(shell, SWT.DROP_DOWN);
	fileMenuItem.setMenu(fileMenu);

	createMenuItem(fileMenu, "&Open...\tCtrl+O", SWT.MOD1 | 'O', this::handleOpen);
	createMenuItem(fileMenu, "&Save\tCtrl+S", SWT.MOD1 | 'S', this::handleSave);
	createMenuItem(fileMenu, "Save &As...\tCtrl+Shift+S", SWT.MOD1 | SWT.MOD2 | 'S', this::handleSaveAs);
	menuSeparator(fileMenu);
	createMenuItem(fileMenu, "&Previous Image\tLeft", SWT.ARROW_LEFT, this::handlePreviousImage);
	createMenuItem(fileMenu, "&Next Image\tRight", SWT.ARROW_RIGHT, this::handleNextImage);
	menuSeparator(fileMenu);
	createMenuItem(fileMenu, "E&xit\tAlt+F4", SWT.ALT | SWT.F4, this::handleExit);
    }

    private void createHelpMenu() {
	final var helpMenuItem = new MenuItem(menuBar, SWT.CASCADE);
	helpMenuItem.setText("&Help");
	final var helpMenu = new Menu(shell, SWT.DROP_DOWN);
	helpMenuItem.setMenu(helpMenu);
	createMenuItem(helpMenu, "&About", SWT.NONE, this::handleAbout);
    }

    private void createMenuBar() {
	menuBar = new Menu(shell, SWT.BAR);
	shell.setMenuBar(menuBar);
	createFileMenu();
	createEditMenu();
	createViewMenu();
	createHelpMenu();
    }

    private Shell createShell() {
	shell = new Shell(display);
	shell.setImage(Icons.getImage(Icons.APP_ICON));
	shell.setText(APP_TITLE);
	shell.setMinimumSize(MIN_WIDTH, MIN_HEIGHT);
	shell.setMaximized(true);
	configureShellLayout();
	return shell;
    }

    private void createStatusBar() {
	statusBar = new Composite(shell, SWT.NONE);
	statusBar.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
	statusBar.setLayout(new GridLayout(1, false));

	statusLabel = new Label(statusBar, SWT.NONE);
	statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	updateStatus("Ready");
    }

    private void createToolBar() {
	toolBar = new ToolBar(shell, SWT.FLAT | SWT.HORIZONTAL);
	toolBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
	toolBar.setFont(shell.getFont());

	createToolItem(toolBar, "\uD83D\uDCC2", "Open", this::handleOpen);
	createToolItem(toolBar, "\uD83D\uDCBE", "Save", this::handleSave);
	toolSeparator(toolBar);
	createToolItem(toolBar, "\u25C0\uFE0F", "Previous Image", this::handlePreviousImage);
	createToolItem(toolBar, "\u25B6\uFE0F", "Next Image", this::handleNextImage);
	toolSeparator(toolBar);
	createToolItem(toolBar, "\u21A9\uFE0F", "Rotate Clockwise", this::handleRotateRight);
	createToolItem(toolBar, "\u21AA\uFE0F", "Rotate Counter-Clockwise", this::handleRotateLeft);
	toolSeparator(toolBar);
	createToolItem(toolBar, "\u2795", "Zoom In", this::handleZoomIn);
	createToolItem(toolBar, "\u2796", "Zoom Out", this::handleZoomOut);
	createToolItem(toolBar, "\uD83E\uDE9F", "Fit to Window", this::handleFitToWindow);
    }

    private void createViewMenu() {
	final var viewMenuItem = new MenuItem(menuBar, SWT.CASCADE);
	viewMenuItem.setText("&View");
	final var viewMenu = new Menu(shell, SWT.DROP_DOWN);
	viewMenuItem.setMenu(viewMenu);

	createMenuItem(viewMenu, "Zoom &In\t+", SWT.KEYPAD_ADD, this::handleZoomIn);
	createMenuItem(viewMenu, "Zoom &Out\t-", SWT.KEYPAD_SUBTRACT, this::handleZoomOut);
	createMenuItem(viewMenu, "&Fit to Window\tCtrl+0", SWT.MOD1 | '0', this::handleFitToWindow);
	createMenuItem(viewMenu, "&Actual Size\tCtrl+1", SWT.MOD1 | '1', this::handleActualSize);
	menuSeparator(viewMenu);

	final var resizeQualityMenuItem = new MenuItem(viewMenu, SWT.CASCADE);
	resizeQualityMenuItem.setText("Resize &Quality");
	final var resizeQualityMenu = new Menu(shell, SWT.DROP_DOWN);
	resizeQualityMenuItem.setMenu(resizeQualityMenu);

	// Create radio menu items for each filter type
	for (final var filter : ResizeFilter.values()) {
	    final var filterItem = new MenuItem(resizeQualityMenu, SWT.RADIO);
	    filterItem.setText(filter.getDisplayName());
	    filterItem.setSelection(filter == currentResizeFilter);
	    filterItem.addListener(SWT.Selection, _ -> {
		if (filterItem.getSelection()) {
		    currentResizeFilter = filter;
		    updateStatus("Resize quality: " + filter.getDisplayName());
		    // Re-apply current zoom with new filter
		    if (originalImage != null && !originalImage.isDisposed()) {
			applyZoom(currentZoom);
		    }
		}
	    });
	}

	menuSeparator(viewMenu);
	createMenuItem(viewMenu, "Full &Screen\tEnter", SWT.NONE, this::handleToggleFullScreen);
	menuSeparator(viewMenu);
	createMenuItem(viewMenu, "&Background Color...", SWT.NONE, () -> {
	    final var colorDialog = new ColorDialog(shell);
	    final var selectedColor = colorDialog.open();
	    if (selectedColor != null) {
		backgroundColor = new Color(selectedColor);
		canvas.redraw();
		updateStatus("Background color changed");
	    }
	});
	menuSeparator(viewMenu);

	final var effectsMenuItem = new MenuItem(viewMenu, SWT.CASCADE);
	effectsMenuItem.setText("&Effects");
	final var effectsMenu = new Menu(shell, SWT.DROP_DOWN);
	effectsMenuItem.setMenu(effectsMenu);

	createMenuItem(effectsMenu, "Adjust &Brightness...", SWT.NONE, this::handleAdjustBrightness);
	createMenuItem(effectsMenu, "Adjust &Contrast...", SWT.NONE, this::handleAdjustContrast);
	createMenuItem(effectsMenu, "B&lur...", SWT.NONE, this::handleBlur);
	menuSeparator(effectsMenu);
	createMenuItem(effectsMenu, "&Grayscale", SWT.NONE, this::handleGrayscale);
	createMenuItem(effectsMenu, "&Invert Colors", SWT.NONE, this::handleInvert);
    }

    private void disposeCurrentImage() {
	if (currentImage != null && !currentImage.isDisposed()) {
	    currentImage.dispose();
	    currentImage = null;
	}
	if (originalImage != null && !originalImage.isDisposed()) {
	    originalImage.dispose();
	    originalImage = null;
	}
	currentZoom = 1.0;
    }

    Shell getShell() {
	return shell;
    }

    private void handleAbout() {
	final var messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
	messageBox.setText("About ImageViewer");
	messageBox.setMessage("ImageViewer v0.1.0\n\nwith Java SWT and Rust integration.");
	messageBox.open();
    }

    private void handleActualSize() {
	if (originalImage == null || originalImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	applyZoom(1.0);
    }

    private static void toggleUIVisibility(final boolean visible, final Control control) {
	if (control == null || control.isDisposed()) {
	    return;
	}
	control.setVisible(visible);
	((GridData) control.getLayoutData()).exclude = !visible;
    }

    private void handleToggleFullScreen() {
	isFullScreen = !isFullScreen;
	shell.setFullScreen(isFullScreen);

	if (isFullScreen) {
	    // Hide UI elements in fullscreen
	    shell.setMenuBar(null);
	    toggleUIVisibility(false, toolBar);
	    toggleUIVisibility(false, statusBar);
	    updateStatus("Full screen mode - Press Enter to exit");
	} else {
	    // Restore UI elements when exiting fullscreen
	    if (menuBar != null && !menuBar.isDisposed()) {
		shell.setMenuBar(menuBar);
	    }
	    toggleUIVisibility(true, toolBar);
	    toggleUIVisibility(true, statusBar);
	    updateStatus("Exited full screen mode");
	}
	shell.layout(true, true);

	// Refit image to new window size after toggling
	if (originalImage != null && !originalImage.isDisposed()) {
	    display.asyncExec(this::handleFitToWindow);
	}

	canvas.setFocus();
	shell.setActive();
    }

    private void handleExit() {
	shell.close();
    }

    private void handleFitToWindow() {
	if (originalImage == null || originalImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	final var clientArea = canvas.getClientArea();
	final var imageBounds = originalImage.getBounds();

	// Calculate zoom to fit
	final var zoomX = (double) clientArea.width / imageBounds.width;
	final var zoomY = (double) clientArea.height / imageBounds.height;
	final var newZoom = Math.min(zoomX, zoomY);

	applyZoom(newZoom);
    }

    private void handleFlipHorizontal() {
	if (currentImage == null || currentImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	try {
	    final var flipped = ImageService.flipHorizontal(display, currentImage);
	    if (flipped != null) {
		disposeCurrentImage();
		currentImage = flipped;
		originalImage = flipped;
		currentZoom = 1.0;
		canvas.redraw();
		updateStatus("Flipped horizontally");
	    } else {
		updateStatus("Failed to flip image");
	    }
	} catch (final Exception e) {
	    updateStatus("Error flipping image: " + e.getMessage());
	}
    }

    private void handleFlipVertical() {
	if (currentImage == null || currentImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	try {
	    final var flipped = ImageService.flipVertical(display, currentImage);
	    if (flipped != null) {
		disposeCurrentImage();
		currentImage = flipped;
		originalImage = flipped;
		currentZoom = 1.0;
		canvas.redraw();
		updateStatus("Flipped vertically");
	    } else {
		updateStatus("Failed to flip image");
	    }
	} catch (final Exception e) {
	    updateStatus("Error flipping image: " + e.getMessage());
	}
    }

    private void handleOpen() {
	final var dialog = new FileDialog(shell, SWT.OPEN);
	dialog.setText("Open Image");
	dialog.setFilterNames(new String[] { "All Supported Images", "PNG Images", "JPEG Images", "GIF Images",
		"WebP Images", "TIFF Images", "BMP Images", "TGA Images (Targa)", "DDS Images", "ICO Images",
		"HDR Images", "OpenEXR Images", "PNM Images (PBM/PGM/PPM/PAM)", "Farbfeld Images", "All Files (*.*)" });
	dialog.setFilterExtensions(new String[] {
		"*.png;*.jpg;*.jpeg;*.jpe;*.jfif;*.gif;*.webp;*.tiff;*.tif;*.bmp;*.dib;*.tga;*.ico;*.hdr;*.exr;*.pbm;*.pgm;*.ppm;*.pam;*.dds;*.ff",
		"*.png", "*.jpg;*.jpeg;*.jpe;*.jfif", "*.gif", "*.webp", "*.tiff;*.tif", "*.bmp;*.dib", "*.tga",
		"*.dds", "*.ico", "*.hdr", "*.exr", "*.pbm;*.pgm;*.ppm;*.pam", "*.ff", "*.*" });
	final var selected = dialog.open();
	if (selected != null) {
	    loadImage(selected);
	}
    }

    private void handleRotateLeft() {
	if (currentImage == null || currentImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	try {
	    final var rotated = ImageService.rotateLeft(display, currentImage);
	    if (rotated != null) {
		disposeCurrentImage();
		currentImage = rotated;
		originalImage = rotated;
		currentZoom = 1.0;
		canvas.redraw();
		updateStatus("Rotated left 90°");
	    } else {
		updateStatus("Failed to rotate image");
	    }
	} catch (final Exception e) {
	    updateStatus("Error rotating image: " + e.getMessage());
	}
    }

    private void handleRotateRight() {
	if (currentImage == null || currentImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	try {
	    final var rotated = ImageService.rotateRight(display, currentImage);
	    if (rotated != null) {
		disposeCurrentImage();
		currentImage = rotated;
		originalImage = rotated;
		currentZoom = 1.0;
		canvas.redraw();
		updateStatus("Rotated right 90°");
	    } else {
		updateStatus("Failed to rotate image");
	    }
	} catch (final Exception e) {
	    updateStatus("Error rotating image: " + e.getMessage());
	}
    }

    private void handleSave() {
	if (currentImage == null || currentImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	// If we have a current file path, save to it; otherwise, use Save As dialog
	if (currentFilePath != null && !currentFilePath.isEmpty()) {
	    try {
		final var success = ImageService.saveImage(currentImage, currentFilePath);
		if (success) {
		    updateStatus("Saved: " + currentFilePath);
		} else {
		    updateStatus("Failed to save image");
		}
	    } catch (final Exception e) {
		updateStatus("Error saving image: " + e.getMessage());
	    }
	} else {
	    handleSaveAs();
	}
    }

    private void handleSaveAs() {
	if (currentImage == null || currentImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	final var dialog = new FileDialog(shell, SWT.SAVE);
	dialog.setText("Save Image As");
	dialog.setFilterNames(new String[] { "PNG Images", "JPEG Images", "GIF Images", "BMP Images", "TIFF Images",
		"TGA Images (Targa)", "ICO Images", "PNM Images (PBM/PGM/PPM/PAM)", "Farbfeld Images",
		"All Files (*.*)" });
	dialog.setFilterExtensions(new String[] { "*.png", "*.jpg;*.jpeg", "*.gif", "*.bmp", "*.tiff;*.tif", "*.tga",
		"*.ico", "*.pbm;*.pgm;*.ppm;*.pam", "*.ff", "*.*" });
	dialog.setOverwrite(true);

	final var selected = dialog.open();
	if (selected != null) {
	    try {
		final var success = ImageService.saveImage(currentImage, selected);
		if (success) {
		    updateStatus("Saved: " + selected);
		    updateWindowTitle(selected);
		} else {
		    updateStatus("Failed to save image");
		}
	    } catch (final Exception e) {
		updateStatus("Error saving image: " + e.getMessage());
	    }
	}
    }

    private void handleZoomIn() {
	if (originalImage == null || originalImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	final var newZoom = Math.min(currentZoom + ZOOM_STEP, MAX_ZOOM);
	if (newZoom != currentZoom) {
	    applyZoom(newZoom);
	}
    }

    private void handleZoomOut() {
	if (originalImage == null || originalImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	final var newZoom = Math.max(currentZoom - ZOOM_STEP, MIN_ZOOM);
	if (newZoom != currentZoom) {
	    applyZoom(newZoom);
	}
    }

    private void initializeUI() {
	createMenuBar();
	createToolBar();
	createCanvas();
	createStatusBar();
	loadInitialImage();
    }

    private void loadImage(final String filePath) {
	try {
	    disposeCurrentImage();
	    currentImage = ImageService.loadImage(display, filePath);
	    if (currentImage != null) {
		originalImage = currentImage;
		currentFilePath = filePath;
		currentZoom = 1.0;
		updateFolderImagesList(filePath);
		updateWindowTitle(filePath);
		display.asyncExec(this::handleFitToWindow);
	    } else {
		updateStatus("Failed to load image: " + filePath);
	    }
	} catch (final Exception e) {
	    updateStatus("Error loading image: " + e.getMessage());
	}
    }

    private void loadInitialImage() {
	if (initialFilePath != null && !initialFilePath.isEmpty()) {
	    loadImage(initialFilePath);
	}
    }

    private void paintCanvas(final PaintEvent e) {
	final var gc = e.gc;
	final var clientArea = canvas.getClientArea();

	// Fill background
	if (backgroundColor != null && !backgroundColor.isDisposed()) {
	    gc.setBackground(backgroundColor);
	} else {
	    gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
	}
	gc.fillRectangle(clientArea);

	// Draw image if available
	if (currentImage != null && !currentImage.isDisposed()) {
	    final var imageBounds = currentImage.getBounds();
	    final var destRect = calculateCenteredRectangle(imageBounds, clientArea);
	    gc.drawImage(currentImage, 0, 0, imageBounds.width, imageBounds.height, destRect.x, destRect.y,
		    destRect.width, destRect.height);
	} else {
	    // Draw placeholder text
	    final var message = "No image loaded";
	    final var extent = gc.textExtent(message);
	    gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
	    gc.drawString(message, (clientArea.width - extent.x) / 2, (clientArea.height - extent.y) / 2, true);
	}
    }

    private void updateFolderImagesList(final String filePath) {
	folderImages.clear();
	currentImageIndex = -1;

	final var file = new File(filePath);
	final var parentDir = file.getParentFile();

	if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
	    return;
	}
	final var files = parentDir.listFiles();
	if (files == null) {
	    return;
	}
	for (final var f : files) {
	    if (f.isFile() && isImageFile(f.getName())) {
		folderImages.add(f.getAbsolutePath());
	    }
	}
	// Sort files alphabetically
	folderImages.sort(String.CASE_INSENSITIVE_ORDER);
	// Find current file index
	currentImageIndex = folderImages.indexOf(filePath);
    }

    private static boolean isImageFile(final String fileName) {
	final var lowerName = fileName.toLowerCase();
	for (final var ext : IMAGE_EXTENSIONS) {
	    if (lowerName.endsWith(ext)) {
		return true;
	    }
	}
	return false;
    }

    private void handlePreviousImage() {
	if (folderImages.isEmpty() || currentImageIndex <= 0) {
	    updateStatus("No previous image");
	    return;
	}
	currentImageIndex--;
	loadImage(folderImages.get(currentImageIndex));
    }

    private void handleNextImage() {
	if (folderImages.isEmpty() || currentImageIndex >= folderImages.size() - 1) {
	    updateStatus("No next image");
	    return;
	}
	currentImageIndex++;
	loadImage(folderImages.get(currentImageIndex));
    }

    private void handleAdjustBrightness() {
	showAdjustmentDialog("Adjust Brightness", "Brightness (-100 to 100):", 0, 200, 100, 1, 10,
		scaleValue -> String.valueOf(scaleValue - 100), scaleValue -> {
		    final var value = scaleValue - 100;
		    applyImageEffect(() -> ImageService.adjustBrightness(display, currentImage, value),
			    "Brightness adjusted: " + value, "Failed to adjust brightness",
			    "Error adjusting brightness");
		});
    }

    private void handleAdjustContrast() {
	showAdjustmentDialog("Adjust Contrast", "Contrast (0.0 to 5.0):", 0, 500, 100, 1, 10,
		scaleValue -> "%.2f".formatted(Double.valueOf(scaleValue / 100.0f)), scaleValue -> {
		    final var value = scaleValue / 100.0f;
		    applyImageEffect(() -> ImageService.adjustContrast(display, currentImage, value),
			    "Contrast adjusted: %.2f".formatted(Double.valueOf(value)), "Failed to adjust contrast",
			    "Error adjusting contrast");
		});
    }

    private void handleBlur() {
	showAdjustmentDialog("Blur Effect", "Blur Strength (0.1 to 10.0):", 1, 100, 10, 1, 5,
		scaleValue -> "%.1f".formatted(Double.valueOf(scaleValue / 10.0f)), scaleValue -> {
		    final var value = scaleValue / 10.0f;
		    applyImageEffect(() -> ImageService.blur(display, currentImage, value),
			    "Blur applied: %.1f".formatted(Double.valueOf(value)), "Failed to apply blur",
			    "Error applying blur");
		});
    }

    private void showAdjustmentDialog(final String title, final String labelText, final int min, final int max,
	    final int defaultValue, final int increment, final int pageIncrement, final ScaleValueFormatter formatter,
	    final ScaleValueHandler handler) {
	if (currentImage == null || currentImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	final var dialog = new Shell(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	dialog.setText(title);
	dialog.setLayout(new GridLayout(2, false));

	final var label = new Label(dialog, SWT.NONE);
	label.setText(labelText);
	label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

	final var scale = new Scale(dialog, SWT.HORIZONTAL);
	scale.setMinimum(min);
	scale.setMaximum(max);
	scale.setSelection(defaultValue);
	scale.setIncrement(increment);
	scale.setPageIncrement(pageIncrement);
	scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

	final var valueLabel = new Label(dialog, SWT.NONE);
	valueLabel.setText(formatter.format(defaultValue));
	valueLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

	scale.addListener(SWT.Selection, _ -> valueLabel.setText(formatter.format(scale.getSelection())));

	final var okButton = new Button(dialog, SWT.PUSH);
	okButton.setText("OK");
	okButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	okButton.addListener(SWT.Selection, _ -> {
	    handler.handle(scale.getSelection());
	    dialog.close();
	});

	final var cancelButton = new Button(dialog, SWT.PUSH);
	cancelButton.setText("Cancel");
	cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	cancelButton.addListener(SWT.Selection, _ -> dialog.close());

	dialog.pack();
	dialog.setLocation(shell.getLocation().x + (shell.getSize().x - dialog.getSize().x) / 2,
		shell.getLocation().y + (shell.getSize().y - dialog.getSize().y) / 2);
	dialog.open();
    }

    private void applyImageEffect(final ImageEffectSupplier effectSupplier, final String successMessage,
	    final String failureMessage, final String errorMessagePrefix) {
	try {
	    final var result = effectSupplier.apply();
	    if (result != null) {
		disposeCurrentImage();
		currentImage = result;
		originalImage = result;
		currentZoom = 1.0;
		canvas.redraw();
		updateStatus(successMessage);
	    } else {
		updateStatus(failureMessage);
	    }
	} catch (final Exception e) {
	    updateStatus(errorMessagePrefix + ": " + e.getMessage());
	}
    }

    private void handleGrayscale() {
	if (currentImage == null || currentImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	try {
	    final var grayscaleImage = ImageService.grayscale(display, currentImage);
	    if (grayscaleImage != null) {
		disposeCurrentImage();
		currentImage = grayscaleImage;
		originalImage = grayscaleImage;
		currentZoom = 1.0;
		canvas.redraw();
		updateStatus("Converted to grayscale");
	    } else {
		updateStatus("Failed to convert to grayscale");
	    }
	} catch (final Exception e) {
	    updateStatus("Error converting to grayscale: " + e.getMessage());
	}
    }

    private void handleInvert() {
	if (currentImage == null || currentImage.isDisposed()) {
	    updateStatus("No image loaded");
	    return;
	}

	try {
	    final var inverted = ImageService.invert(display, currentImage);
	    if (inverted != null) {
		disposeCurrentImage();
		currentImage = inverted;
		originalImage = inverted;
		currentZoom = 1.0;
		canvas.redraw();
		updateStatus("Colors inverted");
	    } else {
		updateStatus("Failed to invert colors");
	    }
	} catch (final Exception e) {
	    updateStatus("Error inverting colors: " + e.getMessage());
	}
    }

    private void updateStatus(final String message) {
	if (statusLabel != null && !statusLabel.isDisposed()) {
	    statusLabel.setText(message);
	}
    }

    private void updateWindowTitle(final String filePath) {
	final var fileName = filePath.substring(filePath.lastIndexOf('\\') + 1);
	shell.setText(new StringBuilder().append(APP_TITLE).append(" - ").append(fileName).toString());
    }

    @FunctionalInterface
    private interface ScaleValueFormatter {
	String format(int scaleValue);
    }

    @FunctionalInterface
    private interface ScaleValueHandler {
	void handle(int scaleValue);
    }

    @FunctionalInterface
    private interface ImageEffectSupplier {
	Image apply() throws Exception;
    }
}
