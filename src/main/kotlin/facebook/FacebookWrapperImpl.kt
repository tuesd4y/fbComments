package facebook

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rx_response
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable


/*
 * Written by Christopher Stelzmüller <tuesd4y@protonmail.ch>, October 2017
 */

class FacebookWrapperImpl() : FacebookWrapper {
    companion object {
        val apiVersion: String = "2.10"
        val baseUrl: String = "https://graph.facebook.com/v${apiVersion}/"
        val parser: Parser = Parser()
    }


    private var apiKey: String = ""
    private var basePathSet = false
    private val accessTokenPart
        get() = "access_token=$apiKey"

    constructor(apiKey: String) : this() {
        this.apiKey = apiKey
    }

    override fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    /**
     * example from graph api:
     * curl -i -X GET \
     * "https://graph.facebook.com/v2.10/10155004153142688/comments?access_token=${token}"
     */
    override fun getComments(objectId: String): Observable<FacebookWrapper.Comment> {
        prepare()

        return "$objectId/comments?limit=500&$accessTokenPart"
                .httpGet()
                .rx_response()
                .map {
                    if(it.first.statusCode == 200) {
                        it.second.component1()
                    } else {
                        handleError(it.second.component2()!!)
                    }
                }
                .map { parser.parse(it.inputStream()) as JsonObject }
                .flatMapObservable { (it["data"] as JsonArray<JsonObject>).value.toObservable() }
                .map { FacebookWrapper.Comment(it) }
    }

    private fun prepare() {
        if (apiKey.isNullOrEmpty()) throw Error("api key is not set")
        if (!basePathSet) {
            FuelManager.instance.basePath = baseUrl
            basePathSet = true
        }
    }

    private fun <T> handleError(error: FuelError): T {
        throw Error(error)
    }
}