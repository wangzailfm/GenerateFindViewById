package utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.*
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.JBColor
import constant.Constant
import entitys.Element
import org.apache.commons.lang.StringUtils
import java.awt.Color
import java.util.*
import java.util.regex.Pattern

/**
 * @author Jowan
 */
object Util {
    /**
     * 通过strings.xml获取的值
     */
    private var StringValue: String? = null

    /**
     * 显示dialog

     * @param editor editor
     * *
     * @param result 内容
     * *
     * @param time   显示时间，单位秒
     */
    fun showPopupBalloon(editor: Editor, result: String?, time: Int) {
        ApplicationManager.getApplication().invokeLater {
            val factory = JBPopupFactory.getInstance()
            factory.createHtmlTextBalloonBuilder(result ?: Constant.utils.UNKNOWN_ERROR, null, JBColor(Color(116, 214, 238), Color(76, 112, 117)), null).setFadeoutTime((time * 1000).toLong()).createBalloon().show(factory.guessBestPopupLocation(editor), Balloon.Position.below)
        }
    }

    /**
     * 驼峰

     * @param fieldNames fieldName
     * *
     * @param type      type
     * *
     * @return String
     */
    fun getFieldName(fieldNames: String, type: Int): String {
        var fieldName = fieldNames
        if (!StringUtils.isEmpty(fieldName)) {
            val names = fieldName.split("_".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            if (type == 2) {
                // aaBbCc
                val sb = StringBuilder()
                for (i in names.indices) {
                    if (i == 0) {
                        sb.append(names[i])
                    } else {
                        sb.append(Util.firstToUpperCase(names[i]))
                    }
                }
                sb.append("View")
                fieldName = sb.toString()
            } else {
                // mAaBbCc
                val sb = StringBuilder()
                for (i in names.indices) {
                    if (i == 0) {
                        sb.append("m")
                    }
                    sb.append(Util.firstToUpperCase(names[i]))
                }
                sb.append("View")
                fieldName = sb.toString()
            }
        }
        return fieldName
    }

    /**
     * 第一个字母大写

     * @param key key
     * *
     * @return String
     */
    fun firstToUpperCase(key: String): String {
        return key.substring(0, 1).toUpperCase(Locale.CHINA) + key.substring(1)
    }

    /**
     * 解析xml获取string的值

     * @param psiFile psiFile
     * *
     * @param text    text
     * *
     * @return String
     */
    private fun getTextFromStringsXml(psiFile: PsiFile, text: String): String? {
        psiFile.accept(object : XmlRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element is XmlTag) {
                    if (element.name == "string" && element.getAttributeValue("name") == text) {
                        val children = element.children
                        val value = StringBuilder()
                        for (child in children) {
                            value.append(child.text)
                        }
                        // value = <string name="app_name">My Application</string>
                        // 用正则获取值
                        val p = Pattern.compile("<string name=\"$text\">(.*)</string>")
                        val m = p.matcher(value.toString())
                        while (m.find()) {
                            StringValue = m.group(1)
                        }
                    }
                }
            }
        })
        return StringValue
    }

    /**
     * 获取所有id

     * @param file     file
     * *
     * @param elements elements
     */
    fun getIDsFromLayout(file: PsiFile, elements: MutableList<Element>) {
        // To iterate over the elements in a file
        // 遍历一个文件的所有元素
        file.accept(object : XmlRecursiveElementVisitor() {

            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                // 解析Xml标签
                if (element is XmlTag) {
                    // 获取Tag的名字（TextView）或者自定义
                    val name: String = element.name
                    // 如果有include
                    if (name.equals("include", ignoreCase = true)) {
                        // 获取布局
                        val layout = element.getAttribute("layout", null)
                        // 获取project
                        val project = file.project
                        // 布局文件
                        var include: XmlFile? = null
                        var psiFiles = arrayOfNulls<PsiFile>(0)
                        if (layout != null) {
                            psiFiles = FilenameIndex.getFilesByName(project, getLayoutName(layout.value)!! + Constant.SELECTED_TEXT_SUFFIX, GlobalSearchScope.allScope(project))
                        }
                        if (psiFiles.isNotEmpty()) {
                            include = psiFiles[0] as XmlFile
                        }
                        if (include != null) {
                            // 递归
                            getIDsFromLayout(include, elements)
                            return
                        }
                    }
                    // 获取id字段属性
                    val id = element.getAttribute("android:id", null) ?: return
                    // 获取id的值
                    val idValue = id.value ?: return
                    // 获取clickable
                    val clickableAttr = element.getAttribute("android:clickable", null)
                    var clickable = false
                    if (clickableAttr != null && !StringUtils.isEmpty(clickableAttr.value)) {
                        clickable = clickableAttr.value == "true"
                    }
                    if (name == "Button") {
                        clickable = true
                    }
                    // 添加到list
                    try {
                        val e = Element(name, idValue, clickable, element)
                        elements.add(e)
                    } catch (ignored: IllegalArgumentException) {

                    }

                }
            }
        })


    }

    /**
     * layout.getValue()返回的值为@layout/layout_view

     * @param layout layout
     * *
     * @return String
     */
    private fun getLayoutName(layout: String?): String? {
        if (layout == null || !layout.startsWith("@") || !layout.contains("/")) {
            return null
        }

        // @layout layout_view
        val parts = layout.split("/".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        if (parts.size != 2) {
            return null
        }
        // layout_view
        return parts[1]
    }

    /**
     * 根据当前文件获取对应的class文件
     * @param editor editor
     * *
     * @param file   file
     * *
     * @return PsiClass
     */
    fun getTargetClass(editor: Editor, file: PsiFile?): PsiClass? {
        val offset = editor.caretModel.offset
        val element = file?.findElementAt(offset) ?: return null
        val target = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
        return if (target is SyntheticElement) null else target
    }

    /**
     * 判断mClass是不是继承activityClass或者activityCompatClass

     * @param mProject mProject
     * *
     * @param mClass   mClass
     * *
     * @return boolean
     */
    fun isExtendsActivityOrActivityCompat(mProject: Project, mClass: PsiClass): Boolean {
        // 根据类名查找类
        val activityClass = JavaPsiFacade.getInstance(mProject).findClass("android.app.Activity", EverythingGlobalScope(mProject))
        val activityCompatClass = JavaPsiFacade.getInstance(mProject).findClass("android.support.v7.app.AppCompatActivity", EverythingGlobalScope(mProject))
        return activityClass != null && mClass.isInheritor(activityClass, true) || activityCompatClass != null && mClass.isInheritor(activityCompatClass, true)
    }

    /**
     * 判断mClass是不是继承fragmentClass或者fragmentV4Class

     * @param mProject mProject
     * *
     * @param mClass   mClass
     * *
     * @return boolean
     */
    fun isExtendsFragmentOrFragmentV4(mProject: Project, mClass: PsiClass): Boolean {
        // 根据类名查找类
        val fragmentClass = JavaPsiFacade.getInstance(mProject).findClass("android.app.Fragment", EverythingGlobalScope(mProject))
        val fragmentV4Class = JavaPsiFacade.getInstance(mProject).findClass("android.support.v4.app.Fragment", EverythingGlobalScope(mProject))
        return fragmentClass != null && mClass.isInheritor(fragmentClass, true) || fragmentV4Class != null && mClass.isInheritor(fragmentV4Class, true)
    }

    /**
     * 判断是否存在ButterKnife.bind(this)/ButterKnife.bind(this, view)

     * @param mClass mClass
     * *
     * @return boolean
     */
    fun isButterKnifeBindExist(mClass: PsiClass): Boolean {
        val onCreateMethod = getPsiMethodByName(mClass, Constant.PSI_METHOD_BY_ONCREATE)
        val onCreateViewMethod = getPsiMethodByName(mClass, Constant.PSI_METHOD_BY_ONCREATEVIEW)
        return !(onCreateMethod != null && onCreateMethod.body != null
                && onCreateMethod.body!!.text.contains(Constant.utils.FIELD_BUTTERKNIFE_BIND) || onCreateViewMethod != null && onCreateViewMethod.body != null
                && onCreateViewMethod.body!!.text.contains(Constant.utils.FIELD_BUTTERKNIFE_BIND))
    }

    /**
     * 创建onCreate方法

     * @param mSelectedText  mSelectedText
     * *
     * @param mIsButterKnife mIsButterKnife
     * *
     * @return String
     */
    internal fun createOnCreateMethod(mSelectedText: String, mIsButterKnife: Boolean): String {
        val method = StringBuilder()
        val viewStr = "\tsetContentView(R.layout.$mSelectedText);\n"
        method.append("@Override protected void onCreate(android.os.Bundle savedInstanceState) {\n")
        method.append("super.onCreate(savedInstanceState);\n")
        method.append("\t// TODO:OnCreate Method has been created, run ")
        if (!mIsButterKnife) {
            method.append("FindViewById")
            method.append(" again to generate code\n")
            method.append(viewStr)
            method.append("\t\tinitView();\n")
        } else {
            method.append("ButterKnife")
            method.append(" again to generate code\n")
            method.append(viewStr)
            method.append("\t\tButterKnife.bind(this);\n")
        }
        method.append("}")
        return method.toString()
    }

    /**
     * 创建onCreate方法(Fragment)

     * @param mSelectedText mSelectedText
     * *
     * @return String
     */
    internal fun createFragmentOnCreateMethod(mSelectedText: String): String {
        return "@Override public void onCreate(@Nullable android.os.Bundle savedInstanceState) {\n" +
                "super.onCreate(savedInstanceState);\n" +
                "\tview = View.inflate(getActivity(), R.layout." + mSelectedText + ", null);\n" +
                "}"
    }

    /**
     * 创建onCreateView方法

     * @param mIsButterKnife mIsButterKnife
     * *
     * @return String
     */
    internal fun createOnCreateViewMethod(mIsButterKnife: Boolean): String {
        val method = StringBuilder()
        method.append("@Nullable @Override public View onCreateView(android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container, @Nullable android.os.Bundle savedInstanceState) {\n")
        method.append("\t// TODO:OnCreateView Method has been created, run ")
        if (!mIsButterKnife) {
            method.append("FindViewById")
            method.append(" again to generate code\n")
            method.append("\t\tinitView(view);\n")
        } else {
            method.append("ButterKnife")
            method.append(" again to generate code\n")
            method.append("\t\tunbinder = ButterKnife.bind(this, view);\n")
        }
        method.append("return view;")
        method.append("}\n")
        return method.toString()
    }

    /**
     * 创建initView方法，Fragment

     * @return String
     */
    internal fun createFragmentInitViewMethod(): String {
        return "public void initView(View view) {\n" + "}"
    }

    /**
     * 创建initView方法

     * @return String
     */
    internal fun createInitViewMethod(): String {
        return "public void initView() {\n" + "}"
    }

    /**
     * 创建OnDestroyView方法，里面包含unbinder.unbind()

     * @return String
     */
    internal fun createOnDestroyViewMethod(): String {
        return "@Override public void onDestroyView() {\n" +
                "\tsuper.onDestroyView();" +
                "\tunbinder.unbind();" +
                "}"
    }

    /**
     * 判断是否实现了OnClickListener接口

     * @param referenceElements referenceElements
     * *
     * @return boolean
     */
    internal fun isImplementsOnClickListener(referenceElements: Array<PsiJavaCodeReferenceElement>): Boolean {
        return referenceElements.any { it.text.contains("OnClickListener") }
    }

    /**
     * 获取initView方法里面的每条数据

     * @param mClass mClass
     * *
     * @return PsiStatement[]
     */
    fun getInitViewBodyStatements(mClass: PsiClass): Array<PsiStatement>? {
        // 获取initView方法
        val method = mClass.findMethodsByName(Constant.utils.CREATOR_INITVIEW_NAME, false)
        var statements: Array<PsiStatement>? = null
        if (method.isNotEmpty() && method[0].body != null) {
            val methodBody = method[0].body
            statements = methodBody?.statements
        }
        return statements
    }

    /**
     * 获取onClick方法里面的每条数据

     * @param mClass mClass
     * *
     * @return PsiElement[]
     */
    fun getOnClickStatement(mClass: PsiClass): Array<PsiElement>? {
        // 获取onClick方法
        val onClickMethods = mClass.findMethodsByName(Constant.FIELD_ONCLICK, false)
        var psiElements: Array<PsiElement>? = null
        if (onClickMethods.isNotEmpty() && onClickMethods[0].body != null) {
            val onClickMethodBody = onClickMethods[0].body
            psiElements = onClickMethodBody?.children
        }
        return psiElements
    }

    /**
     * 获取包含@OnClick注解的方法

     * @param mClass mClass
     * *
     * @return PsiMethod
     */
    fun getPsiMethodByButterKnifeOnClick(mClass: PsiClass): PsiMethod? {
        for (psiMethod in mClass.methods) {
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
     * 根据方法名获取方法

     * @param mClass     mClass
     * *
     * @param methodName methodName
     * *
     * @return PsiMethod
     */
    internal fun getPsiMethodByName(mClass: PsiClass, methodName: String): PsiMethod? {
        return mClass.methods.firstOrNull { it.name == methodName }
    }

    /**
     * 获取View类型的变量名

     * @param mClass mClass
     * *
     * @return String
     */
    internal fun getPsiMethodParamsViewField(mClass: PsiClass): String? {
        val butterKnifeOnClickMethod = getPsiMethodByButterKnifeOnClick(mClass)
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

     * @return List
     */
    fun getPsiMethodByButterKnifeOnClickValue(mClass: PsiClass): MutableList<String> {
        val onClickValue = ArrayList<String>()
        val butterKnifeOnClickMethod = Util.getPsiMethodByButterKnifeOnClick(mClass)
        if (butterKnifeOnClickMethod != null) {// 获取方法的注解
            val modifierList = butterKnifeOnClickMethod.modifierList
            val annotations = modifierList.annotations
            for (annotation in annotations) {
                if (annotation.qualifiedName != null && annotation.qualifiedName == "butterknife.OnClick") {
                    val text = annotation.text.replace("(", "").replace(")", "").replace("{", "").replace("}", "").replace(" ", "").replace("@OnClick", "")
                    if (!StringUtils.isEmpty(text)) {
                        val split = text.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
                        split.filterNotTo(onClickValue) { StringUtils.isEmpty(it) }
                    }
                    break
                }
            }
        }
        return onClickValue
    }

    /**
     * 添加注解到方法

     * @param mClass       mClass
     * *
     * @param mFactory     mFactory
     * *
     * @param onClickValue onClickValue
     */
    internal fun createOnClickAnnotation(mClass: PsiClass, mFactory: PsiElementFactory, onClickValue: List<String>) {
        val butterKnifeOnClickMethod = Util.getPsiMethodByButterKnifeOnClick(mClass)
        if (butterKnifeOnClickMethod != null) {// 获取方法的注解
            val modifierList = butterKnifeOnClickMethod.modifierList
            val annotations = modifierList.annotations
            for (annotation in annotations) {
                if (annotation.qualifiedName != null && annotation.qualifiedName == "butterknife.OnClick") {
                    val annotationText = StringBuilder()
                    annotationText.append("@OnClick(")
                    if (onClickValue.size == 1) {
                        annotationText.append(onClickValue[0])
                    } else {
                        annotationText.append("{")
                        for (j in onClickValue.indices) {
                            val value = onClickValue[j]
                            if (j != 0) {
                                annotationText.append(", ")
                            }
                            annotationText.append(value)
                        }
                        annotationText.append("}")
                    }
                    annotationText.append(")")
                    modifierList.addBefore(mFactory.createAnnotationFromText(annotationText.toString(), modifierList), annotation)
                    annotation.delete()
                    break
                }
            }
        }
    }

    /**
     * 获取OnClickList里面的id集合

     * @param mOnClickList clickable的Element集合
     * *
     * @return List
     */
    internal fun getOnClickListById(mOnClickList: List<Element>): List<String> {
        val list: MutableList<String> = mutableListOf()
        mOnClickList.forEach { list.add(it.fullID) }
        return list
    }

    /**
     * 获取注解里面跟OnClickList的id集合

     * @param annotationList OnClick注解里面的id集合
     * *
     * @param onClickIdList  clickable的Element集合
     * *
     * @return List
     */
    internal fun createOnClickValue(annotationList: MutableList<String>, onClickIdList: List<String>): List<String> {
        onClickIdList
                .filterNot { annotationList.contains(it) }
                .forEach { annotationList.add(it) }
        return annotationList
    }

    /**
     * FindViewById，获取xml里面的text
     * @param element  Element
     * *
     * @param mProject Project
     * *
     * @return String
     */
    internal fun createFieldText(element: Element, mProject: Project): String? {
        // 如果是text为空，则获取hint里面的内容
        var text: String? = element.xml.getAttributeValue("android:text") ?: element.xml.getAttributeValue("android:hint")
        // 如果是@string/app_name类似
        if (!StringUtils.isEmpty(text) && text!!.contains("@string/")) {
            text = text.replace("@string/", "")
            // 获取strings.xml
            val psiFiles = FilenameIndex.getFilesByName(mProject, "strings.xml", GlobalSearchScope.allScope(mProject))
            if (psiFiles.isNotEmpty()) {
                psiFiles
                        .asSequence()
                        .filter {
                            // 获取src\main\res\values下面的strings.xml文件
                            it.parent != null && it.parent!!.toString().contains("src\\main\\res\\values")
                        }
                        .forEach { text = Util.getTextFromStringsXml(it, text!!) }
            }
        }
        return text
    }

    /**
     * FindViewById，创建字段

     * @param text                注释内容
     * *
     * @param element             Element
     * *
     * @param mIsLayoutInflater   是否选中LayoutInflater
     * *
     * @param mLayoutInflaterText 选中的布局的变量名
     * *
     * @param mLayoutInflaterType mLayoutInflaterType
     * *
     * @return String
     */
    internal fun createFieldByElement(text: String?, element: Element, mIsLayoutInflater: Boolean, mLayoutInflaterText: String, mLayoutInflaterType: Int): String {
        val fromText = StringBuilder()
        text?.let {
            fromText.append("/** ")
            fromText.append(text)
            fromText.append(" */\n")
        }
        fromText.append("private ")
        fromText.append(element.name)
        fromText.append(" ")
        fromText.append(element.fieldName)
        if (mIsLayoutInflater) fromText.append(layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType))
        fromText.append(";")
        return fromText.toString()
    }

    /**
     * FindViewById，创建findViewById代码到initView方法里面

     * @param findPre             Fragment的话要view.findViewById
     * *
     * @param mIsLayoutInflater   是否选中LayoutInflater
     * *
     * @param mLayoutInflaterText 选中的布局的变量名
     * *
     * @param context             context
     * *
     * @param mSelectedText       选中的布局
     * *
     * @param mElements           Element的List
     * *
     * @param mLayoutInflaterType type
     * *
     * @param mNeedCast           是否需要强转
     * *
     * @return String
     */
    internal fun createFieldsByInitViewMethod(findPre: String?, mIsLayoutInflater: Boolean, mLayoutInflaterText: String, context: String, mSelectedText: String, mElements: List<Element>, mLayoutInflaterType: Int, mNeedCast: Boolean): String {
        val initView = StringBuilder()
        if (StringUtils.isEmpty(findPre)) {
            initView.append("private void initView() {\n")
        } else {
            initView.append("private void initView(View ")
            initView.append(findPre)
            initView.append(") {\n")
        }
        if (mIsLayoutInflater) {
            // 添加LayoutInflater.from(this).inflate(R.layout.activity_main, null);
            val layoutInflater = "$mLayoutInflaterText = LayoutInflater.from($context).inflate(R.layout.$mSelectedText, null);\n"
            initView.append(layoutInflater)
        }
        for (element in mElements) {
            if (element.isEnable) {
                var pre = findPre?.let { findPre + "." } ?: ""
                var inflater = ""
                if (mIsLayoutInflater) {
                    inflater = layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType)
                    pre = mLayoutInflaterText + "."
                }
                initView.append(element.fieldName)
                initView.append(inflater)
                initView.append(" = ")
                if (mNeedCast) {
                    initView.append("(")
                    initView.append(element.name)
                    initView.append(")")
                }
                initView.append(pre)
                initView.append("findViewById(")
                initView.append(element.fullID)
                initView.append(");\n")
                if (element.isClickable && element.isClickEnable) {
                    initView.append(element.fieldName)
                    initView.append(inflater)
                    initView.append(".setOnClickListener(this);\n")
                }
            }
        }
        initView.append("}\n")
        return initView.toString()
    }

    /**
     * 根据layoutInflaterType生成不同内容

     * @param mLayoutInflaterText mLayoutInflaterText
     * *
     * @param mLayoutInflaterType mLayoutInflaterType
     * *
     * @return String
     */
    internal fun layoutInflaterType2Str(mLayoutInflaterText: String, mLayoutInflaterType: Int): String {
        return when (mLayoutInflaterType) {
            2 -> Util.firstToUpperCase(mLayoutInflaterText)
            else -> mLayoutInflaterText.substring(1)
        }
    }


    /**
     * ButterKnife，创建findById代码到init方法里面

     * @param mIsLayoutInflater   mIsLayoutInflater
     * *
     * @param mLayoutInflaterText mLayoutInflaterText
     * *
     * @param context             context
     * *
     * @param mSelectedText       mSelectedText
     * *
     * @param mElements           mElements
     * *
     * @param viewMethodName      viewMethodName
     * *
     * @return String
     */
    internal fun createButterKnifeViewMethod(mIsLayoutInflater: Boolean, mLayoutInflaterText: String, context: String, mSelectedText: String, mElements: List<Element>, viewMethodName: String, mLayoutInflaterType: Int): String {
        val initView = StringBuilder()
        initView.append("// TODO:Copy method name to use\n")
        initView.append("private void ")
        initView.append(viewMethodName)
        initView.append("() {\n")
        if (mIsLayoutInflater) {
            // 添加LayoutInflater.from(this).inflate(R.layout.activity_main, null);
            val layoutInflater = "$mLayoutInflaterText = LayoutInflater.from($context).inflate(R.layout.$mSelectedText, null);\n"
            initView.append(layoutInflater)
        }
        for (element in mElements) {
            if (element.isEnable) {
                var inflater = ""
                if (mIsLayoutInflater) {
                    inflater = layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType)
                }
                initView.append(element.fieldName)
                initView.append(inflater)
                initView.append(" = ")
                initView.append("ButterKnife.findById(")
                initView.append(mLayoutInflaterText)
                initView.append(", ")
                initView.append(element.fullID)
                initView.append(");\n")
            }
        }
        initView.append("}\n")
        return initView.toString()
    }

    /**
     * FindViewById，创建OnClick方法和switch

     * @param mOnClickList 可onclick的Element的集合
     * *
     * @return String
     */
    internal fun createFindViewByIdOnClickMethodAndSwitch(mOnClickList: List<Element>): String {
        val onClick = StringBuilder()
        onClick.append("@Override public void onClick(View v) {\n")
        onClick.append("switch (v.getId()) {\n")
        // add default statement
        onClick.append("\tdefault:\n")
        onClick.append("\t\tbreak;\n")
        for (element in mOnClickList) {
            if (element.isClickable) {
                onClick.append("\tcase ")
                onClick.append(element.fullID)
                onClick.append(":\n")
                onClick.append("\t\tbreak;\n")
            }
        }
        onClick.append("}\n")
        onClick.append("}\n")
        return onClick.toString()
    }

    /**
     * ButterKnife，在OnClick方法里面创建switch

     * @param psiMethodParamsViewField View类型的变量名
     * *
     * @param onClickValues            注解里面跟OnClickList的id集合
     * *
     * @return String
     */
    internal fun createSwitchByOnClickMethod(psiMethodParamsViewField: String, onClickValues: List<String>): String {
        val psiSwitch = StringBuilder()
        psiSwitch.append("switch (")
        psiSwitch.append(psiMethodParamsViewField)
        psiSwitch.append(".getId()) {\n")
        // add default statement
        psiSwitch.append("\tdefault:\n")
        psiSwitch.append("\t\tbreak;\n")
        for (value in onClickValues) {
            psiSwitch.append("\tcase ")
            psiSwitch.append(value)
            psiSwitch.append(":\n")
            psiSwitch.append("\t\tbreak;\n")
        }
        psiSwitch.append("}")
        return psiSwitch.toString()
    }

    /**
     * ButterKnife，创建OnClick方法和switch

     * @param mOnClickList 可onclick的Element的集合
     * *
     * @return String
     */
    internal fun createButterKnifeOnClickMethodAndSwitch(mOnClickList: List<Element>): String {
        val onClick = StringBuilder()
        onClick.append("@butterknife.OnClick(")
        if (mOnClickList.size == 1) {
            onClick.append(mOnClickList[0].fullID)
        } else {
            onClick.append("{")
            for (i in mOnClickList.indices) {
                val element = mOnClickList[i]
                if (i != 0) {
                    onClick.append(", ")
                }
                onClick.append(element.fullID)
            }
            onClick.append("}")
        }
        onClick.append(")\n")
        onClick.append("public void onClick(View v) {\n")
        onClick.append("switch (v.getId()) {\n")
        // add default statement
        onClick.append("\tdefault:\n")
        onClick.append("\t\tbreak;\n")
        for (element in mOnClickList) {
            onClick.append("\tcase ")
            onClick.append(element.fullID)
            onClick.append(":\n")
            onClick.append("\t\tbreak;\n")
        }
        onClick.append("}\n")
        onClick.append("}\n")
        return onClick.toString()
    }

    /**
     * FindViewById，创建ViewHolder

     * @param viewHolderName     viewHolderName
     * *
     * @param viewHolderRootView viewHolderRootView
     * *
     * @param mElements          mElements
     * *
     * @param mNeedCasts         是否需要强转
     * *
     * @return String
     */
    internal fun createFindViewByIdViewHolder(viewHolderName: String, viewHolderRootView: String, mElements: List<Element>, mNeedCasts: Boolean): String {
        // ViewHolder
        val viewHolderText = StringBuilder()
        // ViewHolder的Constructor
        val viewHolderConstructorText = StringBuilder()
        // rootView
        viewHolderText.append("android.view.View ")
        viewHolderText.append(viewHolderRootView)
        viewHolderText.append(";\n")
        // Constructor
        viewHolderConstructorText.append(viewHolderName)
        viewHolderConstructorText.append("(android.view.View ")
        viewHolderConstructorText.append(viewHolderRootView)
        viewHolderConstructorText.append(") {\n")
        viewHolderConstructorText.append("this.")
        viewHolderConstructorText.append(viewHolderRootView)
        viewHolderConstructorText.append(" = ")
        viewHolderConstructorText.append(viewHolderRootView)
        viewHolderConstructorText.append(";\n")
        // 添加field和findViewById
        for (element in mElements) {
            // 添加Field
            viewHolderText.append(element.name)
            viewHolderText.append(" ")
            viewHolderText.append(element.fieldName)
            viewHolderText.append(";\n")
            // 添加findViewById
            viewHolderConstructorText.append("this.")
            viewHolderConstructorText.append(element.fieldName)
            viewHolderConstructorText.append(" = ")
            if (mNeedCasts) {
                viewHolderConstructorText.append("(")
                viewHolderConstructorText.append(element.name)
                viewHolderConstructorText.append(") ")
            }
            viewHolderConstructorText.append(viewHolderRootView)
            viewHolderConstructorText.append(".findViewById(")
            viewHolderConstructorText.append(element.fullID)
            viewHolderConstructorText.append(");\n")
        }
        viewHolderConstructorText.append("}")
        // 添加Constructor到ViewHolder
        viewHolderText.append(viewHolderConstructorText.toString())
        return viewHolderText.toString()
    }

    /**
     * ButterKnife，创建ViewHolder

     * @param viewHolderName     viewHolderName
     * *
     * @param viewHolderRootView viewHolderRootView
     * *
     * @param mElements          mElements
     * *
     * @return String
     */
    internal fun createButterKnifeViewHolder(viewHolderName: String, viewHolderRootView: String, mElements: List<Element>): String {
        // ViewHolder
        val viewHolderText = StringBuilder()
        // ViewHolder的Constructor
        val viewHolderConstructorText = StringBuilder()
        // 添加field和findViewById
        for (element in mElements) {
            // 添加Field
            viewHolderText.append("@BindView(")
            viewHolderText.append(element.fullID)
            viewHolderText.append(")\n")
            viewHolderText.append(element.name)
            viewHolderText.append(" ")
            viewHolderText.append(element.fieldName)
            viewHolderText.append(";\n")
        }
        // Constructor
        viewHolderConstructorText.append(viewHolderName)
        viewHolderConstructorText.append("(android.view.View ")
        viewHolderConstructorText.append(viewHolderRootView)
        viewHolderConstructorText.append(") {\n")
        viewHolderConstructorText.append("ButterKnife.bind(this, ")
        viewHolderConstructorText.append(viewHolderRootView)
        viewHolderConstructorText.append(");\n")
        viewHolderConstructorText.append("}")
        // 添加Constructor到ViewHolder
        viewHolderText.append(viewHolderConstructorText.toString())
        return viewHolderText.toString()
    }
}
