package games.escampe;

import iialib.games.model.IMove;

/**
 * Classe représentant un coup dans le jeu Escampe
 * 
 * Deux types de coups sont possibles :
 * 1. Placement initial : "C6/A6/B5/D5/E6/F5" (Licorne en premier, puis les 5
 * paladins)
 * 2. Déplacement : "B1-D1" (case départ - case arrivée)
 * 3. Passer son tour : "E"
 */
public class EscampeMove implements IMove {

    // Type de coup
    public enum MoveType {
        PLACEMENT, // Placement initial des pièces
        MOVE, // Déplacement d'une pièce
        PASS // Passer son tour
    }

    private final MoveType type;

    // Pour PLACEMENT : positions des pièces (Licorne en [0], Paladins en [1-5])
    private final int[] placementCols; // colonnes (0-5 pour A-F)
    private final int[] placementRows; // lignes (0-5 pour 1-6)

    // Pour MOVE : case départ et arrivée
    private final int fromCol, fromRow;
    private final int toCol, toRow;

    /**
     * Constructeur pour un coup de type PASS
     */
    public EscampeMove() {
        this.type = MoveType.PASS;
        this.placementCols = null;
        this.placementRows = null;
        this.fromCol = -1;
        this.fromRow = -1;
        this.toCol = -1;
        this.toRow = -1;
    }

    /**
     * Constructeur pour un coup de type MOVE
     * 
     * @param fromCol colonne de départ (0-5)
     * @param fromRow ligne de départ (0-5)
     * @param toCol   colonne d'arrivée (0-5)
     * @param toRow   ligne d'arrivée (0-5)
     */
    public EscampeMove(int fromCol, int fromRow, int toCol, int toRow) {
        this.type = MoveType.MOVE;
        this.fromCol = fromCol;
        this.fromRow = fromRow;
        this.toCol = toCol;
        this.toRow = toRow;
        this.placementCols = null;
        this.placementRows = null;
    }

    /**
     * Constructeur pour un coup de type PLACEMENT
     * 
     * @param cols tableau des colonnes (6 éléments : Licorne puis 5 Paladins)
     * @param rows tableau des lignes (6 éléments : Licorne puis 5 Paladins)
     */
    public EscampeMove(int[] cols, int[] rows) {
        this.type = MoveType.PLACEMENT;
        this.placementCols = cols.clone();
        this.placementRows = rows.clone();
        this.fromCol = -1;
        this.fromRow = -1;
        this.toCol = -1;
        this.toRow = -1;
    }

    /**
     * Parse un coup à partir d'une chaîne de caractères
     * 
     * @param moveStr la chaîne représentant le coup
     * @return le coup parsé
     */
    public static EscampeMove parse(String moveStr) {
        if (moveStr == null || moveStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Coup vide");
        }

        moveStr = moveStr.trim().toUpperCase();

        // Cas PASS
        if (moveStr.equals("E")) {
            return new EscampeMove();
        }

        // Cas MOVE : "B1-D1"
        if (moveStr.contains("-")) {
            String[] parts = moveStr.split("-");
            if (parts.length != 2 || parts[0].length() != 2 || parts[1].length() != 2) {
                throw new IllegalArgumentException("Format de coup invalide : " + moveStr);
            }

            int fromCol = parts[0].charAt(0) - 'A';
            int fromRow = parts[0].charAt(1) - '1';
            int toCol = parts[1].charAt(0) - 'A';
            int toRow = parts[1].charAt(1) - '1';

            return new EscampeMove(fromCol, fromRow, toCol, toRow);
        }

        // Cas PLACEMENT : "C6/A6/B5/D5/E6/F5"
        if (moveStr.contains("/")) {
            String[] parts = moveStr.split("/");
            if (parts.length != 6) {
                throw new IllegalArgumentException("Placement doit contenir 6 positions : " + moveStr);
            }

            int[] cols = new int[6];
            int[] rows = new int[6];

            for (int i = 0; i < 6; i++) {
                if (parts[i].length() != 2) {
                    throw new IllegalArgumentException("Position invalide : " + parts[i]);
                }
                cols[i] = parts[i].charAt(0) - 'A';
                rows[i] = parts[i].charAt(1) - '1';
            }

            return new EscampeMove(cols, rows);
        }

        throw new IllegalArgumentException("Format de coup non reconnu : " + moveStr);
    }

    // Getters

    public MoveType getType() {
        return type;
    }

    public boolean isPass() {
        return type == MoveType.PASS;
    }

    public boolean isPlacement() {
        return type == MoveType.PLACEMENT;
    }

    public boolean isMove() {
        return type == MoveType.MOVE;
    }

    public int getFromCol() {
        return fromCol;
    }

    public int getFromRow() {
        return fromRow;
    }

    public int getToCol() {
        return toCol;
    }

    public int getToRow() {
        return toRow;
    }

    public int[] getPlacementCols() {
        return placementCols;
    }

    public int[] getPlacementRows() {
        return placementRows;
    }

    /**
     * Convertit une colonne (0-5) en lettre (A-F)
     */
    public static char colToLetter(int col) {
        return (char) ('A' + col);
    }

    /**
     * Convertit une ligne (0-5) en chiffre (1-6)
     */
    public static char rowToDigit(int row) {
        return (char) ('1' + row);
    }

    @Override
    public String toString() {
        switch (type) {
            case PASS:
                return "E";
            case MOVE:
                return "" + colToLetter(fromCol) + rowToDigit(fromRow) +
                        "-" + colToLetter(toCol) + rowToDigit(toRow);
            case PLACEMENT:
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 6; i++) {
                    if (i > 0)
                        sb.append("/");
                    sb.append(colToLetter(placementCols[i]));
                    sb.append(rowToDigit(placementRows[i]));
                }
                return sb.toString();
            default:
                return "?";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        EscampeMove other = (EscampeMove) obj;
        return this.toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
