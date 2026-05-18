package com.example.myapplication.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import java.util.Locale

class VehicleAdapter(
    private val onEdit: (Vehicle) -> Unit,
    private val onDelete: (Vehicle) -> Unit
) : ListAdapter<Vehicle, VehicleAdapter.VehicleViewHolder>(VehicleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VehicleViewHolder(
        itemView: View,
        private val onEdit: (Vehicle) -> Unit,
        private val onDelete: (Vehicle) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.textVehicleName)
        private val textPlate: TextView = itemView.findViewById(R.id.textPlate)
        private val textFuelAndOdo: TextView = itemView.findViewById(R.id.textFuelAndOdo)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btnMoreVehicle)

        fun bind(vehicle: Vehicle) {
            textName.text = vehicle.name
            textPlate.text = vehicle.plateNumber
            
            val eff = vehicle.calculatedEfficiency ?: vehicle.manualEfficiency
            textFuelAndOdo.text = "${vehicle.fuelType} • ${String.format(Locale.US, "%.1f", eff)} km/L • ${vehicle.currentOdometer.toInt()} km"

            btnMore.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add("Edit")
                popup.menu.add("Hapus")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Edit" -> onEdit(vehicle)
                        "Hapus" -> onDelete(vehicle)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    class VehicleDiffCallback : DiffUtil.ItemCallback<Vehicle>() {
        override fun areItemsTheSame(oldItem: Vehicle, newItem: Vehicle): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Vehicle, newItem: Vehicle): Boolean = oldItem == newItem
    }
}
