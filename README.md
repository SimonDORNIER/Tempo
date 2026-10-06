# TempoSave 1.10.1

Sauvegarde stable de Tempo avant la refonte santé / entraînement.

## Identité Android

- Nom affiché : `TempoSave`
- Package conservé : `fr.tempo.sport`
- Signature conservée : secrets GitHub `TEMPO_SIGNING_KEY_B64` / `TEMPO_SIGNING_CERT_B64`
- Version figée : `1.10.1`

Le package est volontairement conservé pour qu'une installation de TempoSave remplace l'ancien Tempo sans perdre ses données locales. La future application utilisera un nouvel applicationId et pourra donc être installée en parallèle.

## Données utilisateur

Les exercices personnalisés restent dans IndexedDB (`tempo-offline`) et les réglages dans `localStorage`.

## Build de sauvegarde

Le workflow `.github/workflows/release.yml` de cette branche produit et signe `TempoSave-1.10.1.apk`.
Le tag dédié est `temposave-v1.10.1`.

Le mécanisme de mise à jour natif est figé sur ce tag TempoSave afin que cette sauvegarde ne suive pas les futures versions de la nouvelle application.
