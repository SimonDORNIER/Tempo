# Tests Tempo Coach 2.6.2 — Sons apaisants

## Sur Pixel 7a : écouter et comparer

1. Installer directement sur Tempo Coach 2.6.1 sans désinstaller ; conserver l’historique et les sons/hauteurs déjà personnalisés.
2. **Pratiquer → Respiration → icône haut-parleur** : sélectionner **Carillons**. Tester « Écouter » pour inspiration puis expiration : son doux et rond, environ 0,9 seconde, qui commence et se termine progressivement, sans clic ni bip strident.
3. Sélectionner **Fluides** : les aperçus durent environ 1,7 seconde ; le son est discret, stable, non agressif, avec fondu d’entrée et de sortie.
4. Faire une séance de 4 s inspiration / 2 s pause / 6 s expiration / 2 s pause : les phases respiratoires sont différenciées par leurs hauteurs, les deux pauses par deux petites clochettes graves, sans détonation ni souffle bruyant.
5. Tester les 3 timbres Doux / Pur / Clair ainsi que les différentes hauteurs : pas de distorsion même à volume élevé ; la version « Clair » reste douce.
6. Tester sons continus sur une expiration de 10 s : pas d’arrêt anticipé et fondu à la fin de la phase. Tester l’arrêt, la pause, le changement de mode et la coupure rapide : extinction immédiate sans maintien audio.
7. Préécoute même avec les sons des séances désactivés ; aucune régression du volume multimédia Android ni de la sortie Bluetooth.
8. Régler 0 s pour une pause du cycle personnalisé, vérifier que la phase est ignorée sans carillon supplémentaire.
9. Vérifier le minuteur d’effort classique, Santé Connect, les 4 onglets, la sauvegarde Drive et les exercices personnels : inchangés.

## Tests unitaires

- `BreathingSoundEnvelopeTest` contrôle la durée des carillons (~900 ms), les gains nuls aux extrémités, la montée et l’extinction des nappes et le silence entre les deux clochettes de pause.
- `BreathingPatternTest` reste en place pour les 4 phases et les phases à 0 seconde.

Le caractère relaxant d’un son est subjectif : confirmer le rendu sur les vrais haut-parleurs du Pixel 7a avant validation finale.
