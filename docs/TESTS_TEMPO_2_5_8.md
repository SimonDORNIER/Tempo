# Tempo Coach 2.5.8 — Aujourd'hui accessible depuis tous les onglets

## Correction
- Le tap sur l'icône « Aujourd'hui » dans la barre inférieure appelle d'abord `popBackStack("today", inclusive = false)` : aucune restauration d'un ancien écran Suivi.
- Si la racine « today » n'existe pas dans la pile, navigation de secours sans `restoreState`.
- Les trois autres onglets conservent la conservation de leur position. Tous les écrans, données de santé, couleurs de la version 2.5.7, exercices, minuteur et sauvegardes sont inchangés.

## Vérifications

- [ ] Depuis Suivi → Aujourd'hui : rester sur le titre Aujourd'hui avec la carte Forme et les mesures.
- [ ] Depuis Pratiquer → Aujourd'hui, puis Moi → Aujourd'hui : afficher Aujourd'hui.
- [ ] Depuis Aujourd'hui → Suivi → Aujourd'hui à plusieurs reprises : aucun renvoi automatique à Suivi.
- [ ] Depuis Aujourd'hui → carte Sommeil ou Pas : Suivi s'ouvre volontairement ; appuyer ensuite sur Aujourd'hui pour revenir à l'accueil.
- [ ] Depuis Aujourd'hui → carte FC repos / VFC : Détails santé s'ouvre ; revenir à l'accueil.
- [ ] Réinstallation par-dessus la 2.5.7 : aucune perte de données.
- [ ] Tests unitaires Android, compilation release, signature et publication réussies.
