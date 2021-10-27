package com.jay.generic

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.util.containsKey
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.jay.generic.more.LoadMore
import com.jay.generic.more.LoadMoreListenerImpl


open class GenericAdapter<T>() :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TAG = "GenericAdapter"
        private const val VIEW_TYPE_EMPTY = -1

        private const val VIEW_TYPE_LOADING = -2

        private const val VIEW_TYPE_ERROR = -3
    }

    private val dataList: MutableList<T> = mutableListOf()

    private val binders: MutableList<TypeBinder<*>> = mutableListOf()

    private var viewStateBinders: SparseArray<ViewStateBinder> = SparseArray()

    private var typeCondition = { _: Int, _: T -> 0 }

    private val callbacks: MutableList<ItemClick<T>> = mutableListOf()

    private var itemsTheSame = { _: T, _: T, oldPos: Int, newPost: Int -> oldPos == newPost }

    private var contentsTheSame = { oldItem: T, newItem: T, _: Int, _: Int -> oldItem == newItem }

    private var diffCallback = DiffCallback()

    private var detectMoves: Boolean? = null

    private var recyclerView: RecyclerView? = null

    private var defaultItemAnimator: RecyclerView.ItemAnimator? = null

    private var dataLoaded: Boolean = false

    private var inState: InState = InState.NORMAL


    private val loadMoreImpl: LoadMoreListenerImpl<T> by lazy { LoadMoreListenerImpl<T>(this) }

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            notifyItemRangeChanged(position, count, payload)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onInserted(position: Int, count: Int) {
            notifyItemRangeInserted(position, count)
//            if (position == 0) {
//                recyclerView?.scrollToPosition(0)
//            }
        }

        override fun onRemoved(position: Int, count: Int) {
            notifyItemRangeRemoved(position, count)
        }

    }

    constructor(list: List<T>) : this() {
        if (list.isNotEmpty()) {
            dataList.addAll(list)
        }
        dataLoaded = true
    }


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        defaultItemAnimator = recyclerView.itemAnimator
        recyclerView.itemAnimator = null
        recyclerView.addOnScrollListener(loadMoreImpl)

    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.itemAnimator = defaultItemAnimator
        recyclerView.removeOnScrollListener(loadMoreImpl)
        this.recyclerView = null
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when {
            viewType == VIEW_TYPE_EMPTY || viewType == VIEW_TYPE_ERROR
                    || viewType == VIEW_TYPE_LOADING
            -> {
                val binder = getViewTypeBinder(viewType)
                val v = LayoutInflater.from(parent.context)
                        .inflate(binder.getLayoutId(), parent, false)
                val holder = ViewStateHolder(v)
                v.setOnClickListener {
                    binder.onClick(it)
                }
                binder.onCreateView(holder,holder.view,parent)
                return holder
            }

            viewType >= 0 -> {
                val binder = getBinder(viewType)

                val v =
                        LayoutInflater.from(parent.context).inflate(binder.getLayoutId(), parent, false)
                val holder =
                        LayoutHolder(v)
                v.setOnClickListener {

                    binder.handleClick(
                            holder.layoutPosition,
                            it,
                            getDataList()[holder.layoutPosition]
                    )
                    postClick(
                            holder.view,
                            holder.layoutPosition,
                            getDataList()[holder.layoutPosition]
                    )
                }
                binder.onCreateView(holder, holder.view, parent, viewType)
                return holder

            }
            else -> {
                throw NullPointerException("View is null !! You must use TypeBinder<>")
            }
        }
    }


    private fun getBinder(viewType: Int): TypeBinder<*> {
        return binders.singleOrNull { it.getViewType() == viewType }
                ?: throw IllegalArgumentException(
                        "viewType '${viewType}' not found " +
                                ",If it is multi-type, you must call setTypeCondition() and  override getViewType()"
                )
    }


    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun getViewTypeBinder(viewType: Int): ViewStateBinder {
        return viewStateBinders[viewType]
                ?: throw NullPointerException(
                        "state '${
                            when (viewType) {
                                VIEW_TYPE_EMPTY -> "EMPTY"
                                VIEW_TYPE_ERROR -> "ERROR"
                                VIEW_TYPE_LOADING -> "LOADING"
                                else -> viewType
                            }
                        }' not found " +
                                ",Did you forget to bind?"
                )
    }


    private fun postClick(view: View, position: Int, data: T) {
        onClick(view, position, data)
        for (c in callbacks) {
            c.onClick(view, position, data)
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

    }

    override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
            payloads: MutableList<Any>,
    ) {

        when (holder) {
            is LayoutHolder -> {
                val binder = getBinder(getItemViewType(position))

                    binder.handleBindPayload(holder,
                            holder.view,
                            position,
                            getDataList()[position],
                            payloads)
            }

            is ViewStateHolder -> {
                val binder = getViewTypeBinder(getItemViewType(position))
                binder.onBind(holder,holder.view)
            }

        }

    }


    override fun getItemCount(): Int
    //資料如果已經載入，並且list 為空
            = when {
        inState == InState.LOADING || inState == InState.ERROR -> 1
        checkEmptyState() -> {
            inState = InState.EMPTY
            1
        }
        else -> {
            inState = InState.NORMAL
            getDataList().size
        }
    }


    override fun getItemViewType(position: Int): Int = when (inState) {
        InState.ERROR -> VIEW_TYPE_ERROR
        InState.LOADING -> VIEW_TYPE_LOADING
        InState.EMPTY -> VIEW_TYPE_EMPTY
        InState.NORMAL ->
            typeCondition(position, getDataList()[position]).apply {
                if (this < 0) throw IllegalArgumentException("typeCondition error: viewType must > 0")
            }
    }


    private fun checkEmptyState() =
            dataLoaded && getDataList().isEmpty() && viewStateBinders.containsKey(VIEW_TYPE_EMPTY)


    private fun <TYPE> addTypeBinder(

        @LayoutRes layoutId: Int,
        type: Int = 0,
        create: (holder: LayoutHolder, v: View, p: ViewGroup, vt: Int) -> Unit = { _: LayoutHolder, _: View, _: ViewGroup, _: Int -> },
        bind: (h: LayoutHolder, v: View, pos: Int, da: TYPE, payloads: MutableList<Any>) -> Unit = { _: LayoutHolder, _: View, _: Int, _: TYPE, _: MutableList<Any> -> },
        click: (pos: Int, v: View, da: TYPE) -> Unit = { _: Int, _: View, _: TYPE -> },
    ): GenericAdapter<T> {
        addBinders(object : TypeBinder<TYPE>() {
            override fun getLayoutId(): Int = layoutId
            override fun getViewType(): Int = type
            override fun onBind(
                holder: LayoutHolder,
                view: View,
                position: Int,
                data: TYPE,
                payloads: MutableList<Any>
            ) = bind(holder, view, position, data, payloads)

            override fun onCreateView(
                holder: LayoutHolder,
                view: View,
                parent: ViewGroup,
                viewType: Int,
            ) = create(holder, view, parent, viewType)

            override fun onClick(position: Int, view: View, data: TYPE) = click(position, view, data)

        })


        return this
    }


    private fun handlerViewStateBinder(
        @LayoutRes layoutId: Int,
        state: Int,
        b: ViewStateBuilder.() -> Unit,
    ) {
        val eb = ViewStateBuilder().apply(b)
        viewStateBinders.put(state, object : ViewStateBinder() {
            override fun getLayoutId(): Int = layoutId

            override fun onBind(holder: ViewStateHolder, view: View) = eb.mBind(holder,view)

            override fun onClick(v: View) = eb.mClick(v)

            override fun onCreateView(holder: ViewStateHolder, view: View, parent: ViewGroup) = eb.mCreate(holder,view, parent)

        })
    }


    /* 對外fun */

    fun getLoadMore() = loadMoreImpl as LoadMore<T>

    fun setState(state: State): GenericAdapter<T> {
        this.inState = when (state) {
            State.ERROR -> InState.ERROR
            State.LOADING -> InState.LOADING
        }
        dataList.clear()
        notifyDataSetChanged()
        return this
    }

    fun getState(): InState = inState

    fun isDataLoaded() = dataLoaded

    fun <TYPE> addTypeBinder(
        @LayoutRes layoutId: Int,
        binder: TypeBuilder<TYPE>.() -> Unit,
    ): GenericAdapter<T> {
        val bb = TypeBuilder<TYPE>().apply(binder)
        return addTypeBinder(layoutId, bb.mType, bb.mCreate, bb.mBind, bb.mClick)
    }


    fun addBinder(
        @LayoutRes layoutId: Int,
        binder: TypeBuilder<T>.() -> Unit = {},
    ): GenericAdapter<T> {
        val bb = TypeBuilder<T>().apply(binder)
        return addTypeBinder(layoutId, bb.mType, bb.mCreate, bb.mBind, bb.mClick)
    }


    fun emptyBinder(
        @LayoutRes layoutId: Int,
        empty: ViewStateBuilder.() -> Unit = {},
    ): GenericAdapter<T> {
        handlerViewStateBinder(layoutId, VIEW_TYPE_EMPTY, empty)
        return this
    }

    fun emptyBinder(empty: ViewStateBinder): GenericAdapter<T> {
        viewStateBinders.put(VIEW_TYPE_EMPTY, empty)
        return this
    }

    fun loadingBinder(
        @LayoutRes layoutId: Int,
        loading: ViewStateBuilder.() -> Unit = {},
    ): GenericAdapter<T> {
        handlerViewStateBinder(layoutId, VIEW_TYPE_LOADING, loading)
        return this
    }

    fun loadingBinder(loading: ViewStateBinder): GenericAdapter<T> {
        viewStateBinders.put(VIEW_TYPE_LOADING, loading)
        return this
    }


    fun errorBinder(
        @LayoutRes layoutId: Int,
        error: ViewStateBuilder.() -> Unit = {},
    ): GenericAdapter<T> {
        handlerViewStateBinder(layoutId, VIEW_TYPE_ERROR, error)
        return this
    }

    fun errorBinder(loading: ViewStateBinder): GenericAdapter<T> {
        viewStateBinders.put(VIEW_TYPE_ERROR, loading)
        return this
    }


    fun setTypeCondition(typeCondition: (pos: Int, obj: T) -> Int): GenericAdapter<T> {
        this.typeCondition = typeCondition
        return this
    }

    fun removeEmptyBinder(): GenericAdapter<T> {
        viewStateBinders.remove(VIEW_TYPE_EMPTY)
        return this
    }

    fun removeLoadingBinder(): GenericAdapter<T> {
        viewStateBinders.remove(VIEW_TYPE_LOADING)
        return this
    }

    fun removeErrorBinder(): GenericAdapter<T> {
        viewStateBinders.remove(VIEW_TYPE_ERROR)
        return this
    }

    fun addBinders(vararg binder: TypeBinder<*>): GenericAdapter<T> {
        for (b in binder) {
            if (b.getViewType() < 0) {
                throw IllegalArgumentException("viewType must > 0")
            }

            if (binders.any { it.getViewType() == b.getViewType() }) {
                throw IllegalArgumentException(
                        "viewType '${b.getViewType()}' is exist " +
                                ", you must override getViewType() and use different types"
                )
            }
        }
        binders.addAll(binder)
        return this
    }

    fun addItemClick(c: (view: View, position: Int, data: T) -> Unit): GenericAdapter<T> {
        addItemClick(object :
            ItemClick<T> {
            override fun onClick(view: View, position: Int, data: T) {
                c(view, position, data)
            }
        })
        return this
    }

    fun addItemClick(callback: ItemClick<T>): GenericAdapter<T> {
        if (!callbacks.contains(callback))
            callbacks.add(callback)
        return this
    }

    fun removeClick(callback: ItemClick<T>): GenericAdapter<T> {
        callbacks.remove(callback)
        return this
    }

    fun submitList(
            newDataList: List<T>,
            detectMoves: Boolean = this.detectMoves ?: false,
    ): GenericAdapter<T> {
        if (inState != InState.NORMAL || !dataLoaded) {
            update(newDataList)
        } else {
         //   loadMoreCheckIsLoadFinish(newDataList)
            dataLoaded = true
            inState = InState.NORMAL

            DiffUtil.calculateDiff(diffCallback(dataList, newDataList), detectMoves).apply {
                dataList.clear()
                dataList.addAll(newDataList)
            }.dispatchUpdatesTo(listUpdateCallback)


        }
        return this
    }


    private fun update(newDataList: List<T>): GenericAdapter<T> {

        dataList.clear()
        dataList.addAll(newDataList)

        dataLoaded = true
        inState = InState.NORMAL
        notifyDataSetChanged()
        return this
    }


    fun setItemsTheSame(itemsTheSame: (oldItem: T, newItem: T, oldPos: Int, newPos: Int) -> Boolean): GenericAdapter<T> {
        this.itemsTheSame = itemsTheSame
        return this
    }

    fun setContentsTheSame(contentsTheSame: (oldItem: T, newItem: T, oldPos: Int, newPos: Int) -> Boolean): GenericAdapter<T> {
        this.contentsTheSame = contentsTheSame
        return this
    }

    fun setDetectMoves(detectMoves: Boolean): GenericAdapter<T> {
        this.detectMoves = detectMoves
        return this
    }

    fun getDataList(): List<T> = dataList

    fun getItem(position: Int): T = getDataList()[position]


    protected open fun onClick(view: View, position: Int, data: T) {

    }


    /*inner class , interface*/

    interface ItemClick<T> {
        fun onClick(view: View, position: Int, data: T)
    }

    class ViewStateHolder(val view: View) : RecyclerView.ViewHolder(view) {

        private val viewMap: MutableMap<Int,View> = mutableMapOf()

        var payload: Any? = null

        fun <V:View> find(id: Int): V {
            var v = viewMap[id]

            if (v != null) return v as V

            v = view.findViewById(id)

            viewMap[id] = v

            return v as V
        }
    }

    class LayoutHolder(val view: View) : RecyclerView.ViewHolder(view) {

        private val viewMap: MutableMap<Int,View> = mutableMapOf()

        var payload: Any? = null


        fun <V:View> find(id: Int): V {
            var v = viewMap[id]

            if (v != null) return v as V

            v = view.findViewById(id)

            viewMap[id] = v

            return v as V
        }
    }


    abstract class ViewStateBinder {

        abstract fun getLayoutId(): Int

        open fun onCreateView(holder: ViewStateHolder, view: View, parent: ViewGroup) {}

        open fun onClick(v: View) {}

        open fun onBind(holder:ViewStateHolder,view: View) {}


    }


    @Suppress("UNCHECKED_CAST")
    abstract class TypeBinder<TYPE> {
        abstract fun getLayoutId(): Int

        open fun getViewType(): Int = 0

        open fun onCreateView(holder: LayoutHolder, view: View, parent: ViewGroup, viewType: Int) {}

        open fun onClick(position: Int, view: View, data: TYPE) {}

        open fun onBind(holder: LayoutHolder, view: View, position: Int, data: TYPE, payloads: MutableList<Any>) {

        }

        fun handleClick(position: Int, v: View, data: Any?) {
            onClick(position, v, data as TYPE)
        }

        fun handleBindPayload(
            holder: LayoutHolder, view: View, position: Int, data: Any?,
            payloads: MutableList<Any>,
        ) {
            onBind(holder, view, position, data as TYPE, payloads)
        }



    }

    inner class TypeBuilder<TYPE> {
        internal var mType: Int = 0

        internal var mCreate: (holder: LayoutHolder, v: View, p: ViewGroup, vt: Int) -> Unit =
                { _: LayoutHolder, _: View, _: ViewGroup, _: Int -> }

        internal var mClick: (pos: Int, v: View, da: TYPE) -> Unit = { _: Int, _: View, _: TYPE -> }



        internal var mBind: (h: LayoutHolder, v: View, pos: Int, da: TYPE, payloads: MutableList<Any>) -> Unit =
                { _: LayoutHolder, _: View, _: Int, _: TYPE, _: MutableList<Any> -> }


        fun onBindView(bind: (holder: LayoutHolder, view: View, pos: Int, data: TYPE,
                                        payloads: MutableList<Any>) -> Unit) {
            mBind = bind
        }

        fun getViewType(type: () -> Int) {
            mType = type()
        }

        fun onCreateView(create: (holder: LayoutHolder, view: View, viewGroup: ViewGroup, viewType: Int) -> Unit) {
            mCreate = create
        }

        fun onViewClick(click: (pos: Int, view: View, data: TYPE) -> Unit) {
            mClick = click
        }

    }


    inner class ViewStateBuilder {
        internal var mCreate: (holder: ViewStateHolder,v: View, p: ViewGroup) -> Unit = { _: ViewStateHolder,_: View, _: ViewGroup -> }
        internal var mClick: (v: View) -> Unit = { _: View -> }
        internal var mBind: (holder: ViewStateHolder,v: View) -> Unit = { _:ViewStateHolder ,_: View -> }
        fun onBindView(bind: (holder:ViewStateHolder,v: View) -> Unit) {
            mBind = bind
        }

        fun onCreateView(create: (holder: ViewStateHolder,v: View, p: ViewGroup) -> Unit) {
            mCreate = create
        }

        fun onViewClick(click: (v: View) -> Unit) {
            mClick = click
        }


    }


    inner class DiffCallback : DiffUtil.Callback() {

        private lateinit var oldList: List<T>

        private lateinit var newList: List<T>


        operator fun invoke(old: List<T>, new: List<T>): DiffUtil.Callback {
            oldList = old
            newList = new
            return this
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return itemsTheSame(
                    oldList[oldItemPosition],
                    newList[newItemPosition],
                    oldItemPosition,
                    newItemPosition
            )
        }


        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                contentsTheSame(
                        oldList[oldItemPosition],
                        newList[newItemPosition],
                        oldItemPosition,
                        newItemPosition
                )

    }

    enum class State {
        ERROR, LOADING
    }


    enum class InState {
        ERROR, EMPTY, NORMAL, LOADING
    }


}