package net.jcom.jchess.client.logic;

import net.jcom.jchess.server.generated.MoveData;
import net.jcom.jchess.server.logic.Color;
import net.jcom.jchess.server.logic.Position;
import net.jcom.jchess.server.logic.pieces.Piece;

import java.util.ArrayList;
import java.util.List;

public class ClientPosition extends Position {

    @Override
    public List<MoveData> generateAllMoves(Color color) {
        List<MoveData> ret = new ArrayList<>();

        for (Piece piece : this.getPieceList(color)) {
            ret.addAll(piece.possibleToMoveTo(this));
        }

        return ret;
    }

}
