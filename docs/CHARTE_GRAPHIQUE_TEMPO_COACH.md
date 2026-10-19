# Tempo Coach — Charte graphique et UX

## Tempo Coach 2.3.0 — Graphiques sémantiques, courbes et points (19 octobre 2026)

La direction Option B reste inchangée (fond sombre, violet prononcé et vert forêt). Cette version ajoute des **couleurs d'état** en complément des couleurs de catégories. Le vert, l'ambre et le rouge signalent respectivement une valeur proche de la référence personnelle, un écart modéré et un écart important **quand une référence suffisamment renseignée existe**.

- **Courbe + points visibles** : VFC/HRV, FC de repos, respiration (courbe neutre pour respiration tant qu'aucune règle de tendance personnelle validée n'existe).
- **Histogrammes** : pas, durée du sommeil, minutes actives et volumes de séances ; ce sont des cumuls ou durées par jour.
- **Forme & préparation** : vert/orange/rouge calculés via `RecoveryEngine`, et pas avec un nouvel algorithme ou des données artificielles. Aucun graphique de forme historique n'est inventé : l'application n'enregistre pas encore un score par jour.
- **Comparaison** : médiane des 21 jours précédents, au minimum 5 observations valides. En deçà : état « Référence à construire », sans alerte colorée artificielle.
- **VFC** : orange si <= 90 % de la référence ; rouge si <= 80 %. **FC repos** : orange si >= +4 bpm ; rouge si >= +8 bpm. **Sommeil** : orange si au moins 40 minutes sous la référence ; rouge si au moins 90 minutes sous cette référence. Ce sont des signaux indicatifs, jamais un diagnostic.
- **Données absentes** : jours calendaires fixes, pas d'interpolation entre deux points séparés par des jours manquants ; libellé explicite avec nombre de jours renseignés.
- **SpO₂** : aucune donnée de saturation n'est stockée dans la version actuelle ; ne pas fabriquer de valeur ni de catégorie médicale.
- **Lisibilité** : points de physiologie et segments de courbe colorés individuellement, badges texte et bordures assorties ; une couleur seule ne suffit pas à l'interprétation.


## Implémentation Option B — Tempo Coach 2.2.0 (19 octobre 2026)

La refonte a été appliquée aux écrans Kotlin/Compose dans `health/v2.2.0-option-b`. Les teintes historiques ne sont plus utilisées ; leurs identifiants demeurent comme alias de compatibilité pour ne pas casser les sous-écrans.

### Tokens d'implémentation exacts
| Élément | Valeur |
|---|---|
| Fond uniforme | `#0C1118` |
| Carte neutre | `#191F2C` |
| Carte élevée | `#232A3A` |
| Texte | `#F8F7FD` |
| Texte secondaire | `#C9CDDE` |
| Bordure neutre | `#384158` |
| Violet principal | `#9638E8` |
| Violet lumineux | `#C06CFF` |
| Violet clair / chiffres | `#E0A6FF` |
| Fond dégradé violet | `#381653` |
| Vert forêt | `#127E51` |
| Vert lumineux | `#50E49A` |
| Fond dégradé vert | `#0B3A2B` |

La navigation reste à 4 onglets avec capsule violette. Les cartes importantes ont un trait de couleur et un fond dégradé ; les pages secondaires partagent désormais la bordure et la palette Material 3.

Les histogrammes 7 jours représentent toujours 7 jours calendaires et différencient une mesure 0 d'une mesure absente. Le nombre des séances terminées de « Cette semaine » est calculé sur la période concernée, pas sur toute la base. Le volume de données locales, les schémas Room, les clés de sauvegarde, les permissions et l'identifiant d'application n'ont pas été modifiés.

La compilation CI et les tests réels sur le Pixel 7a doivent être validés avant de qualifier la version de définitive.

---


## Direction graphique approuvée — option B (16 octobre 2026)

**Décision utilisateur la plus récente, PRIORITAIRE sur la palette « Prune & Rose poudré » historique.**
Le visuel de référence est la capture envoyée dans la conversation le 16/10/2026 intitulée **« Option B »**, montrant l'écran **Suivi** en mode sombre avec grands encadrés violets et verts, légers dégradés internes, graphiques colorés et une capsule violette active dans la navigation.

