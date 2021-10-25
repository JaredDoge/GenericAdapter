package com.jay.generic.demo

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.jay.generic.GenericAdapter


class Type1Binder :GenericAdapter.TypeBinder<Data.Type1>(){


    override fun getLayoutId(): Int = R.layout.adapter_item1

    //設定viewType 多type時必須設定不同的viewType，預設為0
    //如果是單獨type 不需設定
    override fun getViewType(): Int {
        return 1
    }


    override fun onCreateView(
        holder: GenericAdapter.LayoutHolder,
        view: View,
        parent: ViewGroup,
        viewType: Int
    ) {
        //test_payload
        holder.payload = "test_payload"
    }

    override fun onBind(
        holder: GenericAdapter.LayoutHolder,
        view: View,
        position: Int,
        data: Data.Type1,
        payloads: MutableList<Any>
    ) {

        val tv: TextView = holder.find(R.id.tv)
        tv.text = data.t1

        /**
         * 這裡的payload跟 上方的參數payloads不同
         * 這裡的payload 是 holder在create時可帶入一些自定義的參數
         * 而payloads
         * @see androidx.recyclerview.widget.RecyclerView.Adapter.onBindViewHolder(VH, int, java.util.List<java.lang.Object>)
         * */

        Log.d("GenericAdapter",holder.payload.toString())

    }

    override fun onClick(position: Int, view: View, data: Data.Type1) {
        super.onClick(position, view, data)
        //view click
        Toast.makeText(view.context, "click Type1 pos:${position}", Toast.LENGTH_SHORT)
            .show()
    }





}