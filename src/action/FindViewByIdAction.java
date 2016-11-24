package action;

import Utils.Utils;
import Utils.IdCreator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.xml.XmlFile;
import entity.Element;
import org.apache.http.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class FindViewByIdAction extends AnAction {

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
        String selectedText = model.getSelectedText();
        if (TextUtils.isEmpty(selectedText)) {
            // 未选中布局内容，显示dialog
            selectedText = Messages.showInputDialog(project, "layout（不需要输入R.layout.）：" , "未选中布局内容，请输入layout文件名", Messages.getInformationIcon());
            if (TextUtils.isEmpty(selectedText)) {
                Utils.showPopupBalloon(mEditor, "未输入layout文件名");
                return;
            }
        }
        // 获取布局文件，通过FilenameIndex.getFilesByName获取
        // GlobalSearchScope.allScope(project)搜索整个项目
        PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, selectedText + ".xml", GlobalSearchScope.allScope(project));
        if (psiFiles.length <= 0) {
            Utils.showPopupBalloon(mEditor, "未找到选中的布局文件");
            return;
        }
        XmlFile xmlFile = (XmlFile) psiFiles[0];
        List<Element> elements = new ArrayList<>();
        Utils.getIDsFromLayout(xmlFile, elements);
        // 将代码写入文件，不允许在主线程中进行实时的文件写入
        if (elements != null && elements.size() != 0) {
            //获取当前文件
            PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(mEditor, project);
            new IdCreator(psiFile, getTargetClass(mEditor, psiFile), "Generate Injections", elements, selectedText).execute();
            Utils.showPopupBalloon(mEditor, "生成成功");
        } else {
            Utils.showPopupBalloon(mEditor, "未找到任何Id");
        }
    }


    /**
     * 根据当前文件获取对应的class文件
     * @param editor
     * @param file
     * @return
     */
    protected PsiClass getTargetClass(Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if(element == null) {
            return null;
        } else {
            PsiClass target = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            return target instanceof SyntheticElement ?null:target;
        }
    }
}
