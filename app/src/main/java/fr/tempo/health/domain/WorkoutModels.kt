package fr.tempo.health.domain

enum class ExerciseCategory {
    WARMUP,
    FULL_BODY,
    UPPER_BODY,
    CORE,
    LOWER_BODY,
    MOBILITY,
    STRETCHING
}

data class Exercise(
    val id: String,
    val name: String,
    val category: ExerciseCategory,
    val defaultWorkSeconds: Int,
    val instructions: String,
    val cue: String
)

data class WorkoutExercise(
    val exercise: Exercise,
    val workSeconds: Int = exercise.defaultWorkSeconds,
    val restSeconds: Int = 15
)

data class WorkoutPlan(
    val title: String,
    val intensity: Int,
    val items: List<WorkoutExercise>
) {
    val estimatedMinutes: Int
        get() {
            val totalSeconds = items.sumOf {
                5 + it.workSeconds + it.restSeconds
            }
            return ((totalSeconds + 59) / 60).coerceAtLeast(1)
        }
}

object ExerciseLibrary {
    private fun exercise(
        id: String,
        name: String,
        category: ExerciseCategory,
        seconds: Int,
        instructions: String,
        cue: String
    ) = Exercise(id, name, category, seconds, instructions, cue)

    val all: List<Exercise> = listOf(
        exercise(
            "march-place",
            "Marche dynamique",
            ExerciseCategory.WARMUP,
            45,
            "Marche sur place en montant progressivement les genoux. Balance les bras naturellement et garde le buste haut.",
            "Respire régulièrement et commence tranquillement."
        ),
        exercise(
            "jumping-jacks",
            "Jumping jacks",
            ExerciseCategory.WARMUP,
            40,
            "Écarte simultanément les pieds en levant les bras, puis reviens au centre. Garde des appuis souples.",
            "Cherche un rythme fluide plutôt que la vitesse."
        ),
        exercise(
            "arm-circles",
            "Cercles d'épaules",
            ExerciseCategory.MOBILITY,
            40,
            "Bras relâchés ou légèrement ouverts, dessine de grands cercles contrôlés avec les épaules dans les deux sens.",
            "Évite de hausser les épaules vers les oreilles."
        ),
        exercise(
            "squats",
            "Squats",
            ExerciseCategory.LOWER_BODY,
            45,
            "Pieds environ largeur d'épaules. Recule les hanches comme pour t'asseoir, genoux dans l'axe des pieds, puis remonte.",
            "Garde le poids réparti sur tout le pied et le dos long."
        ),
        exercise(
            "reverse-lunges",
            "Fentes arrière alternées",
            ExerciseCategory.LOWER_BODY,
            45,
            "Recule un pied, descends verticalement sans forcer l'amplitude, puis pousse dans le pied avant pour revenir.",
            "Le genou avant reste orienté dans l'axe du pied."
        ),
        exercise(
            "glute-bridge",
            "Pont fessier",
            ExerciseCategory.LOWER_BODY,
            45,
            "Allongé sur le dos, genoux fléchis et pieds au sol. Serre les fessiers pour lever le bassin puis redescends lentement.",
            "Monte avec les hanches, sans cambrer exagérément le bas du dos."
        ),
        exercise(
            "calf-raises",
            "Montées sur pointes",
            ExerciseCategory.LOWER_BODY,
            40,
            "Debout, monte lentement sur la pointe des pieds puis redescends en contrôlant. Utilise un support si nécessaire.",
            "Reste grand et évite de rebondir."
        ),
        exercise(
            "wall-pushups",
            "Pompes au mur",
            ExerciseCategory.UPPER_BODY,
            40,
            "Mains au mur un peu plus larges que les épaules. Plie les coudes pour rapprocher le buste du mur puis repousse.",
            "Le corps reste gainé de la tête aux talons."
        ),
        exercise(
            "pushups",
            "Pompes",
            ExerciseCategory.UPPER_BODY,
            40,
            "Mains sous ou légèrement plus larges que les épaules. Descends le corps en bloc puis repousse. Pose les genoux si nécessaire.",
            "Choisis une variante qui te permet de garder une exécution propre."
        ),
        exercise(
            "scapular-pushups",
            "Pompes scapulaires",
            ExerciseCategory.UPPER_BODY,
            40,
            "En position de planche ou contre un mur, garde les coudes tendus. Rapproche puis éloigne doucement les omoplates.",
            "Le mouvement vient des omoplates, pas des coudes."
        ),
        exercise(
            "dead-bug",
            "Dead bug",
            ExerciseCategory.CORE,
            45,
            "Sur le dos, hanches et genoux fléchis. Éloigne lentement bras et jambe opposés puis reviens et alterne.",
            "Garde le bas du dos stable et réduis l'amplitude si nécessaire."
        ),
        exercise(
            "bird-dog",
            "Bird dog",
            ExerciseCategory.CORE,
            45,
            "À quatre pattes, tends bras et jambe opposés sans tourner le bassin, reviens puis alterne.",
            "Imagine un verre posé sur ton bassin : il ne doit pas se renverser."
        ),
        exercise(
            "forearm-plank",
            "Planche avant",
            ExerciseCategory.CORE,
            35,
            "Appui sur les avant-bras et les pieds, ou sur les genoux. Aligne tête, tronc et bassin et maintiens sans bloquer la respiration.",
            "Arrête la série avant de perdre la position."
        ),
        exercise(
            "cat-cow",
            "Dos rond / dos creux",
            ExerciseCategory.MOBILITY,
            45,
            "À quatre pattes, alterne lentement l'arrondi puis l'ouverture de la colonne en suivant ta respiration.",
            "Cherche la fluidité, sans forcer les amplitudes."
        ),
        exercise(
            "thoracic-rotation",
            "Rotation thoracique",
            ExerciseCategory.MOBILITY,
            45,
            "À quatre pattes, place une main derrière la tête puis ouvre le coude vers le plafond avant de revenir. Change de côté à mi-parcours.",
            "Le bassin reste aussi stable que possible."
        ),
        exercise(
            "hip-90-90",
            "Transitions 90/90",
            ExerciseCategory.MOBILITY,
            45,
            "Assis, genoux fléchis, fais basculer doucement les jambes d'un côté puis de l'autre en gardant le mouvement contrôlé.",
            "Réduis l'amplitude si les hanches tirent trop."
        ),
        exercise(
            "ankle-rocks",
            "Mobilité de cheville",
            ExerciseCategory.MOBILITY,
            40,
            "En fente courte face à un mur, avance doucement le genou au-dessus des orteils sans décoller le talon. Alterne les côtés.",
            "Le talon reste lourd au sol."
        ),
        exercise(
            "hip-flexor-stretch",
            "Étirement fléchisseur de hanche",
            ExerciseCategory.STRETCHING,
            40,
            "En fente à genou ou debout, rentre légèrement le bassin puis avance jusqu'à sentir un étirement doux devant la hanche arrière.",
            "Pas de douleur : cherche une tension légère et stable."
        ),
        exercise(
            "hamstring-hinge",
            "Étirement ischio-jambiers",
            ExerciseCategory.STRETCHING,
            40,
            "Place un pied légèrement devant, jambe presque tendue. Recule les hanches en gardant le dos long jusqu'à une tension douce.",
            "Ne cherche pas à toucher le sol."
        ),
        exercise(
            "chest-opener",
            "Ouverture de poitrine",
            ExerciseCategory.STRETCHING,
            40,
            "Avant-bras contre un mur ou mains derrière le dos, ouvre doucement la poitrine sans cambrer fortement.",
            "Garde les épaules basses et respire lentement."
        ),
        exercise(
            "child-pose-reach",
            "Posture de l'enfant avec portée",
            ExerciseCategory.STRETCHING,
            45,
            "Depuis quatre pattes, recule les hanches vers les talons et allonge les bras. Décale légèrement les mains pour étirer chaque côté.",
            "Laisse la respiration relâcher progressivement le dos."
        )
    )