### Critères visuels validés
- Fond charbon très sombre, **uni** ; aucun paysage, photo, trame ni illustration en fond.
- **Violet saturé et prononcé** : sommeil, sélection, composants clés et bordures ; tons violets plus lumineux que dans la version 2.1.1.
- **Vert forêt plus lumineux, naturel et identifiable** : pas, activité et graphiques ; éviter le sauge grisé de la version 2.1.1.
- **Encadrés fins colorés** autour des cartes importantes (violet pour sommeil, vert pour activité) ; c'est un aspect explicitement apprécié.
- **Dégradés doux à l'intérieur des cartes** pour donner du relief, sans envahir toute l'application. L'option B montre aussi un motif graphique géométrique subtil à l'intérieur des cartes : garder seulement s'il ne nuit pas à la lecture.
- Chiffres importants très contrastés, textes principaux clairs, sous-titres gris-lavande lisibles ; graphes violet/vert vifs ; bas de l'application avec capsule active violette bien distincte.
- L'organisation 4 onglets Aujourd'hui / Pratiquer / Suivi / Moi reste la même, le Coach reste intégré à Aujourd'hui, les exercices sont regroupés dans Pratiquer.
- La direction est **validée comme référence de design**, **pas encore implémentée** dans la release 2.1.1. Ne pas annoncer de mise à jour avant compilation et tests.

### Tokens proposés pour mise en œuvre (à calibrer visuellement sur Pixel 7a)
| Rôle | Proposition initiale |
|---|---|
| Arrière-plan | `#10151C` |
| Surface neutre | `#1B2230` |
| Surface élevée | `#232A39` |
| Texte fort | `#F8F7FD` |
| Texte secondaire | `#C9CADB` |
| Violet principal | `#8A35E6` |
| Violet lumineux | `#B865FF` |
| Violet clair pour chiffre | `#DEA8FF` |
| Vert forêt | `#147D4D` |
| Vert lumineux | `#56E49A` |
| Vert clair chiffres | `#A6F7CA` |
| Bordure neutre | `#354056` |

Ces hex sont **des propositions de traduction visuelle**, pas des couleurs mesurées exactement sur l'image. Les états texte et accessibilité doivent passer les seuils de contraste ; ne pas peindre toutes les cartes en violet/vert et éviter le rendu fluo. Différencier les couleurs par icône et libellé, pas seulement par la couleur.

---

Version 1.1 — 14 octobre 2026
Statut : **identité et palette validées**. Contrastes et composants à tester en implémentation.
Bibliothèque officielle : [COMPONENTS_TEMPO_COACH.md](COMPONENTS_TEMPO_COACH.md).

## 1. Décisions utilisateur définitives
- **Direction actualisée : santé + détente**, premium, moderne, sobre et légèrement dynamique ; **pas de style sport**.
- **Palette choisie par l'utilisateur : Prune & Rose poudré**, sur fond sombre, nuances douces et dégradés subtils.
- **Aucune image de fond**, photographie ou paysage décoratif.
- **B.3 : accueil mixte** : statistiques santé + séance/coaching.
- **C.1 : très compact**, conçu pour un Pixel 7a ; comprendre la journée en trois secondes.
- Priorité aux icônes claires, données chiffrées et courts libellés, sans textes envahissants.
- Pas d'abonnement additionnel, pas de collecte automatique, usage possible entièrement sur téléphone.

## 2. Principes UX
- Afficher d'abord : nuit dernière, récupération, pas du jour, action pertinente.
- Un seul bouton d'action principal à la fois ; tout le reste en cartes secondaires.
- Une carte montre la valeur, l'unité, la période et si pertinent une tendance ; détail au toucher.
- Les conseils courts restent à l'accueil ; les explications vont dans Coach.
- Informations colorées mais jamais codées uniquement par leur couleur.
- Animations discrètes, aucun effet gratuit, aucune carte ou bannière inutile.
- Ne jamais afficher une donnée inventée ou une indication médicale catégorique.

