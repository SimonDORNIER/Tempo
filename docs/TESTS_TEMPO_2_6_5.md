# Tempo Coach 2.6.5 — Récupération VFC

## Problème remonté
Google Health affiche une VFC de 40 ms pour le 16/11/2026 et une courbe renseignée sur la semaine, tandis que la fiche VFC de Tempo Coach 2.6.4 affiche « — » et une courbe vide.

## Changement
- Conserver la lecture de `HeartRateVariabilityRmssdRecord` depuis Santé Connect (RMSSD en ms), **sans extrapoler** une VFC à partir de la fréquence cardiaque.
- Chercher prioritairement les relevés nocturnes, puis, s'ils sont absents, élargir à la journée locale entière pour inclure une éventuelle mesure quotidienne exportée après le réveil.
- Parcourir toutes les pages de réponses Santé Connect.
- Sur la fiche VFC et **uniquement lorsqu'aucune valeur n'est disponible**, distinguer permission VFC manquante et absence de valeur importée ; bouton d'autorisation ou resynchronisation, roue de chargement.
- Le fait que Google Health possède une VFC ne prouve pas qu'elle ait été écrite dans Santé Connect : si cette source n'a aucun enregistrement, le correctif ne fabrique pas 40 ms.
- Préserver les autres mesures, le cache Room, la sauvegarde, les programmes et les 4 onglets.

## Tests
- [ ] Tests unitaires `HrvReadWindowsTest` : sommeil puis journée, journée sans sommeil, plage invalide, pas de doublon.
- [ ] `testDebugUnitTest` et `assembleDebug` en CI.
- [ ] Sur Pixel 7a, permission VFC accordée, effectuer une synchronisation 28 jours.
- [ ] Quand la VFC existe dans Santé Connect mais est horodatée en dehors du sommeil, vérifier qu'elle apparaît sur la bonne journée dans Tempo Coach.
- [ ] Quand aucune VFC n'est importable malgré la synchronisation, ne pas afficher de valeur inventée et proposer une explication concise.
- [ ] Sans permission VFC, la fiche doit afficher « Accès VFC manquant » et permettre de demander la permission.
- [ ] Quand une synchronisation est en cours, le bouton affiche une animation.
- [ ] Les données historiques du sommeil, du pouls et des pas sont inchangées.
- [ ] Ne pas désinstaller Tempo Coach lors de la mise à jour, et vérifier la conservation des données locales.
