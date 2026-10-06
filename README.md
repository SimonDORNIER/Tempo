# Tempo Health

Nouvelle application santé / entraînement issue du projet Tempo.

## Version actuelle

Fondation native Android **0.1.0**.

- Kotlin + Jetpack Compose
- package Android : `fr.tempo.health`
- nom affiché : **Tempo Health**
- Android SDK 36
- fonctionnement hors ligne pour le socle
- navigation : Aujourd'hui, Santé, Entraînement, Progression, Coach, Paramètres

Cette application peut être installée en parallèle de **TempoSave**, qui conserve le package historique `fr.tempo.sport`.

## Architecture cible

Santé Connect → base locale → références personnelles → récupération → moteur de décision → séance → moteur Tempo → historique.

La couche ChatGPT restera optionnelle et ne sera jamais requise pour le fonctionnement principal.

## Ancien Tempo

Les anciens fichiers Capacitor / `dist` sont temporairement conservés sur cette branche comme référence pendant le portage du moteur Tempo et de la bibliothèque d'exercices.

## Build

Le workflow GitHub Actions compile une application Android native, la signe avec les secrets existants et publie un APK dans les Releases.

Tag de cette version : `health-v0.1.0`.
