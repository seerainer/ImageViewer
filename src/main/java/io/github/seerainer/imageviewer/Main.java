package io.github.seerainer.imageviewer;

import org.eclipse.swt.widgets.Display;

public class Main {

    static {
	System.setProperty("org.eclipse.swt.display.useSystemTheme", "true");
    }

    private Main() {
	throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static void main(final String[] args) {
	final var display = Display.getDefault();
	try {
	    final var mainUI = new MainWindow(display, args.length > 0 ? args[0] : null);
	    final var shell = mainUI.getShell();
	    while (!shell.isDisposed()) {
		if (!display.readAndDispatch()) {
		    display.sleep();
		}
	    }
	} catch (final Exception e) {
	    e.printStackTrace();
	} finally {
	    Icons.dispose();
	    display.dispose();
	}
    }
}