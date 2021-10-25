package com.jay.generic.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.jay.generic.GenericAdapter
import com.jay.generic.demo.databinding.ActivityMainBinding
import com.jay.generic.more.DefaultCursor
import com.jay.generic.more.LoadMore
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var adapter:GenericAdapter<Data>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val list = getData()

        binding.rv.layoutManager = LinearLayoutManager(this)

        adapter= GenericAdapter(list)

            //如果為multiType 則必須呼叫，Generic才能判斷type
            //這裡的type 對應你的TypeBinder 裡的 getViewType()
            .setTypeCondition { pos, obj ->
                if(obj is Data.Type1) 1 else 2
            }

            .addBinders(Type1Binder(),Type2Binder())

            .addItemClick { view, position, data ->
                Log.d("GenericAdapter","itemClick : $position")
            }
            //當dataList size為0時 會自動顯示emptyView
            .emptyBinder(R.layout.adapter_empty)
            //當要顯示errorView時需手動setState(GenericAdapter.State.ERROR)
            .errorBinder(R.layout.adapter_error)
            //當要顯示loadingView時需手動setState(GenericAdapter.State.LOADING)
            .loadingBinder(R.layout.adapter_loading)


        adapter.getLoadMore().setListener(
            object :LoadMore.Callback{
                override fun more() {
                  //load more
                  adapter.getLoadMore().lock()
                  //mock
                  Handler(Looper.getMainLooper()).postDelayed({
                        adapter.submitList(getMoreData(adapter.getDataList(),20))
                        adapter.getLoadMore().unlock()
                  },3000)
                }
            }
        )

        binding.rv.adapter=adapter


        binding.btnData.setOnClickListener {
            adapter.submitList(getData())
        }

        binding.btnEmpty.setOnClickListener {
            adapter.submitList(mutableListOf())
        }

        binding.btnError.setOnClickListener {
           adapter.setState(GenericAdapter.State.ERROR)
        }

        binding.btnLoading.setOnClickListener {
            adapter.setState(GenericAdapter.State.LOADING)
        }

    }

    private fun getRandom(): Int {
        return (Math.random()*50).toInt()+1
    }


    private fun getData(): List<Data> {
        val list = mutableListOf<Data>()
        for (index in 0..getRandom()) {
            if (index % 2 == 0) {
                list.add(Data.Type1("$index"))
            } else {
                list.add(Data.Type2("$index"))
            }
        }
        return list
    }

    private fun getMoreData(old:List<Data>,c:Int):List<Data>{
        val list= mutableListOf<Data>()
        list.addAll(old)
        for (index in old.size..old.size+c){
            if (index % 2 == 0) {
                list.add(Data.Type1("$index"))
            } else {
                list.add(Data.Type2("$index"))
            }
        }

        return list
    }
}