package com.jay.generic

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config


@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
internal class GenericAdapterTest {

    data class TestItem(val data: String)

    sealed class TestMultiTypeItem(val type: Int) {
        data class Success(val data: String) : TestMultiTypeItem(100)
        object Failure : TestMultiTypeItem(999)
    }

    lateinit var adapter: GenericAdapter<TestItem>

    lateinit var multiAdapter: GenericAdapter<TestMultiTypeItem>

    @Before
    fun setUp() {
        val data = mutableListOf(TestItem("data1"), TestItem("data2"))
        val mulData = mutableListOf(TestMultiTypeItem.Success("success"), TestMultiTypeItem.Failure)
        adapter = GenericAdapter(data)
        multiAdapter = GenericAdapter(mulData)
            .setTypeCondition { pos, obj ->
                obj.type
            }
    }

    @Test
    fun getItem() {


        //normal
        val item = adapter.getItem(0)
        assertThat(item).isEqualTo(TestItem("data1"))

        //error
        adapter.setState(GenericAdapter.State.ERROR)
        Assert.assertThrows(IndexOutOfBoundsException::class.java) {
            adapter.getItem(0)
        }

        //loading
        adapter.setState(GenericAdapter.State.LOADING)
        Assert.assertThrows(IndexOutOfBoundsException::class.java) {
            adapter.getItem(0)
        }

        //empty
        adapter.submitList(mutableListOf())
        Assert.assertThrows(java.lang.IndexOutOfBoundsException::class.java) {
            adapter.getItem(0)
        }

    }

    @Test
    fun getMultiItem() {
        //normal
        val item = multiAdapter.getItem(0)
        assertThat(item).isEqualTo(TestMultiTypeItem.Success("success"))

        //error
        multiAdapter.setState(GenericAdapter.State.ERROR)
        Assert.assertThrows(IndexOutOfBoundsException::class.java) {
            multiAdapter.getItem(0)
        }

        //loading
        multiAdapter.setState(GenericAdapter.State.LOADING)
        Assert.assertThrows(IndexOutOfBoundsException::class.java) {
            multiAdapter.getItem(0)
        }

        //empty
        multiAdapter.submitList(mutableListOf())
        Assert.assertThrows(java.lang.IndexOutOfBoundsException::class.java) {
            multiAdapter.getItem(0)
        }
    }

    @Test
    fun getItemCount() {

        val count = adapter.itemCount
        assertThat(count).isEqualTo(2)

        //error
        adapter.setState(GenericAdapter.State.ERROR)
        assertThat(adapter.itemCount).isEqualTo(1)

        //loading
        adapter.setState(GenericAdapter.State.LOADING)
        assertThat(adapter.itemCount).isEqualTo(1)

        //empty
        adapter.submitList(mutableListOf())
        assertThat(adapter.itemCount).isEqualTo(1)

    }

    @Test
    fun getMultiItemCount() {

        val count = multiAdapter.itemCount
        assertThat(count).isEqualTo(2)

        //error
        multiAdapter.setState(GenericAdapter.State.ERROR)
        assertThat(multiAdapter.itemCount).isEqualTo(1)

        //loading
        multiAdapter.setState(GenericAdapter.State.LOADING)
        assertThat(multiAdapter.itemCount).isEqualTo(1)

        //empty
        multiAdapter.submitList(mutableListOf())
        assertThat(multiAdapter.itemCount).isEqualTo(1)

    }


    @Test
    fun getItemViewType() {

        val type = adapter.getItemViewType(0)
        assertThat(type).isEqualTo(0)

        //error
        adapter.setState(GenericAdapter.State.ERROR)
        val typeError=adapter.getItemViewType(0)
        assertThat(typeError).isEqualTo(-3)

        //loading
        adapter.setState(GenericAdapter.State.LOADING)
        val typeLoad=adapter.getItemViewType(0)
        assertThat(typeLoad).isEqualTo(-2)

        //empty
        adapter.submitList(mutableListOf())
        val typeEmpty=adapter.getItemViewType(0)
        assertThat(typeEmpty).isEqualTo(-1)
    }

    @Test
    fun getMultiItemViewType() {

        val type = multiAdapter.getItemViewType(0)
        assertThat(type).isEqualTo(100)

        //error
        multiAdapter.setState(GenericAdapter.State.ERROR)
        val typeError=multiAdapter.getItemViewType(0)
        assertThat(typeError).isEqualTo(-3)

        //loading
        multiAdapter.setState(GenericAdapter.State.LOADING)
        val typeLoad=multiAdapter.getItemViewType(0)
        assertThat(typeLoad).isEqualTo(-2)

        //empty
        multiAdapter.submitList(mutableListOf())
        val typeEmpty=multiAdapter.getItemViewType(0)
        assertThat(typeEmpty).isEqualTo(-1)
    }

    @Test
    fun setState(){

        adapter.setState(GenericAdapter.State.LOADING)

        val state=adapter.getState()

        assertThat(state).isEqualTo(GenericAdapter.InState.LOADING)

    }

    @Test
    fun addBinder(){

        val binder=object:GenericAdapter.TypeBinder<TestItem>(){
            override fun getLayoutId(): Int =
                -1
        }
        adapter.addBinders(binder)

        val binders=adapter.getBinders()

        assertThat(binders).contains(binder)

    }



}