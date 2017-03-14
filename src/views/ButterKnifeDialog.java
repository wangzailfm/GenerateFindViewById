package views;

import com.intellij.psi.*;
import entitys.Element;
import utils.Util;

import java.util.List;

/**
 * ButterKnifeDialog
 */
public class ButterKnifeDialog extends GenerateDialog {

    /**
     * ButterKnifeDialog
     * @param builder Builder
     */
    public ButterKnifeDialog(Builder builder) {
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
        boolean isFdExist;
        // 判断是否存在case R.id.id:
        boolean isCaseExist = false;
        // 判断注解是否存在R.id.id
        boolean isAnnotationValueExist = false;
        PsiField[] fields = mClass.getFields();
        PsiElement[] onClickStatement = null;
        List<String> psiMethodByButterKnifeOnClickValue = Util.getPsiMethodByButterKnifeOnClickValue(mClass);
        PsiMethod psiMethodByButterKnifeOnClick = Util.getPsiMethodByButterKnifeOnClick(mClass);
        if (psiMethodByButterKnifeOnClick != null && psiMethodByButterKnifeOnClick.getBody() != null) {
            onClickStatement = psiMethodByButterKnifeOnClick.getBody().getStatements();
        }
        for (Element element : mElements) {
            element.setClickable(true);
            element.setClickEnable();
            isFdExist = checkFieldExist(fields, element);
            if (onClickStatement != null) {
                isCaseExist = checkCaseExist(onClickStatement, element);
            }
            if (psiMethodByButterKnifeOnClickValue.size() > 0) {
                isAnnotationValueExist = psiMethodByButterKnifeOnClickValue.contains(element.getFullID());
            }
            setElementProperty(mElementSize, isFdExist, isCaseExist, isAnnotationValueExist, fields, element);
        }
        setCheckAll();
        checkBind();
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
     * 判断变量是否包含@BindView注解
     * @param fields fields
     * @param element element
     * @return boolean
     */
    private boolean checkFieldExist(PsiField[] fields, Element element) {
        for (PsiField field : fields) {
            if (field.getName() != null
                    && field.getName().equals(element.getFieldName())
                    && field.getText().contains("@BindView(" + element.getFullID() + ")")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 为已存在的变量设置checkbox
     * @param mElementSize mElementSize
     * @param isFdExist 判断是否已存在的变量
     * @param isCaseExist 判断是否存在case R.id.id:
     * @param isAnnotationValueExist 判断注解是否存在R.id.id
     * @param fields fields
     * @param element element
     */
    private void setElementProperty(int mElementSize, boolean isFdExist, boolean isCaseExist,
                                    boolean isAnnotationValueExist, PsiField[] fields, Element element) {
        for (PsiField field : fields) {
            String name = field.getName();
            if (name != null && name.equals(element.getFieldName()) && isFdExist) {
                // 已存在的变量设置checkbox为false
                element.setEnable(false);
                mElementSize = mElementSize - 1;
                setElementSize(mElementSize);
                if (element.isClickEnable() && (!isCaseExist || !isAnnotationValueExist)) {
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
