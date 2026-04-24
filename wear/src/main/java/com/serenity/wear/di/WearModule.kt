package com.serenity.wear.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WearModule
// Wearable clients are obtained directly from Wearable.getXxxClient(context)
// rather than injected, since they are not DI-friendly singletons.
