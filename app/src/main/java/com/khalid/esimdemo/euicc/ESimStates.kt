package com.khalid.esimdemo.euicc

sealed class ESimStates() {
    data class ESimInstallationSuccess(val message: String): ESimStates()
    data class ESimInstallationFailed(val message: String): ESimStates()

}

