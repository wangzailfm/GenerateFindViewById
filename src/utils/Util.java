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
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

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
     * @param editor editor
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
     * @param fieldName fieldName
     * @param type type
     * @return String
     */
    public static String getFieldName(String fieldName, int type) {
        if (!StringUtils.isEmpty(fieldName)) {
            String[] names = fieldName.split("_");
            if (type == 2) {
                // aaBbCc
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < names.length; i++) {
                    if (i == 0) {
                        sb.append(names[i]);
                    } else {
                        sb.append(Util.firstToUpperCase(names[i]));
                    }
                }
                sb.append("View");
                fieldName = sb.toString();
            } else if (type == 3) {
                // mAaBbCc
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < names.length; i++) {
                    if (i == 0) {
                        sb.append("m");
                    }
                    sb.append(Util.firstToUpperCase(names[i]));
                }
                sb.append("View");
                fieldName = sb.toString();
            } else {
                fieldName += "_view";
            }
        }
        return fieldName;
    }

    /**
     * 第一个字母大写
     *
     * @param key key
     * @return String
     */
    public static String firstToUpperCase(String key) {
        return key.substring(0, 1).toUpperCase(Locale.CHINA) + key.substring(1);
    }

    /**
     * 解析xml获取string的值
     *
     * @param psiFile psiFile
     * @param text text
     * @return String
     */
    private static String getTextFromStringsXml(PsiFile psiFile, String text) {
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
     * @param file file
     * @param elements elements
     * @return List<Element>
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
                        PsiFile[] psiFiles = new PsiFile[0];
                        if (layout != null) {
                            psiFiles = FilenameIndex.getFilesByName(project, getLayoutName(layout.getValue()) + Constant.selectedTextSUFFIX, GlobalSearchScope.allScope(project));
                        }
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
                    if (clickableAttr != null && !StringUtils.isEmpty(clickableAttr.getValue())) {
                        clickable = clickableAttr.getValue().equals("true");
                    }
                    if (!StringUtils.isEmpty(name) && name.equals("Button")) {
                        clickable = true;
                    }
                    // 添加到list
                    try {
                        Element e = new Element(name, idValue, clickable, tag);
                        elements.add(e);
                    } catch (IllegalArgumentException ignored) {

                    }
                }
            }
        });


        return elements;
    }

    /**
     * layout.getValue()返回的值为@layout/layout_view
     *
     * @param layout layout
     * @return String
     */
    private static String getLayoutName(String layout) {
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
     * @param editor editor
     * @param file file
     * @return PsiClass
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
     * @param mProject mProject
     * @param mClass mClass
     * @return boolean
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
     * @param mProject mProject
     * @param mClass mClass
     * @return boolean
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
     * @param mClass mClass
     * @return boolean
     */
    public static boolean isButterKnifeBindExist(PsiClass mClass){
        PsiMethod onCreateMethod = getPsiMethodByName(mClass, Constant.psiMethodByOnCreate);
        PsiMethod onCreateViewMethod = getPsiMethodByName(mClass, Constant.psiMethodByOnCreateView);
        return !(onCreateMethod != null && onCreateMethod.getBody() != null
                && onCreateMethod.getBody().getText().contains(Constant.utils.fieldButterKnifeBind)
                || (onCreateViewMethod != null && onCreateViewMethod.getBody() != null
                && onCreateViewMethod.getBody().getText().contains(Constant.utils.fieldButterKnifeBind)));
    }

    /**
     * 创建onCreate方法
     *
     * @param mSelectedText mSelectedText
     * @param mIsButterKnife mIsButterKnife
     * @return String
     */
    static String createOnCreateMethod(String mSelectedText, boolean mIsButterKnife) {
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
        method.append("}");
        return method.toString();
    }

    /**
     * 创建onCreate方法(Fragment)
     *
     * @param mSelectedText mSelectedText
     * @return String
     */
    static String createFragmentOnCreateMethod(String mSelectedText) {
        return "@Override public void onCreate(@Nullable android.os.Bundle savedInstanceState) {\n" +
                "super.onCreate(savedInstanceState);\n" +
                "\tview = View.inflate(getActivity(), R.layout." + mSelectedText + ", null);\n" +
                "}";
    }

    /**
     * 创建onCreateView方法
     *
     * @param mSelectedText mSelectedText
     * @param mIsButterKnife mIsButterKnife
     * @return String
     */
    static String createOnCreateViewMethod(String mSelectedText, boolean mIsButterKnife) {
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
     * @return String
     */
    static String createFragmentInitViewMethod() {
        return "public void initView(View view) {\n" +
                "}";
    }

    /**
     * 创建initView方法
     *
     * @return String
     */
    static String createInitViewMethod() {
        return "public void initView() {\n" +
                "}";
    }

    /**
     * 创建OnDestroyView方法，里面包含unbinder.unbind()
     *
     * @return String
     */
    static String createOnDestroyViewMethod() {
        return "@Override public void onDestroyView() {\n" +
                "\tsuper.onDestroyView();" +
                "\tunbinder.unbind();" +
                "}";
    }

    /**
     * 判断是否实现了OnClickListener接口
     *
     * @param referenceElements referenceElements
     * @return boolean
     */
    static boolean isImplementsOnClickListener(PsiJavaCodeReferenceElement[] referenceElements) {
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
     * @param mClass mClass
     * @return PsiStatement[]
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
     * @param mClass mClass
     * @return PsiElement[]
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
     * @param mClass mClass
     * @return PsiMethod
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
     * @param mClass mClass
     * @param methodName methodName
     * @return PsiMethod
     */
    static PsiMethod getPsiMethodByName(PsiClass mClass, String methodName) {
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
     * @param mClass mClass
     * @return String
     */
    static String getPsiMethodParamsViewField(PsiClass mClass) {
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
     * @return List<String>
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
                    if (!StringUtils.isEmpty(text)) {
                        String[] split = text.split(",");
                        for (String value : split) {
                            if (!StringUtils.isEmpty(value)) {
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
     * @param mClass mClass
     * @param mFactory mFactory
     * @param onClickValue onClickValue
     */
    static void createOnClickAnnotation(PsiClass mClass, PsiElementFactory mFactory, List<String> onClickValue) {
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
     * @return List<String>
     */
    static List<String> getOnClickListById(List<Element> mOnClickList) {
        return mOnClickList.stream().map(Element::getFullID).collect(Collectors.toList());
    }

    /**
     * 获取注解里面跟OnClickList的id集合
     *
     * @param annotationList OnClick注解里面的id集合
     * @param onClickIdList  clickable的Element集合
     * @return List<String>
     */
    static List<String> createOnClickValue(List<String> annotationList, List<String> onClickIdList) {
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
     * @return String
     */
    static String createFieldText(Element element, Project mProject){
        String text = element.getXml().getAttributeValue("android:text");
        if (StringUtils.isEmpty(text)) {
            // 如果是text为空，则获取hint里面的内容
            text = element.getXml().getAttributeValue("android:hint");
        }
        // 如果是@string/app_name类似
        if (!StringUtils.isEmpty(text) && text.contains("@string/")) {
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
     * @param mLayoutInflaterType mLayoutInflaterType
     * @return String
     */
    static String createFieldByElement(String text, Element element, boolean mIsLayoutInflater, String mLayoutInflaterText, int mLayoutInflaterType){
        StringBuilder fromText = new StringBuilder();
        if (!StringUtils.isEmpty(text)) {
            fromText.append("/** ");
            fromText.append(text);
            fromText.append(" */\n");
        }
        fromText.append("private ");
        fromText.append(element.getName());
        fromText.append(" ");
        fromText.append(element.getFieldName());
        if (mIsLayoutInflater) fromText.append(layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType));
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
     * @param mLayoutInflaterType type
     * @return String
     */
    static String createFieldsByInitViewMethod(String findPre, boolean mIsLayoutInflater, String mLayoutInflaterText, String context, String mSelectedText, List<Element> mElements, int mLayoutInflaterType){
        StringBuilder initView = new StringBuilder();
        if (StringUtils.isEmpty(findPre)) {
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
                String pre = StringUtils.isEmpty(findPre) ? "" : findPre + ".";
                String inflater = "";
                if (mIsLayoutInflater) {
                    inflater = layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType);
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
     * 根据layoutInflaterType生成不同内容
     * @param mLayoutInflaterText mLayoutInflaterText
     * @param mLayoutInflaterType mLayoutInflaterType
     * @return String
     */
    static String layoutInflaterType2Str(String mLayoutInflaterText, int mLayoutInflaterType) {
        switch (mLayoutInflaterType) {
            case 1:
                return "_" + mLayoutInflaterText;
            case 2:
                return Util.firstToUpperCase(mLayoutInflaterText);
            default:
                return mLayoutInflaterText.substring(1);
        }
    }


    /**
     *
     * ButterKnife，创建findById代码到init方法里面
     * @param mIsLayoutInflater mIsLayoutInflater
     * @param mLayoutInflaterText mLayoutInflaterText
     * @param context context
     * @param mSelectedText mSelectedText
     * @param mElements mElements
     * @param viewMethodName viewMethodName
     * @return String
     */
    static String createButterKnifeViewMethod(boolean mIsLayoutInflater, String mLayoutInflaterText, String context, String mSelectedText, List<Element> mElements, String viewMethodName, int mLayoutInflaterType){
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
                    inflater = layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType);
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
     * @return String
     */
    static String createFindViewByIdOnClickMethodAndSwitch(List<Element> mOnClickList){
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
     * @return String
     */
    static String createSwitchByOnClickMethod(String psiMethodParamsViewField, List<String> onClickValues){
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
     * @return String
     */
    static String createButterKnifeOnClickMethodAndSwitch(List<Element> mOnClickList){
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

    /**
     * FindViewById，创建ViewHolder
     * @param viewHolderName viewHolderName
     * @param viewHolderRootView viewHolderRootView
     * @param mElements mElements
     * @return String
     */
    @NotNull
    static String createFindViewByIdViewHolder(String viewHolderName, String viewHolderRootView, List<Element> mElements) {
        // ViewHolder
        StringBuilder viewHolderText = new StringBuilder();
        // ViewHolder的Constructor
        StringBuilder viewHolderConstructorText = new StringBuilder();
        // rootView
        viewHolderText.append("android.view.View ");
        viewHolderText.append(viewHolderRootView);
        viewHolderText.append(";\n");
        // Constructor
        viewHolderConstructorText.append(viewHolderName);
        viewHolderConstructorText.append("(android.view.View ");
        viewHolderConstructorText.append(viewHolderRootView);
        viewHolderConstructorText.append(") {\n");
        viewHolderConstructorText.append("this.");
        viewHolderConstructorText.append(viewHolderRootView);
        viewHolderConstructorText.append(" = ");
        viewHolderConstructorText.append(viewHolderRootView);
        viewHolderConstructorText.append(";\n");
        // 添加field和findViewById
        for (Element element : mElements) {
            // 添加Field
            viewHolderText.append(element.getName());
            viewHolderText.append(" ");
            viewHolderText.append(element.getFieldName());
            viewHolderText.append(";\n");
            // 添加findViewById
            viewHolderConstructorText.append("this.");
            viewHolderConstructorText.append(element.getFieldName());
            viewHolderConstructorText.append(" = (");
            viewHolderConstructorText.append(element.getName());
            viewHolderConstructorText.append(") ");
            viewHolderConstructorText.append(viewHolderRootView);
            viewHolderConstructorText.append(".findViewById(");
            viewHolderConstructorText.append(element.getFullID());
            viewHolderConstructorText.append(");\n");
        }
        viewHolderConstructorText.append("}");
        // 添加Constructor到ViewHolder
        viewHolderText.append(viewHolderConstructorText.toString());
        return viewHolderText.toString();
    }

    /**
     * ButterKnife，创建ViewHolder
     * @param viewHolderName viewHolderName
     * @param viewHolderRootView viewHolderRootView
     * @param mElements mElements
     * @return String
     */
    @NotNull
    static String createButterKnifeViewHolder(String viewHolderName, String viewHolderRootView, List<Element> mElements) {
        // ViewHolder
        StringBuilder viewHolderText = new StringBuilder();
        // ViewHolder的Constructor
        StringBuilder viewHolderConstructorText = new StringBuilder();
        // 添加field和findViewById
        for (Element element : mElements) {
            // 添加Field
            viewHolderText.append("@BindView(");
            viewHolderText.append(element.getFullID());
            viewHolderText.append(")\n");
            viewHolderText.append(element.getName());
            viewHolderText.append(" ");
            viewHolderText.append(element.getFieldName());
            viewHolderText.append(";\n");
        }
        // Constructor
        viewHolderConstructorText.append(viewHolderName);
        viewHolderConstructorText.append("(android.view.View ");
        viewHolderConstructorText.append(viewHolderRootView);
        viewHolderConstructorText.append(") {\n");
        viewHolderConstructorText.append("ButterKnife.bind(this, ");
        viewHolderConstructorText.append(viewHolderRootView);
        viewHolderConstructorText.append(");\n");
        viewHolderConstructorText.append("}");
        // 添加Constructor到ViewHolder
        viewHolderText.append(viewHolderConstructorText.toString());
        return viewHolderText.toString();
    }

}
