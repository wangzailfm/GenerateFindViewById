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

import java.util.ArrayList;
import java.util.List;

public class WidgetFieldCreator extends Simple {

    private FindViewByIdDialog mDialog;
    private Editor mEditor;
    private PsiFile mFile;
    private Project mProject;
    private PsiClass mClass;
    private List<Element> mElements;
    private PsiElementFactory mFactory;
    private String mSelectedText;
    private boolean mIsLayoutInflater;
    private String mLayoutInflaterText;
    private List<Element> mOnClickList = new ArrayList<>();

    public WidgetFieldCreator(FindViewByIdDialog dialog, Editor editor, PsiFile psiFile, PsiClass psiClass, String command, List<Element> elements, String selectedText, boolean isLayoutInflater, String text) {
        super(psiClass.getProject(), command);
        mDialog = dialog;
        mEditor = editor;
        mFile = psiFile;
        mProject = psiClass.getProject();
        mClass = psiClass;
        mElements = elements;
        // 获取Factory
        mFactory = JavaPsiFacade.getElementFactory(mProject);
        mSelectedText = selectedText;
        mIsLayoutInflater = isLayoutInflater;
        mLayoutInflaterText = text;
        for (Element mElement : mElements) {
            if (mElement.isEnable() && mElement.isClickEnable() && mElement.isClickable()) {
                mOnClickList.add(mElement);
            }
        }
    }

