package com.khalid.esimdemo.euicc

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf


@TargetApi(29)
class ESimApi26ModelFlow(final val context: Context) {

    private var euiccManager = context.getSystemService(Context.EUICC_SERVICE) as EuiccManager
    private var detailedCode: Int = -1
    private var mResultCode: Int = -1
    private var resultIntent: Intent = Intent()
    private val actionDownloadSubscription = "download_subscription"
    private val actionResolveError = "resolve_error"
    private val lpaDeclaredPermission = "com.singtel.hiappaccount.lpa.permission.BROADCAST"
    val allowedIntents = setOf(actionDownloadSubscription, actionResolveError)



    fun iseSimFeatureEnabled(): Boolean = euiccManager.isEnabled

    @SuppressLint("MissingPermission")
    fun downloadSubscription(encodedActivationCode: String): Flow<ESimStates> {
        callbackFlow<ESimStates> {
            val receiver: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if(!allowedIntents.contains(intent.action)){
                        trySendBlocking(ESimStates.ESimInstallationFailed(""))
                        return
                    }
                    when(intent.action) {
                        actionDownloadSubscription -> {

                        }
                        actionResolveError -> {

                        }
                        else -> {

                        }
                    }

                    detailedCode = intent.getIntExtra(
                        EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE,
                        0 /* defaultValue*/
                    )
                    resultIntent = intent

                }
            }

            context.registerReceiver(
                receiver,
                IntentFilter(actionDownloadSubscription),
                lpaDeclaredPermission /* broadcastPermission*/,
                null /* handler */
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
            awaitClose{
                context.unregisterReceiver(receiver)
            }
        }
        // TODO:
        return flowOf()
    }

}