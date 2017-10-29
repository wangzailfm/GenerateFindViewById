package views

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import entitys.Element
import getPsiMethodByButterKnifeOnClick
import getPsiMethodByButterKnifeOnClickValue
import java.util.*

/**
 * ButterKnifeDialog
 * @author Jowan
 */
class ButterKnifeDialog
/**
 * ButterKnifeDialog
 */
(mProject: Project,
 mEditor: Editor,
 mSelectedText: String,
        /*获取mElements*/
 private val mElements: ArrayList<Element>,
        /*获取当前文件*/
 mPsiFile: PsiFile,
        /*获取class*/
 private val mClass: PsiClass,
        /*判断是否全选*/
 private var elementSize: Int,
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
        checkBind()
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
        var isFdExist: Boolean
        // 判断是否存在case R.id.id:
        var isCaseExist = false
        // 判断注解是否存在R.id.id
        var isAnnotationValueExist = false
        val fields = mClass.fields
        var onClickStatement: Array<out PsiElement>? = null
        val psiMethodByButterKnifeOnClickValue = mClass.getPsiMethodByButterKnifeOnClickValue()
        val psiMethodByButterKnifeOnClick = mClass.getPsiMethodByButterKnifeOnClick()
        if (psiMethodByButterKnifeOnClick != null && psiMethodByButterKnifeOnClick.body != null) {
            onClickStatement = psiMethodByButterKnifeOnClick.body?.statements
        }
        for (element in mElements) {
            element.isClickable = true
            element.setClickEnable()
            isFdExist = checkFieldExist(fields, element)
            if (onClickStatement != null) {
                isCaseExist = checkCaseExist(onClickStatement, element)
            }
            if (psiMethodByButterKnifeOnClickValue.size > 0) {
                isAnnotationValueExist = psiMethodByButterKnifeOnClickValue.contains(element.fullID)
            }
            setElementProperty(elementSize, isFdExist, isCaseExist, isAnnotationValueExist, fields, element)
        }
    }

    /**
     * 判断onClick方法里面是否包含field的case
     * @param onClickStatement onClick方法
     *
     * @param element element
     *
     * @return boolean
     */
    private fun checkCaseExist(onClickStatement: Array<out PsiElement>, element: Element): Boolean {
        val cass = "case " + element.fullID + ":"
        onClickStatement
                .filterIsInstance<PsiSwitchStatement>()
                .mapNotNull { // 获取switch的内容
                    it.body
                }
                .forEach {
                    it.statements
                            .filter { it.text.replace("\n".toRegex(), "").replace("break;".toRegex(), "") == cass }
                            .forEach { return true }
                }
        return false
    }

    /**
     * 判断变量是否包含@BindView注解
     * @param fields fields
     *
     * @param element element
     *
     * @return boolean
     */
    private fun checkFieldExist(fields: Array<PsiField>, element: Element): Boolean {
        fields.forEach {
            if (it.name != null && it.name == element.fieldName && it.text.contains("@BindView(${element.fullID})")) {
                return true
            }
        }
        return false
    }

    /**
     * 为已存在的变量设置checkbox
     * @param mElementSizeOld mElementSizeOld
     *
     * @param isFdExist 判断是否已存在的变量
     *
     * @param isCaseExist 判断是否存在case R.id.id:
     *
     * @param isAnnotationValueExist 判断注解是否存在R.id.id
     *
     * @param fields fields
     *
     * @param element element
     */
    private fun setElementProperty(mElementSizeOld: Int, isFdExist: Boolean, isCaseExist: Boolean,
                                   isAnnotationValueExist: Boolean, fields: Array<PsiField>, element: Element) {
        var mElementSize = mElementSizeOld
        for (field in fields) {
            val name = field.name
            if (name != null && name == element.fieldName && isFdExist) {
                // 已存在的变量设置checkbox为false
                element.isEnable = false
                mElementSize -= 1
                elementSize = mElementSize
                if (element.isClickEnable && (!isCaseExist || !isAnnotationValueExist)) {
                    element.isClickable = true
                    element.isEnable = true
                    mElementSize += 1
                    elementSize = mElementSize
                }
                break
            }
        }
    }
}
