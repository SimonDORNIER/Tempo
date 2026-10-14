# Tempo Coach — Bibliothèque de composants UI
Version 1.0 • Spécification prête à coder (non implémentée) • 14 octobre 2026

> Référence : **Prune & Rose poudré**. Santé, détente, sobriété, interface compacte Pixel 7a. **Pas d'image de fond**. Uniquement aplats sombres + dégradés très subtils dans cartes/boutons.
> La maquette est un concept graphique : les chiffres et mesures affichés sont des exemples, PAS des données réelles ni une preuve que l'app possède un score correspondant.

## 1. Tokens de base

| Token | Valeur | Rôle |
| --- | --- | --- |
| `Background` | `#1A1D1F` | fond, uni |
| `Surface` | `#252A31` | cartes neutres |
| `SurfaceAlt` | `#22262C` | barres, nav, menus |
| `Border` | `#323842` | bordure 1dp discrète |
| `PrimaryPrune` | `#5B2D58` | identité, sommeil, coach |
| `Rose` | `#D99BB6` | graphiques, indicateurs, sélection |
| `Cream` | `#F0DDD6` | CTA principal |
| `Sage` | `#8CA899` | récupération, succès |
| `Text` | `#F7F3F1` | texte principal |
| `TextSub` | `#CFC4C8` | texte secondaire |
| `TextMuted` | `#9D959A` | aide discrète |
| `Warning` | `#D9B49B` | vigilance, à évaluer en contraste |
| `Error` | `#B86D84` | destruction, erreur — doit rester distincte du rose décoratif |

Optionnels : `SleepGradient = #5B2D58 -> #3C273E`, `RecoveryGradient = #415246 -> #2C3430`, `CoachGradient = #4E324D -> #352A3F`, `ButtonGradient = #F0DDD6 -> #E9D5CC`. Degrés de saturation et contrastes à valider sur Android, pas en se fiant seulement aux rendus générés.
- Grille : marges latérales 16dp, écart 8-12dp, espace vertical 12-16dp.
- Police système Roboto; texte 14-16sp, titre section 18sp, KPI 24-28sp, légende 12-14sp, onglets 12-13sp.
- Rayons : KPI 20-22dp, section 22-24dp, mini graph 16-18dp, CTA 28-30dp.
- Icônes exclusivement vectorielles cohérentes, style Material Symbols Rounded, 20-24dp, pastille 36-40dp.
- Touches : **minimum 48x48dp** ; tenir compte de la police système à 1.5 et du contraste WCAG AA.

## 2. Contrats des composants — API Compose proposée

### A. `TempoPrimaryButton`
**Rôle** : la seule action prioritaire de l'écran (« Démarrer ma séance », « Mobilité douce 5 min »).
- Hauteur minimale 56dp, largeur disponible, coins 28dp, padding horizontal 16dp.
- Dégradé discret `Cream -> #E9D5CC`, texte et icône en `Background`.
- Icône 20-22dp ; libellé 15-16sp SemiBold, une ligne lorsque possible.
- Signature proposée : `label: String, icon: ImageVector?, enabled: Boolean, loading: Boolean, onClick: () -> Unit`.
- États : normal, pressé (assombrissement discret), disabled (contraste conservé), loading (action désactivée + indicateur).
- Pas de double lancement si appui rapide ; zone tactile >=48dp ; libellé d'accessibilité.
- Une seule occurrence prioritaire par écran ; si le contenu augmente, le libellé peut passer sur deux lignes sans être tronqué.

### B. `TempoSecondaryButton`
**Rôle** : « Voir les détails », « Personnaliser », « Annuler ».
- Hauteur minimale 48dp, coins 16-20dp, bord 1dp `Border`, fond `SurfaceAlt`, texte `Text`, accent discret prune.
- Variantes : `outlined`, `text`, `danger` (seulement dans les dialogues de confirmation).
- Signature proposée : `label, icon?, variant, enabled, onClick`.
- Ne jamais utiliser la couleur `Cream` pour une action secondaire.
- Actions irréversibles : ouvrir `TempoConfirmDialog` ; jamais de suppression directe.

### C. `TempoMetricCard`
**Rôle** : chiffre essentiel (« Sommeil », « Récupération », « Pas », « Activité »).
- Grille 2 colonnes ; largeur calculée selon écran, hauteur **intrinsèque**, typiquement 112-136dp avec police normale.
- Icône 22-24dp dans une pastille 38dp ; libellé 13-14sp ; valeur 24-26sp SemiBold ; sous-info 12-13sp.
- Coins 22dp, padding 12-14dp ; fond teinté très discret et dégradé léger, jamais de photo.
- Signature proposée : `title, value: String?, subtitle, icon, accent: MetricAccent, trend: TrendData?, freshness: Instant?, onClick`.
- Si `value == null` : affichage « — » + sous-info « Aucune donnée » et accès aux permissions si applicable.
- Sommeil = dernière nuit terminée, avec date au détail ; pas = aujourd'hui ; score = libellé Tempo, ne pas confondre avec Fitbit.
- Anneau de progression **uniquement** si objectif configuré ; ne pas inventer de seuil.
- Tap sur la carte entière ouvre le détail ; pas plusieurs icônes cliquables minuscules.

