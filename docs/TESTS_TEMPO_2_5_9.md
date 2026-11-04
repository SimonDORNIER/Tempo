# Tests Tempo Coach 2.5.9 — Synchronisation automatique

## Sur Pixel 7a / Android

1. Installer l'APK **par-dessus** la 2.5.8 : retrouver les exercices personnels, routines, historiques, sauvegardes et permissions existantes.
2. Ouvrir **Moi > Santé Connect** (écran Santé). Avec les accès usuels accordés, vérifier la nouvelle carte « Synchronisation automatique » et le bouton de demande d'accès en arrière-plan.
3. Autoriser l'accès en arrière-plan dans Santé Connect : vérifier que la carte indique « Toutes les 2 heures environ ». Refuser puis réessayer : l'app doit continuer à fonctionner.
4. Faire « Synchroniser 28 jours » une fois si besoin ; fermer complètement l'interface par les applications récentes, mais **ne pas forcer l'arrêt** depuis les paramètres Android. Patienter plus de 2 h en conditions normales : la date de dernière synchro doit se renouveler lorsqu'une tâche est exécutée.
5. Rouvrir Tempo Coach : les mesures présentes en base s'affichent dès l'accueil, même hors ligne ; aucune relance automatique de 28 jours lorsque le cache est déjà rempli.
6. Faire une mesure récente via Santé Connect (pas/FC) : une tâche périodique doit la récupérer à son prochain passage ; si Android diffère l'exécution, l'ouverture de l'app effectue le rattrapage lorsque les données ont plus de 2 h.
7. Retirer l'autorisation de lecture en arrière-plan : la tâche doit cesser de lire sans erreur, garder les valeurs locales et permettre toujours le bouton de synchronisation manuelle.
8. Révoquer une donnée facultative (ex. SpO₂) ; vérifier que les autres données autorisées continuent à se synchroniser.
9. Vérifier Aujourd'hui (nuit terminée ce matin et pas du jour), Suivi, Pratiquer et Moi : navigation et couleurs 2.5.8 conservées.
10. Vérifier le cas Santé Connect indisponible : pas de crash, valeurs locales conservées.

**Limite Android** : WorkManager n'est pas une alarme précise ; économie d'énergie, Doze et arrêt forcé peuvent différer ou empêcher les passages. Aucun service permanent ni notification ne doit être nécessaire.
