package com.jay.generic.more

import androidx.annotation.IntRange

interface LoadMore<MODEL> {


    //還剩多少的item時載入更多，預設為0(到最底才載入)
    fun prefetchDistance(@IntRange(from = 0) count: Int): LoadMore<MODEL>

    fun setListener(callback: Callback): LoadMore<MODEL>

    //防止無限上滑多次載入，loading或load finish 時呼叫
    fun lock(): LoadMore<MODEL>

    fun unlock() : LoadMore<MODEL>

    interface Callback{
        fun more()
    }
}