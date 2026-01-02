package escampe;

/**
 * Classe implémentant un joueur IA pour le jeu Escampe
 * On utilise l'algorithme Minimax avec élagage Alpha-Beta
 * 
 * @author Votre binôme
 */
public class MonJoueur implements IJoueur {

    // On stocke la couleur du joueur (-1 = BLANC, 1 = NOIR)
    private int maCouleur;

    // On maintient une représentation interne du plateau
    private EscampeBoard plateau;

    // On définit la profondeur maximale de recherche Alpha-Beta
    private static final int PROFONDEUR_MAX = 4;

    // On définit les valeurs pour l'évaluation
    private static final int VICTOIRE = 100000;
    private static final int DEFAITE = -100000;

    // On compte le nombre de coups joués pour adapter la stratégie
    private int nombreCoups = 0;

    // On indique si c'est la phase de placement
    private boolean phaseInitiale = true;

    /**
     * Constructeur par défaut
     */
    public MonJoueur() {
        plateau = new EscampeBoard();
    }

    @Override
    public void initJoueur(int mycolour) {
        // On initialise la couleur du joueur
        this.maCouleur = mycolour;
        // On réinitialise le plateau
        this.plateau = new EscampeBoard();
        this.nombreCoups = 0;
        this.phaseInitiale = true;

        System.out.println("MonJoueur initialisé en " + (mycolour == BLANC ? "BLANC" : "NOIR"));
    }

    @Override
    public int getNumJoueur() {
        return maCouleur;
    }

    @Override
    public String choixMouvement() {
        nombreCoups++;

        String couleurStr = (maCouleur == BLANC) ? "blanc" : "noir";

        // On récupère tous les coups possibles
        String[] coupsPossibles = plateau.possiblesMoves(couleurStr);

        if (coupsPossibles.length == 0) {
            // On ne devrait jamais arriver ici, mais par sécurité
            return "PASSE";
        }

        if (coupsPossibles.length == 1) {
            // On n'a qu'un seul coup possible, on le joue directement
            String coup = coupsPossibles[0];
            plateau.play(coup, couleurStr);

            // On vérifie si c'est un placement initial
            if (coup.contains("/")) {
                phaseInitiale = false;
            }

            return coup;
        }

        // On utilise Alpha-Beta pour choisir le meilleur coup
        String meilleurCoup = null;
        int meilleureValeur = DEFAITE - 1;

        // On adapte la profondeur selon la phase de jeu
        int profondeur = PROFONDEUR_MAX;
        if (phaseInitiale) {
            // On réduit la profondeur pour le placement (beaucoup de combinaisons)
            profondeur = 2;
        }

        for (String coup : coupsPossibles) {
            // On crée une copie du plateau pour simuler le coup
            EscampeBoard plateauSimule = new EscampeBoard(plateau);
            plateauSimule.play(coup, couleurStr);

            // On évalue ce coup avec Alpha-Beta
            int valeur = alphaBeta(plateauSimule, profondeur - 1, DEFAITE, VICTOIRE, false);

            if (valeur > meilleureValeur) {
                meilleureValeur = valeur;
                meilleurCoup = coup;
            }
        }

        // On joue le meilleur coup trouvé
        if (meilleurCoup != null) {
            plateau.play(meilleurCoup, couleurStr);

            // On vérifie si on sort de la phase initiale
            if (meilleurCoup.contains("/")) {
                phaseInitiale = false;
            }

            System.out.println("MonJoueur joue : " + meilleurCoup + " (valeur: " + meilleureValeur + ")");
            return meilleurCoup;
        }

        // On joue le premier coup par défaut (ne devrait pas arriver)
        plateau.play(coupsPossibles[0], couleurStr);
        return coupsPossibles[0];
    }

