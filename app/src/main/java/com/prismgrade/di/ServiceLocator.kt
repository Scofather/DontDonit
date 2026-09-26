package com.prismgrade.di

import android.content.Context
import com.prismgrade.data.image.ImageProcessor
import com.prismgrade.data.local.AppDatabase
import com.prismgrade.data.local.SettingsStore
import com.prismgrade.data.remote.ClaudeGradingService
import com.prismgrade.data.repository.InspectionRepository

/**
 * Manual dependency container.
 *
 * The graph is small and entirely singletons, so a hand-rolled locator keeps
 * the build free of an annotation processor for no lost clarity. Swap for Hilt
 * if the graph grows scopes.
 */
class ServiceLocator(context: Context) {

    private val appContext = context.applicationContext

    val settingsStore: SettingsStore by lazy { SettingsStore(appContext) }

    val imageProcessor: ImageProcessor by lazy { ImageProcessor(appContext) }

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    private val gradingService: ClaudeGradingService by lazy { ClaudeGradingService() }

    val inspectionRepository: InspectionRepository by lazy {
        InspectionRepository(
            gradingService = gradingService,
            imageProcessor = imageProcessor,
            dao = database.inspectionDao(),
            settingsStore = settingsStore,
        )
    }
}
