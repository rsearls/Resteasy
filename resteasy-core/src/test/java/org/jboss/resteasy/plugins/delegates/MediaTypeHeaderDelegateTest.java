package org.jboss.resteasy.plugins.delegates;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MediaTypeHeaderDelegateTest {

    @Test
    public void parsingBrokenMediaTypeShouldThrowIllegalArgumentException_minimized() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MediaTypeHeaderDelegate.parse("x; /x"));
    }

    @Test
    public void parsingBrokenMediaTypeShouldThrowIllegalArgumentException_actual() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MediaTypeHeaderDelegate.parse("() { ::}; echo \"NS:\" $(/bin/sh -c \"expr 123456 - 123456\")"));
    }
}
