package View;

import Utils.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
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
    // 标签JPanel
    private IdBean mPanelTitle = new IdBean();
    private JLabel mTitleName = new JLabel("ViewWidget");
    private JLabel mTitleId = new JLabel("ViewId");
    private JLabel mTitleField = new JLabel("ViewFiled");
    // 确定、取消JPanel
    private IdBean mPanelRight = new IdBean();
    private IdBean mPanelButton = new IdBean();
    // 是否选择LayoutInflater
    private JCheckBox mButtonLayoutInflater = new JCheckBox("LayoutInflater.from?", false);
    // 手动修改LayoutInflater字段名
    private JTextField mButtonLayoutInflaterField;
    private JButton mButtonConfirm = new JButton("确定");
    private JButton mButtonCancel = new JButton("取消");

    public FindViewByIdDialog(Editor editor, Project project, List<Element> elements, String selectedText) {
        mEditor = editor;
        mProject = project;
        mSelectedText = selectedText;
        mElements = elements;
        mPanelTitle.setLayout(new GridLayout(1, 4, 10, 10));
        mPanelTitle.setBorder(new EmptyBorder(5, 10, 5, 10));

        mTitleName.setHorizontalAlignment(JLabel.LEFT);
        mTitleName.setBorder(new EmptyBorder(0, 25, 0, 0));
        mTitleId.setHorizontalAlignment(JLabel.LEFT);
        mTitleField.setHorizontalAlignment(JLabel.LEFT);

        mPanelTitle.add(mTitleName);
        mPanelTitle.add(mTitleId);
        mPanelTitle.add(mTitleField);

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
        initPanel();
        setDialog();
    }

    /**
     * 解析mElements
     */
    private void initPanel() {
        // 添加JPanel
        getContentPane().add(mPanelTitle);
        // 设置内容
        for (Element mElement : mElements) {
            IdBean itemJPanel = new IdBean();
            itemJPanel.setLayout(new GridLayout(1, 4, 10, 10));
            itemJPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
            // 是否生成 + name
            JCheckBox enableCheckBox = new JCheckBox(mElement.getName(), true);
            // 监听
            enableCheckBox.addActionListener(e -> mElement.setEnable(enableCheckBox.isSelected()));
            // 设置边距
            enableCheckBox.setHorizontalAlignment(JLabel.LEFT);
            // id
            JLabel idJLabel = new JLabel(mElement.getId());
            idJLabel.setHorizontalAlignment(JLabel.LEFT);
            // 变量名
            JTextField fieldJTextField = new JTextField(mElement.getFieldName());
            fieldJTextField.setHorizontalAlignment(JTextField.LEFT);
            // 监听
            fieldJTextField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    mElement.setFieldName(fieldJTextField.getText());
                }

                @Override
                public void focusLost(FocusEvent e) {
                    mElement.setFieldName(fieldJTextField.getText());
                }
            });

            itemJPanel.add(enableCheckBox);
            itemJPanel.add(idJLabel);
            itemJPanel.add(fieldJTextField);
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
        // 自适应大小
        pack();
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
        //获取当前文件
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(mEditor, mProject);
        new WidgetFieldCreator(psiFile, Util.getTargetClass(mEditor, psiFile),
                "Generate Injections", mElements, mSelectedText, isLayoutInflater, text)
                .execute();
        Util.showPopupBalloon(mEditor, "生成成功");
    }
}
