package views;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBScrollPane;
import constant.Constant;
import entitys.Element;
import entitys.IdBean;
import utils.GenerateCreator;
import utils.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

/**
 * GenerateDialog
 */
public class GenerateDialog extends JFrame implements ActionListener, ItemListener {
    private final Project mProject;
    private final Editor mEditor;
    private final String mSelectedText;
    private final List<Element> mElements;
    // 获取当前文件
    private final PsiFile mPsiFile;
    // 获取class
    private final PsiClass mClass;
    // 判断是否全选
    private int mElementSize;
    // 判断OnClick是否全选
    private int mOnClickSize;
    // 判断是否是ButterKnife
    private final boolean mIsButterKnife;

    // 标签JPanel
    private JPanel mPanelTitle = new JPanel();
    private JCheckBox mTitleName = new JCheckBox(Constant.dialogs.tableFieldViewWidget);
    private JLabel mTitleId = new JLabel(Constant.dialogs.tableFieldViewId);
    private JCheckBox mTitleClick = new JCheckBox(Constant.FieldOnClick, false);
    // 命名JPanel
    private JPanel mPanelTitleField = new JPanel();
    private ButtonGroup mTitleFieldGroup = new ButtonGroup();
    // aaBb
    private JRadioButton mTitleFieldHump = new JRadioButton("aaBb");
    // mAaBb
    private JRadioButton mTitleFieldPrefix = new JRadioButton("mAaBb", true);

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
    private JCheckBox mLayoutInflater = new JCheckBox(Constant.dialogs.fieldLayoutInflater, false);
    // 手动修改LayoutInflater字段名
    private JTextField mLayoutInflaterField;
    private int type = 3;

    // viewHolder
    private JPanel mPanelViewHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private JCheckBox mViewHolderCheck = new JCheckBox(Constant.dialogs.viewHolderCheck, false);

    // 是否bind，默认是true
    private JCheckBox mBind = new JCheckBox(Constant.dialogs.fieldButterKnifeBind, true);

    // 确定、取消JPanel
    private JPanel mPanelButtonRight = new JPanel();
    private JButton mButtonConfirm = new JButton(Constant.dialogs.buttonConfirm);
    private JButton mButtonCancel = new JButton(Constant.dialogs.buttonCancel);

    // GridBagLayout不要求组件的大小相同便可以将组件垂直、水平或沿它们的基线对齐
    private GridBagLayout mLayout = new GridBagLayout();
    // GridBagConstraints用来控制添加进的组件的显示位置
    private GridBagConstraints mConstraints = new GridBagConstraints();

    /**
     * Builder模式
     */
    public static class Builder {
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
        // 判断是否是ButterKnife
        private boolean mIsButterKnife;

        public Builder(int mElementSize) {
            this.mElementSize = mElementSize;
        }

        public Builder setProject(Project project) {
            mProject = project;
            return this;
        }

        public Builder setEditor(Editor editor) {
            mEditor = editor;
            return this;
        }

        public Builder setSelectedText(String selectedText) {
            mSelectedText = selectedText;
            return this;
        }

        public Builder setElements(List<Element> elements) {
            mElements = elements;
            return this;
        }

        public Builder setPsiFile(PsiFile psiFile) {
            mPsiFile = psiFile;
            return this;
        }

        public Builder setClass(PsiClass aClass) {
            mClass = aClass;
            return this;
        }

        public Builder setIsButterKnife(boolean butterKnife) {
            mIsButterKnife = butterKnife;
            return this;
        }

        public GenerateDialog build() {
            return new GenerateDialog(this);
        }

    }

    GenerateDialog(Builder builder) {
        mProject = builder.mProject;
        mEditor = builder.mEditor;
        mSelectedText = builder.mSelectedText;
        mElements = builder.mElements;
        mPsiFile = builder.mPsiFile;
        mClass = builder.mClass;
        mElementSize = builder.mElementSize;
        mIsButterKnife = builder.mIsButterKnife;
    }

    /**
     * 获取mElementSize
     * @return int
     */
    int getElementSize() {
        return mElementSize;
    }

