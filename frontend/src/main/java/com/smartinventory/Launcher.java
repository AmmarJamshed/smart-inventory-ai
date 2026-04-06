package com.smartinventory;

/**
 * Plain (non-JavaFX) entry point required by jpackage.
 * jpackage cannot directly use a class that extends Application as main class
 * when packaging without the full module system. This shim fixes that.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
