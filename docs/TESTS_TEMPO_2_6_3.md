# Tests Tempo Coach 2.6.3 — Son de pause indépendant

## Réglages et combinaisons

1. Mettre à jour l'app sur le Pixel 7a sans désinstaller la 2.6.2 : vérifier que les hauteurs, timbres, volumes et modes d'inspiration/expiration sont conservés.
2. Ouvrir `Pratiquer → Respiration → haut-parleur`. Le panneau doit contenir deux parties : **Inspiration et expiration** et **Pauses**.
3. Configurer respiration = **Fluides (continu)**, pauses = **Court**, durée de pause = 2 s : entendre la nappe pendant inspiration/expiration, puis deux carillons graves au début de chaque pause suivis de silence.
4. Configurer respiration = **Carillons (court)**, pauses = **Continu** : les sons d'inspiration/expiration ne se déclenchent qu'une fois, mais la nappe feutrée des pauses couvre toute leur durée.
5. Tester les deux autres combinaisons, Court/Court et Continu/Continu. Changer le mode des pauses ne doit pas changer le mode de la respiration, et inversement.
6. Régler **volume pauses = 0** : les pauses doivent être silencieuses sans alerte d'erreur audio ; le son de respiration reste audible. Remettre du volume puis tester l'inverse (respiration = 0 et pauses > 0).
7. Personnaliser les hauteurs et timbres des deux respirations, puis indépendamment ceux des pauses. Vérifier que les changements ne s'appliquent qu'au bon type de phase.
8. Cliquer **Écouter** dans la section « Son des pauses » : entendre le timbre de pause même si les pauses du profil choisi sont à 0 s et même si l'interrupteur des sons de séance est coupé.
9. Configurer une pause à 0 s : cette phase n'existe pas et ne déclenche aucun son ; l'autre pause, non nulle, utilise le profil pause.
10. Tester des pauses de 1, 3 et 10 s en mode continu : la tonalité de pause doit rester pendant toute la pause, avec attaque et extinction douces ; aucune superposition en transition.
11. Activer et couper le son pendant une pause, puis pause/reprise du timer, quitter, terminer et relancer : pas de son restant après arrêt.
12. Vérifier que les réglages de pauses restent après fermeture/réouverture et après sauvegarde/restauration, ainsi que les exercices et mesures de santé.

## Tests unitaires

- `BreathingPauseSoundTest` : sélection du bon mode/volume/hauteur/timbre par phase et durée des pauses courte/continue, fondus.
- `BreathingSoundEnvelopeTest` : signal court composé de deux carillons avec silence intermédiaire, enveloppes douces.
- `BreathingPatternTest` : phases ignorées lorsque la durée vaut 0.

Les tests automatiques ne garantissent pas la qualité perçue du son : valider le rendu avec les haut-parleurs du Pixel 7a.
