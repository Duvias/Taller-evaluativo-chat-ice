package chat.multimedia.client.cli;

import chat.multimedia.client.voice.VoiceClient;
import com.zeroc.Ice.Communicator;
import distributed.chat.ChatServicePrx;

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
        System.out.println("Escribe /help para ver los comandos base.");

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running && scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                try {
                    running = handleCommand(line);
                } catch (Exception exception) {
                    String message = exception.getMessage();

                    if (message == null || message.isBlank()) {
                        message = exception.getClass().getSimpleName();
                    }

                    System.out.println("[ERROR] " + message);
                }
            }
        } finally {
            voiceClient.close();
        }
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
        String help = String.join(System.lineSeparator(),
                "Comandos previstos:",
                "  /login <nickname>",
                "  /logout",
                "  /users",
                "  /rooms",
                "  /create <room>",
                "  /join <room>",
                "  /leave <room>",
                "  /msg <user> <message>",
                "  /roommsg <room> <message>",
                "  /sendfile <target> <path>",
                "  /call <user>",
                "  /accept",
                "  /reject",
                "  /hangup",
                "  /mute",
                "  /gcall <room>",
                "  /gjoin <room>",
                "  /gleave",
                "  /gend",
                "  /help",
                "  /exit"
        );

        System.out.println(help);
    }
}
