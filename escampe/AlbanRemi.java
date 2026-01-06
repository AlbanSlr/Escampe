package escampe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * Classe implémentant un joueur IA pour le jeu Escampe
 * On utilise l'algorithme Minimax avec élagage Alpha-Beta et Iterative
 * Deepening
 * On prend jusqu'à 10 secondes par coup pour réfléchir
 * 
 * Optimisations :
 * - Iterative Deepening : on augmente la profondeur progressivement
 * - Move Ordering : on trie les coups pour élaguer plus vite
 * - Évaluation multicritères : mobilité, distance, protection, contrôle
 * 
 * @author Alban x Remi
 */
public class AlbanRemi implements IJoueur {

    // On stocke la couleur du joueur (-1 = BLANC, 1 = NOIR)
    private int maCouleur;

    // On maintient une représentation interne du plateau
    private EscampeBoard plateau;

    // On définit les valeurs pour l'évaluation
    private static final int VICTOIRE = 100000;
    private static final int DEFAITE = -100000;

    // On gère le temps : 10 secondes max par coup
    private static final long TEMPS_MAX_PAR_COUP = 12000;
    private long debutReflexion;
    private boolean timeout;

    // On stocke le meilleur score trouvé pour l'affichage
    private int dernierScore;
    private int profondeurAtteinte;

    // On compte le temps total utilisé
    private long tempsTotal = 0;

    // Carte des liserés pour le placement stratégique
    private static final int[][] LISERES = {
            { 1, 2, 2, 3, 1, 2 }, // ligne 1 (row 0)
            { 3, 1, 3, 1, 3, 2 }, // ligne 2 (row 1)
            { 2, 3, 1, 2, 1, 3 }, // ligne 3 (row 2)
            { 2, 1, 3, 2, 3, 1 }, // ligne 4 (row 3)
            { 1, 3, 1, 3, 1, 2 }, // ligne 5 (row 4)
            { 3, 2, 2, 1, 3, 2 } // ligne 6 (row 5)
    };

    /**
     * Constructeur par défaut
     */
    public AlbanRemi() {
        plateau = new EscampeBoard();
    }

    @Override
    public void initJoueur(int mycolour) {
        this.maCouleur = mycolour;
        this.plateau = new EscampeBoard();
        this.tempsTotal = 0;

        System.out.println("AlbanRemi initialisé en " + (mycolour == BLANC ? "BLANC" : "NOIR"));
    }

    @Override
    public int getNumJoueur() {
        return maCouleur;
    }

    @Override
    public String choixMouvement() {
        debutReflexion = System.currentTimeMillis();
        timeout = false;
        dernierScore = 0;
        profondeurAtteinte = 0;

        String maCouleurStr = (maCouleur == BLANC) ? "blanc" : "noir";

        try {
            String[] coupsPossibles = plateau.possiblesMoves(maCouleurStr);

            System.out.println("[DEBUG] choixMouvement appelé, " + coupsPossibles.length + " coups possibles");
            if (coupsPossibles.length > 0) {
                System.out.println("[DEBUG] Premier coup : " + coupsPossibles[0]);
            }

            if (coupsPossibles.length == 0) {
                System.out.println("[DEBUG] Aucun coup possible, retourne E");
                return "E";
            }

            // Phase de placement initial
            if (coupsPossibles[0].contains("/")) {
                String placement = getPlacementIntelligent(maCouleurStr);
                plateau.play(placement, maCouleurStr);
                long temps = System.currentTimeMillis() - debutReflexion;
                tempsTotal += temps;
                System.out.println("AlbanRemi placement : " + placement + " - Temps: " + temps + "ms");
                return placement;
            }

            // Détection de coup gagnant immédiat
            for (String coup : coupsPossibles) {
                EscampeBoard copie = new EscampeBoard(plateau);
                copie.play(coup, maCouleurStr);
                if (copie.gameOver() && maCouleurStr.equals(copie.getWinner())) {
                    plateau.play(coup, maCouleurStr);
                    long temps = System.currentTimeMillis() - debutReflexion;
                    tempsTotal += temps;
                    System.out.println("AlbanRemi COUP GAGNANT : " + coup);
                    return coup;
                }
            }

            if (coupsPossibles.length == 1) {
                String coup = coupsPossibles[0];
                plateau.play(coup, maCouleurStr);
                return coup;
            }

            // Iterative Deepening avec Move Ordering
            String meilleurCoup = coupsPossibles[0];
            String[] coupsTries = coupsPossibles.clone();

            for (int profondeur = 1; profondeur <= 50; profondeur++) {
                if (System.currentTimeMillis() - debutReflexion >= TEMPS_MAX_PAR_COUP) {
                    break;
                }

                String coupTrouve = alphaBetaRacine(profondeur, maCouleurStr, coupsTries);

                if (!timeout) {
                    meilleurCoup = coupTrouve;
                    profondeurAtteinte = profondeur;

                    // Move Ordering : on met le meilleur coup en premier pour la prochaine
                    // itération
                    coupsTries = reorderMoves(coupsTries, meilleurCoup);
                } else {
                    break;
                }
            }

            plateau.play(meilleurCoup, maCouleurStr);
            long temps = System.currentTimeMillis() - debutReflexion;
            tempsTotal += temps;
            System.out.println("AlbanRemi joue : " + meilleurCoup + " (score: " + dernierScore
                    + ", prof: " + profondeurAtteinte + ") - Temps: " + temps + "ms (Total: " + tempsTotal + "ms)");
            return meilleurCoup;

        } catch (Exception e) {
            System.err.println("[IA] ERREUR CRITIQUE dans choixMouvement :");
            e.printStackTrace();
            return "E";
        }
    }

