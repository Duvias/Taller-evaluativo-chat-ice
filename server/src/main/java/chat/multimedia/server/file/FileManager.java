package chat.multimedia.server.file;

import chat.multimedia.common.NetworkConfig;
import distributed.chat.FileChunk;

public final class FileManager {

    public static final int DEFAULT_CHUNK_SIZE_BYTES = NetworkConfig.FILE_CHUNK_SIZE_BYTES;

    public void validateChunk(FileChunk chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("El fragmento de archivo es nulo.");
        }
        if (chunk.transferId == null || chunk.transferId.isBlank()) {
            throw new IllegalArgumentException("Falta el identificador de transferencia.");
        }
        if (chunk.fileName == null || chunk.fileName.isBlank()) {
            throw new IllegalArgumentException("Falta el nombre del archivo.");
        }
        if (chunk.sender == null || chunk.sender.isBlank()) {
            throw new IllegalArgumentException("Falta el remitente del archivo.");
        }
        if (chunk.totalSize < 0) {
            throw new IllegalArgumentException("El tamaño total no puede ser negativo.");
        }
        if (chunk.totalChunks < 1 || chunk.chunkIndex < 0 || chunk.chunkIndex >= chunk.totalChunks) {
            throw new IllegalArgumentException("Índice o cantidad de fragmentos inválida.");
        }
        if (chunk.data == null || chunk.data.length > DEFAULT_CHUNK_SIZE_BYTES) {
            throw new IllegalArgumentException("El fragmento supera el límite de 32 KB.");
        }

        long expectedChunks = Math.max(1L,
                (chunk.totalSize + DEFAULT_CHUNK_SIZE_BYTES - 1) / DEFAULT_CHUNK_SIZE_BYTES);
        if (expectedChunks != chunk.totalChunks) {
            throw new IllegalArgumentException("Los metadatos no coinciden con el tamaño total del archivo.");
        }

        long expectedLength;
        if (chunk.totalSize == 0) {
            expectedLength = 0;
        } else if (chunk.chunkIndex < chunk.totalChunks - 1) {
            expectedLength = DEFAULT_CHUNK_SIZE_BYTES;
        } else {
            expectedLength = chunk.totalSize - ((long) chunk.chunkIndex * DEFAULT_CHUNK_SIZE_BYTES);
        }
        if (chunk.data.length != expectedLength) {
            throw new IllegalArgumentException("El tamaño del fragmento no coincide con su posición.");
        }
    }
}
