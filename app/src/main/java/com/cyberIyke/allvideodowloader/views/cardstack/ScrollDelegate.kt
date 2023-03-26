package com.cyberIyke.allvideodowloader.views.cardstack

interface ScrollDelegate {
    fun scrollViewTo(x: Int, y: Int)
    var viewScrollY: Int
    var viewScrollX: Int
}