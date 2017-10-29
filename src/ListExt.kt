import entitys.Element
import java.util.*

/**
 * FindViewById，创建OnClick方法和switch
 *
 * @param mOnClickList 可onclick的Element的集合
 *
 * @return String
 */
fun ArrayList<Element>.createFindViewByIdOnClickMethodAndSwitch(): String {
    val onClick = StringBuilder()
    onClick.append("@Override public void onClick(View v) {\n")
    onClick.append("switch (v.getId()) {\n")
    // add default statement
    onClick.append("\tdefault:\n")
    onClick.append("\t\tbreak;\n")
    this.filter(Element::isClickable).forEach {
        onClick.append("\tcase ${it.fullID}:\n")
        onClick.append("\t\tbreak;\n")
    }
    onClick.append("}\n")
    onClick.append("}\n")
    return onClick.toString()
}

/**
 * ButterKnife，创建OnClick方法和switch
 *
 * @param mOnClickList 可onclick的Element的集合
 *
 * @return String
 */
fun ArrayList<Element>.createButterKnifeOnClickMethodAndSwitch(): String {
    val onClick = StringBuilder()
    onClick.append("@butterknife.OnClick(")
    if (this.size == 1) {
        onClick.append(this[0].fullID)
    } else {
        onClick.append("{")
        this.forEachIndexed { i, element ->
            if (i != 0) {
                onClick.append(", ")
            }
            onClick.append(element.fullID)
        }
        onClick.append("}")
    }
    onClick.append(")\n")
    onClick.append("public void onClick(View v) {\n")
    onClick.append("switch (v.getId()) {\n")
    // add default statement
    onClick.append("\tdefault:\n")
    onClick.append("\t\tbreak;\n")
    this.forEach {
        onClick.append("\tcase ${it.fullID}:\n")
        onClick.append("\t\tbreak;\n")
    }
    onClick.append("}\n")
    onClick.append("}\n")
    return onClick.toString()
}
