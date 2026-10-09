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

enum class Equipment {
    MAT,
    CHAIR
}

data class TrainingPreferences(
    val durationMinutes: Int = 20,
    val availableEquipment: Set<Equipment> = setOf(Equipment.MAT, Equipment.CHAIR),
    val favoriteExerciseIds: Set<String> = emptySet(),
    val avoidedExerciseIds: Set<String> = emptySet(),
    val disabledCategories: Set<ExerciseCategory> = emptySet(),
    val categoryIcons: Map<ExerciseCategory, String> = emptyMap(),
    val exerciseWorkSeconds: Map<String, Int> = emptyMap(),
    val exerciseOverrides: Map<String, Exercise> = emptyMap(),
    val customExercises: List<Exercise> = emptyList()
)

data class Exercise(
    val id: String,
    val name: String,
    val category: ExerciseCategory,
    val defaultWorkSeconds: Int,
    val instructions: String,
    val cue: String,
    val requiredEquipment: Equipment? = null
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

data class WorkoutHistoryHint(
    val startedAtEpochMs: Long,
    val muscleGroups: Set<ExerciseCategory>,
    val perceivedDifficulty: Int?
)

object ExerciseLibrary {
    private fun exercise(
        id: String,
        name: String,
        category: ExerciseCategory,
        seconds: Int,
        instructions: String,
        cue: String,
        equipment: Equipment? = null
    ) = Exercise(
        id = id,
        name = name,
        category = category,
        defaultWorkSeconds = seconds,
        instructions = instructions,
        cue = cue,
        requiredEquipment = equipment
    )

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
            "wall-angels",
            "Wall angels",
            ExerciseCategory.UPPER_BODY,
            40,
            "Dos contre un mur si possible, fais glisser lentement les bras de bas en haut en gardant les côtes calmes.",
            "Ne force pas l'amplitude si les épaules décollent."
        ),
        exercise(
            "prone-swimmers",
            "Nageurs au sol",
            ExerciseCategory.UPPER_BODY,
            40,
            "Allongé sur le ventre, décolle légèrement les bras et fais-les passer lentement de l'avant vers les hanches puis reviens.",
            "Le mouvement reste petit et contrôlé, nuque longue.",
            Equipment.MAT
        ),
        exercise(
            "pike-pushups",
            "Pompes en V",
            ExerciseCategory.UPPER_BODY,
            35,
            "Depuis une position en V inversé, plie les coudes pour rapprocher le haut de la tête du sol puis repousse.",
            "Réduis l'amplitude si les épaules fatiguent.",
            Equipment.MAT
        ),
        exercise(
            "incline-pushups-chair",
            "Pompes inclinées sur chaise",
            ExerciseCategory.UPPER_BODY,
            40,
            "Mains sur une chaise stable placée contre un mur. Garde le corps gainé, descends le buste puis repousse.",
            "La chaise ne doit pas pouvoir glisser.",
            Equipment.CHAIR
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
            "chair-sit-stand",
            "Assis-debout sur chaise",
            ExerciseCategory.LOWER_BODY,
            45,
            "Assieds-toi doucement sur une chaise stable puis relève-toi en poussant dans les pieds, sans te laisser tomber.",
            "Contrôle la descente et garde les genoux dans l'axe.",
            Equipment.CHAIR
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
            "side-lunges",
            "Fentes latérales",
            ExerciseCategory.LOWER_BODY,
            45,
            "Fais un pas latéral, recule les hanches sur la jambe qui plie puis reviens au centre et alterne.",
            "Garde l'autre jambe longue sans verrouiller le genou."
        ),
        exercise(
            "good-mornings",
            "Good mornings",
            ExerciseCategory.LOWER_BODY,
            45,
            "Debout, mains sur les hanches ou la poitrine. Recule les hanches en gardant le dos long puis serre les fessiers pour revenir.",
            "Le mouvement vient des hanches, pas d'un dos qui s'arrondit."
        ),
        exercise(
            "glute-bridge",
            "Pont fessier",
            ExerciseCategory.LOWER_BODY,
            45,
            "Allongé sur le dos, genoux fléchis et pieds au sol. Serre les fessiers pour lever le bassin puis redescends lentement.",
            "Monte avec les hanches, sans cambrer exagérément le bas du dos.",
            Equipment.MAT
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
            "Choisis une variante qui te permet de garder une exécution propre.",
            Equipment.MAT
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
            "Garde le bas du dos stable et réduis l'amplitude si nécessaire.",
            Equipment.MAT
        ),
        exercise(
            "bird-dog",
            "Bird dog",
            ExerciseCategory.CORE,
            45,
            "À quatre pattes, tends bras et jambe opposés sans tourner le bassin, reviens puis alterne.",
            "Imagine un verre posé sur ton bassin : il ne doit pas se renverser.",
            Equipment.MAT
        ),
        exercise(
            "forearm-plank",
            "Planche avant",
            ExerciseCategory.CORE,
            35,
            "Appui sur les avant-bras et les pieds, ou sur les genoux. Aligne tête, tronc et bassin et maintiens sans bloquer la respiration.",
            "Arrête la série avant de perdre la position.",
            Equipment.MAT
        ),
        exercise(
            "side-plank",
            "Planche latérale",
            ExerciseCategory.CORE,
            35,
            "Sur un avant-bras et les pieds ou les genoux, soulève le bassin et garde le corps aligné. Change de côté à mi-parcours.",
            "Épaule loin de l'oreille et bassin haut sans douleur.",
            Equipment.MAT
        ),
        exercise(
            "mountain-climbers",
            "Mountain climbers contrôlés",
            ExerciseCategory.CORE,
            40,
            "Depuis une planche haute, ramène alternativement un genou vers le buste sans laisser le bassin rebondir.",
            "Privilégie le contrôle à la vitesse.",
            Equipment.MAT
        ),
        exercise(
            "standing-knee-drive",
            "Montées de genou contrôlées",
            ExerciseCategory.CORE,
            40,
            "Debout, ramène alternativement un genou vers le buste en gardant le tronc haut et le bassin stable.",
            "Contracte légèrement les abdominaux sans te pencher en arrière."
        ),
        exercise(
            "cat-cow",
            "Dos rond / dos creux",
            ExerciseCategory.MOBILITY,
            45,
            "À quatre pattes, alterne lentement l'arrondi puis l'ouverture de la colonne en suivant ta respiration.",
            "Cherche la fluidité, sans forcer les amplitudes.",
            Equipment.MAT
        ),
        exercise(
            "thoracic-rotation",
            "Rotation thoracique",
            ExerciseCategory.MOBILITY,
            45,
            "À quatre pattes, place une main derrière la tête puis ouvre le coude vers le plafond avant de revenir. Change de côté à mi-parcours.",
            "Le bassin reste aussi stable que possible.",
            Equipment.MAT
        ),
        exercise(
            "seated-thoracic-rotation",
            "Rotation thoracique assise",
            ExerciseCategory.MOBILITY,
            45,
            "Assis au bord d'une chaise, croise les bras sur la poitrine et tourne doucement le haut du corps de chaque côté.",
            "Le bassin reste face à l'avant.",
            Equipment.CHAIR
        ),
        exercise(
            "hip-90-90",
            "Transitions 90/90",
            ExerciseCategory.MOBILITY,
            45,
            "Assis, genoux fléchis, fais basculer doucement les jambes d'un côté puis de l'autre en gardant le mouvement contrôlé.",
            "Réduis l'amplitude si les hanches tirent trop.",
            Equipment.MAT
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
            "world-stretch",
            "Fente + rotation",
            ExerciseCategory.MOBILITY,
            45,
            "Depuis une fente longue, pose une main au sol ou sur la cuisse et ouvre l'autre bras vers le plafond. Alterne les côtés.",
            "Respire lentement et garde une amplitude confortable.",
            Equipment.MAT
        ),
        exercise(
            "single-leg-balance",
            "Équilibre sur une jambe",
            ExerciseCategory.MOBILITY,
            40,
            "Tiens-toi sur une jambe, genou souple, puis change de côté à mi-parcours. Utilise un support si besoin.",
            "Regarde un point fixe et garde le pied actif."
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
            "Laisse la respiration relâcher progressivement le dos.",
            Equipment.MAT
        )
    )

    fun byId(id: String): Exercise =
        all.first { it.id == id }

    fun effective(preferences: TrainingPreferences): List<Exercise> {
        val builtIns = all.map { exercise ->
            preferences.exerciseOverrides[exercise.id] ?: exercise
        }
        return builtIns + preferences.customExercises
    }

    fun resolve(
        id: String,
        preferences: TrainingPreferences
    ): Exercise? =
        preferences.exerciseOverrides[id]
            ?: preferences.customExercises.firstOrNull { it.id == id }
            ?: all.firstOrNull { it.id == id }
}

