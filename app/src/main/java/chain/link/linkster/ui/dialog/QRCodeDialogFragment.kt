package chain.link.linkster.ui.dialog

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import chain.link.linkster.R


class QRCodeDialogFragment : DialogFragment() {

    private lateinit var qrCodeImage: Bitmap // This will be your QR code image

    fun setQRCodeImage(image: Bitmap) {
        qrCodeImage = image
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_qr_code_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.qrCodeImageView).setImageBitmap(qrCodeImage)
    }
}