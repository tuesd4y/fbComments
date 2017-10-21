package facebook

import com.beust.klaxon.JsonObject
import io.reactivex.Observable


/*
 * Written by Christopher Stelzmüller <tuesd4y@protonmail.ch>, October 2017
 */

interface FacebookWrapper {
    fun setApiKey(apiKey: String)
    fun getComments(objectId: String): Observable<Comment>


    data class Comment(val message: String, val from: User) {
        constructor(jsonObject: JsonObject) : this(jsonObject["message"] as String, User(jsonObject["from"] as JsonObject))
    }

    data class User(val id: String, val name: String) {
        constructor(jsonObject: JsonObject) : this(jsonObject["id"] as String, jsonObject["name"] as String)
    }
}