package com.example.mathwake;

import junit.framework.TestCase;

/** Kiểm tra khói tối thiểu cho source set androidTest chuẩn của Android Studio. */
public class MathWakeInstrumentedTest extends TestCase {
    public void testApplicationId() {
        assertEquals("com.example.mathwake", getClass().getPackage().getName());
    }
}
