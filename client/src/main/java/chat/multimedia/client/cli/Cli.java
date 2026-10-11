package chat.multimedia.client.cli;

import chat.multimedia.client.voice.VoiceClient;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import distributed.chat.ChatServicePrx;
import distributed.chat.ClientCallbackPrx;
import distributed.chat.TargetType;

import java.util.Objects;
import java.util.Scanner;

public class Cli {

    private final VoiceClient voiceClient;

    public Cli(
            Communicator communicator,
            ChatServicePrx chatService
    ) {
        this(
                communicator,
                chatService,
                new VoiceClient(chatService)
        );
    }

    public Cli(
            Communicator communicator,
            ChatServicePrx chatService,
            VoiceClient voiceClient
    ) {
        Objects.requireNonNull(
                communicator,
                "communicator"
        );
        Objects.requireNonNull(
                chatService,
                "chatService"
        );
        this.voiceClient = Objects.requireNonNull(
                voiceClient,
                "voiceClient"
        );
    }

    public void run() {
        System.out.println("Chat multimedia CLI. Escribe /help para ver los comandos.");
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running && scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equals("/exit")) {
                    logoutIfNeeded();
                    running = false;
                    continue;
                }
                try {
                    execute(line);
                } catch (Exception exception) {
                    System.err.println("[ERROR] " + usefulMessage(exception));
                }
            }
        } finally {
            logoutIfNeeded();
        }
    }

    private void execute(String line) throws Exception {
        String[] parts = line.split("\\s+", 4);
        String command = parts[0].toLowerCase();
        switch (command) {
            case "/help" -> printHelp();
            case "/login" -> login(require(parts, 1, "/login <nickname>"));
            case "/logout" -> logoutIfNeeded();
            case "/users" -> printItems("Usuarios conectados", chatService.getConnectedUsers());
            case "/rooms" -> printItems("Salas disponibles", chatService.listRooms());
            case "/create" -> {
                requireLogin();
                String room = require(parts, 1, "/create <sala>");
                chatService.createRoom(room);
                System.out.println("[SALAS] Sala creada: " + room);
            }
            case "/join" -> {
                requireLogin();
                String room = require(parts, 1, "/join <sala>");
                chatService.joinRoom(nickname, room);
                System.out.println("[SALAS] Te uniste a: " + room);
            }
            case "/leave" -> {
                requireLogin();
                String room = require(parts, 1, "/leave <sala>");
                chatService.leaveRoom(nickname, room);
                System.out.println("[SALAS] Saliste de: " + room);
            }
            case "/roommsg" -> {
                requireLogin();
                if (parts.length < 3) {
                    throw new IllegalArgumentException("Uso: /roommsg <sala> <mensaje>");
                }
                chatService.sendRoomMessage(nickname, parts[1], parts[2] + (parts.length == 4 ? " " + parts[3] : ""));
                System.out.println("[SALAS] Mensaje enviado a " + parts[1] + ".");
            }
            case "/msg" -> {
                requireLogin();
                if (parts.length < 3) {
                    throw new IllegalArgumentException("Uso: /msg <usuario> <mensaje>");
                }
                chatService.sendPrivateMessage(nickname, parts[1], parts[2] + (parts.length == 4 ? " " + parts[3] : ""));
                System.out.println("[PRIVADO] Mensaje enviado a " + parts[1] + ".");
            }
            case "/sendfile" -> sendFileCommand(line);
            case "/call", "/accept", "/reject", "/hangup", "/mute" ->
                    System.out.println("[VOZ] Este comando corresponde al módulo del integrante 3 y se integrará con VoiceClient.");
            default -> System.out.println("Comando desconocido. Escribe /help.");
        }
    }

    private void login(String requestedNickname) throws Exception {
        if (nickname != null) {
            throw new IllegalStateException("Ya iniciaste sesión como " + nickname + ". Usa /logout primero.");
        }
        ObjectPrx callbackObject = callbackAdapter.createProxy(callbackIdentity);
        ClientCallbackPrx callback = ClientCallbackPrx.checkedCast(callbackObject);
        if (callback == null) {
            throw new IllegalStateException("No se pudo crear el proxy de callback de Ice.");
        }
        chatService.login(requestedNickname, callback);
        nickname = requestedNickname;
        System.out.println("[SESIÓN] Iniciaste sesión como " + nickname + ".");
    }

    private void logoutIfNeeded() {
        if (nickname == null) {
            return;
        }
        String oldNickname = nickname;
        nickname = null;
        try {
            chatService.logout(oldNickname);
            System.out.println("[SESIÓN] Sesión cerrada: " + oldNickname + ".");
        } catch (Exception exception) {
            System.err.println("[SESIÓN] No se pudo cerrar correctamente: " + usefulMessage(exception));
        }
    }

    private void sendFileCommand(String line) throws Exception {
        requireLogin();
        String[] tokens = line.split("\\s+", 4);
        if (tokens.length < 3) {
            throw new IllegalArgumentException("Uso: /sendfile <user|room> <destino> <ruta>\n" +
                    "Ejemplo: /sendfile user ana C:\\Users\\Ana\\foto.png");
        }

        TargetType targetType;
        String target;
        String pathText;
        if (tokens.length == 4 && (tokens[1].equalsIgnoreCase("user") || tokens[1].equalsIgnoreCase("room"))) {
            targetType = tokens[1].equalsIgnoreCase("room") ? TargetType.Room : TargetType.User;
            target = tokens[2];
            pathText = tokens[3];
        } else {
            targetType = TargetType.User;
            String arguments = line.substring("/sendfile".length()).trim();
            int separator = arguments.indexOf(' ');
            if (separator < 1 || separator == arguments.length() - 1) {
                throw new IllegalArgumentException("Uso: /sendfile <usuario> <ruta>");
            }
            target = arguments.substring(0, separator).trim();
            pathText = arguments.substring(separator + 1).trim();
        }
        if (pathText.isBlank()) {
            throw new IllegalArgumentException("Debes indicar la ruta del archivo.");
        }
        fileClient.sendFile(chatService, nickname, targetType, target, Path.of(unquote(pathText)));
    }

    private void requireLogin() {
        if (nickname == null) {
            throw new IllegalStateException("Primero inicia sesión con /login <nickname>.");
        }
    }

    private static String require(String[] parts, int index, String usage) {
        if (parts.length <= index || parts[index].isBlank()) {
            throw new IllegalArgumentException("Uso: " + usage);
        }
        return parts[index];
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static void printItems(String title, String[] values) {
        System.out.println("--- " + title + " ---");
        if (values == null || values.length == 0) {
            System.out.println("(vacío)");
            return;
        }
        Arrays.stream(values).sorted(String.CASE_INSENSITIVE_ORDER).forEach(value -> System.out.println(" - " + value));
    }

    private static String usefulMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public void setLoggedInNickname(String nickname) {
        voiceClient.setLocalNickname(nickname);
    }

    private boolean handleCommand(String line) throws Exception {
        if (line.isBlank()) {
            return true;
        }

        if (line.startsWith("/call ")) {
            voiceClient.startCall(argument(line, "/call"));
            return true;
        }

        if (line.startsWith("/gcall ")) {
            voiceClient.startGroupCall(argument(line, "/gcall"));
            return true;
        }

        if (line.startsWith("/gjoin ")) {
            voiceClient.joinGroupCall(argument(line, "/gjoin"));
            return true;
        }

        return switch (line) {
            case "/help" -> {
                printHelp();
                yield true;
            }
            case "/accept" -> {
                voiceClient.acceptIncomingCall();
                yield true;
            }
            case "/reject" -> {
                voiceClient.rejectIncomingCall();
                yield true;
            }
            case "/hangup" -> {
                voiceClient.endCall();
                yield true;
            }
            case "/mute" -> {
                boolean muted = voiceClient.toggleMute();
                System.out.println(
                        muted
                                ? "[VOZ] Micrófono silenciado."
                                : "[VOZ] Micrófono activado."
                );
                yield true;
            }
            case "/gleave" -> {
                voiceClient.leaveGroupCall();
                yield true;
            }
            case "/gend" -> {
                voiceClient.endGroupCall();
                yield true;
            }
            case "/exit" -> false;
            default -> {
                System.out.println(
                        "Comando todavía no implementado: " + line
                );
                yield true;
            }
        };
    }

    private static String argument(
            String line,
            String command
    ) {
        String value = line.substring(command.length()).trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Falta el argumento para " + command + "."
            );
        }

        return value;
    }

    private void printHelp() {
        System.out.println(String.join(System.lineSeparator(),
                "Comandos disponibles:",
                "  /login <nickname>",
                "  /logout",
                "  /users",
                "  /rooms",
                "  /create <sala>",
                "  /join <sala>",
                "  /leave <sala>",
                "  /msg <usuario> <mensaje>",
                "  /roommsg <sala> <mensaje>",
                "  /sendfile user <usuario> <ruta>",
                "  /sendfile room <sala> <ruta>",
                "  /help",
                "  /exit",
                "Los comandos /call, /accept, /reject, /hangup y /mute se integran con el módulo de voz."));
    }
}
