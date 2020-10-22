package com.fiap.mba_fiap_iot.adapter

import android.hardware.Sensor
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fiap.mba_fiap_iot.R

class AdapterSensors(private val sensors: List<Sensor>) :
    RecyclerView.Adapter<AdapterSensors.MyViewHolder>() {

    class MyViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_sensors, parent, false) as TextView
        return MyViewHolder(textView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textView.text = sensors[position].name
    }

    override fun getItemCount() = sensors.size
}
