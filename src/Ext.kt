import com.intellij.psi.*
import entitys.Element
import org.apache.commons.lang.StringUtils
import java.util.*


/**
 * 创建onCreateView方法
 *
 * @param mIsButterKnife mIsButterKnife
 *
 * @return String
 */
fun createOnCreateViewMethod(mIsButterKnife: Boolean): String {
    val method = StringBuilder()
    method.append("@Nullable @Override public View onCreateView(android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container, @Nullable android.os.Bundle savedInstanceState) {\n")
    method.append("\t// TODO:OnCreateView Method has been created, run ")
    method.append(if (!mIsButterKnife) "FindViewById" else "ButterKnife")
    method.append(" again to generate code\n")
    method.append(if (!mIsButterKnife) "\t\tinitView(view);\n" else "\t\tunbinder = ButterKnife.bind(this, view);\n")
    method.append("return view;")
    method.append("}\n")
    return method.toString()
}

/**
 * 创建initView方法，Fragment

 * @return String
 */
fun createFragmentInitViewMethod(): String = "public void initView(View view) {\n}"

/**
 * 创建initView方法
 *
 * @return String
 */
fun createInitViewMethod(): String = "public void initView() {\n}"

/**
 * 创建OnDestroyView方法，里面包含unbinder.unbind()
 *
 * @return String
 */
fun createOnDestroyViewMethod(): String = "@Override public void onDestroyView() {\n" +
        "\tsuper.onDestroyView();" +
        "\tunbinder.unbind();" +
        "}"

/**
 * 判断是否实现了OnClickListener接口
 * @param referenceElements referenceElements
 *
 * @return boolean
 */
fun isImplementsOnClickListener(referenceElements: Array<PsiJavaCodeReferenceElement>): Boolean {
    referenceElements.forEach {
        if (it.text.contains("OnClickListener")) {
            return true
        }
    }
    return false
}


/**
 * 获取OnClickList里面的id集合
 *
 * @param mOnClickList clickable的Element集合
 *
 * @return List
 */
fun getOnClickListById(mOnClickList: ArrayList<Element>): ArrayList<String> {
    val list: ArrayList<String> = ArrayList<String>()
    mOnClickList.forEach { list.add(it.fullID) }
    return list
}

/**
 * 获取注解里面跟OnClickList的id集合
 *
 * @param annotationList OnClick注解里面的id集合
 *
 * @param onClickIdList  clickable的Element集合
 *
 * @return List
 */
fun createOnClickValue(annotationList: ArrayList<String>, onClickIdList: ArrayList<String>): ArrayList<String> {
    onClickIdList
            .filterNot { annotationList.contains(it) }
            .forEach { annotationList.add(it) }
    return annotationList
}


/**
 * FindViewById，创建findViewById代码到initView方法里面
 *
 * @param findPre             Fragment的话要view.findViewById
 *
 * @param mIsLayoutInflater   是否选中LayoutInflater
 *
 * @param mLayoutInflaterText 选中的布局的变量名
 *
 * @param context             context
 *
 * @param mSelectedText       选中的布局
 *
 * @param mElements           Element的List
 *
 * @param mLayoutInflaterType type
 *
 * @param mNeedCast           是否需要强转
 *
 * @return String
 */
fun createFieldsByInitViewMethod(findPre: String?, mIsLayoutInflater: Boolean,
                                 mLayoutInflaterText: String, context: String,
                                 mSelectedText: String, mElements: ArrayList<Element>,
                                 mLayoutInflaterType: Int, mNeedCast: Boolean): String {
    val initView = StringBuilder()
    initView.append(if (StringUtils.isEmpty(findPre)) "private void initView() {\n" else "private void initView(View $findPre) {\n")
    if (mIsLayoutInflater) {
        // 添加LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        val layoutInflater = "$mLayoutInflaterText = LayoutInflater.from($context).inflate(R.layout.$mSelectedText, null);\n"
        initView.append(layoutInflater)
    }
    mElements.filter(Element::isEnable).forEach {
        with(it) {
            var pre = findPre?.let { findPre + "." } ?: ""
            val inflater = if (mIsLayoutInflater) layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType) else ""
            pre = if (!mIsLayoutInflater) pre else mLayoutInflaterText + "."
            initView.append("$fieldName$inflater = ")
            if (mNeedCast) {
                initView.append("($name)")
            }
            initView.append("${pre}findViewById($fullID);\n")
            if (isClickable && isClickEnable) {
                initView.append("$fieldName$inflater.setOnClickListener(this);\n")
            }
        }
    }
    initView.append("}\n")
    return initView.toString()
}


/**
 * ButterKnife，创建findById代码到init方法里面
 *
 * @param mIsLayoutInflater   mIsLayoutInflater
 *
 * @param mLayoutInflaterText mLayoutInflaterText
 *
 * @param context             context
 *
 * @param mSelectedText       mSelectedText
 *
 * @param mElements           mElements
 *
 * @param viewMethodName      viewMethodName
 *
 * @return String
 */
fun createButterKnifeViewMethod(mIsLayoutInflater: Boolean,
                                mLayoutInflaterText: String, context: String, mSelectedText:
                                String, mElements: ArrayList<Element>, viewMethodName: String,
                                mLayoutInflaterType: Int): String {
    val initView = StringBuilder()
    initView.append("// TODO:Copy method name to use\n")
    initView.append("private void $viewMethodName() {\n")
    if (mIsLayoutInflater) {
        // 添加LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        val layoutInflater = "$mLayoutInflaterText = LayoutInflater.from($context).inflate(R.layout.$mSelectedText, null);\n"
        initView.append(layoutInflater)
    }
    mElements.filter(Element::isEnable).forEach {
        val inflater = if (mIsLayoutInflater) layoutInflaterType2Str(mLayoutInflaterText, mLayoutInflaterType) else ""
        initView.append("${it.fieldName}$inflater = ButterKnife.findById($mLayoutInflaterText, ${it.fullID});\n")
    }
    initView.append("}\n")
    return initView.toString()
}