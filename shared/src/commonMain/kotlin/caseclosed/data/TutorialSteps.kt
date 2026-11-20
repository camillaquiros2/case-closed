package caseclosed.data

/**
 * Pictures a step takes from the active case, so the tutorial never names a case.
 * NONE uses the step's own [TutorialStep.images].
 */
enum class CaseArt { NONE, COVER, SCENE_ZONES, SUSPECTS }

/**
 * One page of the tutorial.
 * [images] are pictures already preloaded by the catalog: one shows that object whole,
 * several are laid out in a grid like the desk. [caseArt] swaps them for the active case's art.
 */
data class TutorialStep(
    val title: String,
    val body: String,
    val images: List<String> = emptyList(),
    val caseArt: CaseArt = CaseArt.NONE,
)

/**
 * How to play, one part of the desk per step.
 * No case content here, so the tutorial works for every case.
 * Keep each body under [MAX_BODY] characters: it has to fit on screen without scrolling.
 */
object TutorialSteps {
    const val MAX_BODY = 240

    val all = listOf(
        TutorialStep(
            "Bienvenido, detective",
            "Cada caso es un crimen sin resolver. Lee los expedientes, interroga a los sospechosos y registra la escena. Cuando sepas quién fue, lo acusas con pruebas.",
            caseArt = CaseArt.COVER,
        ),
        TutorialStep(
            "Tu escritorio",
            "Todo el caso está sobre la mesa. Toca un objeto para abrirlo. Toca el encabezado para repasar la historia y usa Casos para cambiar de expediente o volver al menú.",
            listOf(
                DeskImages.PHONE, DeskImages.FOLDER, DeskImages.SUSPECTS,
                DeskImages.LOCKER, DeskImages.NOTEBOOK, DeskImages.BOARD,
            ),
        ),
        TutorialStep(
            "Teléfono",
            "Aquí llegan las transcripciones del inspector y del forense. El número rojo marca las que no has leído. Revísalo seguido: suelen traer datos nuevos.",
            listOf(DeskImages.PHONE),
        ),
        TutorialStep(
            "Expedientes",
            "Informes, cartas y declaraciones del caso. Toca una frase sospechosa para marcarla y dejarle una nota. Cada frase marcada se guarda como pista.",
            listOf(DeskImages.FOLDER),
        ),
        TutorialStep(
            "Sospechosos",
            "Abre la ficha de una persona para interrogarla. Algunas preguntas aparecen hasta que encuentras la pista correcta. Las respuestas también se pueden marcar.",
            listOf(DeskImages.SUSPECTS),
        ),
        TutorialStep(
            "La escena",
            "Visitar la escena te lleva al lugar del crimen. Recorre cada zona y toca lo que te llame la atención. Lo que encuentres pasa al casillero.",
            caseArt = CaseArt.SCENE_ZONES,
        ),
        TutorialStep(
            "Casillero",
            "Guarda las pruebas físicas que vas encontrando. Abre una para ver su descripción, los hallazgos del laboratorio y tus notas.",
            listOf(DeskImages.LOCKER),
        ),
        TutorialStep(
            "Bitácora",
            "Reúne todas tus pistas: pruebas, extractos y testimonios. Cada una dice de dónde salió. Ver origen te lleva a la fuente y Al tablero la fija.",
            listOf(DeskImages.NOTEBOOK),
        ),
        TutorialStep(
            "Tablero",
            "Aquí armas tu teoría. Fija pruebas, arrástralas y usa Enlazar para unirlas con hilo rojo. Un buen tablero muestra cómo encaja todo.",
            listOf(DeskImages.BOARD),
        ),
        TutorialStep(
            "Acusación",
            "Cuando tengas al culpable, abre la acusación. Primero nombras al acusado y luego eliges las pruebas del tablero que lo demuestran. Si faltan piezas, te avisa.",
            listOf(DeskImages.ACCUSATION),
        ),
        TutorialStep(
            "Listo para investigar",
            "Uno de ellos es el culpable. Todo se guarda solo, así que puedes salir y volver cuando quieras. El tutorial siempre está en el menú principal.",
            caseArt = CaseArt.SUSPECTS,
        ),
    )
}
