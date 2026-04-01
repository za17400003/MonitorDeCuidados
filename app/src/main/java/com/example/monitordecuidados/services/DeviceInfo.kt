package com.example.monitordecuidados.services

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeviceInfo(
    val name: String,
    val ip: String,
    val port: Int
) : Parcelable
