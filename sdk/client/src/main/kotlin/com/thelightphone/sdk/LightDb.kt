package com.thelightphone.sdk

import androidx.room.Room
import androidx.room.RoomDatabase

fun <T : RoomDatabase> SealedLightContext.buildDatabase(dbClass: Class<T>, dbName: String?): T {
    return Room.databaseBuilder(androidContext.applicationContext, dbClass, dbName).build()
}

/**
 * The [RoomDatabase.Builder] form of [buildDatabase], for libraries that
 * configure and build the database themselves — e.g. Trixnity's
 * `RepositoriesModule.room(...)` only accepts a builder.
 */
fun <T : RoomDatabase> SealedLightContext.roomDatabaseBuilder(
    dbClass: Class<T>,
    dbName: String?,
): RoomDatabase.Builder<T> =
    Room.databaseBuilder(androidContext.applicationContext, dbClass, dbName)