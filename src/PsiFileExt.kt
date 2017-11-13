import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.XmlRecursiveElementVisitor
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import constant.Constant
import entitys.Element
import org.apache.commons.lang.StringUtils
import java.util.*
import java.util.regex.Pattern

fun PsiFile.fileAccept(accept: (element: PsiElement) -> Unit) =
        this.accept(object : XmlRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                accept.invoke(element)
            }
        })

/**
 * 获取所有id
 * @param file     file
 *
 * @param elements elements
 */
fun getIDsFromLayoutToList(psiFile: PsiFile, elements: ArrayList<Element>) {
    psiFile.fileAccept { element ->
        // 解析Xml标签
        if (element is XmlTag) {
            with(element) {
                // 获取Tag的名字（TextView）或者自定义
                val name: String = name
                // 如果有include
                if (name.equals("include", ignoreCase = true)) {
                    // 获取布局
                    val layout = getAttribute("layout", null) ?: return@fileAccept
                    val layoutName = layout.value.getLayoutName() ?: return@fileAccept
                    // 获取project
                    val project = this.project
                    // 布局文件
                    var include: XmlFile? = null
                    val psiFiles = FilenameIndex.getFilesByName(project, layoutName + Constant.SELECTED_TEXT_SUFFIX, GlobalSearchScope.allScope(project))
                    if (psiFiles.isNotEmpty()) {
                        include = if (psiFiles.size > 1) {
                            val psiFilePath = psiFile.parent?.toString()!!
                            val psiFiles1 = psiFiles.filter {
                                val modulePath = it.parent?.toString()!!
                                modulePath.contains("\\src\\main\\res\\layout") && psiFilePath.substring(0, psiFilePath.indexOf("\\main\\")) == modulePath.substring(0, modulePath.indexOf("\\main\\"))
                            }
                            if (psiFiles1.isEmpty()) return@fileAccept else psiFiles1[0] as XmlFile
                        } else {
                            psiFiles[0] as XmlFile
                        }
                    }
                    if (include != null) {
                        // 递归
                        getIDsFromLayoutToList(include, elements)
                        return@fileAccept
                    }
                }
                // 获取id字段属性
                val id = getAttribute("android:id", null) ?: return@fileAccept
                // 获取id的值
                val idValue = id.value ?: return@fileAccept
                // 获取clickable
                val clickableAttr = getAttribute("android:clickable", null)
                var clickable = false
                if (clickableAttr != null && !StringUtils.isEmpty(clickableAttr.value)) {
                    clickable = clickableAttr.value == "true"
                }
                if (name == "Button") {
                    clickable = true
                }
                // 添加到list
                try {
                    val e = Element(name, idValue, clickable, this)
                    elements.add(e)
                } catch (ignored: IllegalArgumentException) {
                    ignored.printStackTrace()
                }
            }
        }
    }
}

/**
 * 解析xml获取string的值

 * @param psiFile psiFile
 *
 * @param text    text
 *
 * @return String
 */
fun PsiFile.getTextFromStringsXml(text: String): String? {
    var stringValue: String? = null
    this.fileAccept { element ->
        if (element is XmlTag) {
            with(element) {
                if (name == "string" && getAttributeValue("name") == text) {
                    val value = StringBuilder()
                    children.forEach {
                        value.append(it.text)
                    }
                    // value = <string name="app_name">My Application</string>
                    // 用正则获取值
                    val p = Pattern.compile("<string name=\"$text\">(.*)</string>")
                    val m = p.matcher(value.toString())
                    while (m.find()) {
                        stringValue = m.group(1)
                    }
                }
            }
        }
    }
    return stringValue
}

infix fun PsiClass.checkInheritor(toClass: PsiClass) = this.isInheritor(toClass, true)
