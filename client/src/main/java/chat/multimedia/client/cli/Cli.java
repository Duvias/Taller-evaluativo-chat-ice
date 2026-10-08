package chat.multimedia.client.cli;

import com.zeroc.Ice.Communicator;
import distributed.chat.ChatServicePrx;

import java.util.Scanner;

/**
 * CLI base. Integrante 2 ampliará el parser de comandos.
 */
public class Cli {

    private final Communicator communicator;
    private final ChatServicePrx chatService;

    public Cli(
            Communicator communicator,
            ChatServicePrx chatService
    ) {
        this.communicator = communicator;
        this.chatService = chatService;
    }

    public void run() {
        System.out.println("Escribe /help para ver los comandos base.");

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running && scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                switch (line) {
                    case "/help" -> printHelp();
                    case "/exit" -> running = false;
                    case "" -> {
                        // Ignorar línea vacía.
                    }
                    default -> System.out.println(
                            "Comando todavía no implementado: " + line
                    );
                }
            }
        }
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
                "  /help",
                "  /exit"
        );

        System.out.println(help);
    }
}
