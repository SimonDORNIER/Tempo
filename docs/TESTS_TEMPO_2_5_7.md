# Tempo Coach 2.5.7 — Couleurs d'état à l'accueil

## Comportement

- **Forme & préparation** : le score numérique, le cercle et la carte utilisent le même état issu du moteur de récupération ; pas de score simulé quand la fiabilité manque.
- **Sommeil** : toujours la nuit correspondant au réveil du jour, pas une moyenne. Durée comparée à la médiane des jours de référence (cinq observations minimum), et efficacité de sommeil affichée séparément si effectivement mesurée. Vert habituel, ambre nuit plus courte, rouge nuit nettement raccourcie, gris sans référence/mesure.
- **Pas du jour** : état gris « Journée en cours » pendant la journée, quel que soit le compteur actuel. L'icône garde l'identité vert doux.
- **FC repos et VFC** : application exacte des seuils de Suivi ; indicateur très visible par fond, bordure, chiffre et pastille. L'icône cardio reste rose.
- **Santé & activité** : respiration avec pastille sémantique ; minutes d'activité aujourd'hui laissées neutres en attendant la fin du jour.
- Aucune donnée ajoutée ni modification de Santé Connect, Room, sauvegardes, permissions, package ou signature. Cartes de 162 dp et navigation à quatre onglets inchangées.

## Tests

- [ ] Compiler `app:testDebugUnitTest` et `app:assembleRelease`
- [ ] Comparer les mêmes jours entre Aujourd'hui, Suivi et Détails : mêmes valeurs = mêmes états
- [ ] Référence présente : voir Sommeil en vert/ambre/rouge selon durée, FC repos et VFC avec les bons sens de variation
- [ ] Référence absente : chiffres et bordures gris, aucune fausse alerte
- [ ] Pas à 838 en journée : gris « Journée en cours », pas rouge
- [ ] Vérifier la carte « Respiration », y compris en absence de mesure
- [ ] Vérifier les quatre cartes côte à côte sur Pixel 7a, textes à taille normale et agrandie
- [ ] Vérifier la mise à jour en conservant l'historique, les exercices et les préférences
- [ ] Vérifier qu'un APK signé 2.5.7 est publié et installable par-dessus 2.5.6
