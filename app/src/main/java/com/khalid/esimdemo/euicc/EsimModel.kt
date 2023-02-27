package com.khalid.esimdemo.euicc

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions


class EsimModel(final val context: Context) : ESIMModuleInterface {

    private val eSimApi26Model: ESimApi26Model = ESimApi26Model(context, null)

    private fun isAboveApi9() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    override fun isESimSupported(): Boolean  = isAboveApi9() && eSimApi26Model.iseSimFeatureEnabled()

    override fun scanQRCode() {
        // Register the launcher and result handler
        //getCurrentActivity() from react Native



    }

    override fun installeSim(encodedActivationCode: String) {
        // validate
        if(!isESimSupported()) {
            // TODO: return state in promise
            return
        }

        val callback: (ESimStates) -> Unit = { state ->
            when(state) {
                is ESimStates.ESimInstallationSuccess -> {
                    // TODO: return state in promise

                }

                is ESimStates.ESimInstallationFailed -> {
                    // TODO: return state in promise
                }
            }
        }
        eSimApi26Model.downloadSubscription(encodedActivationCode, callback)
    }


    private fun launchScanner(activity: ComponentActivity){
        // Register the launcher and result handler
        val barcodeLauncher: ActivityResultLauncher<ScanOptions> = activity.registerForActivityResult(
            ScanContract()
        ) { result ->
            if (result.contents == null) {
                Toast.makeText(activity, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    activity,
                    "Scanned: " + result.contents,
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    }


}

inline fun <T> T.runSafe(block: T.() -> Unit, error: (Exception) -> Unit = {}) {
    if(this == null){
        error(Exception("Object is null"))
        return
    }
    try{
        block()
    }catch (e: Exception){
        error(e)
    }
}

