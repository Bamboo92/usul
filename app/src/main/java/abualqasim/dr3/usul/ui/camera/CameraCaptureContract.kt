package abualqasim.dr3.usul.ui.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import java.io.Serializable

private const val EXTRA_START_TYPE = "camera_start_type"
private const val EXTRA_TITLE = "camera_title"
private const val EXTRA_NEAR_PATH = "camera_near_path"
private const val EXTRA_FAR_PATH = "camera_far_path"
private const val EXTRA_RESULT_NEAR = "camera_result_near"
private const val EXTRA_RESULT_FAR = "camera_result_far"

data class CameraCaptureRequest(
    val startType: PhotoType,
    val title: String,
    val nearPath: String?,
    val farPath: String?
) : Serializable

data class CameraCaptureResult(
    val nearPath: String?,
    val farPath: String?
)

class CameraCaptureContract : ActivityResultContract<CameraCaptureRequest, CameraCaptureResult?>() {
    override fun createIntent(context: Context, input: CameraCaptureRequest): Intent {
        return Intent(context, CameraCaptureActivity::class.java).apply {
            putExtra(EXTRA_START_TYPE, input.startType)
            putExtra(EXTRA_TITLE, input.title)
            putExtra(EXTRA_NEAR_PATH, input.nearPath)
            putExtra(EXTRA_FAR_PATH, input.farPath)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CameraCaptureResult? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return CameraCaptureResult(
            nearPath = intent.getStringExtra(EXTRA_RESULT_NEAR),
            farPath = intent.getStringExtra(EXTRA_RESULT_FAR)
        )
    }

    companion object {
        internal fun Intent.putCameraResult(nearPath: String?, farPath: String?): Intent {
            putExtra(EXTRA_RESULT_NEAR, nearPath)
            putExtra(EXTRA_RESULT_FAR, farPath)
            return this
        }

        internal fun Intent.extractStartType(): PhotoType {
            return (getSerializableExtra(EXTRA_START_TYPE) as? PhotoType) ?: PhotoType.NEAR
        }

        internal fun Intent.extractTitle(): String = getStringExtra(EXTRA_TITLE).orEmpty()

        internal fun Intent.extractNear(): String? = getStringExtra(EXTRA_NEAR_PATH)

        internal fun Intent.extractFar(): String? = getStringExtra(EXTRA_FAR_PATH)
    }
}
