package escampe;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Classe représentant le plateau du jeu Escampe
 * Implémente l'interface Partie1 (opérations de base)
 * 
 * Le plateau est un tableau 6x6 avec des cases de type liseré 1, 2 ou 3
 * qui déterminent la distance de déplacement possible.
 * 
 * Représentation interne :
 * - boardGrid[row][col] : contient le type de pièce sur la case
 * - row : 0-5 correspond aux lignes 1-6
 * - col : 0-5 correspond aux colonnes A-F
 */
public class EscampeBoard implements Partie1 {

    // Taille du plateau
    public static final int GRID_SIZE = 6;

    // Types de pièces sur le plateau
    public enum Piece {
        EMPTY('-'),
        LICORNE_NOIRE('N'),
        PALADIN_NOIR('n'),
        LICORNE_BLANCHE('B'),
        PALADIN_BLANC('b');

        private final char symbol;

        Piece(char symbol) {
            this.symbol = symbol;
        }

        public char getSymbol() {
            return symbol;
        }

        public static Piece fromSymbol(char c) {
            for (Piece p : values()) {
                if (p.symbol == c)
                    return p;
            }
            throw new IllegalArgumentException("Symbole de pièce inconnu : " + c);
        }

        public boolean isBlack() {
            return this == LICORNE_NOIRE || this == PALADIN_NOIR;
        }

        public boolean isWhite() {
            return this == LICORNE_BLANCHE || this == PALADIN_BLANC;
        }

        public boolean isLicorne() {
            return this == LICORNE_NOIRE || this == LICORNE_BLANCHE;
        }

        public boolean isPaladin() {
            return this == PALADIN_NOIR || this == PALADIN_BLANC;
        }

        public boolean belongsTo(EscampeRole role) {
            if (role == EscampeRole.NOIR) {
                return isBlack();
            } else {
                return isWhite();
            }
        }
    }

    // Carte des liserés : 1, 2 ou 3 selon le type de cercle
    // Indexé par [row][col] (row 0 = ligne 1, col 0 = colonne A)
    public static final int[][] LISERE_MAP = {
            { 1, 2, 2, 3, 1, 2 }, // Ligne 1 (row 0)
            { 3, 1, 3, 1, 3, 2 }, // Ligne 2 (row 1)
            { 2, 3, 1, 2, 1, 3 }, // Ligne 3 (row 2)
            { 2, 1, 3, 2, 3, 1 }, // Ligne 4 (row 3)
            { 1, 3, 1, 3, 1, 2 }, // Ligne 5 (row 4)
            { 3, 2, 2, 1, 3, 2 } // Ligne 6 (row 5)
    };

    // ---------------------- Attributes ---------------------

    private Piece[][] boardGrid;

    // Dernier coup joué (pour déterminer le liseré obligatoire)
    private int lastMoveToCol = -1;
    private int lastMoveToRow = -1;

    // Indicateurs de phase de jeu
    private boolean blackPlaced = false; // Noir a placé ses pièces
    private boolean whitePlaced = false; // Blanc a placé ses pièces

    // ---------------------- Constructors ---------------------

