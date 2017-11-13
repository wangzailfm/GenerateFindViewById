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
import getIDsFromLayoutToList
import getTargetClass
import isExtendsActivityOrActivityCompat
import isExtendsFragmentOrFragmentV4
import showPopupBalloon
import utils.CreateMethodCreator
import views.ButterKnifeDialog
import java.util.*

/**
 * @author Jowan
 */
class ButterKnifeAction : AnAction() {
    private var mDialog: ButterKnifeDialog? = null

    override fun actionPerformed(e: AnActionEvent) {
        // 获取project
        val project = e.project ?: return
        // 获取选中内容
        val mEditor = e.getData(PlatformDataKeys.EDITOR) ?: return
        // 未选中布局内容，显示dialog
        val popupTime = 5
        val mSelectedText: String? = mEditor.selectionModel.selectedText ?: let {
            Messages.showInputDialog(project,
                    Constant.Action.SELECTED_MESSAGE,
                    Constant.Action.SELECTED_TITLE,
                    Messages.getInformationIcon()) ?: let {
                mEditor.showPopupBalloon(Constant.Action.SELECTED_ERROR_NO_NAME, popupTime)
                return
            }
        }
        // 判断是否有onCreate/onCreateView方法
        val psiFile = PsiUtilBase.getPsiFileInEditor(mEditor, project) ?: return
        val psiClass = mEditor getTargetClass psiFile ?: let {
            mEditor.showPopupBalloon(Constant.Action.SELECTED_ERROR_NO_POINT, popupTime)
            return
        }
        // 判断是否有onCreate方法
        if (project isExtendsActivityOrActivityCompat psiClass && psiClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATE, false).isEmpty()) {
            // 写onCreate方法
            mSelectedText?.let {
                CreateMethodCreator<Any>(mEditor, psiFile, psiClass, Constant.CREATOR_COMMAND_NAME,
                        it, Constant.CLASS_TYPE_BY_ACTIVITY, true).execute()
            }
            return
        }
        // 判断是否有onCreateView方法
        if (project isExtendsFragmentOrFragmentV4 psiClass && psiClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATEVIEW, false).isEmpty()) {
            mSelectedText?.let {
                CreateMethodCreator<Any>(mEditor, psiFile, psiClass, Constant.CREATOR_COMMAND_NAME,
                        it, Constant.CLASS_TYPE_BY_FRAGMENT, true).execute()
            }
            return
        }
        // 获取布局文件，通过FilenameIndex.getFilesByName获取GlobalSearchScope.allScope(project)搜索整个项目
        val psiFiles = FilenameIndex.getFilesByName(project,
                mSelectedText + Constant.SELECTED_TEXT_SUFFIX,
                GlobalSearchScope.allScope(project))
        if (psiFiles.isEmpty()) {
            mEditor.showPopupBalloon(Constant.Action.SELECTED_ERROR_NO_SELECTED, popupTime)
            return
        }
        val xmlFile = if (psiFiles.size > 1) {
            val psiFilePath = psiFile.parent?.toString()!!
            val psiFiles1 = psiFiles.filter {
                val modulePath = it.parent?.toString()!!
                modulePath.contains("\\src\\main\\res\\layout") && psiFilePath.substring(0, psiFilePath.indexOf("\\main\\")) == modulePath.substring(0, modulePath.indexOf("\\main\\"))
            }
            if (psiFiles1.isEmpty()) {
                mEditor.showPopupBalloon(Constant.Action.SELECTED_ERROR_NO_SELECTED, popupTime)
                return
            } else psiFiles1[0] as XmlFile
        } else {
            psiFiles[0] as XmlFile
        }
//        val xmlFile = psiFiles[0] as XmlFile
        val elements = ArrayList<Element>()
        getIDsFromLayoutToList(xmlFile, elements)
        // 将代码写入文件，不允许在主线程中进行实时的文件写入
        if (elements.size == 0) {
            mEditor.showPopupBalloon(Constant.Action.SELECTED_ERROR_NO_ID, popupTime)
            return
        }
        // 有的话就创建变量和ButterKnife代码
        if (mDialog != null && mDialog!!.isShowing) {
            mDialog?.cancelDialog()
        }
        mDialog = ButterKnifeDialog(mEditor = mEditor,
                mProject = project,
                mPsiFile = psiFile,
                mClass = psiClass,
                mElements = elements,
                mSelectedText = mSelectedText!!,
                mIsButterKnife = true,
                elementSize = elements.size)
        mDialog?.showDialog()
    }
}
