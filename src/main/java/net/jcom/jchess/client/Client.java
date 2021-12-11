package net.jcom.jchess.client;

import net.jcom.jchess.client.ai.BaseAi;
import net.jcom.jchess.server.factory.JChessMessageFactory;
import net.jcom.jchess.server.generated.*;
import net.jcom.jchess.server.iostreams.JChessInputStream;
import net.jcom.jchess.server.iostreams.JChessOutputStream;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.*;

import static net.jcom.jchess.client.StartClient.logger;

public class Client {
    private static final int EMERGENCY_SWITCH_PCT = 90;

    private BaseAi mainAi;
    private BaseAi backUpAi;
    private UUID id;
    private long moveStart;
    private boolean run = true;

    private JChessOutputStream ostream;
    private JChessInputStream inputStream;

    public Client(BaseAi mainAi, BaseAi backUpAi, Socket serverSocket) {
        this.mainAi = mainAi;
        this.backUpAi = backUpAi;
        try {
            this.ostream = new JChessOutputStream(serverSocket.getOutputStream());
            this.inputStream = new JChessInputStream(serverSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void login() {
        try {
            JChessMessage login = JChessMessageFactory.createLoginMessage(UUID.randomUUID(), StartClient.PLAYER_NAME);
            ostream.write(login);
            JChessMessage loginReply = inputStream.readJChess();
            switch (loginReply.getMessageType()) {
                case LOGIN_REPLY:
                    this.id = UUID.fromString(loginReply.getPlayerId());
                    this.mainAi.setOwnId(id);
                    this.backUpAi.setOwnId(id);
                    logger.info("Successful Login");
                    break;
                case HEART_BEAT:
                    //TODO reread the loginreply and ignore HeartBeat with a while probably
                case ACCEPT:
                default:
                    AcceptMessage accept = loginReply.getAccept();
                    if (accept == null || accept.getErrorTypeCode().equals(ErrorType.TOO_MANY_TRIES)) {
                        System.exit(1);
                    } else if (accept.getErrorTypeCode().equals(ErrorType.DUPLICATE_NAME)) {
                        StartClient.PLAYER_NAME = StartClient.PLAYER_NAME + UUID.randomUUID();
                    }
                    login();
            }
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    public int play() {
        int exitCode = 0;
        while (this.run) {
            try {
                JChessMessage reply = inputStream.readJChess();
                if (reply == null) continue;
                logger.info(reply.getMessageType());
                switch (reply.getMessageType()) {
                    case LOGIN:
                    case LOGIN_REPLY:
                        //sollte/kann nicht passieren laut Protokoll
                        break;
                    case HEART_BEAT:
                        handleHeartBeat(reply.getHeartBeat());
                        break;
                    case ACCEPT:
                        handleAccept(reply);
                        break;
                    case DISCONNECT:
                        this.run = false;
                        break;
                    case MATCH_FOUND:
                        break;
                    case MATCH_OVER:
                        break;
                    case MATCH_STATUS:
                        break;
                    case GAME_START:
                        break;
                    case GAME_OVER:
                        logger.info(reply.getGameOver().getPgn());
                        break;
                    case AWAIT_MOVE:
                        this.moveStart = System.currentTimeMillis();
                        handleAwaitMove(reply.getAwaitMove());
                        break;
                    case MOVE:
                        break;
                    case REQUEST_DRAW:
                        break;
                    case DRAW_RESPONSE:
                        break;
                }
            } catch (SocketException e) {
                //TODO try reconnect?
                logger.fatal("Server does not seem to be reachable", e);
                logger.fatal("Client will shut down");
                this.run = false;
                exitCode = 1;
            } catch (IOException e) {
                logger.fatal("Client will shut down", e);
                this.run = false;
                exitCode = 1;
            }
        }
        return exitCode;
    }

    private void handleAwaitMove(AwaitMoveMessage awaitMoveMessage) {
        System.out.println("enters handler ");

        ExecutorService es = Executors.newSingleThreadExecutor();
        Future<MoveData> resultMainAI = es.submit(() -> Client.this.mainAi.makeMove(awaitMoveMessage));
        MoveData resultToSend = null;
        //Check if thread is able to finish in time
        try {
            long timeAlreadyPassed = System.currentTimeMillis() - this.moveStart;
            if (timeAlreadyPassed < 0) {
                timeAlreadyPassed = 0;
                System.out.println("Time was mysteriously below 0");
            }
            if (timeAlreadyPassed > StartClient.MAX_MOVE_TIME_MS) {
                this.moveStart = System.currentTimeMillis();
                timeAlreadyPassed = 0;
                System.out.println("Time was mysteriously above " + StartClient.MAX_MOVE_TIME_MS);
            }
            var latestResults = resultMainAI.get((long) (1. * EMERGENCY_SWITCH_PCT / 100 * StartClient.MAX_MOVE_TIME_MS - timeAlreadyPassed), TimeUnit.MILLISECONDS);
            if (latestResults == null) {
                latestResults = backUpAi.makeMove(awaitMoveMessage);
            }
            resultToSend = latestResults;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException ignored) {
            //Time is over
            System.out.println("MainAi took too long");
        }

        JChessMessage msg = JChessMessageFactory.createMoveMessage(this.id, resultToSend);

        try {
            ostream.write(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Zug dauerte " + (System.currentTimeMillis() - this.moveStart) + "ms.");
    }


    private void handleHeartBeat(HeartBeatMessage heartBeat) {
        //logger.info(heartBeat.getClass().getSimpleName());
    }

    public int run() {
        login();
        return play();
    }

    private void handleAccept(JChessMessage reply) {
        AcceptMessage acceptMessageData = reply.getAccept();
        switch (acceptMessageData.getErrorTypeCode()) {
            case NO_ERROR:
            case ERROR:
            case AWAIT_LOGIN:
            case UNSUPPORTED_OPERATION:
            case TOO_MANY_TRIES:
            case TIMEOUT:
                break;
            case DUPLICATE_NAME:
                StartClient.PLAYER_NAME = StartClient.PLAYER_NAME + UUID.randomUUID();
        }
    }
}
