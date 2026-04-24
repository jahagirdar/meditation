package com.serenity.di

import android.content.Context
import androidx.room.Room
import com.serenity.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "serenity.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSessionDao(db: AppDatabase):    SessionDao    = db.sessionDao()
    @Provides fun providePresetDao(db: AppDatabase):     PresetDao     = db.presetDao()
    @Provides fun provideAssessmentDao(db: AppDatabase): AssessmentDao = db.assessmentDao()
    @Provides fun providePranayamaDao(db: AppDatabase):  PranayamaDao  = db.pranayamaDao()
}
