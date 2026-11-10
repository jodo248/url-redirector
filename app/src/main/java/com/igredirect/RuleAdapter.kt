package com.igredirect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.igredirect.databinding.ItemRuleBinding

class RuleAdapter(
    private val items: MutableList<RedirectRule>,
    private val onToggle: (RedirectRule, Boolean) -> Unit,
    private val onDelete: (RedirectRule) -> Unit
) : RecyclerView.Adapter<RuleAdapter.VH>() {

    inner class VH(val b: ItemRuleBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        h.b.tvName.text    = item.name
        h.b.tvPattern.text = item.pattern

        h.b.switchEnabled.setOnCheckedChangeListener(null)
        h.b.switchEnabled.isChecked = item.enabled
        h.b.switchEnabled.setOnCheckedChangeListener { _, checked ->
            onToggle(item, checked)
        }

        h.b.btnDelete.alpha     = if (item.isBuiltIn) 0.3f else 1f
        h.b.btnDelete.isEnabled = !item.isBuiltIn
        h.b.btnDelete.setOnClickListener { onDelete(item) }
    }

    fun replace(newItems: List<RedirectRule>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
