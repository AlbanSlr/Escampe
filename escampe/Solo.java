package escampe;

import java.util.Date;

import javax.swing.JFrame;

/**
 * Petite Classe toute simple qui vous montre comment on peut lancer une partie
 * sur deux IJoueurs...
 * Cela vous servira a debugger facilement votre projet en conditions presque
 * reelles de tournoi
 * 
 * Attention, l'arbitre n'est pas lancé dessus, mais comme il s'agit de deux
 * IJoueur à vous il n'est
 * pas nécessaire de vérifier la validité des coups (bien entendu)
 * 
 * Par contre, comme rien ne vérifie la fin de partie (pas d'arbitre), vos
 * IJoueur devront renvoyer
 * la chaine "xxxxx" pour dire que la partie est finie.
 * 
 * Cette classe n'affiche rien : elle se contente de donner la main
 * alternativement aux deux
 * joueurs.
 * 
 * 2008-2012
 */
public class Solo {
	private static IJoueur joueurBlanc;
	private static IJoueur joueurNoir;

	// Ne pas modifier ces constantes, elles seront utilisees par l'arbitre
	private final static int BLANC = -1;
	private final static int NOIR = 1;

	private static int nbCoups = 0;

	// On active l'applet graphique par défaut
	static boolean APPLETGRAPHIQUE = true;

	// On déclare l'applet pour la vue du jeu
	static private Applet vueDuJeu;
	static private JFrame f = null;

	// On maintient un plateau pour l'affichage graphique
	static private int[][] plateauAffichage = new int[6][6];

	// On définit les constantes pour l'affichage
	private final static int LICORNE_BLANCHE = -2;
	private final static int PALADIN_BLANC = -1;
	private final static int VIDE = 0;
	private final static int PALADIN_NOIR = 1;
	private final static int LICORNE_NOIRE = 2;

	/**
	 * Pour éviter de toujours envoyer des lignes de commandes, vous pouvez renvoyer
	 * automatiquement
	 * dans cette méthode votre joueur par défaut. Attention, il faut bien remplir
	 * le return new
	 * VOTREJOUEUR() pour que cela fonctionne la classe implantee renvoyee doit
	 * implanter
	 * l'interface IJoueur...
	 * 
	 * @param s
	 * @return Ijoueur un joueur demande
	 */
	private static IJoueur getDefaultPlayer(String s) {
		System.out.println(s + " : defaultPlayer");
		// On retourne notre joueur IA par défaut
		return new AlbanRemi();
	}

	/**
	 * Juste pour rendre le tout plus generique, et vous donner une idee de comment
	 * le tournoi sera
	 * lance automatiquement, voici une methode permettant de charger une certaine
	 * classe implantant
	 * un IJoueur
	 * 
	 * @param classeJoueur
	 * @param s
	 * @return la classe chargee dynamiquement
	 */
	private static IJoueur loadNamedPlayer(String classeJoueur, String s) {
		IJoueur joueur;
		System.out.print(s + " : Chargement de la classe joueur " + classeJoueur + "... ");
		try {
			Class<?> cjoueur = Class.forName(classeJoueur);
			joueur = (IJoueur) cjoueur.newInstance();
		} catch (Exception e) {
			System.out.println("Erreur de chargement");
			System.out.println(e);
			return null;
		}
		System.out.println("Ok");
		return joueur;
	}

