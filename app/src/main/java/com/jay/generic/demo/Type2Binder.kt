package com.jay.generic.demo

import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.jay.generic.GenericAdapter

class Type2Binder :GenericAdapter.TypeBinder<Data.Type2>() {

    override fun getLayoutId(): Int =R.layout.adapter_item2

    override fun getViewType(): Int {
        return 2
    }

    override fun onClick(position: Int, view: View, data: Data.Type2) {
        //view click
        Toast.makeText(view.context, "click Type2 pos:${position}", Toast.LENGTH_SHORT)
            .show()
    }

    override fun onBind(
        holder: GenericAdapter.LayoutHolder,
        view: View,
        position: Int,
        data: Data.Type2,
        payloads: MutableList<Any>
    ) {

        val tv: TextView = holder.find(R.id.tv2)
        tv.text = data.t2
    }



}