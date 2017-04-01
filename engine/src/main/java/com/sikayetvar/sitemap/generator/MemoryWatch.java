package com.sikayetvar.sitemap.generator;

public class MemoryWatch {
    private String tag;
    private long startBytes;
    private long finishBytes;

    public MemoryWatch(String tag) {
        this.tag = tag;
        start();
    }

    public MemoryWatch() {
        this(null);
    }

    public MemoryWatch start() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();

        startBytes = runtime.totalMemory() - runtime.freeMemory();
        return this;
    }

    public MemoryWatch stop() {
        return stop(null);
    }

    public MemoryWatch stop(String tag) {
        this.tag = tag;
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();

        finishBytes = runtime.totalMemory() - runtime.freeMemory();
        return this;
    }

    public long getBytes() {
        return finishBytes - startBytes;
    }

    public long getKBytes() {
        return (finishBytes - startBytes) / 1024L;
    }

    public long getMBytes() {
        return (finishBytes - startBytes) / 1024L / 1024L;
    }

    public long getGBytes() {
        return (finishBytes - startBytes) / 1024L / 1024L / 1024L;
    }

    public String getReadableAmount() {
        long bytes = getBytes();

        if (bytes < 1024L)
            return "[" + tag + "]:" + getBytes() + " bytes";

        if (bytes < 1024L * 1024L)
            return "[" + tag + "]:" + getKBytes() + " Kbytes";

        if (bytes < 1024L * 1024L * 1024L)
            return "[" + tag + "]:" + getMBytes() + " Mbytes";

        return "[" + tag + "]:" + getGBytes() + " Gbytes";
    }

    @Override
    public String toString() {
        return getReadableAmount();
    }
}
