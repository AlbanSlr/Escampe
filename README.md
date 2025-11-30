# Escampe - TP Intelligence Artificielle

Projet de jeu Escampe pour le cours d'IA - Polytech Paris-Saclay 2025-2026

**Auteurs :** Alban Sellier & Remi Geraud

## Description

Escampe est un jeu de plateau à deux joueurs (Noir et Blanc) sur un plateau 6x6. Chaque joueur dispose de 6 pièces : une licorne et 5 paladins. Le but est de capturer la licorne adverse avec un paladin.

## Prérequis

- Java
- Gradle

## Compilation

Pour compiler le projet :

```bash
gradle build
```

Ou pour nettoyer et recompiler :

```bash
gradle clean build
```

## Exécution

Pour exécuter les tests de démonstration :

```bash
gradle run
```

## Structure du projet

```
src/main/java/escampe/
├── EscampeBoard.java   # Classe principale représentant le plateau
├── EscampeMove.java    # Classe représentant un coup
├── EscampeRole.java    # Énumération des rôles (NOIR/BLANC)
└── Partie1.java        # Interface des opérations de base
```

## Utilisation de l'API

### Créer un plateau vide
```java
EscampeBoard board = new EscampeBoard();
```

### Charger un plateau depuis un fichier
```java
board.setFromFile("plateau.txt");
```

### Sauvegarder un plateau dans un fichier
```java
board.saveToFile("plateau.txt");
```

### Placer les pièces en début de partie
```java
// Noir place ses pièces (Licorne puis 5 paladins)
board.play("C6/A6/B5/D5/E6/F5", "noir");

// Blanc place ses pièces
board.play("C1/A1/B2/D2/E1/F2", "blanc");
```

### Jouer un coup
```java
board.play("A1-A3", "blanc");  // Déplacer la pièce de A1 vers A3
board.play("E", "noir");        // Passer son tour
```

### Obtenir les coups possibles
```java
String[] moves = board.possiblesMoves("blanc");
```

### Vérifier si un coup est valide
```java
boolean valid = board.isValidMove("A1-A3", "blanc");
```

### Vérifier la fin de partie
```java
boolean finished = board.gameOver();
```

## Format des fichiers de plateau

Les fichiers de plateau utilisent le format suivant :
- `N` : Licorne noire
- `n` : Paladin noir
- `B` : Licorne blanche
- `b` : Paladin blanc
- `-` : Case vide
- `%` : Commentaire (ligne ignorée)

Exemple :
```
% ABCDEF
01 nnN--- 01
02 --b--- 02
03 -----b 03
04 b-nn-- 04
05 --b--- 05
06 n-B--b 06
% ABCDEF
```

## Règles du jeu

1. **Placement initial** : Noir choisit un bord et place ses pièces sur les 2 premières lignes, puis Blanc place les siennes sur le bord opposé.

2. **Déplacement** : Chaque pièce doit partir d'une case ayant le même liseré (1, 2 ou 3) que la case d'arrivée du coup précédent de l'adversaire.

3. **Distance** : Le liseré détermine le nombre de cases à parcourir (1, 2 ou 3).

4. **Contraintes** : Pas de passage par une case occupée, pas de retour sur une case déjà traversée, pas de diagonale.

5. **Fin de partie** : Un paladin capture la licorne adverse.