    /**
     * Réordonne les coups en mettant le meilleur en premier
     */
    private String[] reorderMoves(String[] coups, String meilleurCoup) {
        if (meilleurCoup == null || coups == null || coups.length == 0) {
            return coups;
        }

        // Compter les coups valides
        int count = 0;
        for (String coup : coups) {
            if (coup != null && !coup.isEmpty() && !coup.equals("E")) {
                count++;
            }
        }

        String[] result = new String[count];
        int idx = 0;

        // Mettre le meilleur coup en premier s'il est valide
        if (!meilleurCoup.isEmpty() && !meilleurCoup.equals("E")) {
            result[idx++] = meilleurCoup;
        }

        // Ajouter les autres coups
        for (String coup : coups) {
            if (coup != null && !coup.isEmpty() && !coup.equals("E") && !coup.equals(meilleurCoup)) {
                result[idx++] = coup;
            }
        }

        return result;
    }

    // ==================== PLACEMENT INTELLIGENT ====================

    // Noir placé en haut (lignes 5-6)
    private static final String PLACEMENTS_NOIR_HAUT ="A6/A5/C5/D5/E5/F5";

    // Noir placé en bas
    private static final String PLACEMENTS_NOIR_BAS ="A1/A2/F1/D1/C2/E2";

    // Blanc placé en bas (lignes 1-2)
    private static final String PLACEMENTS_BLANC_BAS ="A1/A2/F1/D1/C2/E2";

    // Blanc placé en haut
    private static final String PLACEMENTS_BLANC_HAUT ="A6/A5/C5/D5/E5/F5";

