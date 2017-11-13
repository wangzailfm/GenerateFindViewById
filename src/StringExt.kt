
import com.intellij.notification.*
import constant.Constant
import entitys.Element
import org.apache.commons.lang.StringUtils
import java.util.*


/**
 * 第一个字母大写
 *
 * @param key key
 *
 * @return String
 */
fun String.firstToUpperCase(): String = this.substring(0, 1).toUpperCase(Locale.CHINA) + this.substring(1)

/**
 * 输出到Log窗口
 */
fun String.outInfo() {
    NotificationsConfiguration.getNotificationsConfiguration().register(Constant.GENERATEFINDVIEWBYID, NotificationDisplayType.NONE)
    Notifications.Bus.notify(
            Notification(Constant.GENERATEFINDVIEWBYID, "${Constant.GENERATEFINDVIEWBYID} [INFO]", this, NotificationType.INFORMATION))
}

/**
 * 根据layoutInflaterType生成不同内容
 *
 * @param mLayoutInflaterText mLayoutInflaterText
 *
 * @param mLayoutInflaterType mLayoutInflaterType
 *
 * @return String
 */
fun layoutInflaterType2Str(mLayoutInflaterText: String, mLayoutInflaterType: Int): String = when (mLayoutInflaterType) {
    1 -> "_$mLayoutInflaterText"
    2 -> mLayoutInflaterText.firstToUpperCase()
    else -> mLayoutInflaterText.substring(1)
}

/**
 * layout.getValue()返回的值为@layout/layout_view
 * @param layout layout
 *
 * @return String
 */
