# Instructions permanentes — Projet Tempo Coach

Ce dépôt contient l'app Android native Tempo Health / Tempo Coach et des composants historiques. Pour les versions Android Health, travailler depuis la branche active issue de health/v2.0-coach-dashboard, PAS depuis le code legacy main par défaut.

## Références obligatoires
- docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md : UI, palette, tokens et règles.
- docs/ROADMAP_TEMPO_COACH.md : décisions du propriétaire et futures fonctionnalités.

## Directions déjà validées
- Style hybride sport/santé premium ; accueil mixte santé/coaching ; densité **très compacte**.
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