    @Override
    protected void run() throws Throwable {
        try {
            generateFindViewById();
        } catch (Exception e) {
            // 异常打印
            mDialog.cancelDialog();
            Util.showPopupBalloon(mEditor, e.getMessage(), 10);
            return;
        }
        // 重写class
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        styleManager.optimizeImports(mFile);
        styleManager.shortenClassReferences(mClass);
        new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false).runWithoutProgress();
        Util.showPopupBalloon(mEditor, "生成成功", 5);
    }

    /**
     * 创建变量
     */
    private void generateFields() {
        if (mIsLayoutInflater) {
            String inflater = "private " + "View " + mLayoutInflaterText + ";";
            // 已存在的变量就不创建
            boolean duplicateField = false;
            for (PsiField field : mClass.getFields()) {
                String name = field.getName();
                if (name != null && name.equals(mLayoutInflaterText)) {
                    duplicateField = true;
                    break;
                }
            }
            if (!duplicateField) {
                mClass.add(mFactory.createFieldFromText(inflater, mClass));
            }
        }
        for (Element element : mElements) {
            // 已存在的变量就不创建
            PsiField[] fields = mClass.getFields();
            boolean duplicateField = false;
            for (PsiField field : fields) {
                String name = field.getName();
                if (!mIsLayoutInflater) {
                    if (name != null && name.equals(element.getFieldName())) {
                        duplicateField = true;
                        break;
                    }
                } else {
                    if (name != null && name.equals(element.getFieldName()
                            + mLayoutInflaterText.substring(1))) {
                        duplicateField = true;
                        break;
                    }
                }
            }
            // 已存在跳出
            if (duplicateField) {
                continue;
            }
            // 设置变量名，获取text里面的内容
            String text = element.getXml().getAttributeValue("android:text");
            if (TextUtils.isEmpty(text)) {
                // 如果是text为空，则获取hint里面的内容
                text = element.getXml().getAttributeValue("android:hint");
            }
            // 如果是@string/app_name类似
            if (!TextUtils.isEmpty(text) && text.contains("@string/")) {
                text = text.replace("@string/", "");
                // 获取strings.xml
                PsiFile[] psiFiles = FilenameIndex.getFilesByName(mProject, "strings.xml", GlobalSearchScope.allScope(mProject));
                if (psiFiles.length > 0) {
                    for (PsiFile psiFile : psiFiles) {
                        // 获取src\main\res\values下面的strings.xml文件
                        String dirName = psiFile.getParent().toString();
                        if (dirName.contains("src\\main\\res\\values")) {
                            text = Util.getTextFromStringsXml(psiFile, text);
                        }
                    }
                }
            }

            StringBuilder fromText = new StringBuilder();
            if (!TextUtils.isEmpty(text)) {
                 fromText.append("/** " + text + " */\n");
            }
            fromText.append("private ");
            fromText.append(element.getName());
            fromText.append(" ");
            fromText.append(element.getFieldName());
            if (mIsLayoutInflater) fromText.append(mLayoutInflaterText.substring(1));
            fromText.append(";");
            if (element.isEnable()) {
                // 添加到class
                mClass.add(mFactory.createFieldFromText(fromText.toString(), mClass));
            }
        }
    }

    /**
     * 设置变量的值FindViewById，Activity和Fragment
     */
    private void generateFindViewById() {
        if (Util.isExtendsActivityOrActivityCompat(mProject, mClass)) {
            // 判断是否有onCreate方法
            if (mClass.findMethodsByName("onCreate", false).length == 0) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateMethod(mSelectedText), mClass));
            } else {
                generateFields();
                // 获取setContentView
                PsiStatement setContentViewStatement = null;
                // onCreate是否存在initView方法
                boolean hasInitViewStatement = false;

                PsiMethod onCreate = mClass.findMethodsByName("onCreate", false)[0];
                for (PsiStatement psiStatement : onCreate.getBody().getStatements()) {
                    // 查找setContentView
                    if (psiStatement.getFirstChild() instanceof PsiMethodCallExpression) {
                        PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) psiStatement.getFirstChild()).getMethodExpression();
                        if (methodExpression.getText().equals("setContentView")) {
                            setContentViewStatement = psiStatement;
                        } else if (methodExpression.getText().equals("initView")) {
                            hasInitViewStatement = true;
                        }
                    }
                }

                if (!hasInitViewStatement && setContentViewStatement != null) {
                    // 将initView()写到setContentView()后面
                    onCreate.getBody().addAfter(mFactory.createStatementFromText("initView();", mClass), setContentViewStatement);
                }

                generatorLayoutCode(null, "getApplicationContext()");
            }

        } else if (Util.isExtendsFragmentOrFragmentV4(mProject, mClass)) {
            // 判断是否有onCreateView方法
            if (mClass.findMethodsByName("onCreateView", false).length == 0) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateViewMethod(mSelectedText), mClass));

            } else {
                generateFields();
                // 查找onCreateView
                PsiReturnStatement returnStatement = null;
                // view
                String returnValue = null;
                // onCreateView是否存在initView方法
                boolean hasInitViewStatement = false;

                PsiMethod onCreate = mClass.findMethodsByName("onCreateView", false)[0];
                for (PsiStatement psiStatement : onCreate.getBody().getStatements()) {
                    if (psiStatement instanceof PsiReturnStatement) {
                        // 获取view的值
                        returnStatement = (PsiReturnStatement) psiStatement;
                        returnValue = returnStatement.getReturnValue().getText();
                    } else if (psiStatement.getFirstChild() instanceof PsiMethodCallExpression) {
                        PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) psiStatement.getFirstChild()).getMethodExpression();
                        if (methodExpression.getText().equals("initView")) {
                            hasInitViewStatement = true;
                        }
                    }
                }

                if (!hasInitViewStatement && returnStatement != null && returnValue != null) {
                    onCreate.getBody().addBefore(mFactory.createStatementFromText("initView(" + returnValue + ");", mClass), returnStatement);
                }
                generatorLayoutCode(returnValue, "getActivity()");
            }
        }
    }

    /**
     * 写initView方法
     *
     * @param findPre Fragment的话要view.findViewById
     * @param context
     */
    private void generatorLayoutCode(String findPre, String context) {
        // 判断是否已有initView方法
        PsiMethod[] initViewMethods = mClass.findMethodsByName("initView", false);
        // 有initView方法
        if (initViewMethods.length > 0 && initViewMethods[0].getBody() != null) {
            PsiCodeBlock initViewMethodBody = initViewMethods[0].getBody();
            // 获取initView方法里面的每条内容
            PsiStatement[] statements = initViewMethodBody.getStatements();
            if (mIsLayoutInflater) {
                // 添加LayoutInflater.from(this).inflate(R.layout.activity_main, null);
                String layoutInflater = mLayoutInflaterText
                        + " = LayoutInflater.from(" + context + ").inflate(R.layout." + mSelectedText + ", null);";
                // 判断是否存在
                boolean isExist = false;
                for (PsiStatement statement : statements) {
                    if (statement.getText().equals(layoutInflater)) {
                        isExist = true;
                        break;
                    } else {
                        isExist = false;
                    }
                }
                // 不存在才添加
                if (!isExist) {
                    initViewMethodBody.add(mFactory.createStatementFromText(layoutInflater, initViewMethods[0]));
                }
            }
            for (Element element : mElements) {
                if (element.isEnable()) {
                    // 判断是否已存在findViewById
                    boolean isFdExist = false;
                    String pre = TextUtils.isEmpty(findPre) ? "" : findPre + ".";
                    String inflater = "";
                    if (mIsLayoutInflater) {
                        inflater = mLayoutInflaterText.substring(1);
                        pre = mLayoutInflaterText + ".";
                    }
                    String findViewById = element.getFieldName() + inflater
                            + " = (" + element.getName() + ") "
                            + pre + "findViewById(" + element.getFullID() + ");";
                    for (PsiStatement statement : statements) {
                        if (statement.getText().equals(findViewById)) {
                            isFdExist = true;
                            break;
                        } else {
                            isFdExist = false;
                        }
                    }
                    // 不存在就添加
                    if (!isFdExist) {
                        initViewMethodBody.add(mFactory.createStatementFromText(findViewById, initViewMethods[0]));
                    }
                    if (element.isClickEnable()) {
                        // 判断是否已存在setOnClickListener
                        boolean isClickExist = false;
                        String setOnClickListener = element.getFieldName()+ inflater + ".setOnClickListener(this);";
                        for (PsiStatement statement : statements) {
                            if (statement.getText().equals(setOnClickListener)) {
                                isClickExist = true;
                                break;
                            } else {
                                isClickExist = false;
                            }
                        }
                        if (!isClickExist && element.isClickable()) {
                            initViewMethodBody.add(mFactory.createStatementFromText(setOnClickListener, initViewMethods[0]));
                        }
                    }
                }
            }
        } else {
            StringBuilder initView = new StringBuilder();
            if (TextUtils.isEmpty(findPre)) {
                initView.append("private void initView() {\n");
            } else {
                initView.append("private void initView(View " + findPre + ") {\n");
            }
            if (mIsLayoutInflater) {
                // 添加LayoutInflater.from(this).inflate(R.layout.activity_main, null);
                String layoutInflater = mLayoutInflaterText
                        + " = LayoutInflater.from(" + context + ").inflate(R.layout." + mSelectedText + ", null);"
                        + "\n";
                initView.append(layoutInflater);
            }

            for (Element element : mElements) {
                if (element.isEnable()) {
                    String pre = TextUtils.isEmpty(findPre) ? "" : findPre + ".";
                    String inflater = "";
                    if (mIsLayoutInflater) {
                        inflater = mLayoutInflaterText.substring(1);
                        pre = mLayoutInflaterText + ".";
                    }
                    initView.append(element.getFieldName() + inflater
                            + " = (" + element.getName() + ")"
                            + pre + "findViewById(" + element.getFullID() + ");\n");
                    if (element.isClickable() && element.isClickEnable()) {
                        initView.append(element.getFieldName() + inflater + ".setOnClickListener(this);\n");
                    }
                }
            }
            initView.append("}\n");
            mClass.add(mFactory.createMethodFromText(initView.toString(), mClass));
        }
        if (mOnClickList.size() != 0) {
            generateOnClickCode();
        }
    }

    /**
     * 添加实现OnClickListener接口
     */
    private void generateOnClickCode() {
        // 获取已实现的接口
        PsiReferenceList implementsList = mClass.getImplementsList();
        boolean isImplOnClick = false;
        if (implementsList != null) {
            // 获取列表
            PsiJavaCodeReferenceElement[] referenceElements = implementsList.getReferenceElements();
            // 是否实现了OnClickListener接口
            isImplOnClick = Util.isImplementsOnClickListener(referenceElements);
        }
        // 未实现添加OnClickListener接口
        if (!isImplOnClick) {
            PsiJavaCodeReferenceElement referenceElementByFQClassName = mFactory.createReferenceElementByFQClassName("android.view.View.OnClickListener", mClass.getResolveScope());
            // 添加的PsiReferenceList
            implementsList.add(referenceElementByFQClassName);
        }
        // 判断是否已有onClick方法
        PsiMethod[] onClickMethods = mClass.findMethodsByName("onClick", false);
        // 已有onClick方法
        if (onClickMethods.length > 0 && onClickMethods[0].getBody() != null) {
            PsiCodeBlock onClickMethodBody = onClickMethods[0].getBody();
            // 获取switch
            for (PsiElement psiElement : onClickMethodBody.getChildren()) {
                if (psiElement instanceof PsiSwitchStatement) {
                    PsiSwitchStatement psiSwitchStatement = (PsiSwitchStatement) psiElement;
                    // 获取switch的内容
                    PsiCodeBlock psiSwitchStatementBody = psiSwitchStatement.getBody();
                    if(psiSwitchStatementBody != null) {
                        for (Element element : mOnClickList) {
                            String cass = "case " + element.getFullID() + ":";
                            // 判断是否存在
                            boolean isExist = false;
                            for (PsiStatement statement : psiSwitchStatementBody.getStatements()) {
                                if (statement.getText().replace("\n","").replace("break;","").equals(cass)) {
                                    isExist = true;
                                    break;
                                } else {
                                    isExist = false;
                                }
                            }
                            // 不存在就添加
                            if (!isExist) {
                                psiSwitchStatementBody.add(mFactory.createStatementFromText(cass, psiSwitchStatementBody));
                                psiSwitchStatementBody.add(mFactory.createStatementFromText("break;", psiSwitchStatementBody));
                            }
                        }
                    }
                }
            }
        } else {
            if (mOnClickList.size() != 0) {
                StringBuilder onClick = new StringBuilder();
                onClick.append("@Override public void onClick(View v) {\n");
                onClick.append("switch (v.getId()) {\n");
                for (Element mElement : mOnClickList) {
                    if (mElement.isClickable()) {
                        onClick.append("case " + mElement.getFullID() + ":\nbreak;\n");
                    }
                }
                onClick.append("}\n");
                onClick.append("}\n");
                mClass.add(mFactory.createMethodFromText(onClick.toString(), mClass));
            }
        }
    }
}