    /**
     * 设置mElementSize
     * @param elementSize elementSize
     */
    void setElementSize(int elementSize) {
        mElementSize = elementSize;
    }

    /**
     * 获取mElements
     *
     * @return List<Element>
     */
    List<Element> getElements() {
        return mElements;
    }

    /**
     * 获取mClass
     *
     * @return PsiClass
     */
    PsiClass getPsiClass() {
        return mClass;
    }

    /**
     * 全选设置
     */
    void setCheckAll() {
        for (Element element : mElements) {
            if (element.isClickable() || mIsButterKnife) {
                mOnClickSize++;
            }
        }
        mTitleName.setSelected(mElementSize == mElements.size());
        mTitleName.addActionListener(this);
        mTitleClick.setSelected(mOnClickSize == mElements.size());
        mTitleClick.addActionListener(this);
    }

    /**
     * 判断是否存在ButterKnife.bind(this)/ButterKnife.bind(this, view)
     */
    void checkBind() {
        mBind.setSelected(Util.isButterKnifeBindExist(mClass));
    }

    /**
     * 添加头部
     */
    void initTopPanel() {
        mPanelTitle.setLayout(new GridLayout(1, 4, 10, 10));
        mPanelTitle.setBorder(new EmptyBorder(5, 10, 5, 10));
        mPanelTitleField.setLayout(new GridLayout(1, 3, 0, 0));
        mTitleName.setHorizontalAlignment(JLabel.LEFT);
        mTitleName.setBorder(new EmptyBorder(0, 5, 0, 0));
        mTitleId.setHorizontalAlignment(JLabel.LEFT);
        mTitleClick.setHorizontalAlignment(JLabel.LEFT);
        // 添加listener
        mTitleFieldHump.addItemListener(this);
        mTitleFieldPrefix.addItemListener(this);
        // 添加到group
        mTitleFieldGroup.add(mTitleFieldHump);
        mTitleFieldGroup.add(mTitleFieldPrefix);
        // 添加到JPanel
        mPanelTitleField.add(mTitleFieldPrefix);
        mPanelTitleField.add(mTitleFieldHump);
        // 添加到JPanel
        mPanelTitle.add(mTitleName);
        mPanelTitle.add(mTitleId);
        mPanelTitle.add(mTitleClick);
        mPanelTitle.add(mPanelTitleField);
        mPanelTitle.setSize(900, 30);
        // 添加到JFrame
        getContentPane().add(mPanelTitle, 0);
    }

    /**
     * 添加底部
     */
    void initBottomPanel() {
        String viewField = Util.getFieldName(mSelectedText, 3);
        mLayoutInflaterField = new JTextField(viewField, viewField.length());
        // 添加监听
        mButtonConfirm.addActionListener(this);
        mButtonCancel.addActionListener(this);
        mViewHolderCheck.addActionListener(this);
        // viewHolder
        mPanelViewHolder.add(mViewHolderCheck);
        // 右边
        mPanelButtonRight.add(mButtonConfirm);
        mPanelButtonRight.add(mButtonCancel);
        // 添加到JPanel
        //mPanelInflater.add(mCheckAll);
        if (mIsButterKnife) {
            mPanelInflater.add(mBind);
        }
        mPanelInflater.add(mLayoutInflater);
        mPanelInflater.add(mLayoutInflaterField);
        // 添加到JFrame
        getContentPane().add(mPanelInflater, 2);
        getContentPane().add(mPanelViewHolder, 3);
        getContentPane().add(mPanelButtonRight, 4);
    }

