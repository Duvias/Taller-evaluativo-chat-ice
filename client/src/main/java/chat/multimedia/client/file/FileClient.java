package chat.multimedia.client.file;

import chat.multimedia.common.NetworkConfig;
import distributed.chat.ChatServicePrx;
import distributed.chat.FileChunk;
import distributed.chat.TargetType;
import distributed.chat.NotInRoom;
import distributed.chat.RoomNotFound;
import distributed.chat.UserNotFound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FileClient {

    private final int chunkSizeBytes;
    private final Path outputDirectory;
    private final ConcurrentMap<String, IncomingTransfer> incomingTransfers = new ConcurrentHashMap<>();

    public FileClient() {
        this(NetworkConfig.FILE_CHUNK_SIZE_BYTES,
                Paths.get(System.getProperty("chat.files.dir", "received_files")));
    }

    public FileClient(int chunkSizeBytes, Path outputDirectory) {
        if (chunkSizeBytes < 1) {
            throw new IllegalArgumentException("El tamaño del fragmento debe ser positivo.");
        }
        this.chunkSizeBytes = chunkSizeBytes;
        this.outputDirectory = outputDirectory;
    }

    public int chunkSizeBytes() {
        return chunkSizeBytes;
    }

    public void sendFile(
            ChatServicePrx chatService,
            String sender,
            TargetType targetType,
            String target,
            Path source
    ) throws IOException, UserNotFound, RoomNotFound, NotInRoom {
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new IOException("No se puede leer el archivo: " + source);
        }
        long totalSize = Files.size(source);
        long chunksLong = Math.max(1L, (totalSize + chunkSizeBytes - 1) / chunkSizeBytes);
        if (chunksLong > Integer.MAX_VALUE) {
            throw new IOException("El archivo tiene demasiados fragmentos para esta implementación.");
        }
        int totalChunks = (int) chunksLong;
        String transferId = UUID.randomUUID().toString();
        String fileName = source.getFileName().toString();

        try (InputStream input = new BufferedInputStream(Files.newInputStream(source))) {
            for (int index = 0; index < totalChunks; index++) {
                int expected = (int) Math.min(chunkSizeBytes,
                        Math.max(0L, totalSize - ((long) index * chunkSizeBytes)));
                byte[] data = input.readNBytes(expected);
                if (data.length != expected) {
                    throw new IOException("El archivo cambió o se truncó durante la transferencia.");
                }
                FileChunk chunk = new FileChunk(
                        transferId, fileName, totalSize, index, totalChunks, sender, data);
                chatService.sendFileChunk(sender, targetType, target, chunk);
            }
        }
        System.out.printf("[ARCHIVO] Enviado %s (%d bytes, %d fragmentos).%n",
                fileName, totalSize, totalChunks);
    }

    public void receiveChunk(TargetType targetType, String target, FileChunk chunk) {
        if (chunk == null || chunk.transferId == null || chunk.transferId.isBlank()) {
            System.err.println("[ARCHIVO] Se ignoró un fragmento sin identificador de transferencia.");
            return;
        }
        try {
            Files.createDirectories(outputDirectory);
            IncomingTransfer transfer = incomingTransfers.computeIfAbsent(
                    chunk.transferId, ignored -> new IncomingTransfer(chunk, safeOutputPath(chunk)));
            Path completedPath = transfer.accept(chunk, chunkSizeBytes);
            if (completedPath != null) {
                incomingTransfers.remove(chunk.transferId, transfer);
                System.out.printf("[ARCHIVO] Recibido de %s para %s '%s': %s (%d bytes).%n",
                        chunk.sender,
                        targetType == TargetType.Room ? "sala" : "usuario",
                        target,
                        completedPath.toAbsolutePath(),
                        chunk.totalSize);
            }
        } catch (IOException | IllegalArgumentException exception) {
            incomingTransfers.remove(chunk.transferId);
            System.err.println("[ARCHIVO] No se pudo reconstruir el archivo: " + exception.getMessage());
        }
    }

    private Path safeOutputPath(FileChunk chunk) {
        String originalName = Paths.get(chunk.fileName == null ? "archivo.bin" : chunk.fileName)
                .getFileName().toString();
        if (originalName.isBlank() || originalName.equals(".") || originalName.equals("..")) {
            originalName = "archivo.bin";
        }
        return outputDirectory.resolve(chunk.transferId + "-" + originalName).normalize();
    }

    private static final class IncomingTransfer {
        private final String transferId;
        private final String fileName;
        private final long totalSize;
        private final int totalChunks;
        private final String sender;
        private final Path outputPath;
        private final BitSet received;
        private int receivedCount;

        private IncomingTransfer(FileChunk first, Path outputPath) {
            if (first.totalSize < 0 || first.totalChunks < 1 || first.totalChunks > 10_000_000) {
                throw new IllegalArgumentException("Metadatos de transferencia inválidos.");
            }
            this.transferId = first.transferId;
            this.fileName = first.fileName;
            this.totalSize = first.totalSize;
            this.totalChunks = first.totalChunks;
            this.sender = first.sender;
            this.outputPath = outputPath;
            this.received = new BitSet(totalChunks);
        }

        private synchronized Path accept(FileChunk chunk, int chunkSize) throws IOException {
            if (!transferId.equals(chunk.transferId)
                    || !fileName.equals(chunk.fileName)
                    || totalSize != chunk.totalSize
                    || totalChunks != chunk.totalChunks
                    || !sender.equals(chunk.sender)) {
                throw new IllegalArgumentException("Los metadatos cambiaron durante la transferencia.");
            }
            if (chunk.chunkIndex < 0 || chunk.chunkIndex >= totalChunks
                    || chunk.data == null || chunk.data.length > chunkSize) {
                throw new IllegalArgumentException("Fragmento fuera de rango o demasiado grande.");
            }
            long offset = (long) chunk.chunkIndex * chunkSize;
            long expectedLength = totalSize == 0 ? 0
                    : Math.min(chunkSize, totalSize - offset);
            if (offset > totalSize || chunk.data.length != expectedLength) {
                throw new IllegalArgumentException("Tamaño o posición del fragmento inválido.");
            }
            if (!received.get(chunk.chunkIndex)) {
                try (RandomAccessFile output = new RandomAccessFile(outputPath.toFile(), "rw")) {
                    output.seek(offset);
                    output.write(chunk.data);
                    output.setLength(totalSize);
                }
                received.set(chunk.chunkIndex);
                receivedCount++;
            }
            return receivedCount == totalChunks ? outputPath : null;
        }
    }
}
