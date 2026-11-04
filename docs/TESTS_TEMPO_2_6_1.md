# Tests Tempo Coach 2.6.1 — Sons audibles et cycle personnalisable

## Son et débogage
1. Installer par-dessus la 2.6.0, sans perte d'exercices, historique, santé et personnalisation.
2. Sur le Pixel 7a, vérifier le **volume multimédia** et la sortie utilisée. Au premier usage sans préférence déjà définie, les sons sont actifs à 70 %. Une préférence explicite Désactivé doit rester désactivée.
3. Ouvrir Respiration → haut-parleur → **Écouter** pour l'inspiration puis l'expiration. Les tests d'aperçu sont audibles même si les sons de séance sont désactivés. Vérifier route haut-parleur puis Bluetooth.
4. Tester sons courts (bip au début de chaque phase) puis continus (tonalité toute la durée de l'inspiration/expiration). Les pauses produisent deux bips graves au départ, puis sont silencieuses.
5. Pendant exercice, couper/rétablir en un toucher. Pause, sortie, fin, changement de réglages : pas de son résiduel ni de superposition.
6. Si le lecteur ne parvient pas à démarrer, un message explicite doit apparaître plutôt que rester silencieux.

## Durées
7. Tester chaque respiration préinstallée : choisir 2/3/5/10 min ou modifier minute par minute de 1 à 60 min. Vérifier décompte et enregistrement historique au bon temps.
8. Passer d'un exercice à l'autre puis revenir : chaque durée choisie reste mémorisée.
9. Sélectionner **Respiration personnalisée**. Saisir 4/2/6/2 (inspiration / pause / expiration / pause) : décompte de phases de 4 s, 2 s, 6 s, 2 s puis nouveau cycle; sons distinctifs pour les pauses.
10. Régler les deux pauses à 0 : passer directement de l'inspiration à l'expiration, puis nouveau cycle. Tester inspiration à 0 avec expiration positive et l'inverse.
11. Régler toutes les phases à 0 : DÉMARRER désactivé et message explicatif. Aucun plantage/modulo par zéro.
12. Tester la saisie numérique et les boutons +/- aux limites 0 et 60 s ; 0 signifie « phase ignorée ». La durée totale de séance reste au minimum 1 min.
13. Quitter/réouvrir l'application : profil personnalisé et durées conservés. Sauvegarder/restaurer via la fonction existante, puis vérifier leur restitution.
14. Vérifier les autres écrans, la navigation 4 onglets et les timers d'entraînement existants.
