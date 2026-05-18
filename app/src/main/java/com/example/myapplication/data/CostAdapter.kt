package com.example.myapplication.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CostAdapter(
    private val onEdit: (CostWithVehicle) -> Unit,
    private val onDelete: (CostWithVehicle) -> Unit
) : ListAdapter<CostWithVehicle, CostAdapter.CostViewHolder>(CostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cost, parent, false)
        return CostViewHolder(view, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: CostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CostViewHolder(
        itemView: View,
        private val onEdit: (CostWithVehicle) -> Unit,
        private val onDelete: (CostWithVehicle) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val textDate: TextView = itemView.findViewById(R.id.textCostDate)
        private val textVehicle: TextView = itemView.findViewById(R.id.textCostVehicle)
        private val textType: TextView = itemView.findViewById(R.id.textCostType)
        private val textAmount: TextView = itemView.findViewById(R.id.textCostAmount)
        private val textInfo: TextView = itemView.findViewById(R.id.textCostInfo)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditCost)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteCost)

        fun bind(cost: CostWithVehicle) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            textDate.text = sdf.format(Date(cost.date)).uppercase()
            textVehicle.text = cost.vehicleName
            textType.text = cost.type
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            textAmount.text = formatter.format(cost.amount)

            var info = ""
            if (cost.odometer != null) info += "${cost.odometer} km"
            if (cost.quantity != null) info += if (info.isEmpty()) "${cost.quantity} L" else " • ${cost.quantity} L"
            textInfo.text = info
            textInfo.visibility = if (info.isEmpty()) View.GONE else View.VISIBLE

            btnEdit.setOnClickListener { onEdit(cost) }
            btnDelete.setOnClickListener { onDelete(cost) }
        }
    }

    class CostDiffCallback : DiffUtil.ItemCallback<CostWithVehicle>() {
        override fun areItemsTheSame(oldItem: CostWithVehicle, newItem: CostWithVehicle): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CostWithVehicle, newItem: CostWithVehicle): Boolean = oldItem == newItem
    }
}
