# Tempo Coach — Charte graphique et UX
Version 1.0 — 14 octobre 2026
Statut : directions validées, détails chromatiques proposés pour approbation visuelle.

## 1. Décisions utilisateur définitives
- **A.4 : hybride sport + santé**, sobre, premium, professionnel.
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

## 3. Palette sombre — proposition
| Rôle | Hex | Utilisation |
|---|---|---|
| Fond | #10151C | arrière-plan |
| Carte | #19222D | surface des indicateurs |
| Carte élevée | #22303D | dialogues et sélection |
| Trait | #354452 | séparateurs discrets |
| Texte principal | #F2F6FA | titres et chiffres |
| Texte secondaire | #B0BFCC | unités et légendes |
| Primaire | #5CBDF0 | boutons, navigation active |
| Sur primaire | #0B1B27 | texte sur bouton principal |
| Activité | #4BD2C1 | pas, exercice, mouvement |
| Sommeil | #B5A3FF | sommeil, nuit |
| Cardio | #FF849B | FC repos, VFC, cœur |
| Favorable | #57D5A1 | bonne récupération / succès |
| Vigilance | #FFC070 | récupération moyenne / incertitude |
| Défavorable | #FF8190 | alerte / actions destructives |

Règle : les couleurs des métriques sont fixes (sommeil violet, activité turquoise, cardio corail), les couleurs vert-orange-rouge décrivent seulement un **état**. Toujours distinguer score Tempo et scores propriétaires de Fitbit. Respecter contraste WCAG AA : >= 4,5:1 texte normal, >= 3:1 gros texte et éléments fonctionnels. Prévoir aussi thème clair cohérent.

## 4. Typographie et mesures
- Typographie : police Android native, chiffres tabulaires pour les comparaisons.
- Chiffre clé 26-30 sp ; chiffres des mini-cartes 23-26 sp ; titres de section 17-18 sp.
- Libellés 13-14 sp ; texte usuel 14-16 sp ; éléments secondaires 12 sp uniquement si lisibles.
- Toujours afficher les unités et la période : 7 h 43, 2 008 pas, 63 bpm, VFC 37 ms.
- Pas plus de deux lignes d'explication dans une carte d'accueil.
- Cible : largeur utile 360-420 dp, et adaptation à plus petit et plus grand.
- Marges horizontales 12-16 dp, espacement 8 dp, cartes arrondies 16-20 dp.
- Deux colonnes pour les KPI, jamais quatre colonnes illisibles.
- Touches interactives >= 48 x 48 dp ; bouton principal 52-56 dp de haut.
- Ne jamais utiliser les dimensions physiques en pixels pour positionner les éléments.
- Tester police agrandie (1,3 et 1,5), barres système, clavier, navigation gestuelle.

## 5. Icônes
- Une seule bibliothèque vectorielle cohérente (Material Symbols / Compose).
- Pictogrammes 20-24 dp et même épaisseur de trait.
- Sémantique stable : lune=sommeil, pas=activité, cœur=cardio, batterie/bouclier=récupération, haltère=séance, courbe=suivi, rouage=paramètres.
- Icône et petit libellé ensemble, avec description TalkBack.
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
