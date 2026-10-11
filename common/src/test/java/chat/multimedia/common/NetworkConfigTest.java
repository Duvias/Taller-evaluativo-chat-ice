package chat.multimedia.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkConfigTest {

    @Test
    void fileChunkSizeIsPositive() {
        assertTrue(NetworkConfig.FILE_CHUNK_SIZE_BYTES > 0);
    }
}