    fun byId(id: String): Exercise =
        all.first { it.id == id }
}

object WorkoutPlanner {
    fun build(recovery: RecoveryResult): WorkoutPlan =
        when (recovery.level) {
            RecoveryLevel.GREEN -> WorkoutPlan(
                title = "Full body + mobilité",
                intensity = recovery.intensity,
                items = listOf(
                    item("march-place", 45, 10),
                    item("squats", 45, 15),
                    item("reverse-lunges", 45, 15),
                    item("pushups", 40, 20),
                    item("dead-bug", 45, 15),
                    item("glute-bridge", 45, 15),
                    item("bird-dog", 45, 15),
                    item("thoracic-rotation", 45, 10),
                    item("hip-flexor-stretch", 40, 0)
                )
            )

            RecoveryLevel.ORANGE -> WorkoutPlan(
                title = "Activation + mobilité",
                intensity = recovery.intensity,
                items = listOf(
                    item("march-place", 45, 10),
                    item("squats", 40, 20),
                    item("wall-pushups", 40, 15),
                    item("glute-bridge", 40, 15),
                    item("dead-bug", 40, 15),
                    item("cat-cow", 45, 10),
                    item("hip-90-90", 45, 0)
                )
            )

            RecoveryLevel.RED -> WorkoutPlan(
                title = "Mobilité + récupération",
                intensity = recovery.intensity,
                items = listOf(
                    item("cat-cow", 45, 10),
                    item("thoracic-rotation", 45, 10),
                    item("hip-90-90", 45, 10),
                    item("ankle-rocks", 40, 10),
                    item("hip-flexor-stretch", 40, 10),
                    item("child-pose-reach", 45, 0)
                )
            )

            RecoveryLevel.UNKNOWN -> WorkoutPlan(
                title = "Mobilité douce",
                intensity = 2,
                items = listOf(
                    item("march-place", 40, 10),
                    item("cat-cow", 40, 10),
                    item("thoracic-rotation", 40, 10),
                    item("hip-90-90", 40, 0)
                )
            )
        }

    private fun item(
        id: String,
        workSeconds: Int,
        restSeconds: Int
    ): WorkoutExercise =
        WorkoutExercise(
            exercise = ExerciseLibrary.byId(id),
            workSeconds = workSeconds,
            restSeconds = restSeconds
        )
}
