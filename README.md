# Tempo Health

Application Android personnelle santé / récupération / entraînement.

## Version 0.3.0

Cette version ajoute le premier moteur de récupération local :

- Santé Connect + synchronisation locale 28 jours ;
- références personnelles 7 et 28 jours ;
- check-in quotidien énergie 1–5 et douleurs 0–3, enregistré dans Room ;
- score de récupération local sur 100 ;
- niveau 🟢 / 🟠 / 🔴 ;
- prise en compte du sommeil, de la HRV, de la FC repos, de la charge sportive récente et du ressenti ;
- recommandation automatique de séance, durée et intensité ;
- fonctionnement du moteur sans Internet et sans IA.

La prochaine étape est le portage du moteur Tempo : exercices, séances, timers, repos, sons et historique d'entraînement.

Package Android : `fr.tempo.health`.
TempoSave reste indépendant sous `fr.tempo.sport`.
