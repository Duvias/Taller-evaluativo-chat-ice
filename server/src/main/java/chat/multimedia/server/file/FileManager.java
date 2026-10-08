package chat.multimedia.server.file;

/**
 * Coordinación de transferencia de archivos por chunks usando Ice.
 * No se debe abrir un socket TCP adicional para archivos.
 */
public class FileManager {

    public static final int DEFAULT_CHUNK_SIZE_BYTES = 32 * 1024;
}
