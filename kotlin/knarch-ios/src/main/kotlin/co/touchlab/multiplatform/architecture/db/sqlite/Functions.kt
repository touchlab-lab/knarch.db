/*
 * Copyright (c) 2018 Touchlab Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.touchlab.multiplatform.architecture.db.sqlite

import co.touchlab.knarch.DefaultSystemContext
import co.touchlab.knarch.SystemContext
import co.touchlab.knarch.io.File
import co.touchlab.multiplatform.architecture.db.DatabaseErrorHandler


var systemContext:SystemContext?=null

/**
 * Not pretty, but you know
 */
fun initSystemContext(sc:SystemContext){
    systemContext = sc
}

actual fun createOpenHelper(
        name:String?,
        callback:PlatformSQLiteOpenHelperCallback,
        errorHandler: DatabaseErrorHandler?):SQLiteOpenHelper{

    if(systemContext == null)
        throw NullPointerException("Must call initSystemContext")

    return PlatformSQLiteOpenHelper(callback,
            systemContext!!,
            name,
            callback.version,
            errorHandler
            )
}

actual fun deleteDatabase(path:String):Boolean{
    return SQLiteDatabase.deleteDatabase(File(path))
}