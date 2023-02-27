package com.khalid.esimdemo.euicc

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccManager
import android.util.Log
import androidx.core.content.ContextCompat


@TargetApi(29)
class ESimApi26Model(private val context: Context, private val activity: Activity?) {

    private var euiccManager = context.getSystemService(Context.EUICC_SERVICE) as EuiccManager
    private val actionDownloadSubscription = "download_subscription"
    private val actionResolveError = "resolve_error"
    private val lpaDeclaredPermission = "com.singtel.hiappaccount.lpa.permission.BROADCAST"
    val allowedIntents = setOf(actionDownloadSubscription, actionResolveError)
    private var mCallback: (ESimStates) -> Unit = {}

    private val downloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // check is only supported intent are handled
            if(!allowedIntents.contains(intent.action)){
                mCallback.invoke(ESimStates.ESimInstallationFailed("No Matching Intent found!"))
                return
            }
            val opCodes = getOperationCodes(intent = intent)
            when(resultCode){
                // if error has occurred in either case try to resolve it
                EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR -> {
                    resolveInstallationErrors(intent)
                    return
                }

                EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR -> {
                    mCallback.invoke(ESimStates.ESimInstallationFailed("failed to install profile! $opCodes"))
                    return
                }

                EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK -> {
                    mCallback.invoke(ESimStates.ESimInstallationSuccess(" detailed code ${opCodes.detailedCode}"))
                    return
                }

                else -> {
                    mCallback.invoke(ESimStates.ESimInstallationFailed("failed to install profile! $opCodes"))
                }
            }
        }
    }

    fun getOperationCodes(intent: Intent): OperationCodes{
        val operationCodes = OperationCodes.toOpCodes(intent = intent)
        Log.d("ESimApi26Model", "op codes $operationCodes")
        return  operationCodes
    }

    fun resolveInstallationErrors(resultIntent: Intent){
        val intent = Intent(actionResolveError)
        val pendingIntentResolvable = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        activity?.run {
            runSafe ( {
                euiccManager.startResolutionActivity(
                    this,
                    0,
                    resultIntent,
                    pendingIntentResolvable)
            }, {
                mCallback.invoke(ESimStates.ESimInstallationFailed(it.message ?: "Failed to start resolve errors"))
            })

        }


    }

    fun iseSimFeatureEnabled(): Boolean = euiccManager.isEnabled

    /** ensure that app's SHA256 key string in set in SMDP+ meta data for this function to install
     *  esim profile
     */
    @SuppressLint("MissingPermission")
    fun downloadSubscription(
        encodedActivationCode: String,
        callback: (ESimStates) -> Unit) {
        // save the lambda
        mCallback = callback
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            IntentFilter(actionDownloadSubscription),
            lpaDeclaredPermission,
            null, /* handler */
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Download subscription asynchronously.
        val sub = DownloadableSubscription
            .forActivationCode(encodedActivationCode /* encodedActivationCode*/)
        val intent = Intent(actionDownloadSubscription)
        val callbackIntent = PendingIntent.getBroadcast(
            context, 0 /* requestCode */, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        euiccManager.downloadSubscription(
            sub,
            true /* switchAfterDownload */,
            callbackIntent
        )
        return
    }


}

data class OperationCodes(
    val detailedCode: String? = "",
    val errorCode: String? = "",
    val smdxSubjectCode: String? = "",
    val smdxReasonCode: String? = "",
) {
    companion object {
        val detailedCode: String = "EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE"
        val errorCode: String = "EXTRA_EMBEDDED_SUBSCRIPTION_ERROR_CODE"
        val smdxSubjectCode: String = "EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_SUBJECT_CODE"
        val smdxReasonCode: String = "EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_REASON_CODE"

        fun toOpCodes(intent: Intent): OperationCodes{
            intent.run {
                return OperationCodes(
                    detailedCode = getStringExtra(detailedCode),
                    errorCode = getStringExtra(errorCode),
                    smdxSubjectCode = getStringExtra(smdxSubjectCode),
                    smdxReasonCode = getStringExtra(smdxReasonCode),
                )
            }
        }
    }

}