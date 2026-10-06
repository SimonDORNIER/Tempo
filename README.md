# Tempo Health

Application Android personnelle santé / récupération / entraînement.

## Version 0.2.0

Cette version ajoute le premier vrai socle Santé Connect :

- intégration native Health Connect en lecture ;
- permissions sommeil, cœur, HRV, respiration, pas, distance, calories, entraînements, poids et VO₂ max ;
- synchronisation des 28 derniers jours ;
- stockage local Room / SQLite ;
- agrégation des pas, distance, calories et FC repos pour limiter les doubles comptages ;
- récupération de la nuit principale et de ses stades ;
- FC moyenne pendant le sommeil ;
- premiers écrans Santé / Aujourd'hui alimentés par les données réelles ;
- premières références personnelles 7 et 28 jours.

Les données restent locales dans cette version.

Package Android : `fr.tempo.health`.
TempoSave reste indépendant sous `fr.tempo.sport`.
