package views

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import entitys.Element
import utils.Util

/**
 * FindViewByIdDialog
 * @author Jowan
 */
class FindViewByIdDialog
/**
 * FindViewByIdDialog
 */
(mProject: Project,
 mEditor: Editor,
 mSelectedText: String,
    /*获取mElements*/
 private val mElements: List<Element>,
    /*获取当前文件*/
 mPsiFile: PsiFile,
    /*获取class*/
 private val mClass: PsiClass,
    /*判断是否全选*/
 private var elementSize: Int = 0,
    /*判断是否是ButterKnife*/
 mIsButterKnife: Boolean)
: GenerateDialog(mProject = mProject,
        mEditor = mEditor,
        mSelectedText = mSelectedText,
        elements = mElements,
        mPsiFile = mPsiFile,
        psiClass = mClass,
        elementSize = elementSize,
        mIsButterKnife = mIsButterKnife) {

    init {
        initExist()
        initTopPanel()
        initContentPanel()
        setCheckAll()
        initBottomPanel()
        setConstraints()
        setDialog()
    }

    /**
     * 判断已存在的变量，设置全选
     * 判断onclick是否写入
     */
    private fun initExist() {
        // 判断是否已存在的变量
        var isFdExist = false
        // 判断是否已存在setOnClickListener
        var isClickExist = false
        // 判断是否存在case R.id.id:
        var isCaseExist = false
        val fields = mClass.fields
        // 获取initView方法的内容
        val statements = Util.getInitViewBodyStatements(mClass)
        val onClickStatement = Util.getOnClickStatement(mClass)
        for (element in mElements) {
            if (statements != null) {
                isFdExist = checkFieldExist(statements, element)
                val setOnClickListener = element.fieldName + ".setOnClickListener(this);"
                isClickExist = checkClickExist(statements, setOnClickListener)
            }
            if (onClickStatement != null) {
                isCaseExist = checkCaseExist(onClickStatement, element)
            }
            setElementProperty(elementSize, isFdExist, isClickExist, isCaseExist, fields, element)
        }
    }

    /**
     * 判断onClick方法里面是否包含field的case
     * @param onClickStatement onClick方法
     * *
     * @param element element
     * *
     * @return boolean
     */
    private fun checkCaseExist(onClickStatement: Array<PsiElement>, element: Element): Boolean {
        val cass = "case " + element.fullID + ":"
        onClickStatement
                .filterIsInstance<PsiSwitchStatement>()
                .mapNotNull { // 获取switch的内容
                    it.body
                }
                .forEach {
                    it.statements
                            .filter { it.text.replace("\n", "").replace("break;", "") == cass }
                            .forEach { return true }
                }
        return false
    }

    /**
     * 判断initView方法里面是否包含field的findViewById
     * @param statements initView方法
     * *
     * @param element element
     * *
     * @return boolean
     */
    private fun checkFieldExist(statements: Array<PsiStatement>, element: Element): Boolean {
        return statements.any { it.text.contains(element.fieldName) && it.text.contains("findViewById(" + element.fullID + ");") }
    }

    /**
     * 判断是否setOnClickListener
     * @param statements onClick方法
     * *
     * @param setOnClickListener setOnClickListener
     * *
     * @return boolean
     */
    private fun checkClickExist(statements: Array<PsiStatement>, setOnClickListener: String): Boolean {
        return statements.any { it.text == setOnClickListener }
    }

    /**
     * 为已存在的变量设置checkbox
     * @param mElementSizes mElementSizes
     * *
     * @param isFdExist 判断是否已存在的变量
     * *
     * @param isClickExist 判断是否已存在setOnClickListener
     * *
     * @param isCaseExist 判断是否存在case R.id.id:
     * *
     * @param fields fields
     * *
     * @param element element
     */
    private fun setElementProperty(mElementSizes: Int, isFdExist: Boolean, isClickExist: Boolean,
                                   isCaseExist: Boolean, fields: Array<PsiField>, element: Element) {
        var mElementSize = mElementSizes
        for (field in fields) {
            val name = field.name
            if (name != null && name == element.fieldName && isFdExist) {
                // 已存在的变量设置checkbox为false
                element.isEnable = false
                mElementSize -= 1
                elementSize = mElementSize
                if (element.isClickEnable && (!isClickExist || !isCaseExist)) {
                    element.isClickable = true
                    element.isEnable = true
                    mElementSize = elementSize + 1
                    elementSize = mElementSize
                }
                break
            }
        }
    }
}
