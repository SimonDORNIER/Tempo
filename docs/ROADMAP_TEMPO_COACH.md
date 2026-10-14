# Tempo Coach — Backlog UX, santé et entraînement
Mis à jour : 14 octobre 2026. État : préparation / discussion, PAS encore développé.
Sources design : docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md, docs/COMPONENTS_TEMPO_COACH.md et **docs/NAVIGATION_TEMPO_COACH.md**.

## Choix utilisateur actés
- Style actualisé : **santé + détente**, premium, un peu dynamique, sans codes sportifs agressifs.
- Palette **Prune & Rose poudré** validée : fond #1A1D1F, prune #5B2D58, rose #D99BB6, crème #F0DDD6, sauge #8CA899.
- **Pas d'image de fond**, ni couleurs électriques. Dégradés subtils dans les cartes et boutons.
- **Navigation confirmée : 4 onglets Aujourd'hui / Pratiquer / Suivi / Moi ; Coach intégré à l'accueil, sans onglet indépendant ; détente et mouvement regroupés dans Pratiquer.**
- Accueil B3 : mix statistiques quotidiennes + coaching/séance.
- Densité C1 : très compact, adapté à Pixel 7a.
- Graphiques de suivi réels ; peu de texte ; icônes cohérentes ; code et UI normalisés.
- L'utilisateur valide les idées suivantes comme base de discussion. Ne pas les traiter comme détails techniques définitivement figés.
- Conserver le fonctionnement 100 % téléphone, gratuit au-delà de l'abonnement ChatGPT actuel, sans API payante obligatoire, stockage et sauvegardes fiables.
- Ne pas coder une nouvelle version sur la seule base de ce document sans prioriser avec l'utilisateur si une question produit reste ouverte.

## 2.1 — Design system professionnel (priorité haute)
- Centraliser tokens de couleur, typographies, espacements, rayons et tailles selon la palette Prune & Rose poudré validée.
- Famille d'icônes vectorielles cohérentes pour navigation, catégories et statistiques.
- Composants réutilisables : MetricCard, CoachAction, RecoveryCard, EmptyState, ConfirmDialog, chart.
- Dark mode soigné + light mode accessible, support police élargie, descriptions TalkBack.
- Remplacer progressivement emojis et textes superflus par pictogrammes accompagnés de labels.

