package utils;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction.Simple;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import constant.Constant;

public class CreateMethodCreator extends Simple {

    private Editor mEditor;
    private PsiFile mFile;
    private Project mProject;
    private PsiClass mClass;
    private PsiElementFactory mFactory;
    private String mSelectedText;
    // activity/fragment
    private String mType;
    private boolean mIsButterKnife;

    public CreateMethodCreator(Editor editor, PsiFile psiFile, PsiClass psiClass, String command, String selectedText, String type, boolean isButterKnife) {
        super(psiClass.getProject(), command);
        mEditor = editor;
        mFile = psiFile;
        mProject = psiClass.getProject();
        mClass = psiClass;
        // 获取Factory
        mFactory = JavaPsiFacade.getElementFactory(mProject);
        mSelectedText = selectedText;
        mType = type;
        mIsButterKnife = isButterKnife;
    }

    @Override
    protected void run() throws Throwable {
        try {
            createMethod();
        } catch (Exception e) {
            Util.showPopupBalloon(mEditor, e.getMessage(), 10);
            return;
        }
        // 重写class
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        styleManager.optimizeImports(mFile);
        styleManager.shortenClassReferences(mClass);
        new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false).runWithoutProgress();
        String actionName = Constant.actionFindViewById;
        if (mIsButterKnife) {
            actionName = Constant.actionButterKnife;
        }
        if (mType.equals(Constant.classTypeByActivity)) {
            Util.showPopupBalloon(mEditor, Constant.utils.creatorNoOnCreateMethod + actionName, 10);
        } else if (mType.equals(Constant.classTypeByFragment)) {
            Util.showPopupBalloon(mEditor, Constant.utils.creatorNoOnCreateViewMethod + actionName, 10);
        }
    }

    /**
     * 设置Activity的onCreate方法和Fragment的onCreateView方法
     */
    private void createMethod() {
        if (Util.isExtendsActivityOrActivityCompat(mProject, mClass)) {
            // 判断是否有onCreate方法
            if (mClass.findMethodsByName(Constant.psiMethodByOnCreate, false).length == 0) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateMethod(mSelectedText, mIsButterKnife), mClass));
                if (!mIsButterKnife && mClass.findMethodsByName(Constant.utils.creatorInitViewName, false).length == 0) {
                    mClass.add(mFactory.createMethodFromText(Util.createInitViewMethod(), mClass));
                }
            }

        } else if (Util.isExtendsFragmentOrFragmentV4(mProject, mClass)) {
            boolean isViewExist = false;
            boolean isUnbinderExist = false;
            for (PsiField psiField : mClass.getFields()) {
                if (psiField.getName() != null && psiField.getName().equals(Constant.utils.creatorViewName)) {
                    isViewExist = true;
                }
                if (psiField.getName() != null && psiField.getName().equals(Constant.utils.creatorUnbinderName)) {
                    isUnbinderExist = true;
                }
            }
            if (!isViewExist) {
                mClass.add(mFactory.createFieldFromText("private View view;", mClass));
            }
            if (mIsButterKnife && !isUnbinderExist) {
                mClass.add(mFactory.createFieldFromText("private Unbinder unbinder;", mClass));
            }
            // 判断是否有onCreate方法
            if (mClass.findMethodsByName(Constant.psiMethodByOnCreate, false).length == 0) {
                mClass.add(mFactory.createMethodFromText(Util.createFragmentOnCreateMethod(mSelectedText), mClass));
            } else {
                // onCreate是否存在View.inflate方法
                boolean hasInflateStatement = false;
                PsiMethod onCreate = mClass.findMethodsByName(Constant.psiMethodByOnCreate, false)[0];
                if (onCreate.getBody() != null) {
                    for (PsiStatement psiStatement : onCreate.getBody().getStatements()) {
                        if (psiStatement.getText().contains("View.inflate(getActivity(), R.layout." + mSelectedText + ", null);")) {
                            hasInflateStatement = true;
                            break;
                        } else {
                            hasInflateStatement = false;
                        }
                    }
                    if (!hasInflateStatement) {
                        onCreate.getBody().add(mFactory.createStatementFromText("view = View.inflate(getActivity(), R.layout." + mSelectedText + ", null);", mClass));
                    }
                }
            }
            // 判断是否有onCreateView方法
            if (mClass.findMethodsByName(Constant.psiMethodByOnCreateView, false).length == 0) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateViewMethod(mSelectedText, mIsButterKnife), mClass));
                if (!mIsButterKnife && mClass.findMethodsByName(Constant.utils.creatorInitViewName, false).length == 0) {
                    mClass.add(mFactory.createMethodFromText(Util.createFragmentInitViewMethod(), mClass));
                }
            }
            // ButterKnife判断是否有onDestroyView方法
            if (mIsButterKnife) {
                if (mClass.findMethodsByName(Constant.utils.creatorOnDestroyViewMethod, false).length == 0) {
                    mClass.add(mFactory.createMethodFromText(Util.createOnDestroyViewMethod(), mClass));
                } else {
                    // onDestroyView是否存在unbinder.unbind();
                    boolean hasUnbinderStatement = false;
                    PsiMethod onCreate = mClass.findMethodsByName(Constant.utils.creatorOnDestroyViewMethod, false)[0];
                    if (onCreate.getBody() != null) {
                        for (PsiStatement psiStatement : onCreate.getBody().getStatements()) {
                            if (psiStatement.getText().contains(Constant.utils.creatorUnbinderField)) {
                                hasUnbinderStatement = true;
                                break;
                            } else {
                                hasUnbinderStatement = false;
                            }
                        }
                        if (!hasUnbinderStatement) {
                            onCreate.getBody().add(mFactory.createStatementFromText(Constant.utils.creatorUnbinderField, mClass));
                        }
                    }
                }
            }

        }
    }
}
