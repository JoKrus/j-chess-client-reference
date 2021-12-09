package net.jcom.jchess.client.ai;

import net.jcom.jchess.server.generated.AwaitMoveMessage;
import net.jcom.jchess.server.generated.MoveData;

import java.util.UUID;

public abstract class BaseAi {
    protected UUID ownId;

    public void setOwnId(UUID ownId) {
        this.ownId = ownId;
    }


    public abstract MoveData makeMove(AwaitMoveMessage awaitMoveMessage);
}
