package views;

import com.intellij.psi.*;
import entitys.Element;
import utils.Util;

import java.util.List;

/**
 * FindViewByIdDialog
 */
public class FindViewByIdDialog extends GenerateDialog {

    /**
     * FindViewByIdDialog
     * @param builder Builder
     */
    public FindViewByIdDialog(Builder builder) {
        super(builder);
        initExist();
        setCheckAll();
        initTopPanel();
        initContentPanel();
        initBottomPanel();
        setConstraints();
        setDialog();
    }

    /**
     * 判断已存在的变量，设置全选
     * 判断onclick是否写入
     */
    private void initExist() {
        PsiClass mClass = getPsiClass();
        List<Element> mElements = getElements();
        int mElementSize = getElementSize();
        // 判断是否已存在的变量
        boolean isFdExist = false;
        // 判断是否已存在setOnClickListener
        boolean isClickExist = false;
        // 判断是否存在case R.id.id:
        boolean isCaseExist = false;
        PsiField[] fields = mClass.getFields();
        // 获取initView方法的内容
        PsiStatement[] statements = Util.getInitViewBodyStatements(mClass);
        PsiElement[] onClickStatement = Util.getOnClickStatement(mClass);
        for (Element element : mElements) {
            if (statements != null) {
                isFdExist = checkFieldExist(statements, element);
                String setOnClickListener = element.getFieldName() + ".setOnClickListener(this);";
                isClickExist = checkClickExist(statements, setOnClickListener);
            }
            if (onClickStatement != null) {
                isCaseExist = checkCaseExist(onClickStatement, element);
            }
            setElementProperty(mElementSize, isFdExist, isClickExist, isCaseExist, fields, element);
        }
    }

    /**
     * 判断onClick方法里面是否包含field的case
     * @param onClickStatement onClick方法
     * @param element element
     * @return boolean
     */
    private boolean checkCaseExist(PsiElement[] onClickStatement, Element element) {
        String cass = "case " + element.getFullID() + ":";
        for (PsiElement psiElement : onClickStatement) {
            if (psiElement instanceof PsiSwitchStatement) {
                PsiSwitchStatement psiSwitchStatement = (PsiSwitchStatement) psiElement;
                // 获取switch的内容
                PsiCodeBlock psiSwitchStatementBody = psiSwitchStatement.getBody();
                if (psiSwitchStatementBody != null) {
                    for (PsiStatement statement : psiSwitchStatementBody.getStatements()) {
                        if (statement.getText().replace("\n", "").replace("break;", "").equals(cass)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断initView方法里面是否包含field的findViewById
     * @param statements initView方法
     * @param element element
     * @return boolean
     */
    private boolean checkFieldExist(PsiStatement[] statements, Element element) {
        for (PsiStatement statement : statements) {
            if (statement.getText().contains(element.getFieldName())
                    && statement.getText().contains("findViewById(" + element.getFullID() + ");")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否setOnClickListener
     * @param statements onClick方法
     * @param setOnClickListener setOnClickListener
     * @return boolean
     */
    private boolean checkClickExist(PsiStatement[] statements, String setOnClickListener) {
        for (PsiStatement statement : statements) {
            if (statement.getText().equals(setOnClickListener)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 为已存在的变量设置checkbox
     * @param mElementSize mElementSize
     * @param isFdExist 判断是否已存在的变量
     * @param isClickExist 判断是否已存在setOnClickListener
     * @param isCaseExist 判断是否存在case R.id.id:
     * @param fields fields
     * @param element element
     */
    private void setElementProperty(int mElementSize, boolean isFdExist, boolean isClickExist,
                                    boolean isCaseExist, PsiField[] fields, Element element) {
        for (PsiField field : fields) {
            String name = field.getName();
            if (name != null && name.equals(element.getFieldName()) && isFdExist) {
                // 已存在的变量设置checkbox为false
                element.setEnable(false);
                mElementSize = mElementSize - 1;
                setElementSize(mElementSize);
                if (element.isClickEnable() && (!isClickExist || !isCaseExist)) {
                    element.setClickable(true);
                    element.setEnable(true);
                    mElementSize = mElementSize + 1;
                    setElementSize(mElementSize);
                }
                break;
            }
        }
    }
}
