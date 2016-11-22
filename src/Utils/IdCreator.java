package Utils;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.WriteCommandAction.Simple;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.EverythingGlobalScope;
import entity.Element;
import org.apache.http.util.TextUtils;

import java.util.List;

/**
 * Created by pc on 2016/11/22.
 */
public class IdCreator extends Simple {

    private PsiFile mFile;
    private Project mProject;
    private PsiClass mClass;
    private List<Element> mElements;
    private PsiElementFactory mFactory;
    private String mSelectText;

    public IdCreator(PsiFile psiFile, PsiClass psiClass, String command, List<Element> elements, String selectText) {
        super(psiClass.getProject(), command);

        mFile = psiFile;
        mProject = psiClass.getProject();
        mClass = psiClass;
        mElements = elements;
        // 获取Factory
        mFactory = JavaPsiFacade.getElementFactory(mProject);
        mSelectText = selectText;
    }

    @Override
    protected void run() throws Throwable {
        generateFields();
        generateFindViewById();
        // 重写class
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        styleManager.optimizeImports(mFile);
        styleManager.shortenClassReferences(mClass);
        new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false).runWithoutProgress();
    }

    /**
     * 创建变量
     */
    private void generateFields() {
        for (Element element : mElements) {

            // remove duplicate field
            PsiField[] fields = mClass.getFields();
            boolean duplicateField = false;
            for (PsiField field : fields) {
                String name = field.getName();
                if (name != null && name.equals(element.getFieldName())) {
                    duplicateField = true;
                    break;
                }
            }

            if (duplicateField) {
                continue;
            }
            // 设置变量
            String text = element.xml.getAttributeValue("android:text");
            // text
            String fromText = "private " + element.name + " " + element.getFieldName() + ";";
            if (!TextUtils.isEmpty(text)) {
                fromText = "/** " + text + " */\n" + fromText;
            }
            // 添加到class
            mClass.add(mFactory.createFieldFromText(fromText, mClass));
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
                method.append(mSelectText);
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

                generatorLayoutCode(null);
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
                method.append(mSelectText);
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
                generatorLayoutCode(returnValue);
            }
        }
    }

    /**
     * 写initView方法
     *
     * @param findPre Fragment的话要view.findViewById
     */
    private void generatorLayoutCode(String findPre) {
        // 判断是否已有initView方法
        PsiMethod[] initViewMethods = mClass.findMethodsByName("initView", false);
        if (initViewMethods.length > 0 && initViewMethods[0].getBody() != null) {
            PsiCodeBlock initViewMethodBody = initViewMethods[0].getBody();
            for (Element element : mElements) {
                String pre = TextUtils.isEmpty(findPre) ? "" : findPre + ".";
                String s2 = element.getFieldName() + " = (" + element.name + ") " + pre + "findViewById(" + element.getFullID() + ");";
                initViewMethodBody.add(mFactory.createStatementFromText(s2, initViewMethods[0]));
            }
        } else {
            StringBuilder initView = new StringBuilder();
            if (TextUtils.isEmpty(findPre)) {
                initView.append("private void initView() {\n");
            } else {
                initView.append("private void initView(View " + findPre + ") {\n");
            }

            for (Element element : mElements) {
                String pre = TextUtils.isEmpty(findPre) ? "" : findPre + ".";
                initView.append(element.getFieldName() + " = (" + element.name + ")" + pre + "findViewById(" + element.getFullID() + ");\n");
            }
            initView.append("}\n");
            mClass.add(mFactory.createMethodFromText(initView.toString(), mClass));
        }

    }
}
