package com.fastingtracker.di

import com.fastingtracker.util.DefaultTickerProvider
import com.fastingtracker.util.TickerProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTickerProvider(impl: DefaultTickerProvider): TickerProvider
}
