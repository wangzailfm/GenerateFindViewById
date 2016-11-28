package Utils;

import View.FindViewByIdDialog;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction.Simple;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import entity.Element;
import org.apache.http.util.TextUtils;

import java.util.List;

public class CreateMethodCreator extends Simple {

    private Editor mEditor;
    private PsiFile mFile;
    private Project mProject;
    private PsiClass mClass;
    private PsiElementFactory mFactory;
    private String mSelectedText;
    // activity/fragment
    private String mType;

    public CreateMethodCreator(Editor editor, PsiFile psiFile, PsiClass psiClass, String command, String selectedText, String type) {
        super(psiClass.getProject(), command);
        mEditor = editor;
        mFile = psiFile;
        mProject = psiClass.getProject();
        mClass = psiClass;
        // 获取Factory
        mFactory = JavaPsiFacade.getElementFactory(mProject);
        mSelectedText = selectedText;
        mType = type;
    }

    @Override
    protected void run() throws Throwable {
        try {
            createMethod(mType);
        } catch (Exception e) {
            Util.showPopupBalloon(mEditor, e.getMessage());
            return;
        }
        // 重写class
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        styleManager.optimizeImports(mFile);
        styleManager.shortenClassReferences(mClass);
        new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false).runWithoutProgress();
        if (mType.equals("activity")) {
            Util.showPopupBalloon(mEditor, "没有OnCreate方法，已创建OnCreate方法，请重新使用FindViewById");
        } else if (mType.equals("fragment")) {
            Util.showPopupBalloon(mEditor, "没有OnCreateView方法，已创建OnCreate方法，请重新使用FindViewById");
        }
    }

    /**
     * 设置Activity的onCreate方法和Fragment的onCreateView方法
     * @param mType activity/fragment
     */
    private void createMethod(String mType) {
        if (Util.isExtendsActivityOrActivityCompat(mProject, mClass)) {
            // 判断是否有onCreate方法
            if (mClass.findMethodsByName("onCreate", false).length == 0) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateMethod(mSelectedText), mClass));
            }

        } else if (Util.isExtendsFragmentOrFragmentV4(mProject, mClass)) {
            // 判断是否有onCreateView方法
            if (mClass.findMethodsByName("onCreateView", false).length == 0) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateViewMethod(mSelectedText), mClass));

            }
        }
    }
}
