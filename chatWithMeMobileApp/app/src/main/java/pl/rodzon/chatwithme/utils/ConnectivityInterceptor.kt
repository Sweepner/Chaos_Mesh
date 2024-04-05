package pl.rodzon.chatwithme.utils

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

import java.io.IOException

class ConnectivityInterceptor(context: Context): Interceptor {
    private var context: Context

    init {
        this.context = context
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (NetworkCheck.isNetworkAvailable(context)) {
            throw IOException("No internet connection")
        } else {
            return chain.proceed(chain.request())
        }
    }
}