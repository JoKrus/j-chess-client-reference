package net.jcom.jchess.client;

import net.jcom.jchess.client.ai.FoolsMateAi;
import net.jcom.jchess.client.ai.RandomAi;
import net.jcom.jchess.server.logging.Logger;
import net.jcom.jchess.server.logging.LoggerBuilder;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;

/**
 * Hello world!
 *
 */
public class StartClient
{
    public static final Logger logger = LoggerBuilder.init().format(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"))
            .addWriteStream(System.out, System.err).minimumLevel(Logger.LoggingLevel.DEBUG).build();
    public static final int MAX_MOVE_TIME_MS = 60_000;
    static String PLAYER_NAME = "ReferenceClient";
    private static final String OPTION_HOSTNAME = "host";
    private static final String OPTION_PORT = "port";
    private static final String OPTION_HELP = "help";

    private static String hostname = "localhost";
    private static int port = 5123;
    private static final Options options = new Options();

    static {
        String descriptionHost = "Festlegen zu welchem Host verbunden werden soll";
        String descriptionPort = "Festlegen auf welchen Port auf dem Zielsystem verbunden werden soll";
        String descriptionHelp = "Anzeigen dieser Hilfe";

        options.addOption(
                Option.builder().longOpt(OPTION_HOSTNAME).desc(descriptionHost).hasArg().argName("hostname").build());
        options.addOption(Option.builder().longOpt(OPTION_PORT).desc(descriptionPort).hasArg().argName("port").build());
        options.addOption(Option.builder().longOpt(OPTION_HELP).desc(descriptionHelp).build());
    }

    private static void parseArgs(String[] args) {
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);
            // wenn Hilfe angezeigt wird, wird der Rest ignoriert
            if (commandLine.hasOption(OPTION_HELP)) {
                formatter.printHelp(PLAYER_NAME, options);
                System.exit(0);
            }
            if (commandLine.hasOption(OPTION_HOSTNAME))
                hostname = commandLine.getOptionValue(OPTION_HOSTNAME);
            if (commandLine.hasOption(OPTION_PORT))
                port = Integer.parseInt(commandLine.getOptionValue(OPTION_PORT));
        } catch (ParseException e) {
            // sobald ein ungueltiger Parameter vorhanden ist
            System.err.println("Ungültige Parameter vorhanden -> Anzeigen der Hilfe");
            formatter.printHelp(PLAYER_NAME, options);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        parseArgs(args);
        System.out.println("Team: " + PLAYER_NAME);
        System.out.println("Host: " + hostname);
        System.out.println("Port: " + port);

        Socket toServer = null;

        try {
            toServer = new Socket(hostname, port);
        } catch (IOException e) {
            logger.error("Can not connect to server", e);
            return;
        }

        int exitCode = new Client(new FoolsMateAi(), new RandomAi(), toServer).run();
        System.exit(exitCode);
    }
}
