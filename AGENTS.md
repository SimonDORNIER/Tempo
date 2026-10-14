# Instructions permanentes — Projet Tempo Coach

Ce dépôt contient l'app Android native Tempo Health / Tempo Coach et des composants historiques. Pour les versions Android Health, travailler depuis la branche active issue de health/v2.0-coach-dashboard, PAS depuis le code legacy main par défaut.

## Références obligatoires
- docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md : identité santé + détente et palette choisie.
- docs/COMPONENTS_TEMPO_COACH.md : six composants réutilisables, tailles, variantes et états.
- docs/ROADMAP_TEMPO_COACH.md : décisions du propriétaire et futures fonctionnalités.
- docs/NAVIGATION_TEMPO_COACH.md : navigation validée à 4 onglets et parcours UX ; respecter les décisions, ne pas créer de nouveau cinquième onglet Coach.

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
