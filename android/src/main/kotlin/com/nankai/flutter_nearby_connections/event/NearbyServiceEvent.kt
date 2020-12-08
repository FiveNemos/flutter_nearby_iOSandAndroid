package com.nankai.flutter_nearby_connections.event

import com.google.android.gms.nearby.connection.Strategy
import io.flutter.plugin.common.MethodChannel

interface NearbyServiceEvent {
    fun onCreate()

    fun onDestroy()

    fun onStart(channel: MethodChannel, serviceType: String)

    fun startAdvertising(strategy: Strategy, deviceName: String)

    fun startDiscovery(strategy: Strategy, deviceName: String)

    fun connect(endpointId: String, displayName: String)

    fun stopDiscovery()

    fun stopAdvertising()

    fun disconnect(endpointId: String)

    fun sendStringPayload(endpointId: String, str: String)
}