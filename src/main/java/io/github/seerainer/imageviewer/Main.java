package io.github.seerainer.imageviewer;

import org.eclipse.swt.widgets.Display;

public class Main {

    private Main() {
	throw new UnsupportedOperationException("Utility class");
    }

    public static void main(final String[] args) {
	System.setProperty("org.eclipse.swt.display.useSystemTheme", "true");
	final var display = Display.getDefault();
	final var mainUI = new MainWindow(display, args.length > 0 ? args[0] : null);
	final var shell = mainUI.getShell();
	while (!shell.isDisposed()) {
	    if (!display.readAndDispatch()) {
		display.sleep();
	    }
	}
	Icons.dispose();
	display.dispose();
    }
}