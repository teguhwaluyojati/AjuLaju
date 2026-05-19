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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter(
    private val onClick: (TripWithVehicle) -> Unit,
    private val onEdit: (TripWithVehicle) -> Unit,
    private val onDelete: (TripWithVehicle) -> Unit
) : ListAdapter<TripWithVehicle, TripAdapter.TripViewHolder>(TripDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view, onClick, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TripViewHolder(
        itemView: View,
        private val onClick: (TripWithVehicle) -> Unit,
        private val onEdit: (TripWithVehicle) -> Unit,
        private val onDelete: (TripWithVehicle) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val textDate: TextView = itemView.findViewById(R.id.textDate)
        private val textVehicle: TextView = itemView.findViewById(R.id.textVehicle)
        private val textDestination: TextView = itemView.findViewById(R.id.textDestination)
        private val textKmRange: TextView = itemView.findViewById(R.id.textKmRange)
        private val textTotalKm: TextView = itemView.findViewById(R.id.textTotalKm)
        private val textNotes: TextView = itemView.findViewById(R.id.textNotes)

        fun bind(trip: TripWithVehicle) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            textDate.text = sdf.format(Date(trip.date)).uppercase()
            textVehicle.text = trip.vehicleName
            textDestination.text = trip.destination
            textKmRange.text = "${trip.startKm} km - ${trip.endKm} km"
            textTotalKm.text = "+${String.format("%.1f", trip.endKm - trip.startKm)} km"
            
            if (!trip.notes.isNullOrEmpty()) {
                textNotes.text = trip.notes
                textNotes.visibility = View.VISIBLE
            } else {
                textNotes.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(trip) }

            // Kita gunakan Long Click pada kartu untuk Edit/Delete agar tampilan tetap bersih sesuai gambar
            itemView.setOnLongClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add("Edit")
                popup.menu.add("Hapus")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Edit" -> onEdit(trip)
                        "Hapus" -> onDelete(trip)
                    }
                    true
                }
                popup.show()
                true
            }
        }
    }

    class TripDiffCallback : DiffUtil.ItemCallback<TripWithVehicle>() {
        override fun areItemsTheSame(oldItem: TripWithVehicle, newItem: TripWithVehicle): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TripWithVehicle, newItem: TripWithVehicle): Boolean = oldItem == newItem
    }
}
