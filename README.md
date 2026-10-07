# Tempo Health

Application Android personnelle santé / récupération / entraînement.

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
