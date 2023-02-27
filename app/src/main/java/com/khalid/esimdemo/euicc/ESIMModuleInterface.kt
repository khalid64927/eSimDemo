package com.khalid.esimdemo.euicc

interface ESIMModuleInterface {
    fun isESimSupported(): Boolean
    fun scanQRCode()
    fun installeSim(activationCode: String)
}