	/**
	 * Boucle principale du jeu, en utilisant une version de l'arbitre identique a
	 * celle du tournoi
	 * L'arbitre sera le garant de la validite des coups, et de leur affichage
	 * standard pour la
	 * publication via le site web.
	 * 
	 * @param joueurBlanc
	 * @param joueurNoir
	 */
	public static void gameLoop(IJoueur joueurBlanc, IJoueur joueurNoir) {
		String coup;
		boolean partieFinie = false;
		IJoueur joueurCourant = joueurNoir; // Dans Escampe le joueur Noir commence

		// On maintient un plateau interne pour l'affichage
		EscampeBoard plateauInterne = new EscampeBoard();

		while (!partieFinie) {
			nbCoups++;

			System.out.println("\n*********\nOn demande à " + joueurCourant.binoName() + " de jouer...");
			long waitingTime1 = new Date().getTime();

			coup = joueurCourant.choixMouvement();

			long waitingTime2 = new Date().getTime();
			// On rajoute 1 pour eliminer les temps infinis
			long waitingTime = waitingTime2 - waitingTime1 + 1;
			System.out.println(
					"Le joueur " + joueurCourant.binoName() + " a joué le coup " + coup + " en " + waitingTime + "ms.");

			// On met à jour le plateau interne et l'affichage graphique
			if (!coup.equals("xxxxx")) {
				String couleurJoueur = (joueurCourant.getNumJoueur() == BLANC) ? "blanc" : "noir";
				plateauInterne.play(coup, couleurJoueur);

				// On met à jour l'affichage graphique
				if (APPLETGRAPHIQUE && vueDuJeu != null) {
					mettreAJourPlateauAffichage(plateauInterne);
					vueDuJeu.addBoard(coup, plateauAffichage);
				}
			}

			try {
				Thread.sleep(100); // On attend un peu pour voir l'affichage
			} catch (InterruptedException e) {
			}

			if (coup.compareTo("xxxxx") == 0)
				partieFinie = true;
			else if (nbCoups == 2) { // Dans Escampe le joueur Blanc rejoue après avoir posé ses pièces
				// On avertit le joueur Noir du placement des pièces
				joueurNoir.mouvementEnnemi(coup);
			} else {
				if (joueurCourant.getNumJoueur() == BLANC)
					joueurCourant = joueurNoir;
				else
					joueurCourant = joueurBlanc;

				// On avertit le second joueur du coup calcule par le precedent
				joueurCourant.mouvementEnnemi(coup);
				// Ce sera ensuite à lui de jouer de nouveau en haut de la boucle
			}

			// On vérifie si la partie est terminée
			if (plateauInterne.gameOver()) {
				String gagnant = plateauInterne.getWinner();
				System.out.println("\n*** PARTIE TERMINÉE ! Gagnant : " + gagnant + " ***\n");
				partieFinie = true;
			}
		}

		System.out.println("Partie finie en " + nbCoups + " coups.\n");
	}

	/**
	 * On convertit le plateau EscampeBoard en tableau d'entiers pour l'affichage
	 */
	private static void mettreAJourPlateauAffichage(EscampeBoard plateau) {
		for (int row = 0; row < 6; row++) {
			for (int col = 0; col < 6; col++) {
				EscampeBoard.Piece piece = plateau.getPiece(row, col);
				switch (piece) {
					case LICORNE_BLANCHE:
						plateauAffichage[row][col] = LICORNE_BLANCHE;
						break;
					case PALADIN_BLANC:
						plateauAffichage[row][col] = PALADIN_BLANC;
						break;
					case LICORNE_NOIRE:
						plateauAffichage[row][col] = LICORNE_NOIRE;
						break;
					case PALADIN_NOIR:
						plateauAffichage[row][col] = PALADIN_NOIR;
						break;
					default:
						plateauAffichage[row][col] = VIDE;
						break;
				}
			}
		}
	}

	/**
	 * On charge eventuellement les classes demandee pour les joueurs, et on lance
	 * la boucle
	 * principale
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		// On initialise le plateau d'affichage à vide
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				plateauAffichage[i][j] = VIDE;
			}
		}

		// On initialise l'applet graphique
		if (APPLETGRAPHIQUE) {
			f = new JFrame("Escampe - Vue du jeu");
			vueDuJeu = new Applet();
			vueDuJeu.buildUI(f.getContentPane());
			f.setSize(vueDuJeu.getDimension());
			vueDuJeu.setMyFrame(f);
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.setVisible(true);
			vueDuJeu.addBoard("Départ", plateauAffichage);
		}

		System.out.println("Partie solo ...");

		if (args.length == 0) { // On a deux classes à charger
			joueurBlanc = getDefaultPlayer("Blanc");
			joueurNoir = getDefaultPlayer("Noir");
		} else if (args.length == 2) { // On a deux classes à charger
			joueurBlanc = getDefaultPlayer("Blanc");
			joueurNoir = getDefaultPlayer("Noir");
		} else if (args.length == 3) {
			joueurBlanc = loadNamedPlayer(args[0], "Blanc");
			joueurNoir = loadNamedPlayer(args[0], "Noir");
		} else if (args.length == 4) {
			joueurBlanc = loadNamedPlayer(args[0], "Blanc");
			joueurNoir = loadNamedPlayer(args[1], "Noir");
		}

		joueurBlanc.initJoueur(BLANC);
		System.out.println("Joueur Blanc : " + joueurBlanc.binoName());

		joueurNoir.initJoueur(NOIR);
		System.out.println("Joueur Noir : " + joueurNoir.binoName());

		System.out.println("Initialisation des deux joueurs ok.");

		gameLoop(joueurBlanc, joueurNoir);
	}
}
