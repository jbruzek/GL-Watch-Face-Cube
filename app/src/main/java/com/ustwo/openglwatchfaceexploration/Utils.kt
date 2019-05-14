package com.ustwo.openglwatchfaceexploration

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class Utils {
    companion object {
        /**
         * Reads an asset file into a string, returning the resulting string
         * @param context The asset's context
         * @param asset The name of the asset
         * @return The string if successful
         */
        @Throws(IOException::class)
        fun readStringAsset(context: Context, asset: String): String {
            val stringBuilder = StringBuilder()
            val inputStream = context.assets.open(asset)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            var line: String? = bufferedReader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                stringBuilder.append('\n')
                line = bufferedReader.readLine()
            }
            return stringBuilder.toString()
        }
    }
}