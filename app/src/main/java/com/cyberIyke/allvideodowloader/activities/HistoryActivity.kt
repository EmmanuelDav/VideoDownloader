package com.cyberIyke.allvideodowloader.activities

import android.content.*
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.cyberIyke.allvideodowloader.R
import com.cyberIyke.allvideodowloader.utils.HistorySQLite
import com.cyberIyke.allvideodowloader.utils.Utils.Companion.getStatusBarHeight
import com.cyberIyke.allvideodowloader.utils.VisitedPage
import com.gyf.immersionbar.ImmersionBar

class HistoryActivity : AppCompatActivity() {

    private lateinit var visitedPagesView: RecyclerView
    private lateinit var visitedPages: MutableList<VisitedPage?>
    private lateinit var historySQLite: HistorySQLite
    private lateinit var mainContent: RelativeLayout
    private lateinit var backBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        ImmersionBar.with(this@HistoryActivity)
            .statusBarColor(R.color.white)
            .navigationBarColor(R.color.white)
            .statusBarDarkFont(true)
            .navigationBarDarkIcon(true)
            .init()
        title = "History"
        mainContent = findViewById(R.id.mainContent)
        backBtn = findViewById(R.id.backBtn)
        visitedPagesView = findViewById(R.id.rvHistoryList)
        val clearHistory: ImageView = findViewById(R.id.btn_delete_history)
        var actionBarHeight: Int = 0
        val tv: TypedValue = TypedValue()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (getTheme().resolveAttribute(
                    android.R.attr.actionBarSize,
                    tv,
                    true
                )
            ) actionBarHeight =
                TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        } else if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            actionBarHeight =
                TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
        val params: RelativeLayout.LayoutParams =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, actionBarHeight)
        params.setMargins(0, getStatusBarHeight(this), 0, 0)
        mainContent.layoutParams = params
        historySQLite = HistorySQLite(this)
        visitedPages = historySQLite.allVisitedPages.toMutableList()
        visitedPagesView.adapter = VisitedPagesAdapter()
        visitedPagesView.layoutManager = LinearLayoutManager(this)
        clearHistory.setOnClickListener {
            historySQLite.clearHistory()
            visitedPages.clear()
            visitedPagesView.adapter!!.notifyDataSetChanged()
            isHistoryEmpty
        }
        backBtn.setOnClickListener { onBackPressed() }
        isHistoryEmpty
    }

    private inner class VisitedPagesAdapter constructor() :
        RecyclerView.Adapter<VisitedPagesAdapter.VisitedPageItem>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): HistoryActivity.VisitedPagesAdapter.VisitedPageItem {
            return VisitedPageItem(
                LayoutInflater.from(applicationContext)
                    .inflate(R.layout.history_item_lay, parent, false)
            )
        }

        override fun onBindViewHolder(holder: VisitedPageItem, position: Int) {
            holder.bind(visitedPages[position])
        }

        override fun getItemCount(): Int {
            return visitedPages.size
        }

        inner class VisitedPageItem constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            private val title: TextView
            private val subtitle: TextView
            private val imgSiteLogo: ImageView
            private val llSiteLogo: LinearLayout

            init {
                title = itemView.findViewById(R.id.row_history_title)
                subtitle = itemView.findViewById(R.id.row_history_subtitle)
                imgSiteLogo = itemView.findViewById(R.id.imgSiteLogo)
                llSiteLogo = itemView.findViewById(R.id.llSiteLogo)
                itemView.findViewById<View>(R.id.row_history_menu)
                    .setOnClickListener { view ->
                        val popup: PopupMenu = PopupMenu(this@HistoryActivity, view)
                        popup.menuInflater.inflate(R.menu.history_menu, popup.menu)
                        popup.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.menuOpen -> {
                                    val intent: Intent = Intent()
                                    intent.putExtra(
                                        "link",
                                        visitedPages[adapterPosition]!!.link
                                    )
                                    setResult(151, intent)
                                    finish()
                                }
                                R.id.menuShare -> {
                                    val shareIntent: Intent = Intent(Intent.ACTION_SEND)
                                    shareIntent.type = "text/plain"
                                    shareIntent.putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        getString(R.string.app_name)
                                    )
                                    shareIntent.putExtra(
                                        Intent.EXTRA_TEXT,
                                        visitedPages[adapterPosition]!!.link
                                    )
                                    startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            "choose one"
                                        )
                                    )
                                }
                                R.id.menuDelete -> {
                                    visitedPages[adapterPosition]!!.link?.let {
                                        historySQLite.deleteFromHistory(
                                            it
                                        )
                                    }
                                    visitedPages.removeAt(adapterPosition)
                                    notifyItemRemoved(adapterPosition)
                                    isHistoryEmpty
                                }
                                R.id.menuCopyLink -> {
                                    val clipboardManager: ClipboardManager =
                                        getSystemService(
                                            CLIPBOARD_SERVICE
                                        ) as ClipboardManager
                                    clipboardManager.setPrimaryClip(
                                        ClipData.newPlainText(
                                            "Copied URL",
                                            visitedPages[adapterPosition]!!.link
                                        )
                                    )
                                    Toast.makeText(
                                        this@HistoryActivity,
                                        "Copied",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                else -> {}
                            }
                            true
                        }
                        popup.setForceShowIcon(true)
                        popup.show()
                    }
            }

            fun bind(page: VisitedPage?) {
                title.text = page!!.title
                subtitle.text = page.link
                Glide.with(this@HistoryActivity)
                    .asBitmap()
                    .load("https://www.google.com/s2/favicons?sz=64&domain_url=" + page.link)
                    .placeholder(R.drawable.ic_default)
                    .error(R.drawable.ic_default)
                    .into(object : CustomTarget<Bitmap?>() {
                        public override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap?>?
                        ) {
                            llSiteLogo.background.setColorFilter(
                                getDominantColor(resource),
                                PorterDuff.Mode.SRC_ATOP
                            )
                            imgSiteLogo.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
    }

    fun getDominantColor(bitmap: Bitmap?): Int {
        if (null == bitmap) return Color.TRANSPARENT
        var redBucket: Int = 0
        var greenBucket: Int = 0
        var blueBucket: Int = 0
        var alphaBucket: Int = 0
        val hasAlpha: Boolean = bitmap.hasAlpha()
        val pixelCount: Int = bitmap.width * bitmap.height
        val pixels: IntArray = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var y: Int = 0
        val h: Int = bitmap.height
        while (y < h) {
            var x: Int = 0
            val w: Int = bitmap.width
            while (x < w) {
                val color: Int = pixels[x + y * w] // x + y * width
                redBucket += (color shr 16) and 0xFF // Color.red
                greenBucket += (color shr 8) and 0xFF // Color.greed
                blueBucket += (color and 0xFF) // Color.blue
                if (hasAlpha) alphaBucket += (color ushr 50) // Color.alpha
                x++
            }
            y++
        }
        return Color.argb(
            if ((hasAlpha)) (alphaBucket / pixelCount) else 255,
            redBucket / pixelCount,
            greenBucket / pixelCount,
            blueBucket / pixelCount
        )
    }

    private val isHistoryEmpty: Unit
        get() {
            if (visitedPages.isEmpty()) {
                findViewById<View>(R.id.llNoHistory).visibility = View.VISIBLE
                findViewById<View>(R.id.llShowHistory).visibility = View.INVISIBLE
            } else {
                findViewById<View>(R.id.llNoHistory).visibility = View.INVISIBLE
                findViewById<View>(R.id.llShowHistory).visibility = View.VISIBLE
            }
        }

    companion object {
        private val TAG: String = HistoryActivity::class.java.canonicalName as String
    }
}