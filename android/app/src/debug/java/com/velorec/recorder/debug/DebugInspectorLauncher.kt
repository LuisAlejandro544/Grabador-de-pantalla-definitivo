package com.velorec.recorder.debug

import android.content.Context
import android.content.Intent

/**
 * DebugInspectorLauncher.kt (Variante Debug)
 *
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Lanza la actividad DebugConsoleActivity en compilaciones Debug.
 * En compilaciones Release se reemplaza por un stub vacío para excluir
 * por completo la consola de diagnóstico del APK estable.
 */
object DebugInspectorLauncher {
    fun open(context: Context) {
        context.startActivity(Intent(context, DebugConsoleActivity::class.java))
    }
}
