# Instructions permanentes — Projet Tempo Coach

> **Décision de design du 16/10/2026** : le propriétaire a sélectionné le visuel **Option B** (Suivi sombre, cartes encadrées en violet et vert forêt, accents plus saturés et graphiques lumineux). Il **remplace les teintes trop fades** de l'ancienne charte comme cible pour le prochain développement. Lire d'abord la section « Direction graphique approuvée — option B » dans `docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md`. Les changements n'ont pas encore été implémentés et ne doivent pas être revendiqués comme une release publiée.


Ce dépôt contient l'app Android native Tempo Health / Tempo Coach et des composants historiques. Pour les évolutions de Tempo Coach, repartir de **health/v2.1.1-wellness-ui** ou de son tag **health-v2.1.1** ; ne pas repartir du code legacy `main` ni d'une ancienne branche de release.

## Références obligatoires
- docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md : identité santé + détente et palette choisie.
- docs/COMPONENTS_TEMPO_COACH.md : six composants réutilisables, tailles, variantes et états.
- docs/ROADMAP_TEMPO_COACH.md : décisions du propriétaire et futures fonctionnalités.
- docs/NAVIGATION_TEMPO_COACH.md : navigation à 4 onglets implémentée et évolutions ; pas de cinquième onglet Coach.
- docs/TESTS_TEMPO_2_1.md : tests de régression et vérifications sur appareil.

## Directions déjà validées
- **Style santé / détente premium** (plus sport) ; palette **Prune & Rose poudré** (#1A1D1F, #5B2D58, #D99BB6, #F0DDD6, #8CA899), **aucune image de fond**, dégradés doux dans cartes/CTA seulement ; accueil mixte santé/coaching ; densité **très compacte**.
- **4 onglets : Aujourd'hui, Pratiquer, Suivi, Moi ; Coach dans l'accueil, détente et mouvement ensemble dans Pratiquer.**
- Mobile-first Pixel 7a ; icônes vectorielles cohérentes + labels courts ; éviter textes superflus.
- Tableau de bord nuit dernière, récupération, pas, activité, action unique ; graphes de suivi pertinents.
- Contraste AA, grands caractères, TalkBack, petits écrans, mode sombre/clair.

## Sécurité et livraison
- Kotlin + Compose, thème partagé et composants réutilisables.
- Ne jamais effacer de données locales, changer le package ou casser les sauvegardes Drive sans migration/test.
- Santé Connect lecture contrôlée, pas d'API payante, hors-ligne prioritaire et pas d'envoi santé non sollicité.
- Un exercice ne se sauve que sur validation ; action destructive => dialogue de confirmation.
- Données manquantes => état explicite, pas de scores ni mesures inventés.
- Faire les versions de manière incrémentale et vérifier Release APK signé avant annonce de succès.
- Documenter les décisions nouvelles dans la roadmap et la charte avant toute divergence.
