package com.amazonaws.services.chime.sdk.meetings.internal.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import java.lang.reflect.Method
import java.net.InetAddress

object DNSServerUtils {
    private val TAG = "DNSServerUtils"

    fun getAvailableDnsServers(context: Context, logger: Logger): Array<String> {
        val dnsHosts: MutableList<String> = ArrayList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
                if (linkProperties != null) {
                    val addresses: List<InetAddress> = linkProperties.dnsServers
                    for (i in addresses.indices) {
                        if (addresses[i].hostAddress.isNotEmpty()) {
                            dnsHosts.add(addresses[i].hostAddress)
                        }
                    }
                    logger.info(TAG, "Get " + dnsHosts.size + " DNS addresses.")
                }
            } catch (e: Exception) {
                logger.error(TAG, "Failed to get active DNS address.")
            }
        } else {
            try {
                @SuppressLint("PrivateApi") val SystemProperties = Class.forName("android.os.SystemProperties")
                val method: Method = SystemProperties.getMethod("get", String::class.java)
                for (name in arrayOf("net.dns1", "net.dns2")) {
                    val value = method.invoke(null, name) as String
                    if (value.isNotEmpty()) {
                        dnsHosts.add(value)
                    }
                }
                logger.info(TAG, "Get " + dnsHosts.size + " DNS addresses,")
            } catch (e: Exception) {
                logger.error(TAG, "Failed to get active DNS address.")
            }
        }
        return dnsHosts.toTypedArray()
    }
}
