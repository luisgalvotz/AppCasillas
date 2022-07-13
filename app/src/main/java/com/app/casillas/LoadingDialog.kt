package com.app.casillas

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater

class LoadingDialog {
    private var activity: Activity
    private lateinit var dialog: AlertDialog

    constructor(myActivity: Activity) {
        activity = myActivity
    }

    fun startLoadingDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val inflater: LayoutInflater = activity.layoutInflater

        builder.setView(inflater.inflate(R.layout.loading_dialog, null))
        builder.setCancelable(false)

        dialog = builder.create()
        dialog.show()
    }

    fun dismissLoadingDialog() {
        dialog.dismiss()
    }
}