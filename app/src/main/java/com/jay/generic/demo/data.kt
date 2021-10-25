package com.jay.generic.demo

sealed class Data {

    data class Type1(val t1: String) : Data()

    data class Type2(val t2: String) : Data()
}