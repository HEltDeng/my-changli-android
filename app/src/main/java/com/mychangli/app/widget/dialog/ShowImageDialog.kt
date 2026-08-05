package com.mychangli.app.widget.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import com.mychangli.app.R
import com.mychangli.app.utils.GlideUtils

class ShowImageDialog(context: Context, private val imageUrl: String) : Dialog(
    context, R.style.CustomDialog
) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId())
        init()
    }

    private fun init() {
        val imageView = findViewById<ImageView>(R.id.preview_image_view)
        GlideUtils.load(
            context,
            imageView,
            imageUrl,
            true
        )
    }

    private fun layoutId() = R.layout.dialog_image_preview
}