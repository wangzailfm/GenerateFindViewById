package utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.*;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.JBColor;
import constant.Constant;
import entitys.Element;
import org.apache.http.util.TextUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Util {
    /**
     * 通过strings.xml获取的值
      */
    private static String StringValue;

    /**
     * 显示dialog
     *
     * @param editor
     * @param result 内容
     * @param time   显示时间，单位秒
     */
    public static void showPopupBalloon(final Editor editor, final String result, final int time) {
        ApplicationManager.getApplication().invokeLater(() -> {
            JBPopupFactory factory = JBPopupFactory.getInstance();
            factory.createHtmlTextBalloonBuilder(result, null, new JBColor(new Color(116, 214, 238), new Color(76, 112, 117)), null)
                    .setFadeoutTime(time * 1000)
                    .createBalloon()
                    .show(factory.guessBestPopupLocation(editor), Balloon.Position.below);
        });
    }

    /**
     * 驼峰
     *
     * @param fieldName
     * @return
     */
    public static String getFieldName(String fieldName) {
        if (!TextUtils.isEmpty(fieldName)) {
            String[] names = fieldName.split("_");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.length; i++) {
                sb.append(firstToUpperCase(names[i]));
            }
            fieldName = sb.toString();
        }
        return fieldName;
    }

    /**
     * 第一个字母大写
     *
     * @param key
     * @return
     */
    public static String firstToUpperCase(String key) {
        return key.substring(0, 1).toUpperCase(Locale.CHINA) + key.substring(1);
    }

    /**
     * 解析xml获取string的值
     *
     * @param psiFile
     * @param text
     * @return
     */
    public static String getTextFromStringsXml(PsiFile psiFile, String text) {
        psiFile.accept(new XmlRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                super.visitElement(element);
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    if (tag.getName().equals("string")
                            && tag.getAttributeValue("name").equals(text)) {
                        PsiElement[] children = tag.getChildren();
                        String value = "";
                        for (PsiElement child : children) {
                            value += child.getText();
                        }
                        // value = <string name="app_name">My Application</string>
                        // 用正则获取值
                        Pattern p = Pattern.compile("<string name=\"" + text + "\">(.*)</string>");
                        Matcher m = p.matcher(value);
                        while (m.find()) {
                            StringValue = m.group(1);
                        }
                    }
                }
            }
        });
        return StringValue;
    }

    /**
     * 获取所有id
     *
     * @param file
     * @param elements
     * @return
     */
    public static java.util.List<Element> getIDsFromLayout(final PsiFile file, final java.util.List<Element> elements) {
        // To iterate over the elements in a file
        // 遍历一个文件的所有元素
        file.accept(new XmlRecursiveElementVisitor() {

            @Override
            public void visitElement(PsiElement element) {
                super.visitElement(element);
                // 解析Xml标签
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    // 获取Tag的名字（TextView）或者自定义
                    String name = tag.getName();
                    // 如果有include
                    if (name.equalsIgnoreCase("include")) {
                        // 获取布局
                        XmlAttribute layout = tag.getAttribute("layout", null);
                        // 获取project
                        Project project = file.getProject();
                        // 布局文件
                        XmlFile include = null;
                        PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, getLayoutName(layout.getValue()) + Constant.selectedTextSUFFIX, GlobalSearchScope.allScope(project));
                        if (psiFiles.length > 0) {
                            include = (XmlFile) psiFiles[0];
                        }
                        if (include != null) {
                            // 递归
                            getIDsFromLayout(include, elements);
                            return;
                        }
                    }
                    // 获取id字段属性
                    XmlAttribute id = tag.getAttribute("android:id", null);
                    if (id == null) {
                        return;
                    }
                    // 获取id的值
                    String idValue = id.getValue();
                    if (idValue == null) {
                        return;
                    }
                    XmlAttribute aClass = tag.getAttribute("class", null);
                    if (aClass != null) {
                        name = aClass.getValue();
                    }
                    // 获取clickable
                    XmlAttribute clickableAttr = tag.getAttribute("android:clickable", null);
                    boolean clickable = false;
                    if (clickableAttr != null && !TextUtils.isEmpty(clickableAttr.getValue())) {
                        clickable = clickableAttr.getValue().equals("true");
                    }
                    // 添加到list
                    try {
                        Element e = new Element(name, idValue, clickable, tag);
                        elements.add(e);
                    } catch (IllegalArgumentException e) {

                    }
                }
            }
        });


        return elements;
    }

    /**
     * layout.getValue()返回的值为@layout/layout_view
     *
     * @param layout
     * @return
     */
    public static String getLayoutName(String layout) {
        if (layout == null || !layout.startsWith("@") || !layout.contains("/")) {
            return null;
        }

        // @layout layout_view
        String[] parts = layout.split("/");
        if (parts.length != 2) {
            return null;
        }
        // layout_view
        return parts[1];
    }

    /**
     * 根据当前文件获取对应的class文件
     *
     * @param editor
     * @param file
     * @return
     */
    public static PsiClass getTargetClass(Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        } else {
            PsiClass target = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            return target instanceof SyntheticElement ? null : target;
        }
    }

    /**
     * 判断mClass是不是继承activityClass或者activityCompatClass
     *
     * @param mProject
     * @param mClass
     * @return
     */
    public static boolean isExtendsActivityOrActivityCompat(Project mProject, PsiClass mClass) {
        // 根据类名查找类
        PsiClass activityClass = JavaPsiFacade.getInstance(mProject).findClass("android.app.Activity", new EverythingGlobalScope(mProject));
        PsiClass activityCompatClass = JavaPsiFacade.getInstance(mProject).findClass("android.support.v7.app.AppCompatActivity", new EverythingGlobalScope(mProject));
        return (activityClass != null && mClass.isInheritor(activityClass, true))
                || (activityCompatClass != null && mClass.isInheritor(activityCompatClass, true));
    }

    /**
     * 判断mClass是不是继承fragmentClass或者fragmentV4Class
     *
     * @param mProject
     * @param mClass
     * @return
     */
    public static boolean isExtendsFragmentOrFragmentV4(Project mProject, PsiClass mClass) {
        // 根据类名查找类
        PsiClass fragmentClass = JavaPsiFacade.getInstance(mProject).findClass("android.app.Fragment", new EverythingGlobalScope(mProject));
        PsiClass fragmentV4Class = JavaPsiFacade.getInstance(mProject).findClass("android.support.v4.app.Fragment", new EverythingGlobalScope(mProject));
        return (fragmentClass != null && mClass.isInheritor(fragmentClass, true))
                || (fragmentV4Class != null && mClass.isInheritor(fragmentV4Class, true));
    }

    /**
     * 判断是否存在ButterKnife.bind(this)/ButterKnife.bind(this, view)
     * @param mClass
     * @return
     */
    public static boolean isButterKnifeBindExist(PsiClass mClass){
        PsiMethod onCreateMethod = getPsiMethodByName(mClass, Constant.psiMethodByOnCreate);
        PsiMethod onCreateViewMethod = getPsiMethodByName(mClass, Constant.psiMethodByOnCreateView);
        if (onCreateMethod != null && onCreateMethod.getBody() != null
                && onCreateMethod.getBody().getText().contains(Constant.utils.fieldButterKnifeBind)
                || (onCreateViewMethod != null && onCreateViewMethod.getBody() != null
                        && onCreateViewMethod.getBody().getText().contains(Constant.utils.fieldButterKnifeBind))) {
            return false;
        }
        return true;
    }

    /**
     * 创建onCreate方法
     *
     * @param mSelectedText
     * @param mIsButterKnife
     * @return
     */
    public static String createOnCreateMethod(String mSelectedText, boolean mIsButterKnife) {
        StringBuilder method = new StringBuilder();
        String viewStr = "\tsetContentView(R.layout." + mSelectedText + ");\n";
        method.append("@Override protected void onCreate(android.os.Bundle savedInstanceState) {\n");
        method.append("super.onCreate(savedInstanceState);\n");
        method.append("\t// TODO:OnCreate Method has been created, run ");
        if (!mIsButterKnife) {
            method.append("FindViewById");
            method.append(" again to generate code\n");
            method.append(viewStr);
            method.append("\t\tinitView();\n");
        } else {
            method.append("ButterKnife");
            method.append(" again to generate code\n");
            method.append(viewStr);
            method.append("\t\tButterKnife.bind(this);\n");
        }
        ;
        method.append("}");
        return method.toString();
    }

    /**
     * 创建onCreate方法(Fragment)
     *
     * @param mSelectedText
     * @return
     */
    public static String createFragmentOnCreateMethod(String mSelectedText) {
        StringBuilder method = new StringBuilder();
        method.append("@Override public void onCreate(@Nullable android.os.Bundle savedInstanceState) {\n");
        method.append("super.onCreate(savedInstanceState);\n");
        method.append("\tview = View.inflate(getActivity(), R.layout.");
        method.append(mSelectedText);
        method.append(", null);\n");
        method.append("}");
        return method.toString();
    }

    /**
     * 创建onCreateView方法
     *
     * @param mSelectedText
     * @param mIsButterKnife
     * @return
     */
    public static String createOnCreateViewMethod(String mSelectedText, boolean mIsButterKnife) {
        StringBuilder method = new StringBuilder();
        method.append("@Nullable @Override public View onCreateView(android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container, @Nullable android.os.Bundle savedInstanceState) {\n");
        method.append("\t// TODO:OnCreateView Method has been created, run ");
        if (!mIsButterKnife) {
            method.append("FindViewById");
            method.append(" again to generate code\n");
            method.append("\t\tinitView(view);\n");
        } else {
            method.append("ButterKnife");
            method.append(" again to generate code\n");
            method.append("\t\tunbinder = ButterKnife.bind(this, view);\n");
        }
        method.append("return view;");
        method.append("}\n");
        return method.toString();
    }

    /**
     * 创建initView方法，Fragment
     *
     * @return
     */
    public static String createFragmentInitViewMethod() {
        StringBuilder method = new StringBuilder();
        method.append("public void initView(View view) {\n");
        method.append("}");
        return method.toString();
    }

    /**
     * 创建initView方法
     *
     * @return
     */
    public static String createInitViewMethod() {
        StringBuilder method = new StringBuilder();
        method.append("public void initView() {\n");
        method.append("}");
        return method.toString();
    }

    /**
     * 创建OnDestroyView方法，里面包含unbinder.unbind()
     *
     * @return
     */
    public static String createOnDestroyViewMethod() {
        StringBuilder method = new StringBuilder();
        method.append("@Override public void onDestroyView() {\n");
        method.append("\tsuper.onDestroyView();");
        method.append("\tunbinder.unbind();");
        method.append("}");
        return method.toString();
    }

    /**
     * 判断是否实现了OnClickListener接口
     *
     * @param referenceElements
     * @return
     */
    public static boolean isImplementsOnClickListener(PsiJavaCodeReferenceElement[] referenceElements) {
        for (PsiJavaCodeReferenceElement referenceElement : referenceElements) {
            if (referenceElement.getText().contains("OnClickListener")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取initView方法里面的每条数据
     *
     * @param mClass
     * @return
     */
    public static PsiStatement[] getInitViewBodyStatements(PsiClass mClass) {
        // 获取initView方法
        PsiMethod[] method = mClass.findMethodsByName(Constant.utils.creatorInitViewName, false);
        PsiStatement[] statements = null;
        if (method.length > 0 && method[0].getBody() != null) {
            PsiCodeBlock methodBody = method[0].getBody();
            statements = methodBody.getStatements();
        }
        return statements;
    }

    /**
     * 获取onClick方法里面的每条数据
     *
     * @param mClass
     * @return
     */
    public static PsiElement[] getOnClickStatement(PsiClass mClass) {
        // 获取onClick方法
        PsiMethod[] onClickMethods = mClass.findMethodsByName(Constant.FieldonClick, false);
        PsiElement[] psiElements = null;
        if (onClickMethods.length > 0 && onClickMethods[0].getBody() != null) {
            PsiCodeBlock onClickMethodBody = onClickMethods[0].getBody();
            psiElements = onClickMethodBody.getChildren();
        }
        return psiElements;
    }

    /**
     * 获取包含@OnClick注解的方法
     *
     * @param mClass
     * @return
     */
    public static PsiMethod getPsiMethodByButterKnifeOnClick(PsiClass mClass) {
        for (PsiMethod psiMethod : mClass.getMethods()) {
            // 获取方法的注解
            PsiModifierList modifierList = psiMethod.getModifierList();
            PsiAnnotation[] annotations = modifierList.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName != null && qualifiedName.equals("butterknife.OnClick")) {
                    // 包含@OnClick注解
                    return psiMethod;
                }
            }
        }
        return null;
    }

    /**
     * 根据方法名获取方法
     *
     * @param mClass
     * @param methodName
     * @return
     */
    public static PsiMethod getPsiMethodByName(PsiClass mClass, String methodName) {
        for (PsiMethod psiMethod : mClass.getMethods()) {
            if (psiMethod.getName().equals(methodName)) {
                return psiMethod;
            }
        }
        return null;
    }

    /**
     * 获取View类型的变量名
     *
     * @param mClass
     * @return
     */
    public static String getPsiMethodParamsViewField(PsiClass mClass) {
        PsiMethod butterKnifeOnClickMethod = getPsiMethodByButterKnifeOnClick(mClass);
        if (butterKnifeOnClickMethod != null) {
            // 获取方法的指定参数类型的变量名
            PsiParameterList parameterList = butterKnifeOnClickMethod.getParameterList();
            PsiParameter[] parameters = parameterList.getParameters();
            for (PsiParameter parameter : parameters) {
                if (parameter.getTypeElement() != null && parameter.getTypeElement().getText().equals("View")) {
                    return parameter.getName();
                }
            }
        }
        return null;
    }

    /**
     * 获取包含@OnClick注解里面的值
     *
     * @return
     */
    public static List<String> getPsiMethodByButterKnifeOnClickValue(PsiClass mClass) {
        List<String> onClickValue = new ArrayList<>();
        PsiMethod butterKnifeOnClickMethod = Util.getPsiMethodByButterKnifeOnClick(mClass);
        if (butterKnifeOnClickMethod != null) {// 获取方法的注解
            PsiModifierList modifierList = butterKnifeOnClickMethod.getModifierList();
            PsiAnnotation[] annotations = modifierList.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                if (annotation.getQualifiedName() != null && annotation.getQualifiedName().equals("butterknife.OnClick")) {
                    String text = annotation.getText()
                            .replace("(", "").replace(")", "")
                            .replace("{", "").replace("}", "")
                            .replace(" ", "").replace("@OnClick", "");
                    if (!TextUtils.isEmpty(text)) {
                        String[] split = text.split(",");
                        for (String value : split) {
                            if (!TextUtils.isEmpty(value)) {
                                onClickValue.add(value);
                            }
                        }
                    }
                    break;
                }
            }
        }
        return onClickValue;
    }

    /**
     * 添加注解到方法
     *
     * @param mClass
     * @param mFactory
     * @param onClickValue
     */
    public static void createOnClickAnnotation(PsiClass mClass, PsiElementFactory mFactory, List<String> onClickValue) {
        PsiMethod butterKnifeOnClickMethod = Util.getPsiMethodByButterKnifeOnClick(mClass);
        if (butterKnifeOnClickMethod != null) {// 获取方法的注解
            PsiModifierList modifierList = butterKnifeOnClickMethod.getModifierList();
            PsiAnnotation[] annotations = modifierList.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                if (annotation.getQualifiedName() != null && annotation.getQualifiedName().equals("butterknife.OnClick")) {
                    StringBuilder annotationText = new StringBuilder();
                    annotationText.append("@OnClick(");
                    if (onClickValue.size() == 1) {
                        annotationText.append(onClickValue.get(0));
                    } else {
                        annotationText.append("{");
                        for (int j = 0; j < onClickValue.size(); j++) {
                            String value = onClickValue.get(j);
                            if (j != 0) {
                                annotationText.append(", ");
                            }
                            annotationText.append(value);
                        }
                        annotationText.append("}");
                    }
                    annotationText.append(")");
                    modifierList.addBefore(mFactory.createAnnotationFromText(annotationText.toString(), modifierList), annotation);
                    annotation.delete();
                    break;
                }
            }
        }
    }

    /**
     * 获取OnClickList里面的id集合
     *
     * @param mOnClickList clickable的Element集合
     * @return
     */
    public static List<String> getOnClickListById(List<Element> mOnClickList) {
        return mOnClickList.stream().map(Element::getFullID).collect(Collectors.toList());
    }

    /**
     * 获取注解里面跟OnClickList的id集合
     *
     * @param annotationList OnClick注解里面的id集合
     * @param onClickIdList  clickable的Element集合
     * @return
     */
    public static List<String> createOnClickValue(List<String> annotationList, List<String> onClickIdList) {
        for (String value : onClickIdList) {
            if (!annotationList.contains(value)) {
                annotationList.add(value);
            }
        }
        return annotationList;
    }

    /**
     * FindViewById，获取xml里面的text
     * @param element Element
     * @param mProject Project
     * @return
     */
    public static String createFieldText(Element element, Project mProject){
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
                    if (psiFile.getParent() != null && psiFile.getParent().toString().contains("src\\main\\res\\values")) {
                        text = Util.getTextFromStringsXml(psiFile, text);
                    }
                }
            }
        }
        return text;
    }

    /**
     * FindViewById，创建字段
     * @param text 注释内容
     * @param element Element
     * @param mIsLayoutInflater 是否选中LayoutInflater
     * @param mLayoutInflaterText 选中的布局的变量名
     * @return
     */
    public static String createFieldByElement(String text, Element element, boolean mIsLayoutInflater, String mLayoutInflaterText){
        StringBuilder fromText = new StringBuilder();
        if (!TextUtils.isEmpty(text)) {
            fromText.append("/** ");
            fromText.append(text);
            fromText.append(" */\n");
        }
        fromText.append("private ");
        fromText.append(element.getName());
        fromText.append(" ");
        fromText.append(element.getFieldName());
        if (mIsLayoutInflater) fromText.append(mLayoutInflaterText.substring(1));
        fromText.append(";");
        return fromText.toString();
    }

    /**
     * FindViewById，创建findViewById代码到initView方法里面
     * @param findPre Fragment的话要view.findViewById
     * @param mIsLayoutInflater 是否选中LayoutInflater
     * @param mLayoutInflaterText 选中的布局的变量名
     * @param context context
     * @param mSelectedText 选中的布局
     * @param mElements Element的List
     * @return
     */
    public static String createFieldsByInitViewMethod(String findPre, boolean mIsLayoutInflater, String mLayoutInflaterText, String context, String mSelectedText, List<Element> mElements){
        StringBuilder initView = new StringBuilder();
        if (TextUtils.isEmpty(findPre)) {
            initView.append("private void initView() {\n");
        } else {
            initView.append("private void initView(View ");
            initView.append(findPre);
            initView.append(") {\n");
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
                initView.append(element.getFieldName());
                initView.append(inflater);
                initView.append(" = (");
                initView.append(element.getName());
                initView.append(")");
                initView.append(pre);
                initView.append("findViewById(");
                initView.append(element.getFullID());
                initView.append(");\n");
                if (element.isClickable() && element.isClickEnable()) {
                    initView.append(element.getFieldName());
                    initView.append(inflater);
                    initView.append(".setOnClickListener(this);\n");
                }
            }
        }
        initView.append("}\n");
        return initView.toString();
    }


    /**
     *
     * ButterKnife，创建findById代码到init方法里面
     * @param mIsLayoutInflater
     * @param mLayoutInflaterText
     * @param context
     * @param mSelectedText
     * @param mElements
     * @param viewMethodName
     * @return
     */
    public static String createButterKnifeViewMethod(boolean mIsLayoutInflater, String mLayoutInflaterText, String context, String mSelectedText, List<Element> mElements, String viewMethodName){
        StringBuilder initView = new StringBuilder();
        initView.append("// TODO:Copy method name to use\n");
        initView.append("private void ");
        initView.append(viewMethodName);
        initView.append("() {\n");
        if (mIsLayoutInflater) {
            // 添加LayoutInflater.from(this).inflate(R.layout.activity_main, null);
            String layoutInflater = mLayoutInflaterText
                    + " = LayoutInflater.from(" + context + ").inflate(R.layout." + mSelectedText + ", null);"
                    + "\n";
            initView.append(layoutInflater);
        }
        for (Element element : mElements) {
            if (element.isEnable()) {
                String inflater = "";
                if (mIsLayoutInflater) {
                    inflater = mLayoutInflaterText.substring(1);
                }
                initView.append(element.getFieldName());
                initView.append(inflater);
                initView.append(" = ");
                initView.append("ButterKnife.findById(");
                initView.append(mLayoutInflaterText);
                initView.append(", ");
                initView.append(element.getFullID());
                initView.append(");\n");
            }
        }
        initView.append("}\n");
        return initView.toString();
    }

    /**
     * FindViewById，创建OnClick方法和switch
     * @param mOnClickList 可onclick的Element的集合
     * @return
     */
    public static String createFindViewByIdOnClickMethodAndSwitch(List<Element> mOnClickList){
        StringBuilder onClick = new StringBuilder();
        onClick.append("@Override public void onClick(View v) {\n");
        onClick.append("switch (v.getId()) {\n");
        for (Element element : mOnClickList) {
            if (element.isClickable()) {
                onClick.append("\tcase ");
                onClick.append(element.getFullID());
                onClick.append(":\n");
                onClick.append("\t\tbreak;\n");
            }
        }
        onClick.append("}\n");
        onClick.append("}\n");
        return onClick.toString();
    }

    /**
     * ButterKnife，在OnClick方法里面创建switch
     * @param psiMethodParamsViewField View类型的变量名
     * @param onClickValues 注解里面跟OnClickList的id集合
     * @return
     */
    public static String createSwitchByOnClickMethod(String psiMethodParamsViewField, List<String> onClickValues){
        StringBuilder psiSwitch = new StringBuilder();
        psiSwitch.append("switch (");
        psiSwitch.append(psiMethodParamsViewField);
        psiSwitch.append(".getId()) {\n");
        for (String value : onClickValues) {
            psiSwitch.append("\tcase ");
            psiSwitch.append(value);
            psiSwitch.append(":\n");
            psiSwitch.append("\t\tbreak;\n");
        }
        psiSwitch.append("}");
        return psiSwitch.toString();
    }

    /**
     * ButterKnife，创建OnClick方法和switch
     * @param mOnClickList 可onclick的Element的集合
     * @return
     */
    public static String createButterKnifeOnClickMethodAndSwitch(List<Element> mOnClickList){
        StringBuilder onClick = new StringBuilder();
        onClick.append("@butterknife.OnClick(");
        if (mOnClickList.size() == 1) {
            onClick.append(mOnClickList.get(0).getFullID());
        } else {
            onClick.append("{");
            for (int i = 0; i < mOnClickList.size(); i++) {
                Element element = mOnClickList.get(i);
                if (i != 0) {
                    onClick.append(", ");
                }
                onClick.append(element.getFullID());
            }
            onClick.append("}");
        }
        onClick.append(")\n");
        onClick.append("public void onClick(View v) {\n");
        onClick.append("switch (v.getId()) {\n");
        for (Element element : mOnClickList) {
            onClick.append("\tcase ");
            onClick.append(element.getFullID());
            onClick.append(":\n");
            onClick.append("\t\tbreak;\n");
        }
        onClick.append("}\n");
        onClick.append("}\n");
        return onClick.toString();
    }

}