    /**
     * Algorithme Alpha-Beta (Minimax avec élagage)
     * 
     * @param board      le plateau à évaluer
     * @param profondeur la profondeur restante
     * @param alpha      la meilleure valeur pour le joueur MAX
     * @param beta       la meilleure valeur pour le joueur MIN
     * @param estMax     true si c'est au tour du joueur MAX (nous)
     * @return la valeur de l'évaluation
     */
    private int alphaBeta(EscampeBoard board, int profondeur, int alpha, int beta, boolean estMax) {
        // On vérifie si la partie est terminée
        if (board.gameOver()) {
            String gagnant = board.getWinner();
            String maCouleurStr = (maCouleur == BLANC) ? "blanc" : "noir";
            if (gagnant != null && gagnant.equals(maCouleurStr)) {
                // On a gagné, on retourne une valeur très positive
                return VICTOIRE + profondeur;
            } else {
                // On a perdu, on retourne une valeur très négative
                return DEFAITE - profondeur;
            }
        }

        // On vérifie si on a atteint la profondeur maximale
        if (profondeur <= 0) {
            return evaluer(board);
        }

        // On détermine quel joueur joue
        String joueurCourant;
        if (estMax) {
            joueurCourant = (maCouleur == BLANC) ? "blanc" : "noir";
        } else {
            joueurCourant = (maCouleur == BLANC) ? "noir" : "blanc";
        }

        String[] coups = board.possiblesMoves(joueurCourant);

        if (coups.length == 0) {
            // On ne devrait pas arriver ici
            return evaluer(board);
        }

        if (estMax) {
            // On cherche à maximiser la valeur
            int valeur = DEFAITE - 1;

            for (String coup : coups) {
                EscampeBoard copie = new EscampeBoard(board);
                copie.play(coup, joueurCourant);

                int eval = alphaBeta(copie, profondeur - 1, alpha, beta, false);
                valeur = Math.max(valeur, eval);
                alpha = Math.max(alpha, eval);

                // On élague si beta <= alpha (coupe Beta)
                if (beta <= alpha) {
                    break;
                }
            }
            return valeur;
        } else {
            // On cherche à minimiser la valeur (adversaire)
            int valeur = VICTOIRE + 1;

            for (String coup : coups) {
                EscampeBoard copie = new EscampeBoard(board);
                copie.play(coup, joueurCourant);

                int eval = alphaBeta(copie, profondeur - 1, alpha, beta, true);
                valeur = Math.min(valeur, eval);
                beta = Math.min(beta, eval);

                // On élague si beta <= alpha (coupe Alpha)
                if (beta <= alpha) {
                    break;
                }
            }
            return valeur;
        }
    }

