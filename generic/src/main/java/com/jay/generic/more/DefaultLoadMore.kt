package com.jay.generic.more

//abstract class DefaultLoadMore<T,MODEL>(private val limit:Int): LoadMore.Callback<DefaultCursor<T>,MODEL> {
//
//    override fun initParams(): DefaultCursor<T> = DefaultCursor(limit)
//
//    override fun mapParams(params: DefaultCursor<T>): DefaultCursor<T>
//      = params.copy(cursor=cursor())
//
////    override fun isLoadFinish(
////        old: List<MODEL>,
////        new: List<MODEL>,
////        params: DefaultCursor<T>
////    ): Boolean {
////        return new.size-old.size<limit
////    }
//
//    abstract fun cursor():T?
//}