object WorkoutPlanner {
    fun build(
        recovery: RecoveryResult,
        history: List<WorkoutHistoryHint> = emptyList(),
        preferences: TrainingPreferences = TrainingPreferences()
    ): WorkoutPlan {
        val recentGroups = recentGroups(history)
        val focus = chooseFocus(history, recentGroups, preferences)
        val lastDifficulty = history.firstOrNull()?.perceivedDifficulty

        val difficultyScale = when {
            lastDifficulty != null && lastDifficulty >= 5 -> 0.85
            lastDifficulty == 4 -> 0.92
            lastDifficulty != null && lastDifficulty <= 2 -> 1.05
            else -> 1.0
        }

        val adjustedIntensity = (
            recovery.intensity +
                when {
                    lastDifficulty != null && lastDifficulty >= 4 -> -1
                    lastDifficulty != null && lastDifficulty <= 2 &&
                        recovery.level == RecoveryLevel.GREEN -> 1
                    else -> 0
                }
            ).coerceIn(1, 10)

        val targetMinutes = when (recovery.level) {
            RecoveryLevel.GREEN -> preferences.durationMinutes
            RecoveryLevel.ORANGE -> preferences.durationMinutes.coerceAtMost(20)
            RecoveryLevel.RED -> preferences.durationMinutes.coerceAtMost(12)
            RecoveryLevel.UNKNOWN -> preferences.durationMinutes.coerceAtMost(12)
        }.coerceIn(8, 40)

        val basePlan = when (recovery.level) {
            RecoveryLevel.GREEN -> buildGreen(
                focus = focus,
                intensity = adjustedIntensity,
                scale = difficultyScale,
                preferences = preferences
            )

            RecoveryLevel.ORANGE -> buildActivation(
                intensity = adjustedIntensity.coerceAtMost(5),
                scale = difficultyScale,
                preferences = preferences
            )

            RecoveryLevel.RED -> buildRecovery(
                intensity = adjustedIntensity.coerceAtMost(3),
                preferences = preferences
            )

            RecoveryLevel.UNKNOWN -> buildRecovery(
                intensity = 2,
                preferences = preferences
            )
        }

        val customizedItems = basePlan.items.mapNotNull { item ->
            customizeItem(item, preferences)
        }

        val diversifiedItems = diversifyOrder(
            items = customizedItems,
            seed = history.size
        )

        return basePlan.copy(
            items = fillToTarget(
                base = diversifiedItems,
                targetMinutes = targetMinutes
            )
        )
    }

