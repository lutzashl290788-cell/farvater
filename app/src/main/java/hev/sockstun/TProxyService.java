package hev.sockstun;

// JNI-вход libhev-socks5-tunnel.so, сигнатуры как в hev-jni.c
public final class TProxyService {
    private TProxyService() {}

    public static native boolean TProxyStartService(String configPath, int fd);

    public static native boolean TProxyStopService();

    public static native boolean TProxyIsRunning();

    // tx_packets, tx_bytes, rx_packets, rx_bytes
    public static native long[] TProxyGetStats();
}
