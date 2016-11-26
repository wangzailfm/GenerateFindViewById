package action;

import Utils.Util;
import View.FindViewByIdDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import entity.Element;
import org.apache.http.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class FindViewByIdAction extends AnAction {
    private FindViewByIdDialog mDialog;
    private String mSelectedText;

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 获取project
        Project project = e.getProject();
        // 获取选中内容
        final Editor mEditor = e.getData(PlatformDataKeys.EDITOR);
        if (null == mEditor) {
            return;
        }
        SelectionModel model = mEditor.getSelectionModel();
        mSelectedText = model.getSelectedText();
        // 未选中布局内容，显示dialog
        if (TextUtils.isEmpty(mSelectedText)) {
            mSelectedText = Messages.showInputDialog(project, "布局内容：（不需要输入R.layout.）", "未选中布局内容，请输入layout文件名", Messages.getInformationIcon());
            if (TextUtils.isEmpty(mSelectedText)) {
                Util.showPopupBalloon(mEditor, "未输入layout文件名");
                return;
            }
        }
        // 获取布局文件，通过FilenameIndex.getFilesByName获取
        // GlobalSearchScope.allScope(project)搜索整个项目
        PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, mSelectedText + ".xml", GlobalSearchScope.allScope(project));
        if (psiFiles.length <= 0) {
            Util.showPopupBalloon(mEditor, "未找到选中的布局文件");
            return;
        }
        XmlFile xmlFile = (XmlFile) psiFiles[0];
        List<Element> elements = new ArrayList<>();
        Util.getIDsFromLayout(xmlFile, elements);
        // 将代码写入文件，不允许在主线程中进行实时的文件写入
        if (elements.size() != 0) {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.cancelDialog();
            }
            mDialog = new FindViewByIdDialog(mEditor, project, elements, mSelectedText);
            mDialog.showDialog();
        } else {
            Util.showPopupBalloon(mEditor, "未找到任何Id");
        }
    }
}
