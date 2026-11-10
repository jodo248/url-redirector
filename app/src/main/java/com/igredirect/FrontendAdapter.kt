package com.igredirect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.igredirect.databinding.ItemFrontendBinding

class FrontendAdapter(
    private val items: MutableList<FrontendInstance>,
    private val onToggle: (FrontendInstance, Boolean) -> Unit,
    private val onDelete: (FrontendInstance) -> Unit
) : RecyclerView.Adapter<FrontendAdapter.VH>() {

    inner class VH(val b: ItemFrontendBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFrontendBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        h.b.tvName.text = item.name
        h.b.tvUrl.text  = item.url

        // Prevent listener firing during rebind
        h.b.switchEnabled.setOnCheckedChangeListener(null)
        h.b.switchEnabled.isChecked = item.enabled
        h.b.switchEnabled.setOnCheckedChangeListener { _, checked ->
            onToggle(item, checked)
        }

        // Built-in items can't be deleted
        h.b.btnDelete.alpha = if (item.isBuiltIn) 0.3f else 1f
        h.b.btnDelete.isEnabled = !item.isBuiltIn
        h.b.btnDelete.setOnClickListener { onDelete(item) }
    }

    fun replace(newItems: List<FrontendInstance>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