    /**
     * 解析mElements，并添加到JPanel
     */
    void initContentPanel() {
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
            itemJPanel.setEnableActionListener(enableCheckBox -> {
                element.setEnable(enableCheckBox.isSelected());
                mElementSize = enableCheckBox.isSelected() ? mElementSize + 1 : mElementSize - 1;
                mTitleName.setSelected(mElementSize == mElements.size());
            });
            itemJPanel.setClickActionListener(clickCheckBox -> {
                element.setClickable(clickCheckBox.isSelected());
                mOnClickSize = clickCheckBox.isSelected() ? mOnClickSize + 1 : mOnClickSize - 1;
                mTitleClick.setSelected(mOnClickSize == mElements.size());
            });
            itemJPanel.setFieldFocusListener(
                    fieldJTextField -> element.setFieldName(fieldJTextField.getText())
            );
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
    void setConstraints() {
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
        mConstraints.fill = GridBagConstraints.HORIZONTAL;
        mConstraints.gridwidth = 0;
        mConstraints.gridx = 0;
        mConstraints.gridy = 3;
        mConstraints.weightx = 1;
        mConstraints.weighty = 0;
        mLayout.setConstraints(mPanelViewHolder, mConstraints);
        mConstraints.fill = GridBagConstraints.NONE;
        mConstraints.gridwidth = 0;
        mConstraints.gridx = 0;
        mConstraints.gridy = 4;
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
    void setDialog() {
        // 设置标题
        if (mIsButterKnife) {
            setTitle(Constant.dialogs.titleButterKnife);
        } else {
            setTitle(Constant.dialogs.titleFindViewById);
        }
        // 设置布局管理
        setLayout(mLayout);
        // 不可拉伸
        setResizable(false);
        // 设置大小
        setSize(810, 405);
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

    /**
     * 刷新JScrollPane内容
     */
    private void refreshJScrollPane() {
        remove(jScrollPane);
        initContentPanel();
        setConstraints();
        revalidate();
    }

    /**
     * 生成
     *
     * @param isLayoutInflater      是否是LayoutInflater.from(this).inflate(R.layout.activity_main, null);
     * @param text                  自定义text
     * @param isBind                是否是bind
     * @param viewHolder            是否是viewHolder
     * @param isButterKnife         是否是ButterKnife
     */
    private void setCreator(boolean isLayoutInflater, String text, boolean isBind, boolean viewHolder, boolean isButterKnife) {
        // 使用Builder模式
        new GenerateCreator.Builder(Constant.creatorCommandName)
                .setDialog(this)
                .setEditor(mEditor)
                .setFile(mPsiFile)
                .setClass(mClass)
                .setProject(mClass.getProject())
                .setElements(mElements)
                .setFactory(JavaPsiFacade.getElementFactory(mClass.getProject()))
                .setSelectedText(mSelectedText)
                .setIsLayoutInflater(isLayoutInflater)
                .setLayoutInflaterText(text)
                .setLayoutInflaterType(type)
                .setIsButterKnife(isButterKnife)
                .setIsBind(isBind)
                .setViewHolder(viewHolder)
                .build()
                .execute();
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case Constant.dialogs.buttonConfirm:
                cancelDialog();
                setCreator(mLayoutInflater.isSelected(), mLayoutInflaterField.getText(), mBind.isSelected(), mViewHolderCheck.isSelected(), mIsButterKnife);
                break;
            case Constant.dialogs.buttonCancel:
                cancelDialog();
                break;
            case Constant.FieldOnClick:
                // 刷新
                for (Element element : mElements) {
                    element.setClickable(mTitleClick.isSelected());
                }
                mOnClickSize = mTitleClick.isSelected() ? mElements.size() : 0;
                refreshJScrollPane();
                break;
            case Constant.dialogs.tableFieldViewWidget:
                // 刷新
                for (Element element : mElements) {
                    element.setEnable(mTitleName.isSelected());
                }
                mElementSize = mTitleName.isSelected() ? mElements.size() : 0;
                refreshJScrollPane();
                break;
            case Constant.dialogs.viewHolderCheck:
                mLayoutInflater.setEnabled(!mViewHolderCheck.isSelected());
                mLayoutInflaterField.setEnabled(!mViewHolderCheck.isSelected());
                mBind.setEnabled(!mViewHolderCheck.isSelected());
                break;
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        type = e.getSource() == mTitleFieldPrefix ? 3 : 2;
        for (Element element : mElements) {
            if (element.isEnable()) {
                // 设置类型
                element.setFieldNameType(type);
                // 置空
                element.setFieldName("");
            }
        }
        mLayoutInflaterField.setText(Util.getFieldName(mSelectedText, type));
        refreshJScrollPane();
    }
}
