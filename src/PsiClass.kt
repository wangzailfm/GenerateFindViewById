import com.intellij.psi.*
import constant.Constant
import org.apache.commons.lang.StringUtils
import java.util.*

/**
 * 获取initView方法里面的每条数据

 * @param mClass mClass
 *
 * @return PsiStatement[]
 */
fun PsiClass.getInitViewBodyStatements(): Array<PsiStatement>? {
    // 获取initView方法
    val method = this.findMethodsByName(Constant.Ext.CREATOR_INITVIEW_NAME, false)
    return if (method.isNotEmpty() && method[0].body != null)
        method[0].body?.statements
    else null
}

/**
 * 获取onClick方法里面的每条数据

 * @param mClass mClass
 *
 * @return PsiElement[]
 */
fun PsiClass.getOnClickStatement(): Array<PsiElement>? {
    // 获取onClick方法
    val onClickMethods = this.findMethodsByName(Constant.FIELD_ONCLICK, false)
    return if (onClickMethods.isNotEmpty() && onClickMethods[0].body != null)
        onClickMethods[0].body?.children
    else null
}
/**
 * 获取包含@OnClick注解的方法

 * @param mClass mClass
 *
 * @return PsiMethod
 */
fun PsiClass.getPsiMethodByButterKnifeOnClick(): PsiMethod? {
    for (psiMethod in this.methods) {
        // 获取方法的注解
        val modifierList = psiMethod.modifierList
        val annotations = modifierList.annotations
        annotations
                .map { it.qualifiedName }
                .filter { it != null && it == "butterknife.OnClick" }
                .forEach {
                    // 包含@OnClick注解
                    return psiMethod
                }
    }
    return null
}

/**
 * 获取View类型的变量名

 * @param mClass mClass
 *
 * @return String
 */
fun PsiClass.getPsiMethodParamsViewField(): String? {
    val butterKnifeOnClickMethod = this.getPsiMethodByButterKnifeOnClick()
    if (butterKnifeOnClickMethod != null) {
        // 获取方法的指定参数类型的变量名
        val parameterList = butterKnifeOnClickMethod.parameterList
        val parameters = parameterList.parameters
        parameters
                .filter { it.typeElement != null && it.typeElement?.text == "View" }
                .forEach { return it.name }
    }
    return null
}

/**
 * 获取包含@OnClick注解里面的值
 *
 * @return List
 */
fun PsiClass.getPsiMethodByButterKnifeOnClickValue(): ArrayList<String> {
    val onClickValue = ArrayList<String>()
    this.getPsiMethodByButterKnifeOnClick()?.let {
        // 获取方法的注解
        val modifierList = it.modifierList
        modifierList.annotations.filter {
            it.qualifiedName != null && it.qualifiedName == "butterknife.OnClick"
        }.forEach {
            val text = it.text.replace("(", "")
                    .replace(")", "")
                    .replace("{", "")
                    .replace("}", "")
                    .replace(" ", "")
                    .replace("@OnClick", "")
            if (!StringUtils.isEmpty(text)) {
                val split = text.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().dropLastWhile(String::isEmpty).toTypedArray()
                split.filterNotTo(onClickValue) { StringUtils.isEmpty(it) }
            }
            return@let
        }
    }
    return onClickValue
}

/**
 * 添加注解到方法

 * @param mClass       mClass
 *
 * @param mFactory     mFactory
 *
 * @param onClickValue onClickValue
 */
fun PsiClass.createOnClickAnnotation(mFactory: PsiElementFactory, onClickValue: ArrayList<String>) {
    this.getPsiMethodByButterKnifeOnClick()?.let {
        // 获取方法的注解
        val modifierList = it.modifierList
        modifierList.annotations.filter {
            it.qualifiedName != null && it.qualifiedName == "butterknife.OnClick"
        }.forEach {
            val annotationText = StringBuilder()
            annotationText.append("@OnClick(")
            if (onClickValue.size == 1) {
                annotationText.append(onClickValue[0])
            } else {
                annotationText.append("{")
                onClickValue.forEachIndexed { index, value ->
                    if (index != 0) {
                        annotationText.append(", ")
                    }
                    annotationText.append(value)
                }
                annotationText.append("}")
            }
            annotationText.append(")")
            modifierList.addBefore(mFactory.createAnnotationFromText(annotationText.toString(), modifierList), it)
            it.delete()
            return@let
        }
    }
}
/**
 * 判断是否存在ButterKnife.bind(this)/ButterKnife.bind(this, view)
 * @param mClass mClass
 *
 * @return boolean
 */
fun PsiClass.isButterKnifeBindExist(): Boolean {
    val onCreateMethod = this.getPsiMethodByName(Constant.PSI_METHOD_BY_ONCREATE)
    val onCreateViewMethod = this.getPsiMethodByName(Constant.PSI_METHOD_BY_ONCREATEVIEW)
    return !(onCreateMethod != null && onCreateMethod.body != null
            && onCreateMethod.body!!.text.contains(Constant.Ext.FIELD_BUTTERKNIFE_BIND)
            || onCreateViewMethod != null && onCreateViewMethod.body != null
            && onCreateViewMethod.body!!.text.contains(Constant.Ext.FIELD_BUTTERKNIFE_BIND))
}


/**
 * 根据方法名获取方法
 *
 * @param mClass     mClass
 *
 * @param methodName methodName
 *
 * @return PsiMethod
 */
fun PsiClass.getPsiMethodByName(methodName: String): PsiMethod? =
        this.methods.firstOrNull { it.name == methodName }
