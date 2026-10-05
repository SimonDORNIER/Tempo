# Tempo

Application Android d'exercices, mobilité et entraînement.

## Données utilisateur

Les exercices personnalisés sont stockés dans IndexedDB (`tempo-offline`) et les réglages dans `localStorage`.
Le package Android reste `fr.tempo.sport` et toutes les versions officielles utilisent la même clé de signature : les mises à jour s'installent donc par-dessus l'application sans supprimer les données locales.

## Publication d'une version

1. Modifier le code.
2. Créer un tag `vX.Y.Z`.
3. GitHub Actions compile, signe et publie automatiquement `Tempo-X.Y.Z.apk` dans Releases.

L'application consulte la dernière Release GitHub au démarrage et affiche un bouton lorsqu'une version plus récente est disponible.

## Secrets requis

- `TEMPO_SIGNING_KEY_B64`
- `TEMPO_SIGNING_CERT_B64`

Ne jamais ajouter la clé de signature directement au dépôt.
