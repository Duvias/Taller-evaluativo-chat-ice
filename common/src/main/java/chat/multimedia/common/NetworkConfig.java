package chat.multimedia.common;

public final class NetworkConfig {

    public static final String ICE_HOST =
            System.getProperty("chat.ice.host", "127.0.0.1");

    public static final int ICE_PORT =
            Integer.getInteger("chat.ice.port", 10000);

    public static final String UDP_RELAY_HOST =
            System.getProperty("chat.udp.relayHost", ICE_HOST);

    public static final int UDP_RELAY_PORT =
            Integer.getInteger("chat.udp.relayPort", 12000);

    public static final int FILE_CHUNK_SIZE_BYTES = 32 * 1024;

    private NetworkConfig() {
    }

    public static String chatServiceProxy() {
        return "chatService:tcp -h " + ICE_HOST + " -p " + ICE_PORT;
    }
}
