package View;

import Utils.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import entity.Element;
import entity.IdBean;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Created by wangzai on 2016/11/24.
 */
public class FindViewByIdDialog extends JFrame implements ActionListener {
    private String mTitle = "FindViewByIdDialog";
    private Project mProject;
    private Editor mEditor;
    private String mSelectedText;
    private List<Element> mElements;
    // 获取当前文件
    private PsiFile mPsiFile;
    // 获取class
    private PsiClass mClass;

    // 标签JPanel
    private JPanel mPanelTitle = new JPanel();
    private JLabel mTitleName = new JLabel("ViewWidget");
    private JLabel mTitleId = new JLabel("ViewId");
    private JLabel mTitleField = new JLabel("ViewFiled");
    // 确定、取消JPanel
    private JPanel mPanelRight = new JPanel();
    private JPanel mPanelButton = new JPanel();

    // 是否选择LayoutInflater
    private JCheckBox mButtonLayoutInflater = new JCheckBox("LayoutInflater.from?", false);
    // 手动修改LayoutInflater字段名
    private JTextField mButtonLayoutInflaterField;
    private JButton mButtonConfirm = new JButton("确定");
    private JButton mButtonCancel = new JButton("取消");

    public FindViewByIdDialog(Editor editor, Project project, PsiFile psiFile, PsiClass psiClass, List<Element> elements, String selectedText) {
        mEditor = editor;
        mProject = project;
        mSelectedText = selectedText;
        mElements = elements;
        mPsiFile = psiFile;
        mClass = psiClass;
        initTopPanel();
        initBottomPanel();
        initContentPanel();
        setDialog();
    }

    private void initBottomPanel() {
        // 添加监听
        mButtonConfirm.addActionListener(this);
        mButtonCancel.addActionListener(this);
        // 添加到JPanel
        mPanelButton.setLayout(new GridLayout(1, 3, 10, 10));
        mPanelButton.setBorder(new EmptyBorder(5, 10, 5, 10));
        // 左边
        mButtonLayoutInflater.setHorizontalAlignment(JCheckBox.LEFT);
        // 中间
        mButtonLayoutInflaterField = new JTextField("m" + Util.getFieldName(mSelectedText) + "View");
        mButtonLayoutInflaterField.setHorizontalAlignment(JTextField.LEFT);
        // 右边
        mPanelRight.add(mButtonConfirm);
        mPanelRight.add(mButtonCancel);
        // 添加到JPanel
        mPanelButton.add(mButtonLayoutInflater);
        mPanelButton.add(mButtonLayoutInflaterField);
        mPanelButton.add(mPanelRight);
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
        mTitleField.setHorizontalAlignment(JLabel.LEFT);
        mPanelTitle.add(mTitleName);
        mPanelTitle.add(mTitleId);
        mPanelTitle.add(mTitleField);
    }

    /**
     * 解析mElements，并添加到JPanel
     */
    private void initContentPanel() {
        // 添加JPanel
        getContentPane().add(mPanelTitle);
        // 获取已存在的变量
        PsiField[] fields = mClass.getFields();
        // 设置内容
        for (Element mElement : mElements) {
            for (PsiField field : fields) {
                String name = field.getName();
                if (name != null && name.equals(mElement.getFieldName())) {
                    // 已存在的变量设置checkbox为false
                    mElement.setEnable(false);
                    break;
                }
            }
            IdBean itemJPanel = new IdBean(new GridLayout(1, 4, 10, 10),
                    new EmptyBorder(5, 10, 5, 10),
                    new JCheckBox(mElement.getName(), mElement.isEnable()),
                    new JLabel(mElement.getId()),
                    new JTextField(mElement.getFieldName()));
            // 监听
            itemJPanel.setEnableActionListener(enableCheckBox1 -> mElement.setEnable(enableCheckBox1.isSelected()));
            itemJPanel.setFieldFocusListener(fieldJTextField -> mElement.setFieldName(fieldJTextField.getText()));
            getContentPane().add(itemJPanel);
        }

        // 添加JPanel
        getContentPane().add(mPanelButton);
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
        setTitle(mTitle);
        // 设置布局管理
        setLayout(new GridLayout(0, 1));
        // 不可拉伸
        setResizable(false);
        // 设置大小
        setSize(640, 360);
        // 自适应大小
        // pack();
        // 设置居中，放在setSize后面
        setLocationRelativeTo(null);
        // 显示最前
        setAlwaysOnTop(true);
    }

    public void cancelDialog() {
        setVisible(false);
        dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "确定":
                cancelDialog();
                setCreator(mButtonLayoutInflater.isSelected(), mButtonLayoutInflaterField.getText());
                break;
            case "取消":
                cancelDialog();
                break;
        }
    }

    /**
     * 生成
     * @param isLayoutInflater 是否是LayoutInflater.from(this).inflate(R.layout.activity_main, null);
     * @param text 自定义text
     */
    private void setCreator(boolean isLayoutInflater, String text) {
        new WidgetFieldCreator(this, mEditor, mPsiFile, mClass,
                "Generate Injections", mElements, mSelectedText, isLayoutInflater, text)
                .execute();
    }
}
