# Tempo Coach 2.5.2 — Fond sombre et cartes régulières

## Décisions du 28 octobre 2026
- Fond global anthracite très sombre : `#090D12` (ancien : `#11171F`).
- Surface des cartes : `#1B222B` (ancienne : `#202833`), surface secondaire `#242C36`, bordures `#35404D`.
- Dégradés moins présents, seulement 18 % puis 8 % d'opacité.
- Les **quatre petites cartes de l'écran Aujourd'hui** (Sommeil, Pas, FC repos, VFC) font chacune exactement **162 dp de haut** sur Android, avec zone intérieure de 136 dp et les mêmes alignements verticaux.
- Les cartes longues (Forme, Coach, habitudes) restent adaptatives à leur contenu : ne pas tronquer l'explication de récupération.
- Palette par thème et couleurs par niveau inchangées, aucun changement de score ou de source de données.
- Même identifiant de package et même base Room, exercices, préférences et données Santé Connect conservés.

## Vérifications
- [ ] Les quatre cartes ont la même hauteur et largeur par colonne, peu importe 1 ou 2 lignes de description
- [ ] Aucun texte écrasé sur un Pixel 7a, notamment Sommeil / Efficacité / dernière nuit
- [ ] Aucun débordement avec la taille de police à 1,3×
- [ ] La navigation et l'arrière-plan sont visiblement plus sombres mais les chiffres restent lisibles
- [ ] Installation par-dessus 2.5.1, données et exercices personnalisés inchangés
- [ ] Compilation et signature identique à 2.5.1
