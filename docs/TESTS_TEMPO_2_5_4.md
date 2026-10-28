# Tempo Coach 2.5.4 — Accents plus visibles (28 octobre 2026)

## Correction du contraste perçu

Retour utilisateur : le passage 2.5.2 → 2.5.3 était imperceptible. Cause : les dégradés des cartes partaient de teintes de catégorie déjà très sombres, puis étaient appliqués à seulement 22 % d'opacité. Les couleurs de thème étaient donc à peine présentes.

- Le fond quasi noir `#090D12` et la surface principale `#1B222B` restent **strictement inchangés**.
- Les cartes à thème ont désormais un fond réellement teinté avec la couleur de la catégorie : 19 % d'opacité côté haut/gauche vers 4,5 % côté bas/droit, sur la surface anthracite.
- Bordures thématiques 1 dp à 64 % de leur couleur, sans glow ; pastilles d'icône à 26 %.
- Nuances des domaines légèrement plus saturées (sommeil `#B8A1E8`, activité `#78D0AE`, cardio `#E7A1B0`, coach `#8FC5F3`), sans revenir aux couleurs néon de l'Option B.
- Graphiques à points : traits et points légèrement épaissis pour une lecture facilitée, en gardant une couleur de catégorie et des points d'état.
- Cartes neutres et sous-écrans restent calmes, afin de réserver les couleurs aux zones informatives.
- Les quatre tuiles de l'accueil conservent leur hauteur 162 dp, les mêmes marges et les mêmes textes.
- Aucune modification des calculs de récupération, de Santé Connect, de la base de données, des fichiers de sauvegarde ou de l'identifiant Android.

## Validation
- [ ] Fond tout aussi sombre que la 2.5.3 et cartes visiblement plus colorées
- [ ] Sommeil lavande, pas vert, cardio rose, Coach bleu sans effet fluorescent
- [ ] Les petites cartes de l'accueil restent parfaitement alignées
- [ ] Les différences rouge/orange/vert portent toujours sur les statuts et non sur l'identité de thème
- [ ] Enregistrer/afficher une séance, vérifier Santé Connect, le sommeil de la dernière nuit et les données manquantes
- [ ] Mise à jour de la 2.5.3 sans désinstallation, même signature APK
