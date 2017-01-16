package views;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.ui.components.JBScrollPane;
import constant.Constant;
import entitys.Element;
import entitys.IdBean;
import utils.Util;
import utils.WidgetFieldCreator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Created by wangzai on 2016/11/24.
 */
public class FindViewByIdDialog extends JFrame implements ActionListener {
    private Project mProject;
    private Editor mEditor;
    private String mSelectedText;
    private List<Element> mElements;
    // 获取当前文件
    private PsiFile mPsiFile;
    // 获取class
    private PsiClass mClass;
    // 判断是否全选
    private int mElementSize;

    // 标签JPanel
    private JPanel mPanelTitle = new JPanel();
    private JLabel mTitleName = new JLabel(Constant.dialogs.tableFieldViewWidget);
    private JLabel mTitleId = new JLabel(Constant.dialogs.tableFieldViewId);
    private JLabel mTitleClick = new JLabel(Constant.FieldOnClick);
    private JLabel mTitleField = new JLabel(Constant.dialogs.tableFieldViewFiled);

    // 内容JPanel
    private JPanel mContentJPanel = new JPanel();
    private GridBagLayout mContentLayout = new GridBagLayout();
    private GridBagConstraints mContentConstraints = new GridBagConstraints();
    // 内容JBScrollPane滚动
    private JBScrollPane jScrollPane;

    // 底部JPanel
    // LayoutInflater JPanel
    private JPanel mPanelInflater = new JPanel(new FlowLayout(FlowLayout.LEFT));
    // 是否选择LayoutInflater
    private JCheckBox mLayoutInflater = new JCheckBox(Constant.dialogs.viewHolderCheck, false);
    // 手动修改LayoutInflater字段名
    private JTextField mLayoutInflaterField;
    // 是否全选
    private JCheckBox mCheckAll = new JCheckBox(Constant.dialogs.fieldCheckAll);

    // viewHolder
    //private JPanel mPanelViewHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
    //private JCheckBox mViewHolderCheck = new JCheckBox(Constant.dialogs.viewHolderCheck, false);

    // 确定、取消JPanel
    private JPanel mPanelButtonRight = new JPanel();
    private JButton mButtonConfirm = new JButton(Constant.dialogs.buttonConfirm);
    private JButton mButtonCancel = new JButton(Constant.dialogs.buttonCancel);

    // GridBagLayout不要求组件的大小相同便可以将组件垂直、水平或沿它们的基线对齐
    private GridBagLayout mLayout = new GridBagLayout();
    // GridBagConstraints用来控制添加进的组件的显示位置
    private GridBagConstraints mConstraints = new GridBagConstraints();

