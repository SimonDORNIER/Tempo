# Tempo Coach 2.5.4 — Accent thématique des surfaces

Retour du 28 octobre 2026 : la différence entre 2.5.2 et 2.5.3 est insuffisante.
La cause était le mélange du fond sombre des domaines avec un alpha très faible, presque identique au fond normal des cartes.

## Correction réelle
- On conserve le fond global `#090D12`, les tailles (162 dp pour les quatre tuiles) et les valeurs de la 2.5.3.
- Les cartes **Sommeil / Pas / FC repos / VFC / Coach / Pratiquer / Suivi** ont un dégradé calculé à partir de leur *accent pastel clair*, et non de leur teinte de fond sombre.
- Intensité : 18 % côté fort et 4 % côté faible, sur les surfaces `#1B222B`. Une carte rouge/orange liée à la récupération est limitée à 13 % pour ne pas créer d'alerte agressive.
- Bordure à 63 % d'opacité et 1 dp, pastille d'icône à 22 %. Les cartes neutres ne prennent pas de couleur arbitraire.
- La couleur sémantique vert / ambre / rouge reste réservée au niveau mesuré ; ne pas la confondre avec la couleur stable de thème.

## Contrôles de régression
- [ ] Une différence immédiatement perceptible par rapport à 2.5.3 sur la photo de l'accueil, surtout les *fonds* Sommeil, Pas et Cardio.
- [ ] Niveaux lisibles, absence de néon, gradient discret, fond global très sombre.
- [ ] Dimensions identiques des quatre tuiles, textes non coupés.
- [ ] Aucun changement de données, Room, Santé Connect, exercices, habitudes, notifications ni sauvegardes.
- [ ] Build GitHub Actions réussi, signature identique à la 2.5.3 et installation par-dessus.
