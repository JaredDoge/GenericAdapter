# GenericAdapter
[![](https://jitpack.io/v/JaredDoge/GenericAdapter.svg)](https://jitpack.io/#JaredDoge/GenericAdapter)

Easy RecyclerView Adapter
# Getting started
build.gradle
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
```
dependencies {
	        implementation 'com.github.JaredDoge:GenericAdapter:{latest_version}'
	}
```
# Usage
kotlin
```
   recyclerview.layoutManager = LinearLayoutManager(context)

   adapter= GenericAdapter(list)
            .addBinder(R.layout.adapter_item1){
                onBindView { holder, view, pos, data, payloads ->
                    val tv:TextView = holder.find(R.id.tv)
                    tv.text=data.msg
                }
            }
            .addItemClick { view, position, data ->
                  //do someing
            }
 
   recyclerview.adapter=adapter

```
# MultiType
SampleModel
```
sealed class Data {

    data class Type1(val t1: String) : Data()

    data class Type2(val t2: String) : Data()
}
```
Adaptaer
```
 adapter= GenericAdapter(list)
            .setTypeCondition { pos, obj ->
            
                //如果為multiType 則必須呼叫，Generic才能判斷type
                //這裡的type 對應你的TypeBinder 裡的 getViewType()
                
                if(obj is Data.Type1) 1 else 2
            }
            .addTypeBinder<Data.Type1>(R.layout.adapter_item1){
                onBindView { holder, view, pos, data, payloads ->
                   //type 1 bind view
                   val v1= holder.find<TextView>(R.id.tv)
                   v1.text=data.t1 
                }
            }
            .addTypeBinder<Data.Type2>(R.layout.adapter_item2){
                onBindView { holder, view, pos, data, payloads -> 
                    //type 2 bind view
                    val v2= holder.find<TextView>(R.id.tv2)
                    v2.text=data.t2
                }
            }                    

```
#State
提供三種狀態error、empty、loading顯示的畫面
```
GenericAdapter(list)
   ...
   .emptyBinder(R.layout.adapter_empty)//當dataList size為0時 會自動顯示emptyView
   
   .errorBinder(R.layout.adapter_error){//當要顯示errorView時需手動setState(GenericAdapter.State.ERROR)
      onBindView { holder, v ->
                    val btn:Button = holder.find(R.id.btn_retry)
                    btn.setOnClickListener {
                        adapter.submitList(getData())
                    }
      }
    }
   .loadingBinder(R.layout.adapter_loading)//當要顯示loadingView時需手動setState(GenericAdapter.State.LOADING)
```
# LoadMore
```
adapter.getLoadMore().setListener(
            object :LoadMore.Callback{
                override fun more() {
                  //load more
                  adapter.getLoadMore().lock()
                  //get more data 
                
                }
            }
        )
```
何時載入更多?
```
adapter.getLoadMore().prefetchDistance(5)//還剩多少的item時載入更多，預設為0(到最底才載入)
```
載入前，呼叫lock()，防止多次上滑載入
```
adapter.getLoadMore().lock()
```
載入完，呼叫unlock()，如果已經完全載入完成則不必呼叫。
```
adapter.getLoadMore().unlock()
```
# Submit
呼叫adapter.submitList()更新data，內部實現DiffUtil計算出兩個列表間差異，自動實現非全量更新。
```
adapter.submitList(getData())
```
當然，你也可以使用原生的更新方式
```
notifyItemChanged(int)
notifyItemInserted(int)
notifyItemRemoved(int)
...
```



   
