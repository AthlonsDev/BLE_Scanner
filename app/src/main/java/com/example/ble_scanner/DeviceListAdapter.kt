package com.example.ble_scanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class DeviceListAdapter(private val deviceList: List<ItemsViewModel>):
    RecyclerView.Adapter<DeviceViewHolder>() {

    private var onClickListener: View.OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_view_design, parent, false)
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val itemsViewModel = deviceList[position]
        holder.deviceName.text = itemsViewModel.name
        holder.deviceAddress.text = itemsViewModel.address
        holder.deviceRssi.text = itemsViewModel.rssi.toString()

    }

    fun setOnClickListener(onClickListener: View.OnClickListener) {
        this.onClickListener = onClickListener
    }

}