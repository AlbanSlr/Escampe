package escampe;

/**
 * Enumération représentant les rôles (couleurs) des joueurs dans le jeu Escampe
 * NOIR : Le joueur noir, qui choisit son bord et place ses pièces en premier
 * BLANC : Le joueur blanc, qui joue en premier après le placement
 */
public enum EscampeRole {
    NOIR,
    BLANC;

    /**
     * Retourne le rôle adverse
     * 
     * @return le rôle opposé
     */
    public EscampeRole opponent() {
        return this == NOIR ? BLANC : NOIR;
    }

    /**
     * Convertit une chaîne de caractères en EscampeRole
     * 
     * @param player la chaîne représentant le joueur ("noir" ou "blanc")
     * @return le rôle correspondant
     */
    public static EscampeRole fromString(String player) {
        if (player.equalsIgnoreCase("noir")) {
            return NOIR;
        } else if (player.equalsIgnoreCase("blanc")) {
            return BLANC;
        }
        throw new IllegalArgumentException("Joueur inconnu : " + player + ". Utiliser 'noir' ou 'blanc'");
    }

    @Override
    public String toString() {
        return this == NOIR ? "noir" : "blanc";
    }
}