    public FindViewByIdDialog(Editor editor, Project project, PsiFile psiFile, PsiClass psiClass, List<Element> elements, String selectedText) {
        mEditor = editor;
        mProject = project;
        mSelectedText = selectedText;
        mElements = elements;
        mPsiFile = psiFile;
        mClass = psiClass;
        mElementSize = mElements.size();
        initExist();
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
                for (PsiStatement statement : statements) {
                    if (statement.getText().contains(element.getFieldName())
                            && statement.getText().contains("findViewById(" + element.getFullID() + ");")) {
                        isFdExist = true;
                        break;
                    } else {
                        isFdExist = false;
                    }
                }
                String setOnClickListener = element.getFieldName() + ".setOnClickListener(this);";
                for (PsiStatement statement : statements) {
                    if (statement.getText().equals(setOnClickListener)) {
                        isClickExist = true;
                        break;
                    } else {
                        isClickExist = false;
                    }
                }
            }
            if (onClickStatement != null) {
                String cass = "case " + element.getFullID() + ":";
                for (PsiElement psiElement : onClickStatement) {
                    if (psiElement instanceof PsiSwitchStatement) {
                        PsiSwitchStatement psiSwitchStatement = (PsiSwitchStatement) psiElement;
                        // 获取switch的内容
                        PsiCodeBlock psiSwitchStatementBody = psiSwitchStatement.getBody();
                        if (psiSwitchStatementBody != null) {
                            for (PsiStatement statement : psiSwitchStatementBody.getStatements()) {
                                if (statement.getText().replace("\n", "").replace("break;", "").equals(cass)) {
                                    isCaseExist = true;
                                    break;
                                } else {
                                    isCaseExist = false;
                                }
                            }
                        }
                        if (isCaseExist) {
                            break;
                        }
                    }
                }
            }
            for (PsiField field : fields) {
                String name = field.getName();
                if (name != null && name.equals(element.getFieldName()) && isFdExist) {
                    // 已存在的变量设置checkbox为false
                    element.setEnable(false);
                    mElementSize--;
                    if (element.isClickEnable() && (!isClickExist || !isCaseExist)) {
                        element.setClickable(true);
                        element.setEnable(true);
                        mElementSize++;
                    }
                    break;
                }
            }
        }
        mCheckAll.setSelected(mElementSize == mElements.size());
        mCheckAll.addActionListener(this);
    }

    /**
     * 添加头部
     */
    private void initTopPanel() {
        mPanelTitle.setLayout(new GridLayout(1, 4, 10, 10));
        mPanelTitle.setBorder(new EmptyBorder(5, 10, 5, 10));
        mTitleName.setHorizontalAlignment(JLabel.LEFT);
        mTitleName.setBorder(new EmptyBorder(0, 25, 0, 0));
        mTitleId.setHorizontalAlignment(JLabel.LEFT);
        mTitleClick.setHorizontalAlignment(JLabel.LEFT);
        mTitleField.setHorizontalAlignment(JLabel.LEFT);
        mPanelTitle.add(mTitleName);
        mPanelTitle.add(mTitleId);
        mPanelTitle.add(mTitleClick);
        mPanelTitle.add(mTitleField);
        mPanelTitle.setSize(720, 30);
        // 添加到JFrame
        getContentPane().add(mPanelTitle, 0);
    }

    /**
     * 添加底部
     */
    private void initBottomPanel() {
        // 添加监听
        mButtonConfirm.addActionListener(this);
        mButtonCancel.addActionListener(this);
        // 左边
        String viewField = "m" + Util.getFieldName(mSelectedText) + "View";
        mLayoutInflaterField = new JTextField(viewField, viewField.length());
        // viewHolder
        //mPanelViewHolder.add(mViewHolderCheck);
        // 右边
        mPanelButtonRight.add(mButtonConfirm);
        mPanelButtonRight.add(mButtonCancel);
        // 添加到JPanel
        mPanelInflater.add(mCheckAll);
        mPanelInflater.add(mLayoutInflater);
        mPanelInflater.add(mLayoutInflaterField);
        // 添加到JFrame
        getContentPane().add(mPanelInflater, 2);
        //getContentPane().add(mPanelViewHolder, 3);
        getContentPane().add(mPanelButtonRight, 3);
    }

    /**
     * 解析mElements，并添加到JPanel
     */
    private void initContentPanel() {
        mContentJPanel.removeAll();
        // 设置内容
        for (int i = 0; i < mElements.size(); i++) {
            Element element = mElements.get(i);
            IdBean itemJPanel = new IdBean(new GridLayout(1, 4, 10, 10),
                    new EmptyBorder(5, 10, 5, 10),
                    new JCheckBox(element.getName()),
                    new JLabel(element.getId()),
                    new JCheckBox(),
                    new JTextField(element.getFieldName()),
                    element.isEnable(),
                    element.isClickable(),
                    element.isClickEnable());
            // 监听
            itemJPanel.setEnableActionListener(enableCheckBox -> element.setEnable(enableCheckBox.isSelected()));
            itemJPanel.setClickActionListener(clickCheckBox -> element.setClickable(clickCheckBox.isSelected()));
            itemJPanel.setFieldFocusListener(fieldJTextField -> element.setFieldName(fieldJTextField.getText()));
            mContentJPanel.add(itemJPanel);
            mContentConstraints.fill = GridBagConstraints.HORIZONTAL;
            mContentConstraints.gridwidth = 0;
            mContentConstraints.gridx = 0;
            mContentConstraints.gridy = i;
            mContentConstraints.weightx = 1;
            mContentLayout.setConstraints(itemJPanel, mContentConstraints);
        }
        mContentJPanel.setLayout(mContentLayout);
        jScrollPane = new JBScrollPane(mContentJPanel);
        jScrollPane.revalidate();
        // 添加到JFrame
        getContentPane().add(jScrollPane, 1);
    }

    /**
     * 设置Constraints
     */
    private void setConstraints() {
        // 使组件完全填满其显示区域
        mConstraints.fill = GridBagConstraints.BOTH;
        // 设置组件水平所占用的格子数，如果为0，就说明该组件是该行的最后一个
        mConstraints.gridwidth = 0;
        // 第几列
        mConstraints.gridx = 0;
        // 第几行
        mConstraints.gridy = 0;
        // 行拉伸0不拉伸，1完全拉伸
        mConstraints.weightx = 1;
        // 列拉伸0不拉伸，1完全拉伸
        mConstraints.weighty = 0;
        // 设置组件
        mLayout.setConstraints(mPanelTitle, mConstraints);
        mConstraints.fill = GridBagConstraints.BOTH;
        mConstraints.gridwidth = 1;
        mConstraints.gridx = 0;
        mConstraints.gridy = 1;
        mConstraints.weightx = 1;
        mConstraints.weighty = 1;
        mLayout.setConstraints(jScrollPane, mConstraints);
        mConstraints.fill = GridBagConstraints.HORIZONTAL;
        mConstraints.gridwidth = 0;
        mConstraints.gridx = 0;
        mConstraints.gridy = 2;
        mConstraints.weightx = 1;
        mConstraints.weighty = 0;
        mLayout.setConstraints(mPanelInflater, mConstraints);
//        mConstraints.fill = GridBagConstraints.HORIZONTAL;
//        mConstraints.gridwidth = 0;
//        mConstraints.gridx = 0;
//        mConstraints.gridy = 3;
//        mConstraints.weightx = 1;
//        mConstraints.weighty = 0;
//        mLayout.setConstraints(mPanelViewHolder, mConstraints);
        mConstraints.fill = GridBagConstraints.NONE;
        mConstraints.gridwidth = 0;
        mConstraints.gridx = 0;
        mConstraints.gridy = 3;
        mConstraints.weightx = 0;
        mConstraints.weighty = 0;
        mConstraints.anchor = GridBagConstraints.EAST;
        mLayout.setConstraints(mPanelButtonRight, mConstraints);
    }

    /**
     * 显示dialog
     */
    public void showDialog() {
        // 显示
        setVisible(true);
    }

    /**
     * 设置JFrame参数
     */
    private void setDialog() {
        // 设置标题
        setTitle(Constant.dialogs.titleFindViewById);
        // 设置布局管理
        setLayout(mLayout);
        // 不可拉伸
        setResizable(false);
        // 设置大小
        setSize(720, 405);
        // 自适应大小
        // pack();
        // 设置居中，放在setSize后面
        setLocationRelativeTo(null);
        // 显示最前
        setAlwaysOnTop(true);
    }

    /**
     * 关闭dialog
     */
    public void cancelDialog() {
        setVisible(false);
        dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case Constant.dialogs.buttonConfirm:
                cancelDialog();
//                setCreator(mLayoutInflater.isSelected(), mLayoutInflaterField.getText(), mViewHolderCheck.isSelected());
                setCreator(mLayoutInflater.isSelected(), mLayoutInflaterField.getText(), false);
                break;
            case Constant.dialogs.buttonCancel:
                cancelDialog();
                break;
            case Constant.dialogs.fieldCheckAll:
                // 刷新
                for (Element element : mElements) {
                    element.setEnable(mCheckAll.isSelected());
                }
                remove(jScrollPane);
                initContentPanel();
                setConstraints();
                revalidate();
                break;
        }
    }

    /**
     * 生成
     *  @param isLayoutInflater 是否是LayoutInflater.from(this).inflate(R.layout.activity_main, null);
     * @param text             自定义text
     * @param viewHolder 是否创建ViewHolder
     */
    private void setCreator(boolean isLayoutInflater, String text, boolean viewHolder) {
        new WidgetFieldCreator(this, mEditor, mPsiFile, mClass,
                Constant.creatorCommandName, mElements, mSelectedText, isLayoutInflater, text, false, viewHolder)
                .execute();
    }
}
