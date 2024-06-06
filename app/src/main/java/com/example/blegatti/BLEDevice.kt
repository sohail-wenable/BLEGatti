package com.example.blegatti

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

data class BLEDevice(val name: String?, val address: String)

class DeviceListAdapter(private val context: Context, private val devices: List<BLEDevice>, private val targetDeviceAddress: String) : BaseAdapter() {
    override fun getCount(): Int {
        return devices.size
    }

    override fun getItem(position: Int): Any {
        return devices[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false)
        val device = devices[position]

        val textViewName = view.findViewById<TextView>(android.R.id.text1)
        val textViewAddress = view.findViewById<TextView>(android.R.id.text2)

        textViewName.text = device.name ?: "Unknown Device"
        textViewAddress.text = device.address

        // Highlight the target device in green
        if (device.address == targetDeviceAddress) {
            view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
        } else {
            view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }

        return view
    }
}
