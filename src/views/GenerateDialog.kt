package views

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.ui.components.JBScrollPane
import constant.Constant
import entitys.Element
import entitys.IdBean
import utils.GenerateCreator
import utils.Util
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * GenerateDialog
 * @author Jowan
 */
open class GenerateDialog(
        private val mProject: Project,
        private val mEditor: Editor,
        private val mSelectedText: String,
        /*获取mElements*/
        private val elements: List<Element>,
        /*获取当前文件*/
        private val mPsiFile: PsiFile,
        /*获取class*/
        private val psiClass: PsiClass,
        /*判断是否全选*/
        private var elementSize: Int = 0,
        /*判断是否是ButterKnife*/
        private val mIsButterKnife: Boolean
) : JFrame(), ActionListener, ItemListener {

    // 判断OnClick是否全选
    private var mOnClickSize: Int = 0

    // 标签JPanel
    private val mPanelTitle = JPanel()
    private val mTitleName = JCheckBox(Constant.dialogs.TABLE_FIELD_VIEW_WIDGET)
    private val mTitleId = JLabel(Constant.dialogs.TABLE_FIELD_VIEW_ID)
    private val mTitleClick = JCheckBox(Constant.FIELD_ON_CLICK, false)
    // 命名JPanel
    private val mPanelTitleField = JPanel()
    private val mTitleFieldGroup = ButtonGroup()
    // aaBb
    private val mTitleFieldHump = JRadioButton("aaBb")
    // mAaBb
    private val mTitleFieldPrefix = JRadioButton("mAaBb", true)

    // 内容JPanel
    private val mContentJPanel = JPanel()
    private val mContentLayout = GridBagLayout()
    private val mContentConstraints = GridBagConstraints()
    // 内容JBScrollPane滚动
    private var jScrollPane: JBScrollPane? = null

    // 底部JPanel
    // LayoutInflater JPanel
    private val mPanelInflater = JPanel(FlowLayout(FlowLayout.LEFT))
    // 是否选择LayoutInflater
    private val mLayoutInflater = JCheckBox(Constant.dialogs.FIELD_LAYOUT_INFLATER, false)
    // 手动修改LayoutInflater字段名
    private var mLayoutInflaterField: JTextField? = null
    private var type = 3

    // viewHolder
    private val mPanelViewHolder = JPanel(FlowLayout(FlowLayout.LEFT))
    private val mViewHolderCheck = JCheckBox(Constant.dialogs.VIEWHOLDER_CHECK, false)

    // 是否需要强转
    private val mPanelNeedCasts = JPanel(FlowLayout(FlowLayout.LEFT))
    private val mNeedCastsCheck = JCheckBox(Constant.dialogs.NEED_CASTS, true)

    // 是否bind，默认是true
    private val mBind = JCheckBox(Constant.dialogs.FIELD_BUTTERKNIFE_BIND, true)

    // 确定、取消JPanel
    private val mPanelButtonRight = JPanel()
    private val mButtonConfirm = JButton(Constant.dialogs.BUTTON_CONFIRM)
    private val mButtonCancel = JButton(Constant.dialogs.BUTTON_CANCEL)

    // GridBagLayout不要求组件的大小相同便可以将组件垂直、水平或沿它们的基线对齐
    private val mLayout = GridBagLayout()
    // GridBagConstraints用来控制添加进的组件的显示位置
    private val mConstraints = GridBagConstraints()


    /**
     * 全选设置
     */
    internal fun setCheckAll() {
        elements
                .filter { it.isClickable || mIsButterKnife }
                .forEach { mOnClickSize++ }
        mTitleName.isSelected = elementSize == elements.size
        mTitleName.addActionListener(this)
        mTitleClick.isSelected = mOnClickSize == elements.size
        mTitleClick.addActionListener(this)
    }

    /**
     * 判断是否存在ButterKnife.bind(this)/ButterKnife.bind(this, view)
     */
    internal fun checkBind() {
        mBind.isSelected = Util.isButterKnifeBindExist(psiClass)
    }

    /**
     * 添加头部
     */
    internal fun initTopPanel() {
        mPanelTitle.layout = GridLayout(1, 4, 8, 10)
        mPanelTitle.border = EmptyBorder(5, 10, 5, 10)
        mPanelTitleField.layout = GridLayout(1, 3, 0, 0)
        mTitleName.horizontalAlignment = JLabel.LEFT
        mTitleName.border = EmptyBorder(0, 5, 0, 0)
        mTitleId.horizontalAlignment = JLabel.LEFT
        mTitleClick.horizontalAlignment = JLabel.LEFT
        // 添加listener
        mTitleFieldHump.addItemListener(this)
        mTitleFieldPrefix.addItemListener(this)
        // 添加到group
        mTitleFieldGroup.add(mTitleFieldHump)
        mTitleFieldGroup.add(mTitleFieldPrefix)
        // 添加到JPanel
        mPanelTitleField.add(mTitleFieldPrefix)
        mPanelTitleField.add(mTitleFieldHump)
        // 添加到JPanel
        mPanelTitle.add(mTitleName)
        mPanelTitle.add(mTitleId)
        mPanelTitle.add(mTitleClick)
        mPanelTitle.add(mPanelTitleField)
        mPanelTitle.setSize(900, 30)
        // 添加到JFrame
        contentPane.add(mPanelTitle, 0)
    }

    /**
     * 添加底部
     */
    internal fun initBottomPanel() {
        val viewField = Util.getFieldName(mSelectedText, 3)
        mLayoutInflaterField = JTextField(viewField, viewField.length)
        // 添加监听
        mButtonConfirm.addActionListener(this)
        mButtonCancel.addActionListener(this)
        mViewHolderCheck.addActionListener(this)
        // viewHolder
        mPanelViewHolder.add(mViewHolderCheck)
        // casts
        mNeedCastsCheck.isEnabled = !mIsButterKnife
        mPanelNeedCasts.add(mNeedCastsCheck)
        // 右边
        mPanelButtonRight.add(mButtonConfirm)
        mPanelButtonRight.add(mButtonCancel)
        // 添加到JPanel
        //mPanelInflater.add(mCheckAll);
        if (mIsButterKnife) {
            mPanelInflater.add(mBind)
        }
        mPanelInflater.add(mLayoutInflater)
        mPanelInflater.add(mLayoutInflaterField)
        // 添加到JFrame
        contentPane.add(mPanelInflater, 2)
        contentPane.add(mPanelViewHolder, 3)
        contentPane.add(mPanelNeedCasts, 4)
        contentPane.add(mPanelButtonRight, 5)
    }

    /**
     * 解析mElements，并添加到JPanel
     */
    internal fun initContentPanel() {
        mContentJPanel.removeAll()
        // 设置内容
        for (i in elements.indices) {
            val element = elements[i]
            val itemJPanel = IdBean(GridLayout(1, 4, 10, 10),
                    EmptyBorder(5, 10, 5, 10),
                    JCheckBox(element.name),
                    JLabel(element.id),
                    JCheckBox(),
                    JTextField(element.fieldName),
                    element.isEnable,
                    element.isClickable,
                    element.isClickEnable)
            // 监听
            itemJPanel.setEnableActionListener { enableCheckBox ->
                element.isEnable = enableCheckBox.isSelected
                elementSize = if (enableCheckBox.isSelected) elementSize + 1 else elementSize - 1
                mTitleName.isSelected = elementSize == elements.size
            }
            itemJPanel.setClickActionListener { clickCheckBox ->
                element.isClickable = clickCheckBox.isSelected
                mOnClickSize = if (clickCheckBox.isSelected) mOnClickSize + 1 else mOnClickSize - 1
                mTitleClick.isSelected = mOnClickSize == elements.size
            }
            itemJPanel.setFieldFocusListener { fieldJTextField -> element.fieldName = fieldJTextField.text }
            mContentJPanel.add(itemJPanel)
            mContentConstraints.fill = GridBagConstraints.HORIZONTAL
            mContentConstraints.gridwidth = 0
            mContentConstraints.gridx = 0
            mContentConstraints.gridy = i
            mContentConstraints.weightx = 1.0
            mContentLayout.setConstraints(itemJPanel, mContentConstraints)
        }
        mContentJPanel.layout = mContentLayout
        jScrollPane = JBScrollPane(mContentJPanel)
        jScrollPane?.revalidate()
        // 添加到JFrame
        contentPane.add(jScrollPane, 1)
    }

    /**
     * 设置Constraints
     */
    internal fun setConstraints() {
        // 使组件完全填满其显示区域
        mConstraints.fill = GridBagConstraints.BOTH
        // 设置组件水平所占用的格子数，如果为0，就说明该组件是该行的最后一个
        mConstraints.gridwidth = 0
        // 第几列
        mConstraints.gridx = 0
        // 第几行
        mConstraints.gridy = 0
        // 行拉伸0不拉伸，1完全拉伸
        mConstraints.weightx = 1.0
        // 列拉伸0不拉伸，1完全拉伸
        mConstraints.weighty = 0.0
        // 设置组件
        mLayout.setConstraints(mPanelTitle, mConstraints)
        mConstraints.fill = GridBagConstraints.BOTH
        mConstraints.gridwidth = 1
        mConstraints.gridx = 0
        mConstraints.gridy = 1
        mConstraints.weightx = 1.0
        mConstraints.weighty = 1.0
        mLayout.setConstraints(jScrollPane, mConstraints)
        mConstraints.fill = GridBagConstraints.HORIZONTAL
        mConstraints.gridwidth = 0
        mConstraints.gridx = 0
        mConstraints.gridy = 2
        mConstraints.weightx = 1.0
        mConstraints.weighty = 0.0
        mLayout.setConstraints(mPanelInflater, mConstraints)
        mConstraints.fill = GridBagConstraints.HORIZONTAL
        mConstraints.gridwidth = 0
        mConstraints.gridx = 0
        mConstraints.gridy = 3
        mConstraints.weightx = 1.0
        mConstraints.weighty = 0.0
        mLayout.setConstraints(mPanelViewHolder, mConstraints)
        mConstraints.fill = GridBagConstraints.HORIZONTAL
        mConstraints.gridwidth = 0
        mConstraints.gridx = 0
        mConstraints.gridy = 4
        mConstraints.weightx = 1.0
        mConstraints.weighty = 0.0
        mLayout.setConstraints(mPanelNeedCasts, mConstraints)
        mConstraints.fill = GridBagConstraints.NONE
        mConstraints.gridwidth = 0
        mConstraints.gridx = 0
        mConstraints.gridy = 5
        mConstraints.weightx = 0.0
        mConstraints.weighty = 0.0
        mConstraints.anchor = GridBagConstraints.EAST
        mLayout.setConstraints(mPanelButtonRight, mConstraints)
    }

    /**
     * 显示dialog
     */
    fun showDialog() {
        // 显示
        isVisible = true
    }

    /**
     * 设置JFrame参数
     */
    internal fun setDialog() {
        // 设置标题
        title = if (mIsButterKnife) {
            Constant.dialogs.TITLE_BUTTERKNIFE
        } else {
            Constant.dialogs.TITLE_FINDVIEWBYID
        }
        // 设置布局管理
        layout = mLayout
        // 不可拉伸
        isResizable = false
        // 设置大小
        setSize(810, 405)
        // 自适应大小
        // pack();
        // 设置居中，放在setSize后面
        setLocationRelativeTo(null)
        // 显示最前
        isAlwaysOnTop = true
    }

    /**
     * 关闭dialog
     */
    fun cancelDialog() {
        isVisible = false
        dispose()
    }

    /**
     * 刷新JScrollPane内容
     */
    private fun refreshJScrollPane() {
        remove(jScrollPane)
        initContentPanel()
        setConstraints()
        revalidate()
    }

    /**
     * 生成

     * @param isLayoutInflater      是否是LayoutInflater.from(this).inflate(R.layout.activity_main, null);
     * *
     * @param text                  自定义text
     * *
     * @param isBind                是否是bind
     * *
     * @param viewHolder            是否是viewHolder
     * *
     * @param isButterKnife         是否是ButterKnife
     * *
     * @param mNeedCasts           是否需要强转
     */
    private fun setCreator(isLayoutInflater: Boolean, text: String, isBind: Boolean, viewHolder: Boolean, isButterKnife: Boolean, mNeedCasts: Boolean) {
        // 使用Builder模式
        GenerateCreator<Any>(command = Constant.CREATOR_COMMAND_NAME,
                mDialog = this,
                mEditor = mEditor,
                mFile = mPsiFile,
                mClass = psiClass,
                mProject = psiClass.project,
                mElements = elements,
                mFactory = JavaPsiFacade.getElementFactory(psiClass.project),
                mSelectedText = mSelectedText,
                mIsLayoutInflater = isLayoutInflater,
                mLayoutInflaterText = text,
                mLayoutInflaterType = type,
                mIsButterKnife = isButterKnife,
                mIsBind = isBind,
                mViewHolder = viewHolder,
                mNeedCasts = mNeedCasts).execute()
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.actionCommand) {
            Constant.dialogs.BUTTON_CONFIRM -> {
                cancelDialog()
                setCreator(mLayoutInflater.isSelected, mLayoutInflaterField!!.text, mBind.isSelected, mViewHolderCheck.isSelected, mIsButterKnife, mNeedCastsCheck.isSelected)
            }
            Constant.dialogs.BUTTON_CANCEL -> cancelDialog()
            Constant.FIELD_ON_CLICK -> {
                // 刷新
                for (element in elements) {
                    element.isClickable = mTitleClick.isSelected
                }
                mOnClickSize = if (mTitleClick.isSelected) elements.size else 0
                refreshJScrollPane()
            }
            Constant.dialogs.TABLE_FIELD_VIEW_WIDGET -> {
                // 刷新
                for (element in elements) {
                    element.isEnable = mTitleName.isSelected
                }
                elementSize = if (mTitleName.isSelected) elements.size else 0
                refreshJScrollPane()
            }
            Constant.dialogs.VIEWHOLDER_CHECK -> {
                mLayoutInflater.isEnabled = !mViewHolderCheck.isSelected
                mLayoutInflaterField?.isEnabled = !mViewHolderCheck.isSelected
                mBind.isEnabled = !mViewHolderCheck.isSelected
            }
        }
    }

    override fun itemStateChanged(e: ItemEvent) {
        type = if (e.source === mTitleFieldPrefix) 3 else 2
        for (element in elements) {
            if (element.isEnable) {
                // 设置类型
                element.fieldNameType = type
                // 置空
                element.fieldName = ""
            }
        }
        mLayoutInflaterField?.text = Util.getFieldName(mSelectedText, type)
        refreshJScrollPane()
    }
}
