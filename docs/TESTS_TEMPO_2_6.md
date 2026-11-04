# Tests Tempo Coach 2.6.0 — Sons de respiration

## Installation et navigation
1. Installer l'APK par-dessus la 2.5.9 : vérifier conservation des séances, exercices personnalisés, couleurs, réglages et données Santé Connect.
2. Dans **Pratiquer**, le bloc **Respiration guidée** est distinct de **Mouvement** (mobilité, étirements, renforcement).
3. Les cinq respirations existantes restent disponibles. Depuis la bibliothèque d'exercices, une respiration n'utilise pas le minuteur d'effort classique.

## Sons (volume téléphone activé)
4. Avant démarrage, ouvrir l'icône de haut-parleur : la fenêtre permet Activer, **Courts / Continus**, volume, hauteurs et timbres séparés inspiration/expiration, et **Écouter**.
5. En mode **Courts**, vérifier un signal bref au début de chaque inspiration puis expiration. Aucune répétition toutes les secondes.
6. En mode **Continus**, un son démarre au début de l'inspiration et dure son intervalle, puis le son d'expiration dure l'intervalle suivant (ex. 4 s / 6 s).
7. Modifier les deux hauteurs en sens opposé, tester **Doux**, **Pur**, **Clair** et les boutons d'aperçu. Vérifier le volume de 0 à 100 %.
8. Sur l'écran de respiration, utiliser **Activer / Couper** sans ouvrir la fenêtre : le son s'arrête immédiatement et reprend sur la phase courante à la réactivation.
9. Pendant une phase longue, modifier le volume, la hauteur ou le timbre : le nouveau réglage prend effet pendant la séance. Vérifier absence de superposition des sons.
10. PAUSE : silence. REPRENDRE : reprise du son sur le temps restant. ARRÊTER et Retour confirmé : arrêt du son et comportement d'historique inchangé. À la fin de la séance : silence immédiat.
11. Sortir de la respiration et la rouvrir : réglages conservés. Tester sauvegarde complète puis restauration sur un autre appareil : les sept champs audio sont restaurés.

## Régressions
12. Exercices de mobilité/étirement/renforcement et minuteur libre : leurs sons et réglages de son existants restent inchangés.
13. Test volume média Android muet, sortie audio Bluetooth, écran verrouillé, changement d'appli : pas de plantage; aucune lecture persistante après abandon de l'écran.
14. Affichage Pixel 7a et police agrandie : popup défilante et icône à portée de doigt, aucun élément essentiel masqué.
