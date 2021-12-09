package net.jcom.jchess.client.ai;

import net.jcom.jchess.server.generated.AwaitMoveMessage;
import net.jcom.jchess.server.generated.MoveData;
import net.jcom.jchess.server.logic.Position;

import java.util.Collections;

public class RandomAi extends BaseAi {
    @Override
    public MoveData makeMove(AwaitMoveMessage awaitMoveMessage) {
        var pos = new Position(awaitMoveMessage.getPosition());
        var allMoves = pos.generateAllMoves(pos.getCurrent());
        Collections.shuffle(allMoves);
        return allMoves.get(0);
    }
}
