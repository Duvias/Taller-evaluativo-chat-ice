package chat.multimedia.client.file;

import chat.multimedia.common.NetworkConfig;

/**
 * Chunking y reconstrucción de archivos en el cliente.
 * Integrante 2 completará este módulo.
 */
public class FileClient {

    public int chunkSizeBytes() {
        return NetworkConfig.FILE_CHUNK_SIZE_BYTES;
    }
}