    /**
     * Génère un placement intelligent en évaluant plusieurs configurations
     * 
     * Stratégie :
     * 1. La licorne sur une case liseré 1 (plus difficile à atteindre)
     * 2. La licorne au centre (plus de mobilité)
     * 3. Les paladins répartis sur différents liserés (flexibilité)
     * 4. Les paladins autour de la licorne (protection)
     */
    private String getPlacementIntelligent(String couleur) {
    boolean isNoir = couleur.equals("noir");

    // Déterminer si on joue en haut ou en bas
    boolean placerEnHaut = true;

    if (!isNoir) {
        // Blanc : on regarde où Noir a placé ses pièces
        boolean noirEnBas = false;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 6; col++) {
                if (plateau.getPiece(row, col).isBlack()) {
                    noirEnBas = true;
                    break;
                }
            }
        }
        placerEnHaut = noirEnBas;
    }

    String placementChoisi;

    if (isNoir) {
        placementChoisi = PLACEMENTS_NOIR_HAUT;
    } else {
        placementChoisi = placerEnHaut
                ? PLACEMENTS_BLANC_HAUT
                : PLACEMENTS_BLANC_BAS;
    }

    System.out.println("[IA] Placement initial choisi : " + placementChoisi);
    return placementChoisi;
    }

    // ==================== ALPHA-BETA ====================

    private String alphaBetaRacine(int profondeur, String maCouleurStr, String[] coups) {
        // Filtrer les coups vides ou invalides
        java.util.List<String> coupsValides = new java.util.ArrayList<>();
        for (String coup : coups) {
            if (coup != null && !coup.isEmpty() && !coup.equals("E")) {
                coupsValides.add(coup);
            }
        }

        if (coupsValides.isEmpty()) {
            return "E";
        }

        String[] coupsArray = coupsValides.toArray(new String[0]);
        String meilleurCoup = coupsArray[0];
        int meilleureValeur = DEFAITE - 1;

        // Move Ordering : évaluation rapide pour trier les coups
        Integer[] indices = new Integer[coupsArray.length];
        int[] scores = new int[coupsArray.length];

        for (int i = 0; i < coupsArray.length; i++) {
            indices[i] = i;
            try {
                EscampeBoard copie = new EscampeBoard(plateau);
                copie.play(coupsArray[i], maCouleurStr);
                scores[i] = evaluerRapide(copie, maCouleurStr);
            } catch (Exception e) {
                scores[i] = DEFAITE; // Coup invalide = très mauvais score
            }
        }

        // Trier les indices par score décroissant
        Arrays.sort(indices, (a, b) -> scores[b] - scores[a]);

        for (int idx : indices) {
            String coup = coupsArray[idx];

            if (System.currentTimeMillis() - debutReflexion >= TEMPS_MAX_PAR_COUP) {
                timeout = true;
                break;
            }

            try {
                EscampeBoard copie = new EscampeBoard(plateau);
                copie.play(coup, maCouleurStr);

                int valeur = alphaBeta(copie, profondeur - 1, DEFAITE, VICTOIRE, false, maCouleurStr);

                if (valeur > meilleureValeur) {
                    meilleureValeur = valeur;
                    meilleurCoup = coup;
                }
            } catch (Exception e) {
                // Coup invalide, on l'ignore
            }
        }

        if (!timeout) {
            dernierScore = meilleureValeur;
        }

        return meilleurCoup;
    }

    private int alphaBeta(EscampeBoard board, int profondeur, int alpha, int beta, boolean estMax,
            String maCouleurStr) {
        if (System.currentTimeMillis() - debutReflexion >= TEMPS_MAX_PAR_COUP) {
            timeout = true;
            return 0;
        }

        if (board.gameOver()) {
            String gagnant = board.getWinner();
            if (maCouleurStr.equals(gagnant)) {
                return VICTOIRE + profondeur;
            } else {
                return DEFAITE - profondeur;
            }
        }

        if (profondeur <= 0) {
            return evaluer(board, maCouleurStr);
        }

        String couleurAdverse = maCouleurStr.equals("blanc") ? "noir" : "blanc";
        String joueurCourant = estMax ? maCouleurStr : couleurAdverse;

        String[] coups = board.possiblesMoves(joueurCourant);

        if (coups.length == 0 || (coups.length == 1 && coups[0].equals("E"))) {
            EscampeBoard copie = new EscampeBoard(board);
            copie.play("E", joueurCourant);
            return alphaBeta(copie, profondeur - 1, alpha, beta, !estMax, maCouleurStr);
        }

        if (estMax) {
            int valeur = DEFAITE - 1;
            for (String coup : coups) {
                EscampeBoard copie = new EscampeBoard(board);
                copie.play(coup, joueurCourant);
                int eval = alphaBeta(copie, profondeur - 1, alpha, beta, false, maCouleurStr);
                valeur = Math.max(valeur, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha)
                    break;
            }
            return valeur;
        } else {
            int valeur = VICTOIRE + 1;
            for (String coup : coups) {
                EscampeBoard copie = new EscampeBoard(board);
                copie.play(coup, joueurCourant);
                int eval = alphaBeta(copie, profondeur - 1, alpha, beta, true, maCouleurStr);
                valeur = Math.min(valeur, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha)
                    break;
            }
            return valeur;
        }
    }

    // ==================== ÉVALUATION ====================

    /**
     * Évaluation rapide pour le Move Ordering (moins précise mais plus rapide)
     */
    private int evaluerRapide(EscampeBoard board, String maCouleurStr) {
        if (board.gameOver()) {
            return maCouleurStr.equals(board.getWinner()) ? VICTOIRE : DEFAITE;
        }

        String couleurAdverse = maCouleurStr.equals("blanc") ? "noir" : "blanc";

        // Juste la mobilité et une estimation de distance
        String[] mesCoups = board.possiblesMoves(maCouleurStr);
        String[] coupsAdverses = board.possiblesMoves(couleurAdverse);

        int score = (mesCoups.length - coupsAdverses.length) * 10;

        if (coupsAdverses.length == 0 || (coupsAdverses.length == 1 && coupsAdverses[0].equals("E"))) {
            score += 5000;
        }

        // CRITIQUE : Compter les pièces par liseré
        EscampeRole monRole = maCouleurStr.equals("blanc") ? EscampeRole.BLANC : EscampeRole.NOIR;
        int[] compteurLiseres = new int[4];
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                EscampeBoard.Piece piece = board.getPiece(row, col);
                if (piece.belongsTo(monRole)) {
                    compteurLiseres[LISERES[row][col]]++;
                }
            }
        }
        for (int i = 1; i <= 3; i++) {
            if (compteurLiseres[i] == 0) {
                score -= 1000; // ÉNORME malus si on perd un liseré
            } else if (compteurLiseres[i] == 1) {
                score -= 150; // Malus si seulement 1 pièce (fragile)
            }
        }

        return score;
    }

    /**
     * Évaluation complète multicritères
     */
    private int evaluer(EscampeBoard board, String maCouleurStr) {
        String couleurAdverse = maCouleurStr.equals("blanc") ? "noir" : "blanc";

        if (board.gameOver()) {
            return maCouleurStr.equals(board.getWinner()) ? VICTOIRE : DEFAITE;
        }

        int score = 0;

        // Trouver les positions des pièces
        int[] maLicorne = null;
        int[] licorneAdverse = null;
        ArrayList<int[]> mesPaladins = new ArrayList<>();
        ArrayList<int[]> paladinsAdverses = new ArrayList<>();

        EscampeRole monRole = maCouleurStr.equals("blanc") ? EscampeRole.BLANC : EscampeRole.NOIR;
        EscampeRole roleAdverse = monRole.opponent();

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                EscampeBoard.Piece piece = board.getPiece(row, col);
                if (piece.belongsTo(monRole)) {
                    if (piece.isLicorne()) {
                        maLicorne = new int[] { row, col };
                    } else if (piece.isPaladin()) {
                        mesPaladins.add(new int[] { row, col });
                    }
                } else if (piece.belongsTo(roleAdverse)) {
                    if (piece.isLicorne()) {
                        licorneAdverse = new int[] { row, col };
                    } else if (piece.isPaladin()) {
                        paladinsAdverses.add(new int[] { row, col });
                    }
                }
            }
        }

        // === CRITÈRE 1 : MOBILITÉ (poids: 5) ===
        String[] mesCoups = board.possiblesMoves(maCouleurStr);
        String[] coupsAdverses = board.possiblesMoves(couleurAdverse);

        // Bonus énorme si l'adversaire est bloqué
        if (coupsAdverses.length == 0 || (coupsAdverses.length == 1 && coupsAdverses[0].equals("E"))) {
            score += 2000;
        }
        // Bonus si on a plus de mobilité
        score += (mesCoups.length - coupsAdverses.length) * 5;

        // === CRITÈRE 2 : DISTANCE D'ATTAQUE (poids: 25) ===
        // Distance minimale de nos paladins à la licorne adverse
        if (licorneAdverse != null && !mesPaladins.isEmpty()) {
            int distanceMinAttaque = 100;
            int nbPaladinsProches = 0;

            for (int[] paladin : mesPaladins) {
                int dist = bfs(board, paladin[0], paladin[1], licorneAdverse[0], licorneAdverse[1]);
                if (dist < distanceMinAttaque) {
                    distanceMinAttaque = dist;
                }
                if (dist <= 3) {
                    nbPaladinsProches++;
                }
            }

            // Bonus si très proche (menace imminente)
            if (distanceMinAttaque == 1) {
                score += 3000; // On peut capturer au prochain tour !
            } else if (distanceMinAttaque == 2) {
                score += 800;
            }

            // Malus proportionnel à la distance
            score -= distanceMinAttaque * 25;

            // Bonus pour avoir plusieurs paladins proches (attaque coordonnée)
            score += nbPaladinsProches * 50;
        }

        // === CRITÈRE 3 : DÉFENSE DE MA LICORNE (poids: 30) ===
        if (maLicorne != null && !paladinsAdverses.isEmpty()) {
            int distanceMinDefense = 100;
            int nbMenaces = 0;

            for (int[] paladin : paladinsAdverses) {
                int dist = bfs(board, paladin[0], paladin[1], maLicorne[0], maLicorne[1]);
                if (dist < distanceMinDefense) {
                    distanceMinDefense = dist;
                }
                if (dist <= 2) {
                    nbMenaces++;
                }
            }

            // Malus si très proche (danger imminent)
            if (distanceMinDefense == 1) {
                score -= 1000; // DANGER ! On peut être capturé !
            } else if (distanceMinDefense == 2) {
                score -= 400;
            }

            // Bonus proportionnel à la distance de sécurité
            score += distanceMinDefense * 30;

            // Malus pour plusieurs menaces
            score -= nbMenaces * 100;
        }

        // === CRITÈRE 4 : PROTECTION DE LA LICORNE (poids: 15) ===
        if (maLicorne != null) {
            int paladinsAdjacents = 0;
            for (int[] paladin : mesPaladins) {
                int dist = Math.abs(paladin[0] - maLicorne[0]) + Math.abs(paladin[1] - maLicorne[1]);
                if (dist == 1) {
                    paladinsAdjacents++;
                }
            }
            score += paladinsAdjacents * 15;
        }

        // === CRITÈRE 5 : LISERÉ DE LA LICORNE (poids: 20) ===
        if (maLicorne != null) {
            int lisere = LISERES[maLicorne[0]][maLicorne[1]];
            if (lisere == 1)
                score += 40; // Difficile à atteindre
            else if (lisere == 3)
                score -= 20; // Facile à atteindre
        }

        // === CRITÈRE 6 : CONTRÔLE DES LISERÉS AVEC REDONDANCE (CRITIQUE !) ===
        // On doit avoir au moins une pièce sur chaque liseré, idéalement 2+
        int[] compteurLiseres = new int[4];
        if (maLicorne != null) {
            compteurLiseres[LISERES[maLicorne[0]][maLicorne[1]]]++;
        }
        for (int[] paladin : mesPaladins) {
            compteurLiseres[LISERES[paladin[0]][paladin[1]]]++;
        }

        for (int i = 1; i <= 3; i++) {
            if (compteurLiseres[i] == 0) {
                score -= 2000; // ÉNORME MALUS - on peut être bloqué !
            } else if (compteurLiseres[i] == 1) {
                score -= 200; // Malus - position fragile, une seule pièce sur ce liseré
            } else {
                score += 100; // Bonus pour redondance
            }
        }

        return score;
    }

    /**
     * BFS pour calculer la vraie distance entre deux cases
     */
    private int bfs(EscampeBoard board, int startRow, int startCol, int targetRow, int targetCol) {
        if (startRow == targetRow && startCol == targetCol) {
            return 0;
        }

        boolean[][] visited = new boolean[6][6];
        LinkedList<int[]> queue = new LinkedList<>();
        queue.add(new int[] { startRow, startCol, 0 });
        visited[startRow][startCol] = true;

        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int row = current[0];
            int col = current[1];
            int dist = current[2];

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < 6 && newCol >= 0 && newCol < 6 && !visited[newRow][newCol]) {
                    if (newRow == targetRow && newCol == targetCol) {
                        return dist + 1;
                    }
                    visited[newRow][newCol] = true;
                    queue.add(new int[] { newRow, newCol, dist + 1 });
                }
            }
        }

        return 100;
    }

    @Override
    public void mouvementEnnemi(String coup) {
        String couleurAdverse = (maCouleur == BLANC) ? "noir" : "blanc";
        try {
            plateau.play(coup, couleurAdverse);
            System.out.println("Coup adverse reçu : " + coup);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'application du coup adverse : " + coup);
            e.printStackTrace();
        }
    }

    @Override
    public void declareLeVainqueur(int colour) {
        if (colour == maCouleur) {
            System.out.println("=== VICTOIRE ! ===");
        } else if (colour == 0) {
            System.out.println("=== Match nul ===");
        } else {
            System.out.println("=== Défaite... ===");
        }
        System.out.println("Temps total utilisé : " + tempsTotal + "ms (" + (tempsTotal / 1000) + "s)");
    }

    @Override
    public String binoName() {
        return "AlbanRemi";
    }
}
