package com.forge.farms.output;

/**
 * Composable harvest outputs. A farm runs all enabled modes in priority
 * order every harvest — storage, then hoppers, then auto-sell — instead of
 * forcing one choice.
 */
public enum OutputMode {
    /** Into the farm's virtual storage. */
    STORAGE,
    /** Into adjacent containers (built-in hopper automation). */
    HOPPER,
    /** Converted to economy money (built-in auto-sell). */
    SELL
}
