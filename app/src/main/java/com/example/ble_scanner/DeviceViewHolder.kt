package com.example.ble_scanner

import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class DeviceViewHolder(private val itemView: View) :   ViewHolder(itemView) {
    private val deviceName: TextView = itemView.findViewById(R.id.recycler_device_name)
    private val deviceAddress: TextView = itemView.findViewById(R.id.recycler_device_address)
    private val deviceRssi: TextView = itemView.findViewById(R.id.recycler_device_rssi)

    var pos: Int = 0

    fun setOnClickListener(listener: DeviceListAdapter.OnClickListener) {
        itemView.setOnClickListener {
            listener.onClick(pos, ItemsViewModel(deviceName.text.toString(), deviceAddress.text.toString(), deviceRssi.text.toString().toInt()))
        }
    }


    fun bind(item: ItemsViewModel) {
        deviceName.text = item.name
        deviceAddress.text = item.address
        deviceRssi.text = item.rssi.toString()

    }

}