    private fun buildGreen(
        focus: ExerciseCategory,
        intensity: Int,
        scale: Double,
        preferences: TrainingPreferences
    ): WorkoutPlan {
        val mainIds = when (focus) {
            ExerciseCategory.UPPER_BODY -> listOf(
                "pushups",
                "incline-pushups-chair",
                "scapular-pushups",
                "prone-swimmers",
                "wall-angels",
                "pike-pushups",
                "wall-pushups"
            )

            ExerciseCategory.CORE -> listOf(
                "dead-bug",
                "bird-dog",
                "forearm-plank",
                "side-plank",
                "mountain-climbers",
                "standing-knee-drive"
            )

            else -> listOf(
                "squats",
                "chair-sit-stand",
                "reverse-lunges",
                "side-lunges",
                "good-mornings",
                "glute-bridge",
                "calf-raises"
            )
        }

        val fallback = when (focus) {
            ExerciseCategory.UPPER_BODY -> "wall-pushups"
            ExerciseCategory.CORE -> "standing-knee-drive"
            else -> "squats"
        }

        val mainExercises = chooseExercises(
            ids = mainIds,
            preferences = preferences,
            fallbackId = fallback
        )

        val support = if (focus == ExerciseCategory.CORE) {
            emptyList()
        } else {
            chooseExercises(
                ids = listOf("dead-bug", "bird-dog", "standing-knee-drive"),
                preferences = preferences,
                fallbackId = "standing-knee-drive"
            ).take(2)
        }

        val mobility = chooseExercises(
            ids = listOf(
                "thoracic-rotation",
                "seated-thoracic-rotation",
                "hip-flexor-stretch",
                "ankle-rocks",
                "chest-opener"
            ),
            preferences = preferences,
            fallbackId = "ankle-rocks"
        ).take(2)

        val title = when (focus) {
            ExerciseCategory.UPPER_BODY -> "Haut du corps + mobilité"
            ExerciseCategory.CORE -> "Gainage + mobilité"
            else -> "Bas du corps + mobilité"
        }

        val items = buildList {
            add(work("march-place", 45, 10, 1.0))
            addAll(mainExercises.map { exercise ->
                WorkoutExercise(
                    exercise = exercise,
                    workSeconds = (exercise.defaultWorkSeconds * scale)
                        .toInt()
                        .coerceAtLeast(20),
                    restSeconds = 15
                )
            })
            addAll(support.map { exercise ->
                WorkoutExercise(
                    exercise = exercise,
                    workSeconds = (exercise.defaultWorkSeconds * scale)
                        .toInt()
                        .coerceAtLeast(20),
                    restSeconds = 15
                )
            })
            addAll(mobility.mapIndexed { index, exercise ->
                WorkoutExercise(
                    exercise = exercise,
                    workSeconds = exercise.defaultWorkSeconds,
                    restSeconds = if (index == mobility.lastIndex) 0 else 10
                )
            })
        }

        return WorkoutPlan(
            title = title,
            intensity = intensity,
            items = items
        )
    }

