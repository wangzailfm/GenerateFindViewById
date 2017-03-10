package utils;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction.Simple;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import constant.Constant;
import entitys.Element;
import views.ButterKnifeDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ButterKnifeCreator extends Simple {

    private List<Element> mOnClickList = new ArrayList<>();
    private final ButterKnifeDialog mDialog;
    private final Editor mEditor;
    private final PsiFile mFile;
    private final Project mProject;
    private final PsiClass mClass;
    private final List<Element> mElements;
    private final PsiElementFactory mFactory;
    private final String mSelectedText;
    private final boolean mIsLayoutInflater;
    private final String mLayoutInflaterText;
    private final boolean mIsBind;
    private final boolean mViewHolder;

    /**
     * Builder模式
     */
    public static class Builder {

        private ButterKnifeDialog mDialog;
        private Editor mEditor;
        private PsiFile mFile;
        private Project mProject;
        private PsiClass mClass;
        private final String mCommand;
        private List<Element> mElements;
        private PsiElementFactory mFactory;
        private String mSelectedText;
        private boolean mIsLayoutInflater;
        private String mLayoutInflaterText;
        private boolean mIsBind;
        private boolean mViewHolder;


        public Builder(String mCommand) {
            this.mCommand = mCommand;
        }

        public Builder setDialog(ButterKnifeDialog mDialog) {
            this.mDialog = mDialog;
            return this;
        }

        public Builder setEditor(Editor mEditor) {
            this.mEditor = mEditor;
            return this;
        }

        public Builder setFile(PsiFile mFile) {
            this.mFile = mFile;
            return this;
        }

        public Builder setProject(Project mProject) {
            this.mProject = mProject;
            return this;
        }

        public Builder setClass(PsiClass mClass) {
            this.mClass = mClass;
            return this;
        }

        public Builder setElements(List<Element> mElements) {
            this.mElements = mElements;
            return this;
        }

        public Builder setFactory(PsiElementFactory mFactory) {
            this.mFactory = mFactory;
            return this;
        }

        public Builder setSelectedText(String mSelectedText) {
            this.mSelectedText = mSelectedText;
            return this;
        }

        public Builder setIsLayoutInflater(boolean mIsLayoutInflater) {
            this.mIsLayoutInflater = mIsLayoutInflater;
            return this;
        }

        public Builder setLayoutInflaterText(String mLayoutInflaterText) {
            this.mLayoutInflaterText = mLayoutInflaterText;
            return this;
        }

        public Builder setIsBind(boolean mIsBind) {
            this.mIsBind = mIsBind;
            return this;
        }

        public Builder setViewHolder(boolean mViewHolder) {
            this.mViewHolder = mViewHolder;
            return this;
        }

        public ButterKnifeCreator build() {
            return new ButterKnifeCreator(this);
        }
    }

    private ButterKnifeCreator(Builder builder) {
        super(builder.mClass.getProject(), builder.mCommand);
        mDialog = builder.mDialog;
        mEditor = builder.mEditor;
        mFile = builder.mFile;
        mClass = builder.mClass;
        mProject = builder.mProject;
        mElements = builder.mElements;
        mFactory = builder.mFactory;
        mSelectedText = builder.mSelectedText;
        mIsLayoutInflater = builder.mIsLayoutInflater;
        mLayoutInflaterText = builder.mLayoutInflaterText;
        mIsBind = builder.mIsBind;
        mViewHolder = builder.mViewHolder;
        // 添加有onclick的list
        mOnClickList.addAll(mElements.stream().filter(element -> element.isEnable() && element.isClickable()).collect(Collectors.toList()));
    }

    @Override
    protected void run() throws Throwable {
        try {
            if (mViewHolder) {
                generateViewHolder();
            } else {
                generateButterKnife();
            }
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
        Util.showPopupBalloon(mEditor, Constant.actions.selectedSuccess, 5);
    }

    /**
     * 创建ViewHolder
     */
    private void generateViewHolder() {
        String viewHolderName = "ViewHolder";
        String viewHolderRootView = "view";
        // 创建ViewHolder类
        PsiClass viewHolder = mFactory.createClassFromText(Util.createButterKnifeViewHolder(viewHolderName, viewHolderRootView, mElements), mClass);
        // 设置名字
        viewHolder.setName(viewHolderName);
        // 添加ViewHolder类到类中
        mClass.add(viewHolder);
        // 添加static
        mClass.addBefore(mFactory.createKeyword("static"), mClass.findInnerClassByName(viewHolderName, true));
    }

    /**
     * 创建变量
     *
     * @param type type
     */
    private void generateFields(String type) {
        if (type.equals(Constant.classTypeByFragment)) {
            // 判断View是否存在
            boolean isViewExist = false;
            // 判断Unbinder是否存在
            boolean isUnbinderExist = false;
            for (PsiField field : mClass.getFields()) {
                if (field.getName() != null && field.getName().equals(Constant.utils.creatorViewName)) {
                    isViewExist = true;
                }
                if (field.getName() != null && field.getName().equals(Constant.utils.creatorUnbinderName)) {
                    isUnbinderExist = true;
                }
            }
            if (!isViewExist) {
                mClass.add(mFactory.createFieldFromText("private View view;", mClass));
            }
            if (!isUnbinderExist && mIsBind) {
                mClass.add(mFactory.createFieldFromText("private Unbinder unbinder;", mClass));
            }
        }
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
            if (element.isEnable()) {
                if (mIsLayoutInflater) {
                    boolean duplicateField = false;
                    for (PsiField psiField : mClass.getFields()) {
                        if (psiField.getName() != null
                                && psiField.getName().equals(element.getFieldName() + mLayoutInflaterText.substring(1))) {
                            duplicateField = true;
                        }
                    }
                    // 已存在跳出
                    if (duplicateField) {
                        continue;
                    }
                    // 添加到class
                    mClass.add(mFactory.createFieldFromText(Util.createFieldByElement(
                            Util.createFieldText(element, mProject), element, true, mLayoutInflaterText), mClass));
                } else {
                    // 已存在的变量就不创建
                    boolean isFieldExist = false;
                    boolean isAnnotationExist = false;
                    for (PsiField field : mClass.getFields()) {
                        if (field.getName() != null) {
                            isAnnotationExist = field.getText().contains("@BindView(" + element.getFullID() + ")");
                            isFieldExist = field.getText().contains(element.getFieldName()) && !(field.getText().contains("private") || field.getText().contains("static"));
                            if (isAnnotationExist || isFieldExist) {
                                break;
                            }
                        }
                    }
                    // @BindView(R.id.text) TextView mText;
                    // 如果两个都存在则跳出
                    if (isAnnotationExist && isFieldExist) {
                        continue;
                    }
                    if (!isFieldExist) {
                        // 如果只存在TextView mText
                        String fromText = element.getName() + " " + element.getFieldName() + ";";
                        // 添加到class
                        mClass.add(mFactory.createFieldFromText(fromText, mClass));
                    }

                    for (PsiField field : mClass.getFields()) {
                        if (field.getName() != null && field.getText().contains(element.getFieldName())
                                && !(field.getText().contains("private") || field.getText().contains("static"))) {
                            String annotationText = "@BindView(" + element.getFullID() + ")";
                            // 添加注解到field
                            mClass.addBefore(mFactory.createAnnotationFromText(annotationText, mClass), field);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置变量，Activity和Fragment
     */
    private void generateButterKnife() {
        if (Util.isExtendsActivityOrActivityCompat(mProject, mClass)) {
            // 判断是否有onCreate方法
            if (mClass.findMethodsByName(Constant.psiMethodByOnCreate, false).length == 0) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateMethod(mSelectedText, true), mClass));
            } else {
                generateFields(Constant.classTypeByActivity);
                if (mIsLayoutInflater) {
                    generatorViewMethod("getApplicationContext()");
                }
                // 获取setContentView
                PsiStatement setContentViewStatement = null;
                // onCreate是否ButterKnife.bind(this);
                boolean hasButterKnifeBindStatement = false;

                PsiMethod onCreate = mClass.findMethodsByName(Constant.psiMethodByOnCreate, false)[0];
                if (onCreate.getBody() != null) {
                    for (PsiStatement psiStatement : onCreate.getBody().getStatements()) {
                        // 查找setContentView
                        if (psiStatement.getFirstChild() instanceof PsiMethodCallExpression) {
                            PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) psiStatement.getFirstChild()).getMethodExpression();
                            if (methodExpression.getText().equals(Constant.utils.creatorSetContentViewMethod)) {
                                setContentViewStatement = psiStatement;
                            } else if (methodExpression.getText().contains(Constant.utils.fieldButterKnifeBind)) {
                                hasButterKnifeBindStatement = true;
                            }
                        }
                    }
                    if (setContentViewStatement == null) {
                        onCreate.getBody().add(mFactory.createStatementFromText("setContentView(R.layout." + mSelectedText + ");", mClass));
                    }

                    if (!hasButterKnifeBindStatement) {
                        // 将ButterKnife.bind(this);写到setContentView()后面
                        if (setContentViewStatement != null) {
                            onCreate.getBody().addAfter(mFactory.createStatementFromText("ButterKnife.bind(this);", mClass), setContentViewStatement);
                        } else {
                            onCreate.getBody().add(mFactory.createStatementFromText("ButterKnife.bind(this);", mClass));
                        }
                    }
                }
            }
            if (mOnClickList.size() > 0 && !mIsLayoutInflater) {
                generatorClickCode();
            }

        } else if (Util.isExtendsFragmentOrFragmentV4(mProject, mClass)) {
            // 判断是否有onCreateView方法
            if (mClass.findMethodsByName(Constant.psiMethodByOnCreateView, false).length == 0) {
                // 添加
                mClass.add(mFactory.createMethodFromText(Util.createOnCreateViewMethod(mSelectedText, true), mClass));

            } else {
                generateFields(Constant.classTypeByFragment);
                if (mIsLayoutInflater) {
                    generatorViewMethod("getActivity()");
                }
                // 查找onCreateView
                PsiReturnStatement returnStatement = null;
                // view
                String returnValue = null;

                PsiMethod onCreateView = mClass.findMethodsByName(Constant.psiMethodByOnCreateView, false)[0];
                if (onCreateView.getBody() != null) {
                    for (PsiStatement psiStatement : onCreateView.getBody().getStatements()) {
                        if (psiStatement instanceof PsiReturnStatement) {
                            // 获取view的值
                            returnStatement = (PsiReturnStatement) psiStatement;
                            if (returnStatement.getReturnValue() != null) {
                                returnValue = returnStatement.getReturnValue().getText();
                            }
                        }
                    }

                    if (returnStatement != null && returnValue != null) {
                        boolean isBindExist = false;
                        // 判断是否已存在unbinder = ButterKnife.bind
                        String bind = "unbinder = ButterKnife.bind(this, " + returnValue + ");";
                        // 获取onCreateView方法里面的PsiStatement
                        for (PsiStatement psiStatement : onCreateView.getBody().getStatements()) {
                            if (psiStatement.getText().contains(bind)) {
                                isBindExist = true;
                                break;
                            } else {
                                isBindExist = false;
                            }
                        }
                        if (!isBindExist && mIsBind) {
                            //将ButterKnife.bind(this);写到return view前面
                            onCreateView.getBody().addBefore(mFactory.createStatementFromText(bind, mClass), returnStatement);
                        }
                    }
                }
            }
            if (mOnClickList.size() > 0 && !mIsLayoutInflater) {
                generatorClickCode();
            }
            if (mIsBind) {
                generatorLayoutCode();
            }
        }
    }

    /**
     * 写onClick方法
     */
    private void generatorClickCode() {
        // 判断是否包含@OnClick注解
        PsiMethod butterKnifeOnClickMethod = Util.getPsiMethodByButterKnifeOnClick(mClass);
        // 有@OnClick注解
        if (butterKnifeOnClickMethod != null && butterKnifeOnClickMethod.getBody() != null) {
            PsiCodeBlock onClickMethodBody = butterKnifeOnClickMethod.getBody();
            // 获取switch
            PsiSwitchStatement psiSwitchStatement = null;
            for (PsiElement psiElement : onClickMethodBody.getChildren()) {
                if (psiElement instanceof PsiSwitchStatement) {
                    psiSwitchStatement = (PsiSwitchStatement) psiElement;
                    break;
                }
            }
            List<String> psiMethodByButterKnifeOnClickValue = Util.getPsiMethodByButterKnifeOnClickValue(mClass);
            List<String> onClickIdList = Util.getOnClickListById(mOnClickList);
            List<String> onClickValues = Util.createOnClickValue(psiMethodByButterKnifeOnClickValue, onClickIdList);
            // 有switch
            if (psiSwitchStatement != null) {
                // 获取switch的内容
                PsiCodeBlock psiSwitchStatementBody = psiSwitchStatement.getBody();
                if (psiSwitchStatementBody != null) {
                    for (String onClickValue : onClickValues) {
                        String cass = "case " + onClickValue + ":";
                        // 判断是否存在
                        boolean isExist = false;
                        for (PsiStatement statement : psiSwitchStatementBody.getStatements()) {
                            if (statement.getText().replace("\n", "").replace("break;", "").equals(cass)) {
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
            } else {
                // 没有switch
                String psiMethodParamsViewField = Util.getPsiMethodParamsViewField(mClass);
                if (psiMethodParamsViewField != null) {
                    butterKnifeOnClickMethod.getBody().add(mFactory.createStatementFromText(
                            Util.createSwitchByOnClickMethod(psiMethodParamsViewField, onClickValues), butterKnifeOnClickMethod));
                }
            }
            Util.createOnClickAnnotation(mClass, mFactory, onClickValues);
        } else {
            if (mOnClickList.size() != 0) {
                mClass.add(mFactory.createMethodFromText(Util.createButterKnifeOnClickMethodAndSwitch(mOnClickList), mClass));
            }
        }
    }

    /**
     * 写onDestroyView方法
     */
    private void generatorLayoutCode() {
        // 判断是否已有onDestroyView方法
        PsiMethod[] onDestroyViewMethods = mClass.findMethodsByName(Constant.utils.creatorOnDestroyViewMethod, false);
        // 有onDestroyView方法
        if (onDestroyViewMethods.length > 0 && onDestroyViewMethods[0].getBody() != null) {
            PsiCodeBlock onDestroyViewMethodBody = onDestroyViewMethods[0].getBody();
            // 获取onDestroyView方法里面的每条内容
            PsiStatement[] statements = onDestroyViewMethodBody.getStatements();
            for (Element element : mElements) {
                if (element.isEnable()) {
                    // 判断是否已存在unbinder.unbind();
                    boolean isFdExist = false;
                    for (PsiStatement statement : statements) {
                        if (statement.getText().equals(Constant.utils.creatorUnbinderField)) {
                            isFdExist = true;
                            break;
                        } else {
                            isFdExist = false;
                        }
                    }
                    // 不存在就添加
                    if (!isFdExist) {
                        onDestroyViewMethodBody.add(mFactory.createStatementFromText(Constant.utils.creatorUnbinderField, onDestroyViewMethods[0]));
                        break;
                    }
                }
            }
        } else {
            mClass.add(mFactory.createMethodFromText(Util.createOnDestroyViewMethod(), mClass));
        }
    }

    /**
     * 创建LayoutInflater的方法，包含ButterKnife.findById(view, R.id.id)
     * @param context context
     */
    private void generatorViewMethod(String context){
        String ViewMethodName = "init" + mLayoutInflaterText.substring(1);
        PsiMethod ViewMethod = Util.getPsiMethodByName(mClass, ViewMethodName);
        if (ViewMethod != null && ViewMethod.getBody() != null) {
            PsiCodeBlock body = ViewMethod.getBody();
            // 获取initView方法里面的每条内容
            PsiStatement[] statements = body.getStatements();
            for (Element element : mElements) {
                if (element.isEnable()) {
                    // 判断是否已存在findViewById
                    boolean isFdExist = false;
                    String inflater = "";
                    if (mIsLayoutInflater) {
                        inflater = mLayoutInflaterText.substring(1);
                    }
                    String findViewById = element.getFieldName() + inflater
                            + " = " + "ButterKnife.findById(" + mLayoutInflaterText +", " + element.getFullID() + ");";
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
                        body.add(mFactory.createStatementFromText(findViewById, ViewMethod));
                    }
                }
            }
        } else {
            mClass.add(mFactory.createMethodFromText(
                    Util.createButterKnifeViewMethod(mIsLayoutInflater, mLayoutInflaterText, context, mSelectedText, mElements, ViewMethodName), mClass));
        }
    }
}
