# Tempo Coach 2.6.4 — Santé compacte et navigation métrique

## Objectif
Réduire l'encombrement de Santé, déplacer la synchronisation automatique dans Paramètres et permettre un accès immédiat aux métriques depuis Aujourd'hui et Suivi.

## Vérifications sur Pixel 7a
- [ ] Ouvrir Santé : un seul encart Santé Connect, pas de grand bouton « Synchroniser 28 jours », pas de statut de synchronisation automatique.
- [ ] Toucher les flèches de synchronisation dans l'encart : la synchronisation des 28 jours démarre ; une roue de chargement remplace l'icône durant l'opération ; retour à l'icône ensuite.
- [ ] En absence de permission Santé Connect, le même bouton ouvre l'autorisation ; erreurs conservées, sans message de succès envahissant.
- [ ] Dans Paramètres, trouver l'état de synchronisation automatique (~2 h), ainsi que l'autorisation d'accès en arrière-plan si nécessaire.
- [ ] Vérifier que la synchronisation WorkManager reste opérationnelle sans ouvrir l'application.
- [ ] Dans Santé, sélectionner veille/jour par les chevrons : date lisible sur petit écran, sans retour à la ligne.
- [ ] Vérifier les détails de sommeil (efficacité, phases, éveils), pouls min/max, VFC, respiration, oxygénation, pas, distance, calories, poids, VO₂ max.
- [ ] Basculer 7 / 28 jours et déployer « Voir les autres courbes » ; pas de courbes ni mesures fictives.
- [ ] Depuis Aujourd'hui, toucher Sommeil, Pas, FC repos, VFC : chaque encart ouvre sa fiche correspondante.
- [ ] Depuis Suivi, toucher les mini-courbes Sommeil, Pas, FC repos, VFC ; vérifier aussi le raccourci Respiration.
- [ ] Sur une fiche, changer Jour / 7 jours / 28 jours et contrôler valeur, courbe, moyenne, min/max et liste chronologique.
- [ ] En vue Jour, les pas restent ceux d'aujourd'hui ; l'évaluation d'une journée d'activité incomplète reste neutre.
- [ ] En absence de données, afficher « — » et un graphique vide, sans inventer de valeur.
- [ ] Le bouton « Toutes mes mesures détaillées » ouvre Santé. Le retour Android et les 4 onglets restent corrects.
- [ ] Aucun changement des exercices personnels, sons de respiration, progression, sauvegardes, permissions ou base Room.

## Contraintes de données
Les statistiques Santé Connect sont agrégées par jour dans Room. La vue Jour affiche la mesure courante et une évolution récente calculée sur les journées archivées ; elle n'invente **aucune évolution intra-journalière** (pas horaire).

## Régression
Lancer les tests JUnit et `assembleDebug` dans GitHub Actions, puis contrôler la Release signée sur appareil avant validation définitive.
