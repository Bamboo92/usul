package abualqasim.dr3.usul.ui.camera

import android.content.Intent

fun Intent.extractStartType(): PhotoType =
    PhotoType.valueOf(getStringExtra("startType") ?: PhotoType.NEAR.name)

fun Intent.extractTitle(): String =
    getStringExtra("title") ?: ""

fun Intent.extractNear(): String? =
    getStringExtra("near")

fun Intent.extractFar(): String? =
    getStringExtra("far")

fun Intent.putCameraResult(near: String?, far: String?): Intent =
    apply {
        putExtra("near", near)
        putExtra("far", far)
    }
