package com.example.ble_scanner

import android.content.ClipData.Item
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class DeviceViewHolder(private val itemView: View) :   ViewHolder(itemView) {
    val deviceName: TextView = itemView.findViewById(R.id.recycler_device_name)
    val deviceAddress: TextView = itemView.findViewById(R.id.recycler_device_address)
    val deviceRssi: TextView = itemView.findViewById(R.id.recycler_device_rssi)
}