package Utils;

import View.FindViewByIdDialog;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction.Simple;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import entity.Element;
import org.apache.http.util.TextUtils;

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
    }

    @Override
    protected void run() throws Throwable {
        try {
            generateFields();
            generateFindViewById();
        } catch (Exception e) {
            // 异常打印
            mDialog.cancelDialog();
            Util.showPopupBalloon(mEditor, e.getMessage());
            return;
        }
        // 重写class
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        styleManager.optimizeImports(mFile);
        styleManager.shortenClassReferences(mClass);
        new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false).runWithoutProgress();
        Util.showPopupBalloon(mEditor, "生成成功");
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
        // 根据类名查找类
        PsiClass activityClass = JavaPsiFacade.getInstance(mProject).findClass("android.app.Activity", new EverythingGlobalScope(mProject));
        PsiClass activityCompatClass = JavaPsiFacade.getInstance(mProject).findClass("android.support.v7.app.AppCompatActivity", new EverythingGlobalScope(mProject));
        PsiClass fragmentClass = JavaPsiFacade.getInstance(mProject).findClass("android.app.Fragment", new EverythingGlobalScope(mProject));
        PsiClass fragmentV4Class = JavaPsiFacade.getInstance(mProject).findClass("android.support.v4.app.Fragment", new EverythingGlobalScope(mProject));
        // 判断mClass是不是继承activityClass或者activityCompatClass
        if ((activityClass != null && mClass.isInheritor(activityClass, true))
                || (activityCompatClass != null && mClass.isInheritor(activityCompatClass, true))
                || mClass.getName().contains("Activity")) {
            // 判断是否有onCreate方法
            if (mClass.findMethodsByName("onCreate", false).length == 0) {
                StringBuilder method = new StringBuilder();
                method.append("@Override protected void onCreate(android.os.Bundle savedInstanceState) {\n");
                method.append("super.onCreate(savedInstanceState);\n");
                method.append("\t// TODO:run FindViewById again To setValue in initView method\n");
                method.append("\tsetContentView(R.layout.");
                method.append(mSelectedText);
                method.append(");\n");
                method.append("}");
                // 添加
                mClass.add(mFactory.createMethodFromText(method.toString(), mClass));
            } else {
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

            // 判断mClass是不是继承fragmentClass或者fragmentV4Class
        } else if ((fragmentClass != null && mClass.isInheritor(fragmentClass, true))
                || (fragmentV4Class != null && mClass.isInheritor(fragmentV4Class, true))
                || mClass.getName().contains("Fragment")) {
            // 判断是否有onCreateView方法
            if (mClass.findMethodsByName("onCreateView", false).length == 0) {
                StringBuilder method = new StringBuilder();
                method.append("@Override public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, android.os.Bundle savedInstanceState) {\n");
                method.append("\t// TODO: run FindViewById again To setValue in initView method\n");
                method.append("\tView view = View.inflate(getActivity(), R.layout.");
                method.append(mSelectedText);
                method.append(", null);");
                method.append("return view;");
                method.append("}");
                // 添加
                mClass.add(mFactory.createMethodFromText(method.toString(), mClass));

            } else {
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
        if (initViewMethods.length > 0 && initViewMethods[0].getBody() != null) {
            PsiCodeBlock initViewMethodBody = initViewMethods[0].getBody();
            // 获取initView方法里面的每条findViewById
            PsiStatement[] statements = initViewMethodBody.getStatements();
            if (mIsLayoutInflater) {
                // 添加LayoutInflater.from(this).inflate(R.layout.activity_main, null);
                String layoutInflater = mLayoutInflaterText
                        + " = "
                        + "LayoutInflater.from(" + context + ").inflate(R.layout." + mSelectedText + ", null);";
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
                    boolean isExist = false;
                    String pre = TextUtils.isEmpty(findPre) ? "" : findPre + ".";
                    String inflater = "";
                    if (mIsLayoutInflater) {
                        inflater = mLayoutInflaterText.substring(1);
                        pre = mLayoutInflaterText + ".";
                    }
                    String s2 = element.getFieldName() + inflater
                            + " = (" + element.getName() + ") "
                            + pre + "findViewById(" + element.getFullID() + ");";
                    for (PsiStatement statement : statements) {
                        if (statement.getText().equals(s2)) {
                            isExist = true;
                            break;
                        } else {
                            isExist = false;
                        }
                    }
                    // 不存在就添加
                    if (!isExist) {
                        initViewMethodBody.add(mFactory.createStatementFromText(s2, initViewMethods[0]));
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
                        + " = "
                        + "LayoutInflater.from(" + context + ").inflate(R.layout." + mSelectedText + ", null);"
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
                }
            }
            initView.append("}\n");
            mClass.add(mFactory.createMethodFromText(initView.toString(), mClass));
        }

    }
}
