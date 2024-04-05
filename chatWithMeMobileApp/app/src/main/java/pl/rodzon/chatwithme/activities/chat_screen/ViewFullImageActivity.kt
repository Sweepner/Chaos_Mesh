package pl.rodzon.chatwithme.activities.chat_screen

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.squareup.picasso.Picasso
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.utils.ChatContext
import pl.rodzon.chatwithme.utils.ImageUtil

class ViewFullImageActivity : AppCompatActivity() {
    private var imageViewer: ImageView? = null
    private var image: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_full_image)

        image = ChatContext.getViewFullImage()
        imageViewer = findViewById(R.id.image_viewer)

        Picasso.get().load(ImageUtil.saveBase64ImageToFile(this, image!!, System.currentTimeMillis().toString() + ".jpg")!!).into(imageViewer)

    }

    override fun onDestroy() {
        super.onDestroy()
        ChatContext.setViewFullImage(null)
    }
}