## 3. Palette sombre — Prune & Rose poudré (validée)
| Rôle | Hex | Utilisation |
|---|---|---|
| Fond | #1A1D1F | arrière-plan uni |
| Carte | #252A31 | panneaux neutres |
| Carte élevée | #22262C | menus, navigation |
| Trait | #323842 | séparateurs discrets |
| Texte principal | #F7F3F1 | titres, chiffres |
| Texte secondaire | #CFC4C8 | valeurs secondaires, unités |
| Texte discret | #9D959A | légendes |
| Prune principal | #5B2D58 | sommeil, coach, identité |
| Rose poudré | #D99BB6 | sélection, indicateurs, graphes |
| Crème rosé | #F0DDD6 | bouton principal |
| Vert doux | #8CA899 | récupération, habitudes, succès |
| Vigilance | #D9B49B | prudence |
| Erreur | #B86D84 | suppression / erreur — distinct du rose graphique |

Dégradés autorisés **uniquement dans les surfaces** : sommeil #5B2D58 vers #3C273E ; récupération #415246 vers #2C3430 ; coach #4E324D vers #352A3F ; CTA #F0DDD6 vers #E9D5CC. **Pas d'images de fond**, ni effet fluo, bleu électrique ou couleur saturée. Un fond d'application uni, lisible et neutre.

La couleur sommeil reste prune/rose, récupération vert doux, activité palette neutre/vert ; un statut favorable ou défavorable doit rester distinguable d'une catégorie de donnée. Contraste WCAG AA à **vérifier sur Android** (>= 4,5:1 texte normal, >= 3:1 gros texte/éléments actifs). Prévoir une variante claire.

## 4. Typographie et mesures
- Typographie : police Android native, chiffres tabulaires pour les comparaisons.
- Chiffre clé 26-30 sp ; chiffres des mini-cartes 23-26 sp ; titres de section 17-18 sp.
- Libellés 13-14 sp ; texte usuel 14-16 sp ; éléments secondaires 12 sp uniquement si lisibles.
- Toujours afficher les unités et la période : 7 h 43, 2 008 pas, 63 bpm, VFC 37 ms.
- Pas plus de deux lignes d'explication dans une carte d'accueil.
- Cible : largeur utile 360-420 dp, et adaptation à plus petit et plus grand.
- Marges horizontales 12-16 dp, espacement 8 dp, cartes arrondies 16-20 dp.
- Deux colonnes pour les KPI, jamais quatre colonnes illisibles.
- Mini-graphiques : largeur minimale 132dp, prévoir un défilement horizontal ou deux cartes plutôt que 3 minuscules.
- Touches interactives >= 48 x 48 dp ; bouton principal 52-56 dp de haut.
- Ne jamais utiliser les dimensions physiques en pixels pour positionner les éléments.
- Tester police agrandie (1,3 et 1,5), barres système, clavier, navigation gestuelle.

## 5. Icônes
- Une seule bibliothèque vectorielle cohérente (Material Symbols / Compose).
- Pictogrammes 20-24 dp et même épaisseur de trait.
- Sémantique stable : lune=sommeil, pas=activité, cœur=cardio, batterie/bouclier=récupération, haltère=séance, courbe=suivi, rouage=paramètres.
- Icône et petit libellé ensemble, avec description TalkBack.
- Réutiliser les contrats exacts, dimensions, états et signatures dans [COMPONENTS_TEMPO_COACH.md](COMPONENTS_TEMPO_COACH.md).
- Éviter les emojis hétérogènes comme pictogrammes structurants et les illustrations décoratives.

## 6. Composants réutilisables
- TempoMetricCard : icône, libellé, valeur, unité, variation/micrographe, détail.
- TempoRecoveryCard : score, fiabilité, raisons en cliquant.
- TempoCoachAction : une consigne courte et un CTA contextuel.
- TempoSparkline : tendance sur 7 jours et jours manquants visibles.
- TempoTrendChart : 7 et 28 jours, sélection, valeur/date au toucher.
- TempoHabitRow : case à cocher, libellé, sauvegarde, réinitialisation quotidienne.
- TempoSectionHeader : titre court + voir le détail.
- TempoConfirmDialog : supprimer, réinitialiser, restaurer, quitter, abandonner.
- TempoEmptyState : permissions incomplètes, manque de données, import en cours.
- Tous les composants utilisent des tokens centralisés (couleurs, coins, typographie, marges).

