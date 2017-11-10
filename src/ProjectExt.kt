
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import entitys.Element

infix fun Project.findClassByProject(className: String) =
        JavaPsiFacade.getInstance(this).findClass(className, EverythingGlobalScope(this))

/**
 * 判断mClass是不是继承activityClass或者activityCompatClass
 * @param mProject mProject
 *
 * @param mClass   mClass
 *
 * @return boolean
 */
infix fun Project.isExtendsActivityOrActivityCompat(mClass: PsiClass): Boolean {
    // 根据类名查找类
    val activityClass = this findClassByProject "android.app.Activity"
    val activityCompatClass = this findClassByProject "android.support.v7.app.AppCompatActivity"
    return activityClass != null && mClass checkInheritor activityClass
            || activityCompatClass != null && mClass checkInheritor activityCompatClass
}

/**
 * 判断mClass是不是继承fragmentClass或者fragmentV4Class
 * @param mProject mProject
 *
 * @param mClass   mClass
 *
 * @return boolean
 */
infix fun Project.isExtendsFragmentOrFragmentV4(mClass: PsiClass): Boolean {
    // 根据类名查找类
    val fragmentClass = this findClassByProject "android.app.Fragment"
    val fragmentV4Class = this findClassByProject "android.support.v4.app.Fragment"
    return fragmentClass != null && mClass checkInheritor fragmentClass
            || fragmentV4Class != null && mClass checkInheritor fragmentV4Class
}
/**
 * FindViewById，获取xml里面的text
 *
 * @param element  Element
 *
 * @param mProject Project
 *
 * @param psiFile PsiFile
 *
 * @return String
 */
fun Project.createFieldText(element: Element, psiFile: PsiFile): String? {
    // 如果是text为空，则获取hint里面的内容
    var text: String? = element.xml.getAttributeValue("android:text") ?: element.xml.getAttributeValue("android:hint")
    text?.let {
        if (it.contains("@string/")) {
            text = it.replace("@string/".toRegex(), "")
            // 获取strings.xml
            val psiFiles = FilenameIndex.getFilesByName(this, "strings.xml", GlobalSearchScope.allScope(this))
            if (psiFiles.isNotEmpty()) {
                psiFiles
                        .filter {
                            // 获取src\main\res\values下面的strings.xml文件
                            it.parent != null && it.parent!!.toString().contains("src\\main\\res\\values", false)
                        }
                        .filter {
                            it.parent!!.toString().outInfo()
                            val psiFilePath = psiFile.parent?.toString()!!
                            val modulePath = it.parent?.toString()!!
                            psiFilePath.substring(0, psiFilePath.indexOf("\\main\\")) == modulePath.substring(0, modulePath.indexOf("\\main\\"))
                        }
                        .forEach { text = it.getTextFromStringsXml(text!!) }
            }
        }
    }
    return text
}