fun String?.getLayoutName(): String? {
    if (this == null || !this.startsWith("@") || !this.contains("/")) {
        return null
    }

    // @layout layout_view
    val parts = this.split("/".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
    if (parts.size != 2) {
        return null
    }
    // layout_view
    return parts[1]
}

/**
 * 驼峰
 *
 * @param fieldNames fieldName
 *
 * @param type      type
 *
 * @return String
 */
infix fun String.getFieldName(type: Int): String {
    var fieldName = this
    if (!StringUtils.isEmpty(fieldName)) {
        val names = fieldName.split("_".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        when (type) {
            2 -> {
                // aaBbCc
                val sb = StringBuilder()
                for (i in names.indices) {
                    if (i == 0) {
                        sb.append(names[i])
                    } else {
                        sb.append(names[i].firstToUpperCase())
                    }
                }
                sb.append("View")
                fieldName = sb.toString()
            }
            3 -> {
                // mAaBbCc
                val sb = StringBuilder()
                for (i in names.indices) {
                    if (i == 0) {
                        sb.append("m")
                    }
                    sb.append(names[i].firstToUpperCase())
                }
                sb.append("View")
                fieldName = sb.toString()
            }
            else -> fieldName += "_view"
        }
    }
    return fieldName
}

/**
 * 创建onCreate方法(Fragment)
 *
 * @param mSelectedText mSelectedText
 *
 * @return String
 */
fun String.createFragmentOnCreateMethod(): String = "@Override public void onCreate(@Nullable android.os.Bundle savedInstanceState) {\n" +
        "super.onCreate(savedInstanceState);\n" +
        "\tview = View.inflate(getActivity(), R.layout.$this, null);\n" +
        "}"

/**
 * FindViewById，创建字段
 *
 * @param text                注释内容
 *
 * @param element             Element
 *
 * @param mIsLayoutInflater   是否选中LayoutInflater
 *
 * @param mLayoutInflaterText 选中的布局的变量名
 *
 * @param mLayoutInflaterType mLayoutInflaterType
 *
 * @return String
 */
fun String?.createFieldByElement(element: Element, mIsLayoutInflater: Boolean, mLayoutInflaterText: String, mLayoutInflaterType: Int): String {
    val fromText = StringBuilder()
    this?.let {
        fromText.append("/** $it */\n")
    }
    with(element) {
        fromText.append("private $name $fieldName")
    }
    if (mIsLayoutInflater) {
        fromText.append(layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType))
    }
    fromText.append(";")
    return fromText.toString()
}

/**
 * ButterKnife，在OnClick方法里面创建switch
 *
 * @param psiMethodParamsViewField View类型的变量名
 *
 * @param onClickValues            注解里面跟OnClickList的id集合
 *
 * @return String
 */
fun String.createSwitchByOnClickMethod(onClickValues: ArrayList<String>): String {
    val psiSwitch = StringBuilder()
    psiSwitch.append("switch ($this.getId()) {\n")
    // add default statement
    psiSwitch.append("\tdefault:\n")
    psiSwitch.append("\t\tbreak;\n")
    onClickValues.forEach {
        psiSwitch.append("\tcase $it:\n")
        psiSwitch.append("\t\tbreak;\n")
    }
    psiSwitch.append("}")
    return psiSwitch.toString()
}

/**
 * FindViewById，创建ViewHolder
 *
 * @param viewHolderName     viewHolderName
 *
 * @param viewHolderRootView viewHolderRootView
 *
 * @param mElements          mElements
 *
 * @param mNeedCasts         是否需要强转
 *
 * @return String
 */
fun String.createFindViewByIdViewHolder(viewHolderRootView: String,
                                        mElements: ArrayList<Element>,
                                        mNeedCasts: Boolean): String {
    // ViewHolder
    val viewHolderText = StringBuilder()
    // ViewHolder的Constructor
    val viewHolderConstructorText = StringBuilder()
    // rootView
    viewHolderText.append("android.view.View $viewHolderRootView;\n")
    // Constructor
    viewHolderConstructorText.append("$this(android.view.View ) {\nthis.$viewHolderRootView = $viewHolderRootView;\n")
    // 添加field和findViewById
    mElements.forEach {
        with(it) {
            // 添加Field
            viewHolderText.append("$name $fieldName;\n")
            // 添加findViewById
            viewHolderConstructorText.append("this.$fieldName = ")
            if (mNeedCasts) {
                viewHolderConstructorText.append("($name) ")
            }
            viewHolderConstructorText.append("$viewHolderRootView.findViewById($fullID);\n")
        }
    }
    viewHolderConstructorText.append("}")
    // 添加Constructor到ViewHolder
    viewHolderText.append(viewHolderConstructorText.toString())
    return viewHolderText.toString()
}

/**
 * ButterKnife，创建ViewHolder
 *
 * @param viewHolderName     viewHolderName
 *
 * @param viewHolderRootView viewHolderRootView
 *
 * @param mElements          mElements
 *
 * @return String
 */
fun String.createButterKnifeViewHolder(viewHolderRootView: String, mElements: ArrayList<Element>): String {
    // ViewHolder
    val viewHolderText = StringBuilder()
    // ViewHolder的Constructor
    val viewHolderConstructorText = StringBuilder()
    // 添加field和findViewById
    mElements.forEach {
        // 添加Field
        viewHolderText.append("@BindView(${it.fullID})\n${it.name} ${it.fieldName};\n")
    }
    // Constructor
    viewHolderConstructorText.append("$this(android.view.View $viewHolderRootView) {\nButterKnife.bind(this, $viewHolderRootView);\n}")
    // 添加Constructor到ViewHolder
    viewHolderText.append(viewHolderConstructorText.toString())
    return viewHolderText.toString()
}
/**
 * 创建onCreate方法
 *
 * @param mSelectedText  mSelectedText
 *
 * @param mIsButterKnife mIsButterKnife
 *
 * @return String
 */
fun String.createOnCreateMethod(mIsButterKnife: Boolean): String {
    val method = StringBuilder()
    method.append("@Override protected void onCreate(android.os.Bundle savedInstanceState) {\n")
    method.append("super.onCreate(savedInstanceState);\n")
    method.append("\t// TODO:OnCreate Method has been created, run ")
    method.append(if(!mIsButterKnife) "FindViewById" else "ButterKnife")
    method.append(" again to generate code\n\tsetContentView(R.layout.$this);\n")
    method.append(if(!mIsButterKnife) "\t\tinitView();\n" else "\t\tButterKnife.bind(this);\n")
    method.append("}")
    return method.toString()
}