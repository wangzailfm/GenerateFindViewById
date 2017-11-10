import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntheticElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor
import constant.Constant
import java.awt.Color


/**
 * 显示dialog
 * @param editor editor
 *
 * @param result 内容
 *
 * @param time   显示时间，单位秒
 */
fun Editor.showPopupBalloon(result: String?, time: Int) {
    ApplicationManager.getApplication().invokeLater {
        val factory = JBPopupFactory.getInstance()
        factory.createHtmlTextBalloonBuilder(result ?: Constant.Ext.UNKNOWN_ERROR, null, JBColor(Color(116, 214, 238), Color(76, 112, 117)), null).setFadeoutTime((time * 1000).toLong()).createBalloon().show(factory.guessBestPopupLocation(this), Balloon.Position.below)
    }
    result?.outInfo()
}

/**
 * 根据当前文件获取对应的class文件
 * @param editor editor
 *
 * @param file   file
 *
 * @return PsiClass
 */
infix fun Editor.getTargetClass(file: PsiFile?): PsiClass? {
    val offset = this.caretModel.offset
    val element = file?.findElementAt(offset) ?: return null
    val target = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
    return if (target is SyntheticElement) null else target
}