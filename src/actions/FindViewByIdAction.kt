package actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.xml.XmlFile
import constant.Constant
import entitys.Element
import utils.CreateMethodCreator
import utils.Util
import views.FindViewByIdDialog
import java.util.*

/**
 * @author Jowan
 */
class FindViewByIdAction : AnAction() {
    private var mDialog: FindViewByIdDialog? = null

    override fun actionPerformed(e: AnActionEvent) {
        // 获取project
        val project = e.project ?: return
        // 获取选中内容
        val mEditor = e.getData(PlatformDataKeys.EDITOR) ?: return
        // 未选中布局内容，显示dialog
        val popupTime = 5
        val mSelectedText: String? = mEditor.selectionModel.selectedText ?: let {
            Messages.showInputDialog(project,
                    Constant.actions.SELECTED_MESSAGE,
                    Constant.actions.SELECTED_TITLE,
                    Messages.getInformationIcon()) ?: let {
                Util.showPopupBalloon(mEditor, Constant.actions.SELECTED_ERROR_NO_NAME, popupTime)
                return
            }
        }
        // 判断是否有onCreate/onCreateView方法
        val psiFile = PsiUtilBase.getPsiFileInEditor(mEditor, project) ?: return
        val psiClass = Util.getTargetClass(mEditor, psiFile) ?: let {
            Util.showPopupBalloon(mEditor, Constant.actions.SELECTED_ERROR_NO_POINT, popupTime)
            return
        }
        // 判断是否有onCreate方法
        if (Util.isExtendsActivityOrActivityCompat(project, psiClass) && psiClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATE, false).isEmpty()) {
            // 写onCreate方法
            CreateMethodCreator<Any>(mEditor, psiFile, psiClass, Constant.CREATOR_COMMAND_NAME,
                    mSelectedText!!, Constant.CLASS_TYPE_BY_ACTIVITY, false).execute()
            return
        }
        // 判断是否有onCreateView方法
        if (Util.isExtendsFragmentOrFragmentV4(project, psiClass) && psiClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATEVIEW, false).isEmpty()) {
            CreateMethodCreator<Any>(mEditor, psiFile, psiClass, Constant.CREATOR_COMMAND_NAME,
                    mSelectedText!!, Constant.CLASS_TYPE_BY_FRAGMENT, false).execute()
            return
        }
        // 获取布局文件，通过FilenameIndex.getFilesByName获取
        // GlobalSearchScope.allScope(project)搜索整个项目
        val psiFiles = FilenameIndex.getFilesByName(project,
                mSelectedText + Constant.SELECTED_TEXT_SUFFIX,
                GlobalSearchScope.allScope(project))
        if (psiFiles.isEmpty()) {
            Util.showPopupBalloon(mEditor, Constant.actions.SELECTED_ERROR_NO_SELECTED, popupTime)
            return
        }
        val xmlFile = psiFiles[0] as XmlFile
        val elements = ArrayList<Element>()
        Util.getIDsFromLayout(xmlFile, elements)
        // 将代码写入文件，不允许在主线程中进行实时的文件写入
        if (elements.size == 0) {
            Util.showPopupBalloon(mEditor, Constant.actions.SELECTED_ERROR_NO_ID, popupTime)
            return
        }
        // 有的话就创建变量和findViewById
        if (mDialog != null && mDialog!!.isShowing) {
            mDialog?.cancelDialog()
        }
        mDialog = FindViewByIdDialog(mEditor = mEditor,
                mProject = project,
                mPsiFile = psiFile,
                mClass = psiClass,
                mElements = elements,
                mSelectedText = mSelectedText!!,
                mIsButterKnife = false)
        mDialog?.showDialog()
    }
}
