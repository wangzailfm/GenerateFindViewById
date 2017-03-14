package actions;

import utils.CreateMethodCreator;
import utils.Util;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.xml.XmlFile;
import constant.Constant;
import entitys.Element;
import org.apache.commons.lang.StringUtils;
import views.FindViewByIdDialog;
import views.GenerateDialog;

import java.util.ArrayList;
import java.util.List;

public class FindViewByIdAction extends AnAction {
    private FindViewByIdDialog mDialog;

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 获取project
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        // 获取选中内容
        final Editor mEditor = e.getData(PlatformDataKeys.EDITOR);
        if (mEditor == null) {
            return;
        }
        String mSelectedText = mEditor.getSelectionModel().getSelectedText();
        // 未选中布局内容，显示dialog
        int popupTime = 5;
        if (StringUtils.isEmpty(mSelectedText)) {
            mSelectedText = Messages.showInputDialog(project,
                    Constant.actions.selectedMessage,
                    Constant.actions.selectedTitle,
                    Messages.getInformationIcon());
            if (StringUtils.isEmpty(mSelectedText)) {
                Util.showPopupBalloon(mEditor, Constant.actions.selectedErrorNoName, popupTime);
                return;
            }
        }
        // 获取布局文件，通过FilenameIndex.getFilesByName获取
        // GlobalSearchScope.allScope(project)搜索整个项目
        PsiFile[] psiFiles = new PsiFile[0];
        psiFiles = FilenameIndex.getFilesByName(project,
                mSelectedText + Constant.selectedTextSUFFIX,
                GlobalSearchScope.allScope(project));
        if (psiFiles.length <= 0) {
            Util.showPopupBalloon(mEditor, Constant.actions.selectedErrorNoSelected, popupTime);
            return;
        }
        XmlFile xmlFile = (XmlFile) psiFiles[0];
        List<Element> elements = new ArrayList<>();
        Util.getIDsFromLayout(xmlFile, elements);
        // 将代码写入文件，不允许在主线程中进行实时的文件写入
        if (elements.size() == 0) {
            Util.showPopupBalloon(mEditor, Constant.actions.selectedErrorNoId, popupTime);
            return;
        }
        // 判断是否有onCreate/onCreateView方法
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(mEditor, project);
        PsiClass psiClass = Util.getTargetClass(mEditor, psiFile);
        if (psiClass == null) {
            Util.showPopupBalloon(mEditor, Constant.actions.selectedErrorNoPoint, popupTime);
            return;
        }
        // 判断是否有onCreate方法
        if (Util.isExtendsActivityOrActivityCompat(project, psiClass)
                && psiClass.findMethodsByName(Constant.psiMethodByOnCreate, false).length == 0) {
            // 写onCreate方法
            new CreateMethodCreator(mEditor, psiFile, psiClass, Constant.creatorCommandName,
                    mSelectedText, Constant.classTypeByActivity, false).execute();
            return;
        }
        // 判断是否有onCreateView方法
        if (Util.isExtendsFragmentOrFragmentV4(project, psiClass)
                && psiClass.findMethodsByName(Constant.psiMethodByOnCreateView, false).length == 0) {
            new CreateMethodCreator(mEditor, psiFile, psiClass, Constant.creatorCommandName,
                    mSelectedText, Constant.classTypeByFragment, false).execute();
            return;
        }
        // 有的话就创建变量和findViewById
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancelDialog();
        }
        mDialog = new FindViewByIdDialog(new GenerateDialog.Builder(elements.size())
                .setEditor(mEditor)
                .setProject(project)
                .setPsiFile(psiFile)
                .setClass(psiClass)
                .setElements(elements)
                .setSelectedText(mSelectedText)
                .setIsButterKnife(false));
        mDialog.showDialog();
    }
}
