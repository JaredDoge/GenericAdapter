package com.jay.generic.more

data class DefaultCursor<C>(val limit:Int, val cursor:C?=null)