    private fun buildActivation(
        intensity: Int,
        scale: Double,
        preferences: TrainingPreferences
    ): WorkoutPlan {
        val main = chooseExercises(
            ids = listOf(
                "squats",
                "chair-sit-stand",
                "wall-pushups",
                "incline-pushups-chair",
                "standing-knee-drive",
                "glute-bridge",
                "dead-bug"
            ),
            preferences = preferences,
            fallbackId = "squats"
        )

        val mobility = chooseExercises(
            ids = listOf(
                "arm-circles",
                "cat-cow",
                "seated-thoracic-rotation",
                "hip-90-90",
                "ankle-rocks"
            ),
            preferences = preferences,
            fallbackId = "arm-circles"
        ).take(2)

        return WorkoutPlan(
            title = "Activation + mobilité",
            intensity = intensity,
            items = buildList {
                add(work("march-place", 45, 10, 1.0))
                addAll(main.map { exercise ->
                    WorkoutExercise(
                        exercise = exercise,
                        workSeconds = (exercise.defaultWorkSeconds * scale)
                            .toInt()
                            .coerceAtLeast(20),
                        restSeconds = 15
                    )
                })
                addAll(mobility.mapIndexed { index, exercise ->
                    WorkoutExercise(
                        exercise = exercise,
                        workSeconds = exercise.defaultWorkSeconds,
                        restSeconds = if (index == mobility.lastIndex) 0 else 10
                    )
                })
            }
        )
    }

    private fun buildRecovery(
        intensity: Int,
        preferences: TrainingPreferences
    ): WorkoutPlan {
        val mobility = chooseExercises(
            ids = listOf(
                "arm-circles",
                "cat-cow",
                "thoracic-rotation",
                "seated-thoracic-rotation",
                "hip-90-90",
                "ankle-rocks",
                "world-stretch",
                "single-leg-balance",
                "hip-flexor-stretch",
                "hamstring-hinge",
                "chest-opener",
                "child-pose-reach"
            ),
            preferences = preferences,
            fallbackId = "arm-circles"
        )

        return WorkoutPlan(
            title = "Mobilité + récupération",
            intensity = intensity,
            items = mobility.mapIndexed { index, exercise ->
                WorkoutExercise(
                    exercise = exercise,
                    workSeconds = exercise.defaultWorkSeconds,
                    restSeconds = if (index == mobility.lastIndex) 0 else 10
                )
            }
        )
    }

    private fun chooseExercises(
        ids: List<String>,
        preferences: TrainingPreferences,
        fallbackId: String
    ): List<Exercise> {
        val categories = ids
            .mapNotNull { ExerciseLibrary.resolve(it, preferences)?.category }
            .toSet()

        val builtInCandidates = ids
            .mapNotNull { ExerciseLibrary.resolve(it, preferences) }

        val customCandidates = preferences.customExercises.filter { exercise ->
            exercise.category in categories
        }

        val eligible = (builtInCandidates + customCandidates)
            .distinctBy { it.id }
            .filter { exercise ->
                exercise.id !in preferences.avoidedExerciseIds &&
                    exercise.category !in preferences.disabledCategories &&
                    (exercise.requiredEquipment == null ||
                        exercise.requiredEquipment in preferences.availableEquipment)
            }
            .sortedByDescending { it.id in preferences.favoriteExerciseIds }

        if (eligible.isNotEmpty()) return eligible

        val fallback = ExerciseLibrary.resolve(fallbackId, preferences)
            ?: return emptyList()
        val fallbackAllowed =
            fallback.id !in preferences.avoidedExerciseIds &&
                fallback.category !in preferences.disabledCategories &&
                (fallback.requiredEquipment == null ||
                    fallback.requiredEquipment in preferences.availableEquipment)

        return if (fallbackAllowed) listOf(fallback) else emptyList()
    }

