package com.nankai.flutter_nearby_connections.wifip2p

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.util.Log
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Payload
import com.nankai.flutter_nearby_connections.NearbyService
import com.nankai.flutter_nearby_connections.device.DeviceManager
import com.nankai.flutter_nearby_connections.event.NearbyEvent
import io.flutter.plugin.common.MethodChannel
import java.util.*

class WifiP2PEvent(private val channel: MethodChannel,
                   private val serviceType: String,
                   val deviceManager: DeviceManager,
                   private val service: NearbyService)
    : NearbyEvent {

    private val TAG = "WifiP2pUtils"

    var isWifiP2pEnabled: Boolean = false

    var lastError = -1

    var p2pManager: WifiP2pManager? = null
    var p2pChannel: WifiP2pManager.Channel? = null
    var p2pConfig: WifiP2pConfig = WifiP2pConfig()

    var wifiBroadcastReceiver: WifiBroadcastReceiver? = null
    var wifiManager: WifiManager? = null

    private val r = Random()

    private fun setupIntentFilters(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.apply {
            // Indicates a change in the Wi-Fi P2P status.
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            // Indicates a change in the list of available peers.
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            // Indicates the state of Wi-Fi P2P connectivity has changed.
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            // Indicates this device'base details have changed.
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            // Indicates the state of peer discovery has changed
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        }

        return intentFilter
    }

    init {

        p2pManager = service.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        p2pChannel = p2pManager?.initialize(
                service.applicationContext,
                service.mainLooper
        ) {
            Log.w(TAG, "Wifi P2P disconnected!")
        }


        wifiManager = service.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager?.wifiState != WifiManager.WIFI_STATE_ENABLED) {
            wifiManager?.isWifiEnabled = true
        }

        // remove the previous list of configured Direct AP networks
        wifiManager?.configuredNetworks?.let {
            for (i in it) {
                if (i.SSID != null) {
                    if (i.SSID.contains("DIRECT")) {
                        wifiManager?.removeNetwork(i.networkId)
                    }
                }
            }
        }

        wifiBroadcastReceiver = WifiBroadcastReceiver(p2pManager, wifiManager, p2pChannel, this@WifiP2PEvent)
        service.registerReceiver(wifiBroadcastReceiver, setupIntentFilters())


        // Wi-Fi Direct Auto Accept authentication (Only PBC supported)
        val wdAutoAccept = WifiDirectAutoAccept(p2pManager, p2pChannel)
        wdAutoAccept.intercept(true)

        // Configure the Intention and WPS in wifi P2P
        p2pConfig.groupOwnerIntent = 8 + r.nextInt(6)
        p2pConfig.wps.setup = WpsInfo.PBC

    }

    /**
     * Mapping events
     */

    override fun startAdvertising(deviceName: String, serviceId: String, build: AdvertisingOptions) {
        val record: MutableMap<String, String> = HashMap()
        record["available"] = "visible"
        val serviceMagnet = WifiP2pDnsSdServiceInfo.newInstance("MAGNET", serviceType, record)
        p2pManager?.addLocalService(p2pChannel, serviceMagnet, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                lastError = -1
                Log.e(TAG, "Added local service")
            }

            override fun onFailure(reason: Int) {
                lastError = reason
                Log.e(TAG, "Adding local service failed, error code  + $reason")
            }
        })
    }

    override fun startDiscovery(serviceId: String, deviceName: String, build: DiscoveryOptions) {

    }

    override fun requestConnection(endpointId: String, displayName: String) {

    }

    override fun stopDiscovery() {

    }

    override fun stopAdvertising() {
        p2pManager?.clearLocalServices(p2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                lastError = -1
                Log.e(TAG, "Cleared local services")
            }

            override fun onFailure(reason: Int) {
                lastError = reason
                Log.e(TAG, "Clearing local services failed, error code $reason")
            }
        })
    }

    override fun stopAllEndpoints() {}

    override fun disconnectFromEndpoint(endpointId: String) {

    }

    override fun sendPayload(endpointId: String, fromBytes: Payload) {
    }
}