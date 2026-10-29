# Décision utilisateur du 29/10/2026 — Accueil sémantique 2.5.7

Les cartes Aujourd'hui (Sommeil, Pas, FC repos, VFC) utilisent le statut de TempoHealthSignals pour leur fond, bordure, chiffre et label ; les icônes restent de la couleur du domaine. La respiration dans « Santé & activité » comporte un indicateur d'état ; le score Forme colore le nombre. Pas du jour jamais en alerte, référence insuffisante = gris. C'est la suite de la décision 2.5.6.

# Décision utilisateur du 29/10/2026 — PRIORITÉ SUR LES CONSIGNES CI-DESSOUS

Sur Tempo Coach 2.5.6+, tous les points, segments, barres et chiffres interprétés utilisent la même couleur d'état (`C.good`, `C.amber`, `C.redAlert`, `C.unknown`), indépendante de l'identité des domaines. Aucune fausse référence, pas d'alerte sur journée de pas incomplète. Lire la section 2.5.6 dans la charte.

# Instructions permanentes — Projet Tempo Coach

> **Décision du 28/10/2026 (prioritaire)** : la maquette 2.5 en couleurs douces par domaine remplace Option B. Brancher les futurs développements sur `health/v2.5.0-soft-professional-ui` puis sur la dernière version publiée. Sources : `docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md` en tête et `docs/TESTS_TEMPO_2_5.md`. Ne jamais revenir aux fortes bordures saturées. Pas de fausses données ni de modifications de schéma pour cette refonte.

> **Décision de design du 16/10/2026** : le propriétaire a sélectionné le visuel **Option B** (Suivi sombre, cartes encadrées en violet et vert forêt, accents plus saturés et graphiques lumineux). Il **remplace les teintes trop fades** de l'ancienne charte comme cible pour le prochain développement. Lire d'abord la section « Direction graphique approuvée — option B » dans `docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md`. Implémentation Option B sur la branche `health/v2.2.0-option-b` (statut de publication à vérifier dans GitHub Actions et les Releases).


Ce dépôt contient l'app Android native Tempo Health / Tempo Coach et des composants historiques. Pour les évolutions de Tempo Coach, repartir de **health/v2.2.0-option-b** pour les prochaines évolutions, conserver la branche **health/v2.1.1-wellness-ui** comme base de comparaison ; ne pas repartir du code legacy `main` ni d'une ancienne branche de release.

## Références obligatoires
- docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md : identité santé + détente et palette choisie.
- docs/COMPONENTS_TEMPO_COACH.md : six composants réutilisables, tailles, variantes et états.
- docs/ROADMAP_TEMPO_COACH.md : décisions du propriétaire et futures fonctionnalités.
- docs/NAVIGATION_TEMPO_COACH.md : navigation à 4 onglets implémentée et évolutions ; pas de cinquième onglet Coach.
- docs/TESTS_TEMPO_2_1.md : tests de régression et vérifications sur appareil.

## Directions déjà validées
- **Style santé / détente premium** (plus sport) ; palette **Option B Violet & Vert forêt** (#0C1118, #9638E8, #C06CFF, #127E51, #50E49A), **aucune image de fond**, dégradés doux dans cartes/CTA seulement ; accueil mixte santé/coaching ; densité **très compacte**.
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
