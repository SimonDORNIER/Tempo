# Tempo Coach 2.1 — Validation de la refonte

Statut : contrôle des sources et build CI en cours. Les étapes ci-dessous doivent être testées sur un Pixel 7a avant de conclure qu'une version est sans défaut.

## Apparence et navigation
- [ ] Barre basse à **4 onglets** exactement : Aujourd'hui / Pratiquer / Suivi / Moi.
- [ ] Aucun fond illustré ; palette sombre Prune / Rose poudré, CTA crème, sauge pour la forme.
- [ ] Pas de nom ni d'icône tronqués en format de police Android normal ou agrandi (1,3× et 1,5×).
- [ ] La navigation Android Retour ramène au bon onglet ; scroll conservé si possible.
- [ ] Pendant une séance, barre basse masquée.
- [ ] Tous les liens d'un onglet ouvrent une page ou une action réelle.

## Aujourd'hui
- [ ] Quatre indicateurs affichés : nuit récente, score Forme et préparation Tempo, pas et activité.
- [ ] Score associé à sa fiabilité, jamais valeur inventée ; pas du jour vraiment lié au jour courant.
- [ ] Sans données, affiche « — », pas zéro ni score propriétaire fictif.
- [ ] Réactualisation Santé Connect avec permissions partielles ou refusées.
- [ ] Saisie énergie/douleurs actualise le score.
- [ ] Coche des trois habitudes puis fermeture/réouverture : les coches persistent.
- [ ] Mini-graphes 7 jours montrent les jours sans mesures.

## Pratiquer
- [ ] Bouton Démarrer lance la séance conseillée.
- [ ] Respiration ouvre cinq exercices réels, chrono, pause, arrêt avec confirmation.
- [ ] Mobilité / Étirements / Renforcement ouvrent des exercices consultables, sans les modifier.
- [ ] Démarrer un exercice ouvre la séance dédiée.
- [ ] Minuteur libre et programme conservés.
- [ ] Sortie de séance confirme les pertes éventuelles.
- [ ] Sons début/milieu/fin toujours ajustables.

## Suivi
- [ ] La page santé détaillée s'ouvre et les graphiques restent interactifs.
- [ ] L'historique de séance et le suivi d'activité restent visibles.
- [ ] Absence de valeurs = jours vides (pas d'interpolation inventée).

## Moi, données et sécurité
- [ ] Exercice personnel créé puis Enregistrer reste présent après redémarrage.
- [ ] Annuler l'édition n'enregistre rien ; confirmation avant suppression/reset/restauration.
- [ ] Personnalisation catégorie/exercices, rappels, volumes son et paramètres disponibles.
- [ ] Sauvegarde Google Drive puis restauration testées sur copie de test, sans perte.
- [ ] Mise à jour depuis Tempo Coach 2.0.2 **sans désinstallation**.
- [ ] Vérification du package `fr.tempo.health`, de la signature et de la version 2.1.0.

## Limites
- Impossible de garantir la perfection sur appareil sans exécution ni inspection visuelle réelle.
- Les écrans secondaires existants héritent de la nouvelle palette ; des détails de mise en page pourront encore être harmonisés.
- L'historique des habitudes sur 7/28 jours nécessite une évolution de stockage distincte.

## Régression 2.3.1 — accueil centré sur la journée
- Avec une nuit enregistrée et terminée ce matin : carte « Sommeil » = durée de cette nuit, jamais une moyenne 7 jours.
- Si le sommeil de ce matin est absent mais celui d'hier existe : carte « Sommeil » = « — », sans reprise de l'ancienne valeur.
- Les pas et minutes d'activité ne concernent que la date locale du jour ; si absents, « — ».
- L'accueil ne présente plus de moyenne / mini-tendances 7 jours ; l'historique et les graphiques sont conservés dans « Suivi ».
- Vérifier navigation, synchronisation Santé Connect, récupération, données locales et mise à jour de la version précédente sans désinstaller.
