package com.jay.generic.more

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jay.generic.GenericAdapter


class LoadMoreListenerImpl<MODEL>(private val adapter: GenericAdapter<*>) : RecyclerView.OnScrollListener(),
    LoadMore<MODEL> {


    private var lock = false

    //剩餘多少item時加載更多
    private var prefetchDistance = 0

    private var callback:LoadMore.Callback? = null


    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        callback?.let {


            if (!lock
                && adapter.getState() == GenericAdapter.InState.NORMAL
                && adapter.isDataLoaded()
            ) {
                //目前只支持 LinearLayoutManager
                // 計算是否要加載
                val m = recyclerView.layoutManager
                if (m is LinearLayoutManager) {
                    val itemCount: Int = m.getItemCount()
                    val lastPosition: Int = m.findLastCompletelyVisibleItemPosition()+1
                    val canLoad = itemCount - lastPosition <= prefetchDistance

                    if(canLoad) {
                       // recyclerView.post {
                        callback?.more()
                       // }
                    }
                }
            }
        }

    }


    override fun prefetchDistance(count: Int): LoadMore<MODEL> {
        if(count<0){
            throw Throwable("prefetchDistance must > 0 !!")
        }
        this.prefetchDistance = count
        return this
    }

    override fun  setListener(callback: LoadMore.Callback): LoadMore<MODEL> {
        this.callback=callback
        return this
    }


    override fun lock(): LoadMore<MODEL> {
        lock=true
        return this
    }

    override fun unlock(): LoadMore<MODEL> {
        lock=false
        return this
    }




}


