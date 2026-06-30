# Sudoku Android — fr.svpro.sudoku

Jeu de Sudoku complet pour Android, écrit en Java.

## Architecture

```
fr.svpro.sudoku/
├── model/
│   └── SudokuBoard.java       Logique de grille, génération, validation
├── view/
│   ├── SudokuBoardView.java   Vue personnalisée (Canvas) — grille 9×9
│   └── NumberPadView.java     Clavier numérique 1-9 + effacement
├── util/
│   └── GameTimer.java         Chronomètre avec callbacks
├── MainActivity.java          Écran d'accueil / sélection difficulté
└── GameActivity.java          Écran de jeu principal
```

## Fonctionnalités

| Fonctionnalité | Détail |
|---|---|
| Génération | Backtracking + shuffle aléatoire, solution unique garantie |
| Difficultés | Facile (35 cases vides), Moyen (45), Difficile (55) |
| Sélection | Mise en surbrillance : case, ligne/col/boîte, même chiffre |
| Notes | Mode crayon (petits chiffres bitmask par case) |
| Annulation | Historique de 200 coups |
| Indices | Révèle la bonne valeur (case sélectionnée) |
| Vérification | Surligne toutes les erreurs en rouge |
| Chrono | Pause automatique quand l'app passe en arrière-plan |
| Victoire/Défaite | Dialogue avec stats (temps, erreurs, indices) |

## Configuration minimale

- minSdk : 24 (Android 7.0)
- targetSdk : 34 (Android 14)
- Java 8

## Paramètres personnalisables

Dans `SudokuBoard.Difficulty` : ajustez `cellsToRemove` pour chaque niveau.
Dans `GameActivity` : `MAX_MISTAKES` (défaut 3) limite les erreurs tolérées.
