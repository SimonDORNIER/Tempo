# Tempo Coach (anciennement Tempo Health)

Application Android personnelle santé / récupération / entraînement.

## Références de conception pour les prochaines versions

- **[Charte graphique et UX](docs/CHARTE_GRAPHIQUE_TEMPO_COACH.md)** : style santé/détente et palette Prune & Rose poudré, sans image de fond, interface compacte Pixel 7a.
- **[Bibliothèque de composants](docs/COMPONENTS_TEMPO_COACH.md)** : spécification des boutons, cartes, habitudes, mini-graphes et dialogues.
- **[Feuille de route et backlog](docs/ROADMAP_TEMPO_COACH.md)** : écran d'accueil, graphiques, coaching, sécurité et tests à discuter pour les versions suivantes.
- **[Instructions de développement](AGENTS.md)** : règles à respecter dans les futurs développements.

La version Android publiée est Tempo Coach 2.0.0 ; les notes historiques 0.6.1 ci-dessous restent conservées pour référence.

## Version 0.6.1

Cette version rétablit la mise à jour automatique de Tempo Health.

### Mise à jour au lancement

À chaque lancement, Tempo Health :

1. interroge les Releases GitHub publiques du dépôt ;
2. ne considère que les tags `health-v...` ;
3. compare la dernière version avec la version installée ;
4. propose la mise à jour si une version plus récente existe ;
5. télécharge l'APK officiel signé ;
6. vérifie que l'APK correspond bien au package `fr.tempo.health` et à la version attendue ;
7. ouvre l'installateur Android.

Au premier usage, Android peut demander d'autoriser Tempo Health à installer des applications inconnues. Une fois cette autorisation donnée, les mises à jour suivantes sont beaucoup plus directes.

TempoSave reste figé et n'est plus concerné par les nouveaux développements.

## Fonctionnalités existantes

- Santé Connect et stockage local ;
- score de récupération ;
- check-in énergie / douleurs ;
- moteur Tempo natif ;
- historique des séances ;
- programmation adaptative ;
- durée disponible ;
- matériel ;
- favoris / exercices à éviter ;
- fonctionnement local sans serveur obligatoire.

Package Android : `fr.tempo.health`.
