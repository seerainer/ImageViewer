package io.github.seerainer.imageviewer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("Main Class Unit Tests")
class MainTest {

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Main class should not be instantiable")
    void testMainClassNotInstantiable() {
	assertThatThrownBy(() -> {
	    final var constructor = Main.class.getDeclaredConstructor();
	    constructor.setAccessible(true);
	    constructor.newInstance();
	}).isInstanceOf(InvocationTargetException.class).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @SuppressWarnings("static-method")
    @Test
    @DisplayName("Main class should have main method")
    void testMainMethodExists() throws NoSuchMethodException {
	final var mainMethod = Main.class.getDeclaredMethod("main", String[].class);

	assertThat(mainMethod).isNotNull();
	assertThat(mainMethod.getReturnType()).isEqualTo(void.class);
	assertThat(Modifier.isStatic(mainMethod.getModifiers())).isTrue();
	assertThat(Modifier.isPublic(mainMethod.getModifiers())).isTrue();
    }
}
