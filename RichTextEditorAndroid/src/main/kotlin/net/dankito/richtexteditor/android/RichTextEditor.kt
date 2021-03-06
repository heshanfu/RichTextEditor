package net.dankito.richtexteditor.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import net.dankito.filechooserdialog.service.PermissionsService
import net.dankito.richtexteditor.android.extensions.showKeyboard
import net.dankito.richtexteditor.android.util.KeyboardState
import net.dankito.richtexteditor.model.DownloadImageConfig


open class RichTextEditor : FullscreenWebView {

    constructor(context: Context) : super(context) { initEditor(context, null) }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initEditor(context, attrs) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initEditor(context, attrs) }


    val javaScriptExecutor = AndroidJavaScriptExecutor(this)

    var permissionsService: PermissionsService? = null

    var downloadImageConfig: DownloadImageConfig? = null

    private var isLoaded = false

    private var paddingToSetOnStart: Rect? = null


    @SuppressLint("SetJavaScriptEnabled")
    private fun initEditor(context: Context, attributes: AttributeSet?) {
        attributes?.let { applyAttributes(context, it) }

        this.isHorizontalScrollBarEnabled = false
        this.settings.javaScriptEnabled = true

        this.webChromeClient = WebChromeClient()

        this.settings.defaultTextEncodingName = "UTF-8" // otherwise non ASCII text doesn't get displayed correctly
        this.settings.domStorageEnabled = true // otherwise images won't load
        this.settings.loadsImagesAutomatically = true
        this.settings.setSupportZoom(true)
        this.settings.builtInZoomControls = true
        this.settings.displayZoomControls = false

        javaScriptExecutor.addLoadedListener {
            editorLoaded(context)
        }
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet) {
        val attrsArray = intArrayOf(android.R.attr.gravity)
        val ta = context.obtainStyledAttributes(attrs, attrsArray)

        val gravity = ta.getInt(0, View.NO_ID)
        when (gravity) {
            Gravity.LEFT -> executeEditorJavaScriptFunction("setTextAlign(\"left\")")
            Gravity.RIGHT -> executeEditorJavaScriptFunction("setTextAlign(\"right\")")
            Gravity.TOP -> executeEditorJavaScriptFunction("setVerticalAlign(\"top\")")
            Gravity.BOTTOM -> executeEditorJavaScriptFunction("setVerticalAlign(\"bottom\")")
            Gravity.CENTER_VERTICAL -> executeEditorJavaScriptFunction("setVerticalAlign(\"middle\")")
            Gravity.CENTER_HORIZONTAL -> executeEditorJavaScriptFunction("setTextAlign(\"center\")")
            Gravity.CENTER -> {
                executeEditorJavaScriptFunction("setVerticalAlign(\"middle\")")
                executeEditorJavaScriptFunction("setTextAlign(\"center\")")
            }
        }

        ta.recycle()
    }

    private fun editorLoaded(context: Context) {
        isLoaded = true

        paddingToSetOnStart?.let {
            setPadding(it.left, it.top, it.right, it.bottom)
            paddingToSetOnStart = null
        }

        (context as? Activity)?.runOnUiThread {
            setEditorFontFamily("serif")
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        KeyboardState.init(context as Activity)
    }

    override fun onDetachedFromWindow() {
        KeyboardState.cleanUp() // to avoid memory leaks

        super.onDetachedFromWindow()
    }


    override fun enterEditingMode() {
        super.enterEditingMode()

        javaScriptExecutor.startEditing()
    }


    /**
     * Returns the last cached editor's html.
     * Usually this is the up to date html. But in case user uses swipe input, some swipe keyboards (especially Samsung's) or pasting text on Samsung devices doesn't fire text changed event,
     * so we're not notified of last entered word. In this case use retrieveCurrentHtmlAsync() to ensure to retrieve current html.
     */
    fun getHtml(): String {
        return javaScriptExecutor.getHtml()
    }

    @JvmOverloads
    fun setHtml(html: String, baseUrl: String? = null) {
        javaScriptExecutor.setHtml(html, baseUrl)
    }

    /**
     * Queries underlying JavaScript code for latest html.
     * See getHtml() for explanation when it's sensible to call this method.
     */
    fun retrieveCurrentHtmlAsync(callback: (String) -> Unit) {
        javaScriptExecutor.retrieveCurrentHtmlAsync(callback)
    }


    /*      Editor base settings        */

    fun setEditorFontColor(color: Int) {
        val hex = convertHexColorString(color)
        executeEditorJavaScriptFunction("setBaseTextColor('$hex');")
    }

    fun setEditorFontFamily(fontFamily: String) {
        this.settings.standardFontFamily = fontFamily
        executeEditorJavaScriptFunction("setBaseFontFamily('$fontFamily');")
    }

    fun setEditorFontSize(px: Int) {
        this.settings.defaultFontSize = px
        executeEditorJavaScriptFunction("setBaseFontSize('${px}px');")
    }

    fun setPadding(padding: Int) {
        setPadding(padding, padding, padding, padding)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        if(isLoaded) {
            (context as? Activity)?.runOnUiThread {
                super.setPadding(left, top, right, bottom)
                executeEditorJavaScriptFunction("setPadding('${left}px', '${top}px', '${right}px', '${bottom}px');")
            }

        }
        else { // on older devices setPadding() is called in parent class' constructor -> webView is not set yet
            // don't know why but setPadding() is called multiple times, last times with all values set to 0 and therefore overwriting correct values
            if(paddingToSetOnStart == null || (paddingToSetOnStart?.left == 0 && paddingToSetOnStart?.top == 0 && paddingToSetOnStart?.right == 0 && paddingToSetOnStart?.bottom == 0)) {
                paddingToSetOnStart = Rect(left, top, right, bottom)
            }
        }
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        // still not support RTL.
        setPadding(start, top, end, bottom)
    }

    fun setEditorBackgroundColor(color: Int) {
        setBackgroundColor(color)
    }

    override fun setBackgroundResource(resid: Int) {
        val bitmap = Utils.decodeResource(context, resid)
        setBackground(bitmap)
    }

    override fun setBackground(background: Drawable) {
        val bitmap = Utils.toBitmap(background)
        setBackground(bitmap)
    }

    private fun setBackground(bitmap: Bitmap) {
        val base64 = Utils.toBase64(bitmap)
        bitmap.recycle()

        executeEditorJavaScriptFunction("setBackgroundImage('url(data:image/png;base64,$base64)');")
    }

    fun setBackground(url: String) {
        executeEditorJavaScriptFunction("setBackgroundImage('url($url)');")
    }

    fun setEditorWidth(px: Int) {
        executeEditorJavaScriptFunction("setWidth('" + px + "px');")
    }

    fun setEditorHeight(px: Int) {
        executeEditorJavaScriptFunction("setHeight('" + px + "px');")
    }

    /**
     * Does actually not work for me
     */
    fun setPlaceholder(placeholder: String) {
        executeEditorJavaScriptFunction("setPlaceholder('$placeholder');")
    }

    fun setInputEnabled(inputEnabled: Boolean) {
        executeEditorJavaScriptFunction("setInputEnabled($inputEnabled)")
    }

    fun loadCSS(cssFile: String) {
        val jsCSSImport = "(function() {" +
                "    var head  = document.getElementsByTagName(\"head\")[0];" +
                "    var link  = document.createElement(\"link\");" +
                "    link.rel  = \"stylesheet\";" +
                "    link.type = \"text/css\";" +
                "    link.href = \"" + cssFile + "\";" +
                "    link.media = \"all\";" +
                "    head.appendChild(link);" +
                "}) ();"
        javaScriptExecutor.executeJavaScript(jsCSSImport)
    }

    @JvmOverloads
    fun focusEditor(alsoCallJavaScriptFocusFunction: Boolean = true) {
        this.requestFocus()

        if(alsoCallJavaScriptFocusFunction) { // Calling focus() changes editor's state, this is not desirable in all circumstances
            executeEditorJavaScriptFunction("focus()")
        }
    }

    @JvmOverloads
    fun focusEditorAndShowKeyboard(alsoCallJavaScriptFocusFunction: Boolean = true) {
        focusEditor(alsoCallJavaScriptFocusFunction)

        this.showKeyboard()
    }

    /**
     * At start up we have to wait some time till editor is ready to be focused
     */
    @JvmOverloads
    fun focusEditorAndShowKeyboardDelayed(delayMillis: Long = 250, alsoCallJavaScriptFocusFunction: Boolean = true) {
        postDelayed({
            focusEditorAndShowKeyboard(alsoCallJavaScriptFocusFunction)
        }, delayMillis)
    }

    override fun clearFocus() {
        super.clearFocus()

        executeEditorJavaScriptFunction("blurFocus()")
    }

    private fun convertHexColorString(color: Int): String {
        val alpha = Color.alpha(color)

        if(alpha == 255) { // without alpha
            return String.format("#%06X", 0xFFFFFF and color)
        }
        else {
            return "rgba(${Color.red(color)}, ${Color.green(color)}, ${Color.blue(color)}, $alpha)"
        }
    }


    private fun executeEditorJavaScriptFunction(javaScript: String, resultCallback: ((String) -> Unit)? = null) {
        javaScriptExecutor.executeEditorJavaScriptFunction(javaScript, resultCallback)
    }


}