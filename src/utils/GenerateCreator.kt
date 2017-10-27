package utils

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.command.WriteCommandAction.Simple
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import constant.Constant
import entitys.Element
import org.apache.commons.lang.StringUtils
import views.GenerateDialog
import java.util.*

/**
 * 生成代码
 * @author Jowan
 */
class GenerateCreator<T>(
        private val mDialog: GenerateDialog,
        private val mEditor: Editor,
        private val mFile: PsiFile,
        private val mClass: PsiClass,
        private val mProject: Project,
        private val mElements: List<Element>,
        private val mFactory: PsiElementFactory,
        private val mSelectedText: String,
        private val mIsLayoutInflater: Boolean,
        private val mLayoutInflaterText: String,
        private val mLayoutInflaterType: Int,
        private val mIsButterKnife: Boolean,
        private val mIsBind: Boolean,
        private val mViewHolder: Boolean,
        command: String,
        private val mNeedCasts: Boolean
) : Simple<T>(mClass.project, command) {

    private val mOnClickList = ArrayList<Element>()

    init {
        if (mIsButterKnife) {
            mElements.filterTo(mOnClickList) {
                it.isEnable && it.isClickable
            }
        }
    }

    @Throws(Throwable::class)
    override fun run() {
        try {
            if (mViewHolder) {
                generateViewHolder(mIsButterKnife)
            } else {
                if (mIsButterKnife) {
                    generateButterKnife()
                } else {
                    generateFindViewById()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 异常打印
            mDialog.cancelDialog()
            Util.showPopupBalloon(mEditor, e.message, 10)
            return
        }

        // 重写class
        val styleManager = JavaCodeStyleManager.getInstance(mProject)
        styleManager.optimizeImports(mFile)
        styleManager.shortenClassReferences(mClass)
        ReformatCodeProcessor(mProject, mClass.containingFile, null, false).runWithoutProgress()
        Util.showPopupBalloon(mEditor, Constant.actions.SELECTED_SUCCESS, 5)
    }

    /**
     * 创建ViewHolder

     * @param isButterKnife true FindViewById
     * *                      false ButterKnife
     */
    private fun generateViewHolder(isButterKnife: Boolean) {
        val viewHolderName = "ViewHolder"
        val viewHolderRootView = "view"
        val str: String
        str = if (isButterKnife) {
            Util.createButterKnifeViewHolder(viewHolderName, viewHolderRootView, mElements)
        } else {
            Util.createFindViewByIdViewHolder(viewHolderName, viewHolderRootView, mElements, mNeedCasts)
        }
        // 创建ViewHolder类
        val viewHolder = mFactory.createClassFromText(str, mClass)
        // 设置名字
        viewHolder.setName(viewHolderName)
        // 添加ViewHolder类到类中
        mClass.add(viewHolder)
        // 添加static
        mClass.addBefore(mFactory.createKeyword("static"), mClass.findInnerClassByName(viewHolderName, true))
    }

    /**
     * 设置变量的值FindViewById，Activity和Fragment
     */
    private fun generateFindViewById() {
        if (Util.isExtendsActivityOrActivityCompat(mProject, mClass)) {
            // 判断是否有onCreate方法
            if (mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATE, false).isEmpty()) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateMethod(mSelectedText, false), mClass))
                return
            }
            generateFindViewByIdFields()
            // 获取setContentView
            var setContentViewStatement: PsiStatement? = null
            // onCreate是否存在initView方法
            var hasInitViewStatement = false
            // 获取onCreate方法对象
            val onCreate = mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATE, false)[0]
            if (onCreate.body != null) {
                for (psiStatement in onCreate.body!!.statements) {
                    // 查找setContentView
                    if (psiStatement.firstChild is PsiMethodCallExpression) {
                        val methodExpression = (psiStatement.firstChild as PsiMethodCallExpression).methodExpression
                        if (methodExpression.text == Constant.utils.CREATOR_SETCONTENTVIEW_METHOD) {
                            setContentViewStatement = psiStatement
                        } else if (methodExpression.text == Constant.utils.CREATOR_INITVIEW_NAME) {
                            hasInitViewStatement = true
                        }
                    }
                }
                if (setContentViewStatement == null) {
                    onCreate.body?.add(mFactory.createStatementFromText("setContentView(R.layout.$mSelectedText);", mClass))
                }

                if (!hasInitViewStatement) {
                    // 将initView()写到setContentView()后面
                    setContentViewStatement?.let {
                        onCreate.body?.addAfter(mFactory.createStatementFromText("initView();", mClass), it)
                    } ?: let {
                        onCreate.body?.add(mFactory.createStatementFromText("initView();", mClass))
                    }
                }
            }

            generateFindViewByIdLayoutCode(null, "getApplicationContext()")

            return
        }
        if (Util.isExtendsFragmentOrFragmentV4(mProject, mClass)) {
            var isViewExist = false
            for (psiField in mClass.fields) {
                if (psiField.text != null && psiField.text == "private View view;") {
                    isViewExist = true
                    break
                } else {
                    isViewExist = false
                }
            }
            if (!isViewExist) {
                mClass.add(mFactory.createFieldFromText("private View view;", mClass))
            }
            // 判断是否有onCreateView方法
            if (mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATEVIEW, false).isEmpty()) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateViewMethod(false), mClass))
                return
            }
            generateFindViewByIdFields()
            // 查找onCreateView
            var returnStatement: PsiReturnStatement? = null
            // view
            var returnValue: String? = null
            // onCreateView是否存在initView方法
            var hasInitViewStatement = false

            val onCreate = mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATEVIEW, false)[0]
            if (onCreate.body != null) {
                for (psiStatement in onCreate.body!!.statements) {
                    if (psiStatement is PsiReturnStatement) {
                        // 获取view的值
                        returnStatement = psiStatement
                        if (returnStatement.returnValue != null) {
                            returnValue = returnStatement.returnValue!!.text
                        }
                    } else if (psiStatement.firstChild is PsiMethodCallExpression) {
                        val methodExpression = (psiStatement.firstChild as PsiMethodCallExpression).methodExpression
                        if (methodExpression.text == Constant.utils.CREATOR_INITVIEW_NAME) {
                            hasInitViewStatement = true
                        }
                    }
                }

                if (!hasInitViewStatement && returnStatement != null && returnValue != null) {
                    onCreate.body?.addBefore(mFactory.createStatementFromText("initView($returnValue);", mClass), returnStatement)
                }
            }
            generateFindViewByIdLayoutCode(returnValue, "getActivity()")
        }
    }

    /**
     * 创建变量
     */
    private fun generateFindViewByIdFields() {
        if (mIsLayoutInflater) {
            val inflater = "private View $mLayoutInflaterText;"
            // 已存在的变量就不创建
            val duplicateField = mClass.fields
                    .map { it.name }
                    .any { it != null && it == mLayoutInflaterText }
            if (!duplicateField) {
                mClass.add(mFactory.createFieldFromText(inflater, mClass))
            }
        }
        for (element in mElements) {
            // 已存在的变量就不创建
            val fields = mClass.fields
            var duplicateField = false
            fields
                    .asSequence()
                    .map { it.name }
                    .forEach {
                        if (!mIsLayoutInflater) {
                            if (it != null && it == element.fieldName) {
                                duplicateField = true
                                return@forEach
                            }
                        } else {
                            val layoutField = element.fieldName + Util.layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType)
                            if (it != null && it == layoutField) {
                                duplicateField = true
                                return@forEach
                            }
                        }
                    }
            // 已存在跳出
            if (duplicateField) {
                continue
            }
            // 设置变量名，获取text里面的内容
            if (element.isEnable) {
                // 添加到class
                mClass.add(mFactory.createFieldFromText(Util.createFieldByElement(
                        Util.createFieldText(element, mProject), element, mIsLayoutInflater, mLayoutInflaterText, mLayoutInflaterType), mClass))
            }
        }
    }

    /**
     * 写initView方法

     * @param findPre Fragment的话要view.findViewById
     * *
     * @param context context
     */
    private fun generateFindViewByIdLayoutCode(findPre: String?, context: String) {
        // 判断是否已有initView方法
        val initViewMethods = mClass.findMethodsByName(Constant.utils.CREATOR_INITVIEW_NAME, false)
        // 有initView方法
        if (initViewMethods.isNotEmpty() && initViewMethods[0].body != null) {
            val initViewMethodBody = initViewMethods[0].body
            // 获取initView方法里面的每条内容
            val statements = initViewMethodBody!!.statements
            if (mIsLayoutInflater) {
                // 添加LayoutInflater.from(this).inflate(R.layout.activity_main, null);
                val layoutInflater = "$mLayoutInflaterText = LayoutInflater.from($context).inflate(R.layout.$mSelectedText, null);"
                // 判断是否存在
                var isExist = false
                for (statement in statements) {
                    if (statement.text == layoutInflater) {
                        isExist = true
                        break
                    } else {
                        isExist = false
                    }
                }
                // 不存在才添加
                if (!isExist) {
                    initViewMethodBody.add(mFactory.createStatementFromText(layoutInflater, initViewMethods[0]))
                }
            }
            for (element in mElements) {
                if (element.isEnable) {
                    // 判断是否已存在findViewById
                    var isFdExist = false
                    var pre = if (StringUtils.isEmpty(findPre)) "" else findPre!! + "."
                    var inflater = ""
                    if (mIsLayoutInflater) {
                        inflater = Util.layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType)
                        pre = mLayoutInflaterText + "."
                    }
                    val findViewById = "${element.fieldName}$inflater =" + if(mNeedCasts) "(${element.name})" else "" + "${pre}findViewById(${element.fullID});"
                    for (statement in statements) {
                        if (statement.text == findViewById) {
                            isFdExist = true
                            break
                        } else {
                            isFdExist = false
                        }
                    }
                    // 不存在就添加
                    if (!isFdExist) {
                        initViewMethodBody.add(mFactory.createStatementFromText(findViewById, initViewMethods[0]))
                    }
                    if (element.isClickEnable) {
                        // 判断是否已存在setOnClickListener
                        var isClickExist = false
                        val setOnClickListener = "${element.fieldName}$inflater.setOnClickListener(this);"
                        for (statement in statements) {
                            if (statement.text == setOnClickListener) {
                                isClickExist = true
                                break
                            } else {
                                isClickExist = false
                            }
                        }
                        if (!isClickExist && element.isClickable) {
                            initViewMethodBody.add(mFactory.createStatementFromText(setOnClickListener, initViewMethods[0]))
                        }
                    }
                }
            }
        } else {
            mClass.add(mFactory.createMethodFromText(
                    Util.createFieldsByInitViewMethod(findPre, mIsLayoutInflater, mLayoutInflaterText, context, mSelectedText, mElements, mLayoutInflaterType, mNeedCasts), mClass))
        }
        getFindViewByIdOnClickList()
        if (mOnClickList.size != 0) {
            generateFindViewByIdOnClickListenerCode()
        }
    }

    /**
     * 添加实现OnClickListener接口
     */
    private fun generateFindViewByIdOnClickListenerCode() {
        // 获取已实现的接口
        val implementsList = mClass.implementsList
        var isImplOnClick = false
        if (implementsList != null) {
            // 获取列表
            val referenceElements = implementsList.referenceElements
            // 是否实现了OnClickListener接口
            isImplOnClick = Util.isImplementsOnClickListener(referenceElements)
        }
        // 未实现添加OnClickListener接口
        if (!isImplOnClick) {
            val referenceElementByFQClassName = mFactory.createReferenceElementByFQClassName("android.view.View.OnClickListener", mClass.resolveScope)
            // 添加的PsiReferenceList
            implementsList?.add(referenceElementByFQClassName)
        }
        generateFindViewByIdClickCode()
    }

    /**
     * 获取有OnClick属性的Element
     */
    private fun getFindViewByIdOnClickList() {
        mElements.filterTo(mOnClickList) { it.isEnable && it.isClickEnable && it.isClickable }
    }

    /**
     * 写onClick方法
     */
    private fun generateFindViewByIdClickCode() {
        // 判断是否已有onClick方法
        val onClickMethods = mClass.findMethodsByName(Constant.FIELD_ONCLICK, false)
        // 判断是否包含@OnClick注解
        val butterKnifeOnClickMethod = Util.getPsiMethodByButterKnifeOnClick(mClass)
        // 已有onClick方法
        if (butterKnifeOnClickMethod == null && onClickMethods.isNotEmpty() && onClickMethods[0].body != null) {
            val onClickMethodBody = onClickMethods[0].body
            // 获取switch
            for (psiElement in onClickMethodBody!!.children) {
                if (psiElement is PsiSwitchStatement) {
// 获取switch的内容
                    val psiSwitchStatementBody = psiElement.body
                    if (psiSwitchStatementBody != null) {
                        for (element in mOnClickList) {
                            val cass = "case " + element.fullID + ":"
                            // 判断是否存在
                            var isExist = false
                            for (statement in psiSwitchStatementBody.statements) {
                                if (statement.text.replace("\n", "").replace("break;", "") == cass) {
                                    isExist = true
                                    break
                                } else {
                                    isExist = false
                                }
                            }
                            // 不存在就添加
                            if (!isExist) {
                                psiSwitchStatementBody.add(mFactory.createStatementFromText(cass, psiSwitchStatementBody))
                                psiSwitchStatementBody.add(mFactory.createStatementFromText("break;", psiSwitchStatementBody))
                            }
                        }
                    }
                }
            }
            return
        }
        if (mOnClickList.size != 0) {
            mClass.add(mFactory.createMethodFromText(Util.createFindViewByIdOnClickMethodAndSwitch(mOnClickList), mClass))
        }
    }

    /**
     * 设置变量，Activity和Fragment
     */
    private fun generateButterKnife() {
        if (Util.isExtendsActivityOrActivityCompat(mProject, mClass)) {
            // 判断是否有onCreate方法
            if (mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATE, false).isEmpty()) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateMethod(mSelectedText, true), mClass))
            } else {
                generateButterKnifeFields(Constant.CLASS_TYPE_BY_ACTIVITY)
                if (mIsLayoutInflater) {
                    generateButterKnifeViewMethod("getApplicationContext()")
                }
                // 获取setContentView
                var setContentViewStatement: PsiStatement? = null
                // onCreate是否ButterKnife.bind(this);
                var hasButterKnifeBindStatement = false

                val onCreate = mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATE, false)[0]
                if (onCreate.body != null) {
                    for (psiStatement in onCreate.body!!.statements) {
                        // 查找setContentView
                        if (psiStatement.firstChild is PsiMethodCallExpression) {
                            val methodExpression = (psiStatement.firstChild as PsiMethodCallExpression).methodExpression
                            if (methodExpression.text == Constant.utils.CREATOR_SETCONTENTVIEW_METHOD) {
                                setContentViewStatement = psiStatement
                            } else if (methodExpression.text.contains(Constant.utils.FIELD_BUTTERKNIFE_BIND)) {
                                hasButterKnifeBindStatement = true
                            }
                        }
                    }
                    if (setContentViewStatement == null) {
                        onCreate.body?.add(mFactory.createStatementFromText("setContentView(R.layout.$mSelectedText);", mClass))
                    }

                    if (!hasButterKnifeBindStatement) {
                        // 将ButterKnife.bind(this);写到setContentView()后面
                        if (setContentViewStatement != null) {
                            onCreate.body?.addAfter(mFactory.createStatementFromText("ButterKnife.bind(this);", mClass), setContentViewStatement)
                        } else {
                            onCreate.body?.add(mFactory.createStatementFromText("ButterKnife.bind(this);", mClass))
                        }
                    }
                }
            }
            if (mOnClickList.size > 0 && !mIsLayoutInflater) {
                generateButterKnifeClickCode()
            }
            return
        }
        if (Util.isExtendsFragmentOrFragmentV4(mProject, mClass)) {
            // 判断是否有onCreateView方法
            if (mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATEVIEW, false).isEmpty()) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateViewMethod(true), mClass))

            } else {
                generateButterKnifeFields(Constant.CLASS_TYPE_BY_FRAGMENT)
                if (mIsLayoutInflater) {
                    generateButterKnifeViewMethod("getActivity()")
                }
                // 查找onCreateView
                var returnStatement: PsiReturnStatement? = null
                // view
                var returnValue: String? = null

                val onCreateView = mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATEVIEW, false)[0]
                if (onCreateView.body != null) {
                    for (psiStatement in onCreateView.body!!.statements) {
                        if (psiStatement is PsiReturnStatement) {
                            // 获取view的值
                            returnStatement = psiStatement
                            if (returnStatement.returnValue != null) {
                                returnValue = returnStatement.returnValue?.text
                            }
                        }
                    }

                    if (returnStatement != null && returnValue != null) {
                        var isBindExist = false
                        // 判断是否已存在unbinder = ButterKnife.bind
                        val bind = "unbinder = ButterKnife.bind(this, $returnValue);"
                        // 获取onCreateView方法里面的PsiStatement
                        for (psiStatement in onCreateView.body!!.statements) {
                            if (psiStatement.text.contains(bind)) {
                                isBindExist = true
                                break
                            } else {
                                isBindExist = false
                            }
                        }
                        if (!isBindExist && mIsBind) {
                            //将ButterKnife.bind(this);写到return view前面
                            onCreateView.body?.addBefore(mFactory.createStatementFromText(bind, mClass), returnStatement)
                        }
                    }
                }
            }
            if (mOnClickList.size > 0 && !mIsLayoutInflater) {
                generateButterKnifeClickCode()
            }
            if (mIsBind) {
                generateButterKnifeLayoutCode()
            }
        }
    }

    /**
     * 创建变量

     * @param type type
     */
    private fun generateButterKnifeFields(type: String) {
        if (type == Constant.CLASS_TYPE_BY_FRAGMENT) {
            // 判断View是否存在
            var isViewExist = false
            // 判断Unbinder是否存在
            var isUnBinderExist = false
            for (field in mClass.fields) {
                if (field.name != null && field.name == Constant.utils.CREATOR_VIEW_NAME) {
                    isViewExist = true
                }
                if (field.name != null && field.name == Constant.utils.CREATOR_UNBINDER_NAME) {
                    isUnBinderExist = true
                }
            }
            if (!isViewExist) {
                mClass.add(mFactory.createFieldFromText("private View view;", mClass))
            }
            if (!isUnBinderExist && mIsBind) {
                mClass.add(mFactory.createFieldFromText("private Unbinder unbinder;", mClass))
            }
        }
        if (mIsLayoutInflater) {
            val inflater = "private View $mLayoutInflaterText;"
            // 已存在的变量就不创建
            val duplicateField = mClass.fields
                    .map { it.name }
                    .any { it != null && it == mLayoutInflaterText }
            if (!duplicateField) {
                mClass.add(mFactory.createFieldFromText(inflater, mClass))
            }
        }
        mElements
                .asSequence()
                .filter { it.isEnable }
                .forEach { element ->
                    if (mIsLayoutInflater) {
                        val layoutField = element.fieldName + Util.layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType)
                        val duplicateField = mClass.fields.any { it.name != null && it.name == layoutField }
                        // 已存在跳出
                        if (!duplicateField) {
                            // 添加到class
                            mClass.add(mFactory.createFieldFromText(Util.createFieldByElement(
                                    Util.createFieldText(element, mProject), element, true, mLayoutInflaterText, mLayoutInflaterType), mClass))
                        }
                    } else {
                        // 已存在的变量就不创建
                        var isFieldExist = false
                        var isAnnotationExist = false
                        for (field in mClass.fields) {
                            if (field.name != null) {
                                isAnnotationExist = field.text.contains("@BindView(" + element.fullID + ")")
                                isFieldExist = field.text.contains(element.fieldName) && !(field.text.contains("private") || field.text.contains("static"))
                                if (isAnnotationExist || isFieldExist) {
                                    break
                                }
                            }
                        }
                        // @BindView(R.id.text) TextView mText;
                        // 如果两个都存在则跳出
                        if (!isAnnotationExist || !isFieldExist) {
                            if (!isFieldExist) {
                                // 如果只存在TextView mText
                                val fromText = element.name + " " + element.fieldName + ";"
                                // 添加到class
                                mClass.add(mFactory.createFieldFromText(fromText, mClass))
                            }

                            for (field in mClass.fields) {
                                if (field.name != null && field.text.contains(element.fieldName)
                                        && !(field.text.contains("private") || field.text.contains("static"))) {
                                    val annotationText = "@BindView(" + element.fullID + ")"
                                    // 添加注解到field
                                    mClass.addBefore(mFactory.createAnnotationFromText(annotationText, mClass), field)
                                    break
                                }
                            }
                        }
                    }
                }
    }

    /**
     * 写onClick方法
     */
    private fun generateButterKnifeClickCode() {
        // 判断是否包含@OnClick注解
        val butterKnifeOnClickMethod = Util.getPsiMethodByButterKnifeOnClick(mClass)
        // 有@OnClick注解
        if (butterKnifeOnClickMethod != null && butterKnifeOnClickMethod.body != null) {
            val onClickMethodBody = butterKnifeOnClickMethod.body
            // 获取switch
            var psiSwitchStatement: PsiSwitchStatement? = null
            for (psiElement in onClickMethodBody!!.children) {
                if (psiElement is PsiSwitchStatement) {
                    psiSwitchStatement = psiElement
                    break
                }
            }
            val psiMethodByButterKnifeOnClickValue = Util.getPsiMethodByButterKnifeOnClickValue(mClass)
            val onClickIdList = Util.getOnClickListById(mOnClickList)
            val onClickValues = Util.createOnClickValue(psiMethodByButterKnifeOnClickValue, onClickIdList)
            // 有switch
            if (psiSwitchStatement != null) {
                // 获取switch的内容
                val psiSwitchStatementBody = psiSwitchStatement.body
                if (psiSwitchStatementBody != null) {
                    for (onClickValue in onClickValues) {
                        val cass = "case $onClickValue:"
                        // 判断是否存在
                        var isExist = false
                        for (statement in psiSwitchStatementBody.statements) {
                            if (statement.text.replace("\n", "").replace("break;", "") == cass) {
                                isExist = true
                                break
                            } else {
                                isExist = false
                            }
                        }
                        // 不存在就添加
                        if (!isExist) {
                            psiSwitchStatementBody.add(mFactory.createStatementFromText(cass, psiSwitchStatementBody))
                            psiSwitchStatementBody.add(mFactory.createStatementFromText("break;", psiSwitchStatementBody))
                        }
                    }
                }
            } else {
                // 没有switch
                val psiMethodParamsViewField = Util.getPsiMethodParamsViewField(mClass)
                if (psiMethodParamsViewField != null) {
                    butterKnifeOnClickMethod.body?.add(mFactory.createStatementFromText(
                            Util.createSwitchByOnClickMethod(psiMethodParamsViewField, onClickValues), butterKnifeOnClickMethod))
                }
            }
            Util.createOnClickAnnotation(mClass, mFactory, onClickValues)
            return
        }
        if (mOnClickList.size != 0) {
            mClass.add(mFactory.createMethodFromText(Util.createButterKnifeOnClickMethodAndSwitch(mOnClickList), mClass))
        }
    }

    /**
     * 写onDestroyView方法
     */
    private fun generateButterKnifeLayoutCode() {
        // 判断是否已有onDestroyView方法
        val onDestroyViewMethods = mClass.findMethodsByName(Constant.utils.CREATOR_ONDESTROYVIEW_METHOD, false)
        // 有onDestroyView方法
        if (onDestroyViewMethods.isNotEmpty() && onDestroyViewMethods[0].body != null) {
            val onDestroyViewMethodBody = onDestroyViewMethods[0].body
            // 获取onDestroyView方法里面的每条内容
            val statements = onDestroyViewMethodBody!!.statements
            for (element in mElements) {
                if (element.isEnable) {
                    // 判断是否已存在unbinder.unbind();
                    var isFdExist = false
                    for (statement in statements) {
                        if (statement.text == Constant.utils.CREATOR_UNBINDER_FIELD) {
                            isFdExist = true
                            break
                        } else {
                            isFdExist = false
                        }
                    }
                    // 不存在就添加
                    if (!isFdExist) {
                        onDestroyViewMethodBody.add(mFactory.createStatementFromText(Constant.utils.CREATOR_UNBINDER_FIELD, onDestroyViewMethods[0]))
                        break
                    }
                }
            }
            return
        }
        mClass.add(mFactory.createMethodFromText(Util.createOnDestroyViewMethod(), mClass))
    }

    /**
     * 创建LayoutInflater的方法，包含ButterKnife.findById(view, R.id.id)
     * @param context context
     */
    private fun generateButterKnifeViewMethod(context: String) {
        val viewMethodName = "init" + Util.layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType)
        val viewMethod = Util.getPsiMethodByName(mClass, viewMethodName)
        if (viewMethod != null && viewMethod.body != null) {
            val body = viewMethod.body
            // 获取initView方法里面的每条内容
            val statements = body!!.statements
            for (element in mElements) {
                if (element.isEnable) {
                    // 判断是否已存在findViewById
                    var isFdExist = false
                    var inflater = ""
                    if (mIsLayoutInflater) {
                        inflater = Util.layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType)
                    }
                    val findViewById = "${element.fieldName}$inflater = ButterKnife.findById($mLayoutInflaterText, ${element.fullID});"
                    for (statement in statements) {
                        if (statement.text == findViewById) {
                            isFdExist = true
                            break
                        } else {
                            isFdExist = false
                        }
                    }
                    // 不存在就添加
                    if (!isFdExist) {
                        body.add(mFactory.createStatementFromText(findViewById, viewMethod))
                    }
                }
            }
            return
        }
        mClass.add(mFactory.createMethodFromText(
                Util.createButterKnifeViewMethod(mIsLayoutInflater, mLayoutInflaterText, context, mSelectedText, mElements, viewMethodName, mLayoutInflaterType), mClass))
    }

}
