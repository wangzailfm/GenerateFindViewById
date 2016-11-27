package entity;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Created by wangzai on 2016/11/24.
 */
public class IdBean extends JPanel {
    private JCheckBox mEnableCheckBox;
    private JLabel mIdJLabel;
    private JTextField mFieldJTextField;

    /**
     * mEnableCheckBox接口
     */
    public interface EnableActionListener {
        void setEnable(JCheckBox enableCheckBox);
    }

    private EnableActionListener mEnableListener;

    public void setEnableActionListener(EnableActionListener enableActionListener) {
        mEnableListener = enableActionListener;
    }

    /**
     * mFieldJTextField接口
     */
    public interface FieldFocusListener {
        void setFieldName(JTextField fieldJTextField);
    }

    private FieldFocusListener mFieldFocusListener;

    public void setFieldFocusListener(FieldFocusListener fieldFocusListener) {
        mFieldFocusListener = fieldFocusListener;
    }

    /**
     * 构造方法
     *
     * @param layout      布局
     * @param emptyBorder border
     * @param jCheckBox   是否生成+name
     * @param jLabel      id
     * @param jTextField  字段名
     */
    public IdBean(LayoutManager layout, EmptyBorder emptyBorder, JCheckBox jCheckBox, JLabel jLabel, JTextField jTextField) {
        super(layout);
        initLayout(layout, emptyBorder);

        mEnableCheckBox = jCheckBox;
        mIdJLabel = jLabel;
        mFieldJTextField = jTextField;
        initComponent();
        addComponent();
    }

    /**
     * addComponent
     */
    private void addComponent() {
        this.add(mEnableCheckBox);
        this.add(mIdJLabel);
        this.add(mFieldJTextField);
    }

    /**
     * 设置Component
     */
    private void initComponent() {
        /*
        // 是否生成 + name
        JCheckBox enableCheckBox = new JCheckBox(mElement.getName(), true);
        // 监听
        enableCheckBox.addActionListener(e -> mElement.setEnable(enableCheckBox.isSelected()));
        // 设置左对齐
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
        */
        // 监听
        mEnableCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mEnableListener != null) {
                    mEnableListener.setEnable(mEnableCheckBox);
                }
            }
        });
        // 设置左对齐
        mEnableCheckBox.setHorizontalAlignment(JLabel.LEFT);
        mIdJLabel.setHorizontalAlignment(JLabel.LEFT);
        mFieldJTextField.setHorizontalAlignment(JTextField.LEFT);
        // 监听
        mFieldJTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (mFieldFocusListener != null) {
                    mFieldFocusListener.setFieldName(mFieldJTextField);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (mFieldFocusListener != null) {
                    mFieldFocusListener.setFieldName(mFieldJTextField);
                }
            }
        });
    }

    /**
     * 设置布局相关
     *
     * @param layout
     * @param emptyBorder
     */
    private void initLayout(LayoutManager layout, EmptyBorder emptyBorder) {
        // 设置布局内容
        this.setLayout(layout);
        // 设置border
        this.setBorder(emptyBorder);
    }
}