    /**
     * Fonction d'évaluation heuristique du plateau
     * On évalue la position selon plusieurs critères
     * 
     * @param board le plateau à évaluer
     * @return un score (positif = favorable, négatif = défavorable)
     */
    private int evaluer(EscampeBoard board) {
        int score = 0;

        // On définit les rôles
        EscampeRole monRole = (maCouleur == BLANC) ? EscampeRole.BLANC : EscampeRole.NOIR;
        EscampeRole adversaire = monRole.opponent();

        // On cherche les positions des licornes et des paladins
        int[] maLicorne = null;
        int[] licorneAdverse = null;
        java.util.List<int[]> mesPaladins = new java.util.ArrayList<>();
        java.util.List<int[]> paladinsAdverses = new java.util.ArrayList<>();

        for (int row = 0; row < EscampeBoard.GRID_SIZE; row++) {
            for (int col = 0; col < EscampeBoard.GRID_SIZE; col++) {
                EscampeBoard.Piece piece = board.getPiece(row, col);

                if (piece.belongsTo(monRole)) {
                    if (piece.isLicorne()) {
                        maLicorne = new int[] { row, col };
                    } else if (piece.isPaladin()) {
                        mesPaladins.add(new int[] { row, col });
                    }
                } else if (piece.belongsTo(adversaire)) {
                    if (piece.isLicorne()) {
                        licorneAdverse = new int[] { row, col };
                    } else if (piece.isPaladin()) {
                        paladinsAdverses.add(new int[] { row, col });
                    }
                }
            }
        }

        // Critère 1 : On compte le nombre de paladins (avantage matériel)
        // On n'a pas de prise de paladins dans Escampe, mais c'est une sécurité
        score += (mesPaladins.size() - paladinsAdverses.size()) * 10;

        // Critère 2 : On évalue la distance de nos paladins à la licorne adverse
        // On veut être proche de la licorne ennemie pour la menacer
        if (licorneAdverse != null) {
            int distanceMinimale = Integer.MAX_VALUE;
            for (int[] paladin : mesPaladins) {
                int distance = Math.abs(paladin[0] - licorneAdverse[0]) +
                        Math.abs(paladin[1] - licorneAdverse[1]);
                distanceMinimale = Math.min(distanceMinimale, distance);
            }
            // On préfère être proche (donc on soustrait la distance)
            if (distanceMinimale != Integer.MAX_VALUE) {
                score -= distanceMinimale * 5;
            }
        }

        // Critère 3 : On évalue la distance des paladins adverses à notre licorne
        // On veut qu'ils soient loin de notre licorne
        if (maLicorne != null) {
            int distanceMinimale = Integer.MAX_VALUE;
            for (int[] paladin : paladinsAdverses) {
                int distance = Math.abs(paladin[0] - maLicorne[0]) +
                        Math.abs(paladin[1] - maLicorne[1]);
                distanceMinimale = Math.min(distanceMinimale, distance);
            }
            // On préfère qu'ils soient loin (donc on ajoute la distance)
            if (distanceMinimale != Integer.MAX_VALUE) {
                score += distanceMinimale * 5;
            }
        }

        // Critère 4 : On évalue la mobilité (nombre de coups possibles)
        String maCouleurStr = (maCouleur == BLANC) ? "blanc" : "noir";
        String couleurAdverse = (maCouleur == BLANC) ? "noir" : "blanc";

        int mesMouvements = board.possiblesMoves(maCouleurStr).length;
        int mouvementsAdverse = board.possiblesMoves(couleurAdverse).length;

        // On préfère avoir plus de mobilité
        score += (mesMouvements - mouvementsAdverse) * 2;

        // Critère 5 : On évalue la position centrale de notre licorne
        // On préfère que notre licorne soit au centre (plus de mobilité)
        if (maLicorne != null) {
            int distanceCentre = Math.abs(maLicorne[0] - 2) + Math.abs(maLicorne[1] - 2);
            score -= distanceCentre;
        }

        // Critère 6 : On pénalise si notre licorne est sur un liseré facilement
        // atteignable
        // (Plus le liseré est commun, plus il y a de cases qui peuvent nous atteindre)

        return score;
    }

    @Override
    public void mouvementEnnemi(String coup) {
        // On applique le coup de l'adversaire sur notre plateau
        String couleurAdverse = (maCouleur == BLANC) ? "noir" : "blanc";

        try {
            plateau.play(coup, couleurAdverse);

            // On vérifie si c'est un placement initial
            if (coup.contains("/")) {
                // On note que la phase initiale continue (l'adversaire vient de placer)
            }

            System.out.println("Coup adverse reçu : " + coup);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'application du coup adverse : " + coup);
            e.printStackTrace();
        }
    }

    @Override
    public void declareLeVainqueur(int colour) {
        // On affiche le résultat de la partie
        if (colour == maCouleur) {
            System.out.println("=== VICTOIRE ! ===");
        } else if (colour == 0) {
            System.out.println("=== Match nul ===");
        } else {
            System.out.println("=== Défaite... ===");
        }
    }

    @Override
    public String binoName() {
        // On retourne le nom du binôme (à personnaliser)
        return "Alban x Remi";
    }
}
