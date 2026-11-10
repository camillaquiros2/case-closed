package caseclosed.data

/**
 * Logo slot for each button, in the same place as the mark on the accusation card.
 * Drop a PNG with no background at the path. Until then, the button draws a plain mark.
 */
object ButtonIcons {
    private const val DIR = "images/ui/buttons"

    const val CASOS = "$DIR/casos.png"
    const val ESCENA = "$DIR/escena.png"
    const val CERRAR = "$DIR/cerrar.png"
    const val MENU = "$DIR/menu.png"
    const val ENVIAR = "$DIR/enviar.png"
    const val EMPEZAR = "$DIR/empezar.png"
    const val REANUDAR = "$DIR/reanudar.png"
    const val REINICIAR = "$DIR/reiniciar.png"
    const val VOLVER = "$DIR/volver.png"
    const val GUARDAR = "$DIR/guardar.png"
    const val FIJAR = "$DIR/fijar.png"
    const val CANCELAR = "$DIR/cancelar.png"
    const val CONFIRMAR = "$DIR/confirmar.png"
    const val ACUSAR = "$DIR/acusar.png"
    const val SEGUIR = "$DIR/seguir.png"
    const val SUBIR = "$DIR/subir.png"
    const val BAJAR = "$DIR/bajar.png"
    const val QUITAR = "$DIR/quitar.png"
    const val CORTAR = "$DIR/cortar.png"
    const val ABRIR = "$DIR/abrir.png"
    const val ENLAZAR = "$DIR/enlazar.png"
    const val ANTERIOR = "$DIR/anterior.png"
    const val SIGUIENTE = "$DIR/siguiente.png"
    const val HIPOTESIS = "$DIR/hipotesis.png"
    const val ORIGEN = "$DIR/origen.png"
    const val NOTAS = "$DIR/notas.png"
    const val TAB_TODOS = "$DIR/tab_todos.png"
    const val TAB_SOSPECHOSOS = "$DIR/tab_sospechosos.png"
    const val TAB_OBJETOS = "$DIR/tab_objetos.png"
    const val TAB_CITAS = "$DIR/tab_citas.png"
    const val VER_ORIGEN = "$DIR/ver_origen.png"
    const val AL_TABLERO = "$DIR/al_tablero.png"
    const val ANADIR_TABLERO = "$DIR/anadir_tablero.png"

    val all = listOf(
        CASOS, ESCENA, CERRAR, MENU, ENVIAR, EMPEZAR, REANUDAR, REINICIAR, VOLVER,
        GUARDAR, FIJAR, CANCELAR, CONFIRMAR, ACUSAR, SEGUIR, SUBIR, BAJAR, QUITAR,
        CORTAR, ABRIR, ENLAZAR, ANTERIOR, SIGUIENTE, HIPOTESIS, ORIGEN, NOTAS,
        TAB_TODOS, TAB_SOSPECHOSOS, TAB_OBJETOS, TAB_CITAS, VER_ORIGEN, AL_TABLERO, ANADIR_TABLERO,
    )
}
