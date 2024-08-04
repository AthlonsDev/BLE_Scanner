package com.example.ble_scanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView



class DeviceListAdapter(private val deviceList: List<ItemsViewModel>):
    RecyclerView.Adapter<DeviceViewHolder>() {

    private var onClickListener: OnClickListener? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_view_design, parent, false)

        return DeviceViewHolder(view)

    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val itemsViewModel = deviceList[position]
//        holder.deviceName.text = itemsViewModel.name
//        holder.deviceAddress.text = itemsViewModel.address
//        holder.deviceRssi.text = itemsViewModel.rssi.toString()

        holder.bind(itemsViewModel)

        holder.setOnClickListener(object : DeviceListAdapter.OnClickListener {
            override fun onClick(position: Int, model: ItemsViewModel) {
                onClickListener?.onClick(position, model)
            }
        })

    }

    // Set the click listener for the adapter
    fun setOnClickListener(listener: OnClickListener?) {
        this.onClickListener = listener
    }

    // Interface for the click listener
    interface OnClickListener {
        fun onClick(position: Int, model: ItemsViewModel)
    }


}