## 7. Écran « Aujourd'hui » : plan compact
**En-tête fin** : date + état/heure dernière synchronisation.

**Bloc 2 x 2** : 
- Dernière nuit réellement terminée (durée + date)
- Récupération Tempo (score + confiance, si données disponibles)
- Pas du jour (compteur + progression si objectif configuré)
- Activité du jour (minutes, pas d'« aptitude » Fitbit inventée)

**Action primaire unique** selon contexte :
- « Démarrer la séance » si récupération compatible et programme prévu ;
- « Mobilité douce 5 min » si récupération défavorable ;
- « Voir mon bilan » si action accomplie.

**Coach express** : une phrase utile maximum + « Pourquoi ? ».
**Habitudes** : 3 coches, accessibles en peu de place.
**Mini-graphiques** : sommeil/pas/récupération sur 7 jours, deux ou trois au plus, sous la zone prioritaire ; toucher ouvre Suivi.

Attention : « dernière nuit » n'est pas toujours le dernier jour dans la table ; tenir compte de la fin réelle du sommeil. Si un score de qualité de sommeil ou une métrique Fitbit n'est pas disponible dans Santé Connect, afficher « — » ou ne pas montrer la carte, sans calcul trompeur.

## 8. Suivi et graphiques
- Santé : sommeil (durée et stades si fiables), pas, FC repos, VFC/RMSSD, minutes actives ; poids, VO2 max si présents.
- Entraînement : séances/semaine, durée totale, difficulté ressentie, répartition mobilité/renforcement.
- Habitudes : progression hebdomadaire, jours complétés ; prévoir historique local par jour pour taux et séries.
- Courbes pour FC repos/VFC/sommeil ; barres pour pas/exercices ; anneaux seulement pour objectif défini.
- Périodes 7j / 28j, éventuellement 90j après adaptation de la conservation/synchronisation.
- Comparaison honnête 7 jours récents vs 21 jours précédents, sans recouvrement.
- Toujours marquer données manquantes et nombre de mesures, ne pas interpoler artificiellement.

## 9. Règles de développement
1. **Kotlin, Compose, Material 3** ; thème/tokens partagés, composants réutilisables.
2. **Conserver les données** : même package Android, Room, préférences, exercices persos, sauvegardes, historique et Health Connect.
3. **Pas d'enregistrement implicite** des brouillons ; sauvegarde seulement sur validation.
4. **Confirmation avant destruction** : supprimer, reset, restauration, sortie avec perte de progression.
5. **Sûreté des données médicales** : local/hors ligne, aucune API payante ou transmission non sollicitée.
6. Gérer états vides, sync en cours, autorisations partielles, données obsolètes, erreurs, reconnexion.
7. Ne jamais bloquer l'UI avec les graphes, les calculs ou la synchronisation.
8. Tester Pixel 7a, petit écran, mode sombre ET clair, police agrandie, TalkBack, boutons Retour.
9. À chaque version : build signé + Release GitHub réussie + APK contrôlé avant d'annoncer la disponibilité.
10. Aucune nouvelle section ne doit introduire des paragraphes de texte à l'accueil.

## 10. Checklist de validation
- Comprend-on nuit, forme, pas, prochaine action en trois secondes ?
- Pas de coupure de texte ni débordement sur Pixel 7a ?
- Toutes les icônes sont-elles claires avec libellés accessibles ?
- Toutes les valeurs sont-elles réellement disponibles, datées et sourcées ?
- Les graphes montrent-ils les jours manquants ?
- Les actions destructives sont-elles confirmées ?
- Les données persistent-elles après redémarrage et mise à jour ?
- Le fonctionnement hors ligne et les sauvegardes restent-ils opérationnels ?

Ce document est la source de référence design pour le développement post-Tempo Coach 2.0.
