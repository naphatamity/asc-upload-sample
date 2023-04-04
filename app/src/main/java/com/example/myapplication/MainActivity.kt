package com.example.myapplication

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.amity.socialcloud.sdk.api.core.AmityCoreClient
import com.amity.socialcloud.sdk.core.session.AccessTokenRenewal
import com.amity.socialcloud.sdk.core.session.model.SessionState
import com.amity.socialcloud.sdk.model.core.file.upload.AmityUploadResult
import com.amity.socialcloud.sdk.model.core.session.SessionHandler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers


class MainActivity : AppCompatActivity() {
    private fun getSessionHandler(): SessionHandler {
        return object : SessionHandler {
            override fun sessionWillRenewAccessToken(renewal: AccessTokenRenewal) {
                renewal.renew()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AmityCoreClient.setup(
            apiKey = "apiKey"
        ).subscribe()
        observeSessionState()
        AmityCoreClient.login(userId = "userId",getSessionHandler()).build().submit().doOnComplete {
            openImagePicker()
        }.subscribe()
    }

    private fun observeSessionState() {
        AmityCoreClient.observeSessionState()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { sessionState: SessionState ->
                // Get observe session state here
                when (sessionState) {
                    is SessionState.NotLoggedIn,
                    is SessionState.Establishing -> {
                        // openLoginPage()
                        Log.e("status ", "openLoginPage")
                    }
                    is SessionState.Established,
                    is SessionState.TokenExpired -> {
                        // openHomePage()
                        Log.e("status ", "openHomePage")
                   //     joinChannel()
                    }
                    is SessionState.Terminated -> {
                        // openUserBanPage()
                        Log.e("status ", "openUserBanPage")
                    }
                }
            }
            .subscribe()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, 100)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val uris = mutableListOf<Uri>()
            if (data?.clipData != null) {
                // Multiple images were selected
                val clipData = data.clipData
                for (i in 0 until clipData!!.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    uris.add(uri)
                }
            } else if (data?.data != null) {
                // Single image was selected
                val uri = data.data!!
                uris.add(uri)
            }
            Log.e("FILES",uris.toString())
            uploadMedia(uris[0])
        }
    }

    fun uploadMedia(postMedia: Uri) {
        val fileRepository = AmityCoreClient.newFileRepository();
        fileRepository
            .uploadImage(postMedia)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                Log.e("uploadStatus.getFile()",it.message.toString())
            }
            .doOnNext {
                    uploadStatus ->
                when (uploadStatus) {
                    is AmityUploadResult.COMPLETE -> {
                        Log.e("uploadStatus.getFile()",uploadStatus.getFile().getFileId())
                    }
                    is AmityUploadResult.ERROR, AmityUploadResult.CANCELLED -> {
                        Log.e("uploadStatus.getFile()", uploadStatus.toString())
                    }
                    else -> {
                        Log.e("uploadStatus.getFile()", uploadStatus.toString())
                    }
                }
            }
            .subscribe()
    }
}