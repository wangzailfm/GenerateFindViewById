package entitys

import com.intellij.psi.xml.XmlTag
import firstToUpperCase
import org.apache.commons.lang.StringUtils
import java.util.regex.Pattern

/**
 * @author Jowan
 */
class Element
/**
 * 构造函数
 * @param name View的名字
 *
 * @param id   android:id属性
 *
 * @param clickable clickable
 *
 */
(name: String, id: String, clickable: Boolean, var xml: XmlTag) {
    // id
    var id: String? = null
    // 名字如TextView
    var name: String? = null
    // 命名1 aa_bb_cc; 2 aaBbCc 3 mAaBbCc
    var fieldNameType = 3
    /**
     * 获取变量名
     * @return 变量名
     */
    // aaBbCc mAaBbCc
    var fieldName: String = ""
        get() {
            if (StringUtils.isEmpty(field)) {
                var fieldName: String = id!!
                val names = id!!.split("_".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
                when (fieldNameType) {
                    2 -> {
                        val sb = StringBuilder()
                        for (i in names.indices) {
                            if (i == 0) {
                                sb.append(names[i])
                            } else {
                                sb.append(names[i].firstToUpperCase())
                            }
                        }
                        fieldName = sb.toString()
                    }
                    3 -> {
                        val sb = StringBuilder()
                        for (i in names.indices) {
                            if (i == 0) {
                                sb.append("m")
                            }
                            sb.append(names[i].firstToUpperCase())
                        }
                        fieldName = sb.toString()
                    }
                    1 -> fieldName = id!!
                }
                this.fieldName = fieldName
            }
            return field
        }
    // 是否生成
    var isEnable = true
    // 是否有clickable
    var isClickEnable = false
        private set
    // 是否Clickable
    var isClickable = false

    init {
        // id
        val matcher = sIdPattern.matcher(id)
        if (matcher.find() && matcher.groupCount() > 1) {
            this.id = matcher.group(2)
        }

        if (this.id == null) {
            throw IllegalArgumentException("Invalid format of view id")
        }

        this.name = name

        val packages = name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (packages.size > 1) {
            // com.example.CustomView
            this.name = packages[packages.size - 1]
        }

        this.isClickEnable = true

        this.isClickable = clickable
    }

    fun setClickEnable() {
        this.isClickEnable = true
    }

    /**
     * 获取id，R.id.id
     * @return R.id.id
     */
    val fullID: String
        get() {
            val rPrefix = "R.id."
            return rPrefix + id!!
        }

    companion object {

        // 判断id正则
        private val sIdPattern = Pattern.compile("@\\+?(android:)?id/([^$]+)$", Pattern.CASE_INSENSITIVE)
    }
}
