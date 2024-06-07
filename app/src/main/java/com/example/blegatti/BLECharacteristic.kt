package com.example.blegatti

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.util.UUID

data class BLECharacteristic(val uuid: UUID)

class CharacteristicListAdapter(private val context: Context, private val characteristics: List<BLECharacteristic>) : BaseAdapter() {
    override fun getCount(): Int {
        return characteristics.size
    }

    override fun getItem(position: Int): Any {
        return characteristics[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val characteristic = characteristics[position]

        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = characteristic.uuid.toString()

        return view
    }
}