### D. `TempoCoachCard`
**Rôle** : recommandation du jour, explicable et adaptée aux données disponibles.
- Coins 22dp, padding 14-16dp, gradient prune très léger, icône 24dp en pastille 38-40dp.
- Titre 14-15sp SemiBold ; message principal 14sp, **maximum 2 lignes sur l'accueil**.
- Signature : `title, message, explanation?, confidence: ConfidenceLevel?, onOpenDetails`.
- Action « Pourquoi ? » possible ; le détail précise les éléments de santé ayant guidé le conseil et les données absentes.
- Aucune promesse médicale ; pas de conseil « bonne récupération » si indicateurs insuffisants.
- En l'absence de données : message neutre et lien vers la synchronisation.

### E. `TempoHabitRow` (et `TempoHabitsCard`)
**Rôle** : cocher rapidement une habitude, sans naviguer.
- Conteneur 22-24dp ; header « Habitudes du jour » + compteur `X / Y`.
- Une ligne : min 56dp (cible 60dp), pastille icône 36dp, texte court 14sp, sous-texte 12-13sp, coche tactile 48dp.
- Signature de ligne : `id, title, subtitle?, icon, checked, onCheckedChange`.
- La coche est un contrôle utilisable TalkBack ; changement de jour = réinitialisation **des coches de la nouvelle date** tout en conservant l'historique nécessaire au suivi.
- Sur l'accueil : 3 habitudes maximum visibles ; « Voir tout » mène à la liste complète.
- Ne pas afficher des suivis d'hydratation en litres sans fonctionnalité d'enregistrement/synchronisation correspondante.
- Le compteur est toujours recalculé sur les vrais états, ne pas le coder en dur.

### F. `TempoMiniChartCard`
**Rôle** : aperçu d'une mesure sur les 7 derniers jours sans envahir l'accueil.
- Largeur minimale **132dp** ; hauteur 124-152dp ; coins 18dp, padding 10-12dp.
- Icône 18dp, label 12-13sp, agrégat 17-20sp, 7 barres ou sparkline cohérente, unités et période accessibles.
- Signature : `title, summary: String?, points: List<MetricPoint?>, kind: ChartKind, accent, onClick`.
- **Jamais 3 graphiques trop étroits forcés** sur écran 360dp : préférer carrousel horizontal à snap (ou 2 + Voir tout).
- Null/jour manquant = écart visible ; ne pas dessiner de valeur à zéro ou relier deux valeurs au travers d'un trou.
- Contrôle au toucher / lecteur d'écran expose date et valeur ; mise à jour fluide sans bloquer l'UI.
- Composant plus complet : `TempoTrendChart` en vue Suivi avec sélecteur 7/28 jours.

## 3. Composants transversaux communs
- `TempoSectionHeader(title, actionLabel?, onAction?)` : titre 18sp + chevron, toucher 48dp.
- `TempoStatusPill(state, lastSync)` : « Synchronisé 21:34 » / « À synchroniser » / « Sync en cours » ; ne jamais simuler un succès.
- `TempoConfirmDialog(title, consequences, confirmLabel, onConfirm, onCancel)` : confirmation avant suppression, reset, écrasement, perte de progression.
- `TempoEmptyState(icon, message, action?)` : données/permissions manquantes, erreurs claires et sans effets de style excessifs.
- `TempoBottomNav` : 4 ou 5 onglets **selon validation produit**, pas plus, label court et état actif prune/rose.

## 4. Variantes et validation
Pour chacun : normal, pressé/focus, disabled, chargement si pertinent, donnée absente, police 150 %, hors-ligne et TalkBack.
- Pixel 7a, petit écran 360dp, clair/sombre, landscape si pris en charge ; aucun débordement ni unité coupée.
- Les contrastes doivent être mesurés sur les paires réelles (y compris textes colorés sur fond dégradé).
- Tap source > détail réel ; pas d'icônes/touches inertes.
- Les chiffres montrés dans les maquettes restent **purement illustratifs** jusqu'au branchement aux sources réelles.
- Prochaine phase = implémentation du thème commun et des composants Compose, puis réfection de l'accueil ; **aucun code applicatif modifié par ce document**.

## 5. Cohérence projet
Cette bibliothèque complète `docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md` et `docs/ROADMAP_TEMPO_COACH.md`. Les choix actés « santé / détente », palette Prune & Rose poudré, compact, aucune image de fond priment sur les anciennes mentions « sport » ou « bleu électrique ».
