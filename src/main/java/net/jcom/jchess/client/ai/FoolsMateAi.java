package net.jcom.jchess.client.ai;

import net.jcom.jchess.server.generated.AwaitMoveMessage;
import net.jcom.jchess.server.generated.MoveData;
import net.jcom.jchess.server.logic.Color;
import net.jcom.jchess.server.logic.Position;

import java.util.HashMap;
import java.util.List;

public class FoolsMateAi extends BaseAi {
    private final HashMap<Color, List<MoveData>> moveMap = new HashMap<>() {{
        put(Color.WHITE, List.of(new MoveData() {{
            setFrom("f2");
            setTo("f3");
        }}, new MoveData() {{
            setFrom("g2");
            setTo("g4");
        }}));

        put(Color.BLACK, List.of(new MoveData() {{
            setFrom("e7");
            setTo("e5");
        }}, new MoveData() {{
            setFrom("d8");
            setTo("h4");
        }}));
    }};

    @Override
    public MoveData makeMove(AwaitMoveMessage awaitMoveMessage) {
        var pos = new Position(awaitMoveMessage.getPosition());
        return moveMap.get(pos.getCurrent()).get(pos.getRound() - 1);
    }
}
