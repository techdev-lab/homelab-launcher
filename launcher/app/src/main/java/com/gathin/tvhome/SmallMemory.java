package com.gathin.tvhome;

/**
 * Whether this TV counts as a small-memory device, and what the heavy-effects default is because of it
 * (docs/BATCH_L_LIGHT.md). Small means the system flags it as low-RAM, or the memory it gives Android is at or under
 * {@link #THRESHOLD_BYTES}. The flag alone is not enough: a 2 GB Chromecast reports it as false. Pure Java, so the rule is
 * unit tested on the JVM; {@link Config} supplies the two inputs from {@code ActivityManager}.
 */
final class SmallMemory {
    /** 2,200 MiB. Android reports less than the physical RAM, so a "2 GB" TV is under it and a "3 GB" one is over it. */
    static final long THRESHOLD_BYTES = 2200L * 1024 * 1024;
    private static final long MIB = 1024L * 1024;

    private SmallMemory() {
    }

    /**
     * @param lowRamFlag {@code ActivityManager.isLowRamDevice()}
     * @param totalMemBytes {@code MemoryInfo.totalMem}; zero or negative means "unknown" and counts as not small
     */
    static boolean isSmall(boolean lowRamFlag, long totalMemBytes) {
        if (lowRamFlag) return true;
        return totalMemBytes > 0 && totalMemBytes <= THRESHOLD_BYTES;
    }

    /**
     * The value of a heavy-effects setting: a choice made on the TV first, then a key in config.json, then the gate.
     * A null means "not set". With nothing set, a small TV gets {@code false} and any other TV the resource default.
     */
    static boolean resolve(Boolean tvChoice, Boolean themeValue, boolean small, boolean resourceDefault) {
        if (tvChoice != null) return tvChoice;
        if (themeValue != null) return themeValue;
        return !small && resourceDefault;
    }

    /** The read-only line shown in Launcher settings, e.g. "small (1938 MiB)"; "unknown" when the size was not read. */
    static String describe(boolean small, long totalMemBytes) {
        String size = totalMemBytes > 0 ? (totalMemBytes / MIB) + " MiB" : "unknown";
        return (small ? "small" : "normal") + " (" + size + ")";
    }
}
