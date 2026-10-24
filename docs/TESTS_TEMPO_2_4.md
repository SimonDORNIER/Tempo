# Tempo Coach 2.4.0 — Statistiques détaillées

## Données et migration
- [ ] Mise à jour 2.3.1 → 2.4.0 sans désinstallation (package fr.tempo.health inchangé)
- [ ] Room migration 3 → 4 sans perte de séances, d'exercices personnalisés ou de check-in
- [ ] Sauvegarde Google Drive : nouveaux champs inclus et ancienne sauvegarde toujours restaurable
- [ ] Santé Connect : autorisations SpO₂ et température demandées uniquement si supportées
- [ ] Autorisations partielles, capteurs absents et pas de nuit mesurée : pas de valeur 0 inventée
- [ ] La synchronisation fonctionne si la température cutanée est indisponible sur le téléphone

## Affichage
- [ ] Aujourd'hui : nuit du jour uniquement, durée réelle et efficacité uniquement si calculable
- [ ] Aujourd'hui : ligne cœur / respiration, valeurs manquantes affichées « — »
- [ ] Santé : sélection de jour et affichage sommeil, éveil, profond, léger, REM, efficacité
- [ ] Sommeil : phases affichées sur une frise, période au lit vs sommeil, score Fitbit non inventé
- [ ] Pouls : moyenne, min, max, repos, nocturne, VFC
- [ ] Respiration, SpO₂ et température cutanée : unités et réserves explicites
- [ ] Tendances : 7/28 jours sans interpolation des jours manquants
- [ ] Pixel 7a : textes, contraste Option B, navigation et grands caractères

## Régression
- [ ] Accueil / Pratiquer / Suivi / Moi, coach, minuteur, rappels, sauvegarde et mises à jour
- [ ] Signature Android identique à la version installée
- [ ] Le déclenchement GitHub Actions produit l'APK signé et l'attache à health-v2.4.0

## Méthodologie
L'efficacité du sommeil = durée endormie / durée de session, seulement lorsque les stades classifiés couvrent au moins 70 % de la session. Ce n'est pas un score de qualité Fitbit et ne doit pas être présenté comme un diagnostic médical.
