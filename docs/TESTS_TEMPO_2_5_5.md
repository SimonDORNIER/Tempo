# Tempo Coach 2.5.5 — Couleurs enfin visibles, sans néon

Cette révision clarifie la version 2.5.4 (deux branches de compilation portent son numéro). La 2.5.5 est la référence unique de la retouche demandée.

Le problème des 2.5.2 et 2.5.3 était **l'utilisation d'une couleur déjà sombre, rendue encore transparente**, dans les dégradés. Les cartes à thème sont maintenant réellement teintées en superposant directement la couleur pastel sur leur fond sombre : départ à 19 % d'opacité, arrivée à 4,5 %, bordure 64 % et pastille d'icône 26 %.

La carte Forme suit une règle distincte pour que le rouge d'un score faible ne domine pas : 13 % puis 2,5 % de la couleur d'état, bordure à 57 %.
Les états favorables sont maintenant codés `C.good`, distinct de la couleur de thème Activité.

Conservé : fond `#090D12`, taille des quatre cartes 162 dp, navigation, traitements médicaux, Santé Connect, sauvegardes, calcul du score et exercices personnalisés. Pas de données inventées.

Tests : comparer **visuellement** aux versions 2.5.2/2.5.3 ; vérifier Sommeil lavande, Activité vert, Cardio rose, Coach bleu, sans halo, et les badges d'état vert/ambre/rouge avec un libellé lisible.
