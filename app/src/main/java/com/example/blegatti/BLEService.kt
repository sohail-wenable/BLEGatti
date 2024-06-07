package com.example.blegatti

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.util.UUID

data class BLEService(val uuid: UUID)

class ServiceListAdapter(private val context: Context, private val services: List<BLEService>) : BaseAdapter() {
    override fun getCount(): Int {
        return services.size
    }

    override fun getItem(position: Int): Any {
        return services[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val service = services[position]

        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = service.uuid.toString()

        return view
    }
}
