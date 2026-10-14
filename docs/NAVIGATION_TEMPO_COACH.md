# Tempo Coach — Navigation et parcours (cadrage)

## Décisions validées le 14 octobre 2026
- Navigation principale à **4 onglets** : **Aujourd'hui**, **Pratiquer**, **Suivi**, **Moi**.
- **Coach intégré à Aujourd'hui** (fiche d'explication possible), sans onglet principal.
- **Détente et mouvement regroupés dans Pratiquer**.
- Style santé/détente, thème sombre Prune & Rose poudré, fond uni, compact sur Pixel 7a.
- Conserver les données, exercices personnels, sauvegardes Drive, Santé Connect, sons, rappels et mises à jour.

## Aujourd'hui (tableau de bord)
1. Bandeau date + dernier état de synchronisation.
2. Grille 2x2 : dernière nuit terminée, récupération Tempo + confiance, pas aujourd'hui, minutes d'exercice aujourd'hui.
3. Un seul CTA : séance adaptée ou mobilité douce selon données et ressenti.
4. Check-in express énergie et douleurs ; conseil Coach sur une ou deux lignes avec lien « Pourquoi ? ».
5. Trois habitudes cochables ; mini-graphiques sommeil/pas/récupération 7j.
6. Tap KPI ouvre sa vraie fiche dans Suivi.
Pas de score Fitbit ou hydratation fictifs.

## Respiration guidée — état de développement 2.0.1
- Cinq exercices de respiration disponibles dans la catégorie dédiée et depuis l'écran actuel « Séance ».
- Guide visuel Inspirer/Expirer, chronométré, avec pause et confirmation d'arrêt ; sessions historisées.
- Les autres catégories de détente restent au programme de refonte de « Pratiquer ».
- Score « Forme & préparation » lié au moteur RecoveryEngine, aucun algorithme parallèle.

## Pratiquer
1. Proposition du jour en haut avec durée et bouton de démarrage.
2. Catégories proposées : respiration, relaxation, mobilité, étirements, renforcement, pause active.
3. Mes programmes, favoris, bibliothèque d'exercices, minuteur libre.
4. Séance plein écran ; sons, pause, 5s de préparation, confirmations quitter/passer/remplacer.
5. Fiches d'exercice pour consulter ; éditeur distinct avec brouillon, Enregistrer et Annuler.
Les catégories non développées restent clairement signalées et non cliquables.

## Suivi
1. Période 7/28 jours et date de synchronisation.
2. Sous-sections Santé (sommeil, FC repos, VFC), Activité (pas, minutes), Séances (durée, difficulté), Habitudes (après stockage d'historique).
3. Courbes et barres cliquables : date, valeur, unités, jours manquants visibles, pas d'interpolation.
4. Comparaisons honnêtes entre 7 jours récents et 21 jours précédents.

## Moi
1. Mes objectifs, durée, matériel et préférences.
2. Personnaliser Tempo : exercices, favoris, catégories, icônes, durées.
3. Santé Connect, état des permissions, sauvegardes Drive et confidentialité.
4. Rappels, sons/volumes, affichage, version, mise à jour.
Aucun compte en ligne imposé.

## Parcours prioritaires
| Besoin | Chemin |
|---|---|
| Nuit et récupération | Aujourd'hui > KPI > Suivi > détail |
| Séance suggérée | Aujourd'hui > Démarrer > séance plein écran > bilan |
| Exercices de détente | Pratiquer > catégorie > fiche > démarrer |
| Minute libre | Pratiquer > Minuteur |
| Graphiques sommeil | Suivi > Santé > Sommeil > 7/28 jours |
| Éditer exercice | Moi > Personnaliser > Exercices > Editer > Enregistrer |
| Pourquoi ce conseil ? | Aujourd'hui > Coach « Pourquoi ? » > détail > Retour |
| Restauration | Moi > Données > Sauvegardes > Confirmation > Restaurer |

## Comportements
- Barre du bas sur les 4 racines, masquée pendant une séance guidée.
- Bouton Retour cohérent, protection des brouillons et actions destructives.
- Pas de lien inerte, pas de métrique inventée, états sans données/hors ligne/permissions partielles.
- Conserver la compatibilité avec l'identifiant Android et les données 2.0.

## Livraisons proposées
2.1 thème et composants ; 2.2 navigation et accueil ; 2.3 graphiques ; 2.4 Pratiquer et Moi ; 2.5 historique d'habitudes si validé.
Chaque version est testée et signée séparément.

## Questions à discuter
- Respiration/relaxation guidées dès la première refonte, ou ajout progressif ?
- Habitudes personnalisables, avec historique 7/28j, dans quelle version ?
- Confirmons-nous le KPI Activité = minutes d'exercice, faute de mesure indépendante de « détente » ?