    /**
     * Constructeur par défaut : plateau vide
     */
    public EscampeBoard() {
        boardGrid = new Piece[GRID_SIZE][GRID_SIZE];
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                boardGrid[row][col] = Piece.EMPTY;
            }
        }
    }

    /**
     * Constructeur par copie
     */
    public EscampeBoard(EscampeBoard other) {
        this.boardGrid = other.copyGrid();
        this.lastMoveToCol = other.lastMoveToCol;
        this.lastMoveToRow = other.lastMoveToRow;
        this.blackPlaced = other.blackPlaced;
        this.whitePlaced = other.whitePlaced;
    }

    /**
     * Copie interne de la grille
     */
    private Piece[][] copyGrid() {
        Piece[][] newGrid = new Piece[GRID_SIZE][GRID_SIZE];
        for (int row = 0; row < GRID_SIZE; row++) {
            System.arraycopy(boardGrid[row], 0, newGrid[row], 0, GRID_SIZE);
        }
        return newGrid;
    }

    // ---------------------- Interface Partie1 ---------------------

    @Override
    public void setFromFile(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;

            // Réinitialiser le plateau
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    boardGrid[row][col] = Piece.EMPTY;
                }
            }

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Ignorer les commentaires
                if (line.startsWith("%") || line.isEmpty()) {
                    continue;
                }

                // Extraire le numéro de ligne et le contenu
                // Format : "01 bb---- 01" ou "06 n-N-n- 06"
                int rowIndex = extractRowIndex(line);
                if (rowIndex >= 0 && rowIndex < GRID_SIZE) {
                    String content = extractBoardContent(line);
                    if (content.length() >= GRID_SIZE) {
                        for (int col = 0; col < GRID_SIZE; col++) {
                            boardGrid[rowIndex][col] = Piece.fromSymbol(content.charAt(col));
                        }
                    }
                }
            }

            // Déterminer si les pièces ont été placées
            updatePlacementStatus();

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier : " + fileName, e);
        }
    }

    /**
     * Extrait l'index de ligne (0-5) à partir d'une ligne du fichier
     */
    private int extractRowIndex(String line) {
        // Chercher le numéro au début de la ligne
        line = line.trim();
        if (line.length() >= 2) {
            try {
                int num = Integer.parseInt(line.substring(0, 2).trim());
                return num - 1; // Convertir 01-06 en 0-5
            } catch (NumberFormatException e) {
                // Essayer avec un seul chiffre
                try {
                    int num = Integer.parseInt(line.substring(0, 1));
                    return num - 1;
                } catch (NumberFormatException e2) {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * Extrait le contenu du plateau d'une ligne du fichier
     */
    private String extractBoardContent(String line) {
        // Supprimer les numéros de ligne au début et à la fin
        line = line.trim();

        // Format attendu : "01 bb---- 01" ou simplement "bb----"
        if (line.matches("\\d+\\s+.*\\s+\\d+")) {
            // Retirer le numéro de début et de fin
            int firstSpace = line.indexOf(' ');
            int lastSpace = line.lastIndexOf(' ');
            if (firstSpace != lastSpace) {
                return line.substring(firstSpace + 1, lastSpace).trim();
            }
        }

        // Sinon, essayer de trouver la séquence de 6 caractères valides
        StringBuilder content = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '-' || c == 'N' || c == 'n' || c == 'B' || c == 'b') {
                content.append(c);
            }
        }
        return content.toString();
    }

    /**
     * Met à jour les indicateurs de placement
     */
    private void updatePlacementStatus() {
        blackPlaced = false;
        whitePlaced = false;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (boardGrid[row][col].isBlack()) {
                    blackPlaced = true;
                }
                if (boardGrid[row][col].isWhite()) {
                    whitePlaced = true;
                }
            }
        }
    }

    @Override
    public void saveToFile(String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("% ABCDEF");

            // Écrire de la ligne 1 à la ligne 6 (de bas en haut pour l'affichage)
            for (int row = 0; row < GRID_SIZE; row++) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%02d ", row + 1));
                for (int col = 0; col < GRID_SIZE; col++) {
                    sb.append(boardGrid[row][col].getSymbol());
                }
                sb.append(String.format(" %02d", row + 1));
                writer.println(sb.toString());
            }

            writer.println("% ABCDEF");

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier : " + fileName, e);
        }
    }

    /**
     * Vérifie si un coup est valide pour un joueur
     */
    @Override
    public boolean isValidMove(String move, String player) {
        try {
            EscampeMove m = EscampeMove.parse(move);
            String[] possibles = possiblesMoves(player);

            if (m.isPass()) {
                // Vérifier qu'aucun autre coup n'est possible
                return possibles.length == 1 && possibles[0].equals("PASS");
            }

            for (String possible : possibles) {
                if (possible.equals(move)) {
                    return true;
                }
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String[] possiblesMoves(String player) {
        EscampeRole playerRole = EscampeRole.fromString(player);
        ArrayList<EscampeMove> moves = new ArrayList<>();

        // Phase de placement
        if (!blackPlaced && playerRole == EscampeRole.NOIR) {
            moves.addAll(generatePlacements(4, 5)); // rows 4-5 = lignes 5-6
        } else if (blackPlaced && !whitePlaced && playerRole == EscampeRole.BLANC) {
            boolean blackOnTop = isBlackOnTop();
            if (blackOnTop) {
                moves.addAll(generatePlacements(0, 1)); // rows 0-1 = lignes 1-2
            } else {
                moves.addAll(generatePlacements(4, 5)); // rows 4-5 = lignes 5-6
            }
        } else {
            // Phase de jeu normale
            int requiredLisere = -1;
            if (lastMoveToCol >= 0 && lastMoveToRow >= 0) {
                requiredLisere = LISERE_MAP[lastMoveToRow][lastMoveToCol];
            }

            // Trouver toutes les pièces du joueur
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    Piece piece = boardGrid[row][col];
                    if (piece.belongsTo(playerRole)) {
                        int pieceLisere = LISERE_MAP[row][col];
                        if (requiredLisere == -1 || pieceLisere == requiredLisere) {
                            moves.addAll(generateMovesForPiece(row, col, pieceLisere));
                        }
                    }
                }
            }

            // Si aucun coup possible, on peut passer son tour
            if (moves.isEmpty()) {
                moves.add(new EscampeMove()); // PASS
            }
        }

        String[] result = new String[moves.size()];
        for (int i = 0; i < moves.size(); i++) {
            result[i] = moves.get(i).toString();
        }
        return result;
    }

    @Override
    public void play(String move, String player) {
        EscampeMove m = EscampeMove.parse(move);
        EscampeRole role = EscampeRole.fromString(player);

        // Appliquer le coup directement sur ce plateau
        applyMove(m, role);
    }

    /**
     * Applique un coup sur le plateau (modifie l'état interne)
     */
    private void applyMove(EscampeMove move, EscampeRole role) {
        if (move.isPass()) {
            // Réinitialiser le dernier coup pour permettre un coup libre
            lastMoveToCol = -1;
            lastMoveToRow = -1;
            return;
        }

        if (move.isPlacement()) {
            int[] cols = move.getPlacementCols();
            int[] rows = move.getPlacementRows();

            Piece licorne = (role == EscampeRole.NOIR) ? Piece.LICORNE_NOIRE : Piece.LICORNE_BLANCHE;
            Piece paladin = (role == EscampeRole.NOIR) ? Piece.PALADIN_NOIR : Piece.PALADIN_BLANC;

            // Placer la licorne (position 0)
            boardGrid[rows[0]][cols[0]] = licorne;

            // Placer les paladins (positions 1-5)
            for (int i = 1; i < 6; i++) {
                boardGrid[rows[i]][cols[i]] = paladin;
            }

            if (role == EscampeRole.NOIR) {
                blackPlaced = true;
            } else {
                whitePlaced = true;
            }

            // Pas de dernier coup pour le placement
            lastMoveToCol = -1;
            lastMoveToRow = -1;

        } else if (move.isMove()) {
            int fromCol = move.getFromCol();
            int fromRow = move.getFromRow();
            int toCol = move.getToCol();
            int toRow = move.getToRow();

            // Déplacer la pièce
            Piece piece = boardGrid[fromRow][fromCol];
            boardGrid[fromRow][fromCol] = Piece.EMPTY;
            boardGrid[toRow][toCol] = piece;

            // Mémoriser le dernier coup
            lastMoveToCol = toCol;
            lastMoveToRow = toRow;
        }
    }

    /**
     * Vérifie si la partie est terminée (une licorne a été capturée)
     */
    @Override
    public boolean gameOver() {
        // La partie est terminée si une licorne a été capturée
        boolean whiteLicorneFound = false;
        boolean blackLicorneFound = false;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (boardGrid[row][col] == Piece.LICORNE_BLANCHE) {
                    whiteLicorneFound = true;
                }
                if (boardGrid[row][col] == Piece.LICORNE_NOIRE) {
                    blackLicorneFound = true;
                }
            }
        }

        // La partie est terminée si une des licornes est absente
        // (et que les deux joueurs ont placé leurs pièces)
        if (blackPlaced && whitePlaced) {
            return !whiteLicorneFound || !blackLicorneFound;
        }

        return false;
    }

    // ---------------------- Méthodes internes ---------------------

    /**
     * Vérifie si les noirs sont placés sur le haut du plateau
     */
    private boolean isBlackOnTop() {
        for (int row = 4; row < 6; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (boardGrid[row][col].isBlack()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Génère tous les placements possibles sur deux lignes données
     */
    private ArrayList<EscampeMove> generatePlacements(int row1, int row2) {
        ArrayList<EscampeMove> placements = new ArrayList<>();

        // Récupérer toutes les cases disponibles sur les deux lignes
        ArrayList<int[]> availableCells = new ArrayList<>();
        for (int row = row1; row <= row2; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (boardGrid[row][col] == Piece.EMPTY) {
                    availableCells.add(new int[] { col, row });
                }
            }
        }

        // Générer toutes les permutations de 6 cases parmi les disponibles
        // Pour simplifier, on génère quelques placements types
        if (availableCells.size() >= 6) {
            // Génération de placements : on prend les 6 premières cases disponibles
            // et on génère des permutations
            generatePlacementPermutations(placements, availableCells, new int[6], new int[6], 0,
                    new boolean[availableCells.size()]);
        }

        // Limiter le nombre de placements pour éviter l'explosion combinatoire
        if (placements.size() > 100) {
            return new ArrayList<>(placements.subList(0, 100));
        }

        return placements;
    }

    /**
     * Génère récursivement les permutations de placement
     */
    private void generatePlacementPermutations(ArrayList<EscampeMove> result, ArrayList<int[]> cells,
            int[] cols, int[] rows, int depth, boolean[] used) {
        if (depth == 6) {
            result.add(new EscampeMove(cols.clone(), rows.clone()));
            return;
        }

        // Limiter pour éviter explosion combinatoire
        if (result.size() >= 720) { // 6! = 720
            return;
        }

        for (int i = 0; i < cells.size(); i++) {
            if (!used[i]) {
                used[i] = true;
                cols[depth] = cells.get(i)[0];
                rows[depth] = cells.get(i)[1];
                generatePlacementPermutations(result, cells, cols, rows, depth + 1, used);
                used[i] = false;
            }
        }
    }

    /**
     * Génère tous les déplacements possibles pour une pièce
     * 
     * @param row      ligne de la pièce
     * @param col      colonne de la pièce
     * @param distance nombre de cases à parcourir (liseré)
     */
    private ArrayList<EscampeMove> generateMovesForPiece(int row, int col, int distance) {
        ArrayList<EscampeMove> moves = new ArrayList<>();

        // Explorer tous les chemins possibles de la distance exacte
        Set<String> visited = new HashSet<>();
        visited.add(row + "," + col);

        explorePaths(row, col, col, row, distance, visited, moves);

        return moves;
    }

    /**
     * Explore récursivement les chemins possibles pour un déplacement
     */
    private void explorePaths(int startRow, int startCol, int currentCol, int currentRow,
            int remaining, Set<String> visited, ArrayList<EscampeMove> moves) {
        if (remaining == 0) {
            // Arrivée : ajouter le mouvement si la case d'arrivée est vide ou contient une
            // licorne ennemie
            Piece target = boardGrid[currentRow][currentCol];
            Piece mover = boardGrid[startRow][startCol];

            // On peut aller sur une case vide
            if (target == Piece.EMPTY) {
                moves.add(new EscampeMove(startCol, startRow, currentCol, currentRow));
            }
            // Un paladin peut capturer une licorne adverse
            else if (mover.isPaladin() && target.isLicorne() &&
                    ((mover.isBlack() && target.isWhite()) || (mover.isWhite() && target.isBlack()))) {
                moves.add(new EscampeMove(startCol, startRow, currentCol, currentRow));
            }
            return;
        }

        // Directions : haut, bas, gauche, droite (pas de diagonale)
        int[][] directions = { { 0, 1 }, { 0, -1 }, { -1, 0 }, { 1, 0 } };

        for (int[] dir : directions) {
            int newCol = currentCol + dir[0];
            int newRow = currentRow + dir[1];
            String key = newRow + "," + newCol;

            // Vérifier les limites
            if (newCol < 0 || newCol >= GRID_SIZE || newRow < 0 || newRow >= GRID_SIZE) {
                continue;
            }

            // Vérifier qu'on n'est pas déjà passé par cette case
            if (visited.contains(key)) {
                continue;
            }

            // Vérifier que la case intermédiaire est vide (sauf la dernière)
            if (remaining > 1 && boardGrid[newRow][newCol] != Piece.EMPTY) {
                continue;
            }

            // Si c'est la dernière case, on peut y aller seulement si elle est vide ou
            // capturable
            if (remaining == 1) {
                Piece target = boardGrid[newRow][newCol];
                Piece mover = boardGrid[startRow][startCol];

                if (target != Piece.EMPTY) {
                    // Seul un paladin peut capturer une licorne ennemie
                    if (!(mover.isPaladin() && target.isLicorne() &&
                            ((mover.isBlack() && target.isWhite()) || (mover.isWhite() && target.isBlack())))) {
                        continue;
                    }
                }
            }

            visited.add(key);
            explorePaths(startRow, startCol, newCol, newRow, remaining - 1, visited, moves);
            visited.remove(key);
        }
    }

    /**
     * Retourne le gagnant de la partie
     * 
     * @return "noir" si noir a gagné, "blanc" si blanc a gagné, null si partie non
     *         terminée
     */
    public String getWinner() {
        boolean whiteLicorneFound = false;
        boolean blackLicorneFound = false;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (boardGrid[row][col] == Piece.LICORNE_BLANCHE) {
                    whiteLicorneFound = true;
                }
                if (boardGrid[row][col] == Piece.LICORNE_NOIRE) {
                    blackLicorneFound = true;
                }
            }
        }

        if (!blackLicorneFound) {
            return "blanc"; // Blanc a capturé la licorne noire
        } else if (!whiteLicorneFound) {
            return "noir"; // Noir a capturé la licorne blanche
        }
        return null; // Partie non terminée
    }

    // ---------------------- Autres méthodes ---------------------

    /**
     * Retourne le liseré de la case de destination du dernier coup
     * 
     * @return le liseré (1, 2, ou 3), ou -1 si pas de dernier coup
     */
    public int getLastMoveLisere() {
        if (lastMoveToCol >= 0 && lastMoveToRow >= 0) {
            return LISERE_MAP[lastMoveToRow][lastMoveToCol];
        }
        return -1;
    }

    /**
     * Définit le dernier coup joué (utile pour initialiser le plateau)
     */
    public void setLastMove(int col, int row) {
        this.lastMoveToCol = col;
        this.lastMoveToRow = row;
    }

    /**
     * Retourne la pièce sur une case
     */
    public Piece getPiece(int row, int col) {
        return boardGrid[row][col];
    }

    /**
     * Affichage du plateau
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  A B C D E F\n");

        // Afficher de la ligne 6 à la ligne 1 (de haut en bas)
        for (int row = GRID_SIZE - 1; row >= 0; row--) {
            sb.append((row + 1)).append(" ");
            for (int col = 0; col < GRID_SIZE; col++) {
                sb.append(boardGrid[row][col].getSymbol()).append(" ");
            }
            sb.append((row + 1)).append("\n");
        }

        sb.append("  A B C D E F\n");

        if (lastMoveToCol >= 0 && lastMoveToRow >= 0) {
            sb.append("Dernier coup vers: ").append((char) ('A' + lastMoveToCol))
                    .append(lastMoveToRow + 1)
                    .append(" (liseré ").append(LISERE_MAP[lastMoveToRow][lastMoveToCol]).append(")\n");
        }

        return sb.toString();
    }

    /**
     * Affiche la carte des liserés
     */
    public static String printLisereMap() {
        StringBuilder sb = new StringBuilder();
        sb.append("Carte des liserés :\n");
        sb.append("  A B C D E F\n");

        for (int row = GRID_SIZE - 1; row >= 0; row--) {
            sb.append((row + 1)).append(" ");
            for (int col = 0; col < GRID_SIZE; col++) {
                sb.append(LISERE_MAP[row][col]).append(" ");
            }
            sb.append((row + 1)).append("\n");
        }

        sb.append("  A B C D E F\n");
        return sb.toString();
    }

    // ---------------------- Main pour les tests ---------------------

    public static void main(String[] args) {
        System.out.println("=== Test du jeu Escampe ===\n");

        // Afficher la carte des liserés
        System.out.println(printLisereMap());

        // Test 1 : Création d'un plateau vide
        System.out.println("--- Test 1 : Création d'un plateau vide ---");
        EscampeBoard board = new EscampeBoard();
        System.out.println(board);

        // Test 2 : Placement des pièces noires
        System.out.println("--- Test 2 : Placement des pièces noires ---");
        String[] noirMoves = board.possiblesMoves("noir");
        System.out.println("Nombre de placements possibles pour noir : " + noirMoves.length);
        if (noirMoves.length > 0) {
            System.out.println("Premier placement possible : " + noirMoves[0]);
            // Placer noir en haut : Licorne en C6, paladins en A6, B5, D5, E6, F5
            board.play("C6/A6/B5/D5/E6/F5", "noir");
            System.out.println("Après placement noir :");
            System.out.println(board);
        }

        // Test 3 : Placement des pièces blanches
        System.out.println("--- Test 3 : Placement des pièces blanches ---");
        String[] blancMoves = board.possiblesMoves("blanc");
        System.out.println("Nombre de placements possibles pour blanc : " + blancMoves.length);
        if (blancMoves.length > 0) {
            // Placer blanc en bas : Licorne en C1, paladins en A1, B2, D2, E1, F2
            board.play("C1/A1/B2/D2/E1/F2", "blanc");
            System.out.println("Après placement blanc :");
            System.out.println(board);
        }

        // Test 4 : Vérification de fin de partie
        System.out.println("--- Test 4 : Fin de partie ---");
        System.out.println("Partie terminée ? " + board.gameOver());

        // Test 5 : Coups possibles pour blanc (premier à jouer)
        System.out.println("--- Test 5 : Coups possibles pour blanc ---");
        String[] whiteMoves = board.possiblesMoves("blanc");
        System.out.println("Nombre de coups possibles pour blanc : " + whiteMoves.length);
        for (int i = 0; i < Math.min(10, whiteMoves.length); i++) {
            System.out.println("  " + whiteMoves[i]);
        }
        if (whiteMoves.length > 10) {
            System.out.println("  ... et " + (whiteMoves.length - 10) + " autres coups");
        }

        // Test 6 : Jouer un coup et vérifier la contrainte de liseré
        System.out.println("\n--- Test 6 : Jouer un coup ---");
        if (whiteMoves.length > 0) {
            String moveToPlay = "A1-A3"; // Coup qui va sur une case liseré 2
            if (board.isValidMove(moveToPlay, "blanc")) {
                board.play(moveToPlay, "blanc");
                System.out.println("Coup joué : " + moveToPlay);
                System.out.println(board);
            } else {
                // Essayer un autre coup
                board.play(whiteMoves[0], "blanc");
                System.out.println("Coup joué : " + whiteMoves[0]);
                System.out.println(board);
            }
        }

        // Test 7 : Coups possibles pour noir après le coup de blanc
        System.out.println("--- Test 7 : Coups pour noir (contrainte de liseré) ---");
        String[] blackMoves = board.possiblesMoves("noir");
        System.out.println("Liseré requis : " + board.getLastMoveLisere());
        System.out.println("Nombre de coups possibles pour noir : " + blackMoves.length);
        for (int i = 0; i < Math.min(10, blackMoves.length); i++) {
            System.out.println("  " + blackMoves[i]);
        }

        // Test 8 : Sauvegarde et lecture d'un fichier
        System.out.println("\n--- Test 8 : Sauvegarde et lecture ---");
        String testFile = "test_sauvegarde.txt";
        board.saveToFile(testFile);
        System.out.println("Plateau sauvegardé dans " + testFile);

        EscampeBoard boardFromFile = new EscampeBoard();
        boardFromFile.setFromFile(testFile);
        System.out.println("Plateau lu depuis le fichier :");
        System.out.println(boardFromFile);

        // Test 9 : Simulation d'une fin de partie (capture de la licorne)
        System.out.println("--- Test 9 : Test de fin de partie ---");
        EscampeBoard endGameBoard = new EscampeBoard();
        // Placement des pièces pour simuler une situation de fin de partie
        // Noir place ses pièces
        endGameBoard.play("C6/A6/B6/D6/E6/F6", "noir");
        // Blanc place ses pièces avec la licorne en position vulnérable
        endGameBoard.play("C1/A1/B1/D1/E1/F1", "blanc");
        System.out.println("Configuration initiale :");
        System.out.println(endGameBoard);
        System.out.println("Partie terminée ? " + endGameBoard.gameOver());
        System.out.println("Gagnant : " + endGameBoard.getWinner());

        // Simuler quelques coups pour amener un paladin noir vers la licorne blanche
        // Blanc joue (premier coup libre)
        endGameBoard.play("A1-A2", "blanc"); // Paladin blanc va sur A2 (liseré 3)
        System.out.println("Après A1-A2 (blanc) :");
        System.out.println(endGameBoard);

        // Noir doit jouer depuis une case liseré 3
        // A6 est liseré 3, on peut aller vers C4 (3 cases)
        endGameBoard.play("A6-A3", "noir"); // Paladin noir va sur A3 (liseré 2)
        System.out.println("Après A6-A3 (noir) :");
        System.out.println(endGameBoard);

        // Blanc doit jouer depuis liseré 2
        endGameBoard.play("B1-B3", "blanc"); // B1 est liseré 2, va sur B3 (liseré 3)
        System.out.println("Après B1-B3 (blanc) :");
        System.out.println(endGameBoard);

        // Noir doit jouer depuis liseré 3, A3 est liseré 2 donc ne peut pas jouer
        // B6 est liseré 2, D6 est liseré 1, E6 est liseré 3, F6 est liseré 2
        // Le paladin en E6 (liseré 1) ne peut pas jouer non plus
        // Vérifions les coups possibles
        String[] noirCoups = endGameBoard.possiblesMoves("noir");
        System.out.println("Coups possibles pour noir (liseré requis: " + endGameBoard.getLastMoveLisere() + ") :");
        for (String coup : noirCoups) {
            System.out.println("  " + coup);
        }

        // Créer une configuration où noir peut capturer la licorne blanche
        System.out.println("\n--- Test 10 : Capture de la licorne ---");
        EscampeBoard captureBoard = new EscampeBoard();
        captureBoard.play("C6/A6/B6/D6/E6/F6", "noir");
        captureBoard.play("B2/A1/C1/D1/E1/F1", "blanc"); // Licorne blanche en B2
        System.out.println("Configuration avec licorne blanche en B2 :");
        System.out.println(captureBoard);

        // Blanc joue d'abord (coup libre)
        captureBoard.play("A1-A2", "blanc"); // A1 liseré 1, va sur A2 (liseré 3)
        System.out.println("Après A1-A2 (blanc) - liseré 3 :");
        System.out.println(captureBoard);

        // Noir doit jouer depuis liseré 3. A6 est liseré 3
        // A6 peut aller sur A3 (3 cases) - liseré 2
        captureBoard.play("A6-A3", "noir");
        System.out.println("Après A6-A3 (noir) - liseré 2 :");
        System.out.println(captureBoard);

        // Blanc doit jouer depuis liseré 2. B2 (licorne) est liseré 1, donc ne peut pas
        // jouer
        // Vérifier les coups possibles
        String[] blancCoups = captureBoard.possiblesMoves("blanc");
        System.out.println("Coups possibles pour blanc (liseré requis: 2) :");
        for (String coup : blancCoups) {
            System.out.println("  " + coup);
        }

        // Simuler la capture : on crée un plateau où la capture est possible
        System.out.println("\n--- Test 11 : Fin de partie effective ---");
        EscampeBoard finalBoard = new EscampeBoard();
        finalBoard.play("C2/A6/B6/D6/E6/F6", "noir"); // Licorne noire en C2
        finalBoard.play("C4/A1/B1/D1/E1/F1", "blanc"); // Licorne blanche en C4
        System.out.println("Configuration :");
        System.out.println(finalBoard);
        System.out.println("Partie terminée ? " + finalBoard.gameOver());

        // Blanc joue (coup libre) - on fait jouer le paladin A1 vers A3 (liseré 2)
        finalBoard.play("A1-A2", "blanc"); // liseré 3

        // Noir joue depuis liseré 3 - C2 est liseré 3!
        // La licorne noire peut capturer... non, seuls les paladins peuvent capturer
        // A6 est liseré 3, peut aller en A3 (3 cases)
        finalBoard.play("A6-A3", "noir"); // vers liseré 2

        // On simule directement une capture en modifiant le plateau
        // Créons un plateau où un paladin noir est adjacent à la licorne blanche
        System.out.println("\nSimulation de capture directe :");
        EscampeBoard captureDirecte = new EscampeBoard();
        captureDirecte.play("C6/C3/B6/D6/E6/F6", "noir"); // Paladin noir en C3 (liseré 1)
        captureDirecte.play("C1/A1/B1/D1/E1/F1", "blanc"); // Licorne blanche en C1 (liseré 2)
        System.out.println("Avant capture :");
        System.out.println(captureDirecte);
        System.out.println("Partie terminée ? " + captureDirecte.gameOver());

        // Blanc joue A1-A2 (liseré 3)
        captureDirecte.play("A1-A2", "blanc");

        // Noir doit jouer depuis liseré 3. B6 est liseré 2, D6 est liseré 1...
        // F6 est liseré 2, E6 est liseré 3!
        // E6 peut aller vers E3 (3 cases, liseré 1)
        captureDirecte.play("E6-E3", "noir"); // vers liseré 1

        // Blanc doit jouer depuis liseré 1. C1 (licorne) est liseré 2...
        // D1 est liseré 3, E1 est liseré 1!
        captureDirecte.play("E1-E2", "blanc"); // vers liseré 3

        // Noir doit jouer depuis liseré 3. C3 (paladin) est liseré 1...
        // Cherchons un paladin sur liseré 3
        String[] coupsNoir = captureDirecte.possiblesMoves("noir");
        System.out.println("Coups pour noir (liseré 3) : ");
        for (String c : coupsNoir)
            System.out.println("  " + c);

        // Forçons la fin de partie en supprimant la licorne
        System.out.println("\n--- Simulation finale : suppression de la licorne blanche ---");
        // On recrée un plateau et on simule la capture
        EscampeBoard finPartie = new EscampeBoard();
        finPartie.play("C6/A6/B6/D6/E6/F6", "noir");
        finPartie.play("A2/A1/B1/C1/D1/E1", "blanc"); // Licorne en A2
        finPartie.play("A6-A2", "noir"); // Paladin A6 capture licorne en A2 (si valide)

        // Vérifions si c'est valide
        System.out.println("Configuration après tentative de capture :");
        System.out.println(finPartie);
        System.out.println("Partie terminée ? " + finPartie.gameOver());
        System.out.println("Gagnant : " + finPartie.getWinner());

        System.out.println("\n=== Fin des tests ===");
    }
}