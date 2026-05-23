import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.ayan.p2pxpert.R

class LoadingDialog(private val context: Context) {
    private var dialog: Dialog? = null

    fun show() {
        if (dialog == null) {
            dialog = Dialog(context)
            val view = LayoutInflater.from(context).inflate(R.layout.loading, null)
            dialog!!.setContentView(view)
            dialog!!.setCancelable(false)
            dialog!!.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}