## 2.2 — Accueil / Aujourd'hui refondu (priorité haute)
- Au sommet : date, fraîcheur de synchronisation.
- KPIs 2x2 : **dernière nuit**, récupération Tempo, **pas du jour**, **minutes actives**.
- Détails FC repos, VFC, architecture du sommeil au toucher (pas tout à l'accueil).
- Une seule action contextuelle (séance normale, mobilité douce ou bilan).
- Mini-conseil coach en une phrase, 3 habitudes ultra compactes.
- 2 à 3 mini-tendances 7j sous la zone principale.
- Si l'utilisateur clique sur un KPI, ouvrir le détail dans l'onglet de suivi.
- Reprendre le principe visuel de la capture fournie (infos chiffrées, occupation compacte de l'écran) sans copier des marques ni métriques propriétaires.
- Dernière nuit = dernière session réellement achevée et datée (pas forcément « aujourd'hui »).

## 2.3 — Graphiques de suivi interactifs (priorité haute)
- Santé : sommeil, stade de sommeil fiable, nombre de pas, FC repos, VFC, minutes activité, poids et VO2max si source.
- Entraînement : séances effectuées, temps total, intensité perçue, groupes travaillés, mobilité vs renforcement.
- Habitudes : taux de complétion, calendrier léger, constance hebdo.
- Courbes 7/28j, barres pour quantités, sélection de point pour date et valeur.
- Masquer/indiquer données absentes (pas inventer ou joindre les points à tort).
- Comparer 7 jours à 21 précédents, au moins 4 mesures récentes et 7 historiques pour tendance robuste.
- Si période 90j envisagée, étendre d'abord l'historique synchronisé/conservé.

## 2.4 — Coach plus pertinent (priorité moyenne)
- Une recommandation concise fondée sur sommeil, VFC, FC repos, activité, douleurs, énergie et récupération disponible.
- Conseils contextuels : pause active, mobilité, entraînement, détente, retour au calme.
- Expliquer « pourquoi » sur écran secondaire, données utilisées et incertitude.
- Personnalisation progressive avec l'historique des séances et difficultés, ne pas inférer de diagnostic.
- Planification intelligente respectant jours d'effort, récupération et préférences.
- Tester qualité rédactionnelle et absence de faux messages positifs liés à des données manquantes.

## 2.5 — Qualité, sécurité et compatibilité (en continu)
- Pixel 7a : vérifier lisibilité, densité, respect barres système et confort tactile.
- Petits écrans, grand texte, TalkBack, thème clair/sombre, fonctionnement sans Internet.
- Sync Health Connect : autorisations partielles, erreurs, sources concurrentes, données futures/obsolètes.
- Sauvegarde/restauration complète Drive sur un autre téléphone.
- Migrations Room et préférences sans pertes, conserver exercices personnalisés.
- Toutes actions destructives explicitement confirmées, sauvegarde d'exercice manuelle.
- Tests automatisés UI et logique + checklist fonctionnelle avant Release.
- Versionner les changements un par un, vérifier le build GitHub Actions et l'APK signé.

## Pré-développement — choix à trancher ensemble

**Identité validée** : santé / détente, prune & rose poudré, fond uni sans illustration, gradients discrets, UI compacte Pixel 7a, pas de service payant imposé. Référence absolue : `docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md` et `docs/COMPONENTS_TEMPO_COACH.md`.

**Navigation désormais validée (voir NAVIGATION_TEMPO_COACH.md). Les points restants ne doivent pas faire passer les sous-écrans suggérés pour des fonctionnalités déjà développées.**
1. **Statistiques réellement disponibles** : `DailyHealthEntity` stocke sommeil (durée et stades), FC repos, VFC, pas, distance, calories, minutes d'exercice, etc., mais **pas l'hydratation** ni un score natif de qualité du sommeil Fitbit. Le moteur Recovery produit `score: Int?` et `confidence` distincts d'un score Fitbit. Afficher uniquement des données disponibles, avec date/unité et état « — » sinon. La carte Activité correspond aux minutes d'exercice réelles, pas à une mesure fantaisiste « activité douce ». Objectif de pas uniquement si configuré.
2. **Habitudes** : actuellement 3 coches quotidiennes sauvegardées avec un jour, **sans historique multi-jours**. Pour un graphe de régularité 7/28 jours, créer un historique daté (avec migration/sauvegarde compatibles) et préciser les habitudes que l'utilisateur veut suivre. Ne pas inventer des litres d'eau ou des objectifs non instrumentés.
3. **MVP du nouveau design** : proposer `2.1 thème et six composants`, `2.2 accueil compact + navigation`, `2.3 graphiques de suivi`, `2.4 autres écrans + qualité`. Chaque étape compilée/signée, testable sur Pixel 7a et rétrocompatible, sans refonte totale d'un seul coup.
4. **Comportements** : clic d'une carte -> véritable écran de détail ; conseil en 1-2 lignes + explication si besoin ; une seule action primaire contextualisée ; état sans données/permissions/Internet ; graphiques cliquables avec dates, valeurs et trous visibles ; retour Android et dialogues conservés.

**Points qu'on peut résoudre sans bloquer l'utilisateur** : police native Roboto, 16dp de marge, 48dp de zone tactile, support de police agrandie, thème sombre prioritaire et clair ultérieurement, animation réduite et pas de fond illustré.

## Pistes à discuter plus tard
- Objectifs quotidiens personnalisables (pas/activité/sommeil), pas d'objectifs imposés.
- Organisation détaillée de la navigation « Accueil / Séance / Sommeil / Santé / Suivi / Coach » selon tests d'usage.
- Rappel subtil et personnalisable de bouger ; ne pas inonder de notifications.
- Options de filtrage des graphiques, comparaison par semaine et mensuelle.
- Routines type 2 min / 5 min / 15 min selon disponibilité et récupération.
- Cartes modifiables / tableau de bord configurable à l'avenir, pas en priorité.
- Long terme : stabilité, suivi des habitudes sans culpabilisation ni fausses alertes.

## Critères d'acceptation transversaux
- Compréhension en moins de 3 secondes : sommeil, forme, pas, action.
- Dans la première zone visible, 4 chiffres importants + une action ; autres détails accessibles au tap.
- Aucun débordement Pixel 7a, grand texte ni disparition d'éléments.
- Pas de dépendance à un service payant, aucune transmission santé non sollicitée.
- Pas de régression des paramètres, exercices, sauvegarde et mise à jour automatique.
- Chaque livraison compilée et signée ; l'utilisateur teste avant d'engager un chantier majeur.