    private fun diversifyOrder(
        items: List<WorkoutExercise>,
        seed: Int
    ): List<WorkoutExercise> {
        if (items.size <= 3) return items

        val first = items.first()
        val last = items.last()
        val middle = items.drop(1).dropLast(1)
        if (middle.size <= 1) return items

        val offset = seed % middle.size
        val rotated = middle.drop(offset) + middle.take(offset)
        return listOf(first) + rotated + listOf(last)
    }

    private fun fillToTarget(
        base: List<WorkoutExercise>,
        targetMinutes: Int
    ): List<WorkoutExercise> {
        if (base.isEmpty()) return base

        val targetSeconds = targetMinutes * 60
        val result = mutableListOf<WorkoutExercise>()
        val warmup = base.first()
        val cooldown = base.last()
        val main = base
            .drop(1)
            .dropLast(1)
            .distinctBy { it.exercise.id }
            .ifEmpty { listOf(warmup) }

        result += warmup

        var round = 0
        var cursor = 0

        while (result.size < 28) {
            val offset = if (main.size <= 1) 0 else round % main.size
            val ordered = if (offset == 0) {
                main
            } else {
                main.drop(offset) + main.take(offset)
            }

            val next = ordered[cursor]
            val projected = result.sumOf(::itemSeconds) +
                itemSeconds(next) +
                itemSeconds(cooldown)

            if (projected > targetSeconds) break

            if (result.lastOrNull()?.exercise?.id != next.exercise.id) {
                result += next
            }

            cursor++
            if (cursor >= ordered.size) {
                cursor = 0
                round++
            }
        }

        if (result.size == 1 && base.size > 2) {
            result += main.take(2)
        }

        if (cooldown.exercise.id != result.last().exercise.id) {
            result += cooldown
        }

        return result
    }

    private fun itemSeconds(item: WorkoutExercise): Int =
        5 + item.workSeconds + item.restSeconds

    private fun recentGroups(
        history: List<WorkoutHistoryHint>
    ): Set<ExerciseCategory> {
        val cutoff = System.currentTimeMillis() - 48L * 60L * 60L * 1000L
        return history
            .filter { it.startedAtEpochMs >= cutoff }
            .flatMap { it.muscleGroups }
            .toSet()
    }

    private fun chooseFocus(
        history: List<WorkoutHistoryHint>,
        recentGroups: Set<ExerciseCategory>,
        preferences: TrainingPreferences
    ): ExerciseCategory {
        val strengthGroups = listOf(
            ExerciseCategory.UPPER_BODY,
            ExerciseCategory.LOWER_BODY,
            ExerciseCategory.CORE
        ).filter { it !in preferences.disabledCategories }

        if (strengthGroups.isEmpty()) {
            return ExerciseCategory.CORE
        }

        strengthGroups.firstOrNull { it !in recentGroups }?.let { return it }

        return strengthGroups.maxByOrNull { group ->
            val last = history
                .filter { group in it.muscleGroups }
                .maxOfOrNull { it.startedAtEpochMs } ?: 0L
            System.currentTimeMillis() - last
        } ?: ExerciseCategory.LOWER_BODY
    }

    private fun customizeItem(
        item: WorkoutExercise,
        preferences: TrainingPreferences
    ): WorkoutExercise? {
        val exercise = ExerciseLibrary.resolve(
            item.exercise.id,
            preferences
        ) ?: item.exercise

        if (exercise.id in preferences.avoidedExerciseIds ||
            exercise.category in preferences.disabledCategories ||
            (exercise.requiredEquipment != null &&
                exercise.requiredEquipment !in preferences.availableEquipment)
        ) {
            return null
        }

        val customSeconds = preferences.exerciseWorkSeconds[exercise.id]
            ?.coerceIn(15, 120)

        return item.copy(
            exercise = exercise,
            workSeconds = customSeconds ?: item.workSeconds
        )
    }

    private fun work(
        id: String,
        workSeconds: Int,
        restSeconds: Int,
        scale: Double
    ): WorkoutExercise =
        WorkoutExercise(
            exercise = ExerciseLibrary.byId(id),
            workSeconds = (workSeconds * scale).toInt().coerceAtLeast(20),
            restSeconds = restSeconds
        )
}
