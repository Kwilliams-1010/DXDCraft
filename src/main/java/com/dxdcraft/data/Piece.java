package com.dxdcraft.data;

/** Evil Piece a Devil was reincarnated with. NONE for non-devils. */
public enum Piece {
    NONE("none", "None"),
    PAWN("pawn", "Pawn"),
    KNIGHT("knight", "Knight"),
    BISHOP("bishop", "Bishop"),
    ROOK("rook", "Rook"),
    QUEEN("queen", "Queen");

    private final String id;
    private final String displayName;

    Piece(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }

    public boolean speed() { return this == KNIGHT || this == QUEEN; }
    public boolean power() { return this == ROOK || this == QUEEN; }
    public boolean magic() { return this == BISHOP || this == QUEEN; }

    public static Piece byId(String id) {
        for (Piece p : values()) {
            if (p.id.equalsIgnoreCase(id)) return p;
        }
        return null;
    }
}
