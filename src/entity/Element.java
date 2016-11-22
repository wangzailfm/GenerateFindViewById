package entity;

import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Element {

    // 判断id正则
    private static final Pattern sIdPattern = Pattern.compile("@\\+?(android:)?id/([^$]+)$", Pattern.CASE_INSENSITIVE);
    // id
    public String id;
    // 名字如TextView
    public String name;
    // 命名1 aa_bb_cc; 2 aaBbCc 3 mAaBbCc
    public int fieldNameType = 3;
    public XmlTag xml;

    /**
     * 构造函数
     *
     * @param name View的名字
     * @param id   android:id属性
     * @throws IllegalArgumentException When the arguments are invalid
     */
    public Element(String name, String id, XmlTag xml) {
        // id
        final Matcher matcher = sIdPattern.matcher(id);
        if (matcher.find() && matcher.groupCount() > 1) {
            this.id = matcher.group(2);
        }

        if (this.id == null) {
            throw new IllegalArgumentException("Invalid format of view id");
        }

        String[] packages = name.split("\\.");
        if (packages.length > 1) {
            // com.example.CustomView
            this.name = packages[packages.length - 1];
        } else {
            this.name = name;
        }

        this.xml = xml;
    }

    /**
     * 获取id，R.id.id
     *
     * @return
     */
    public String getFullID() {
        StringBuilder fullID = new StringBuilder();
        String rPrefix = "R.id.";
        fullID.append(rPrefix);
        fullID.append(id);
        return fullID.toString();
    }

    /**
     * 获取变量名
     *
     * @return
     */
    public String getFieldName() {
        String fieldName = id;
        String[] names = id.split("_");
        if (fieldNameType == 2) {
            // aaBbCc
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.length; i++) {
                if (i == 0) {
                    sb.append(names[i]);
                } else {
                    sb.append(firstToUpperCase(names[i]));
                }
            }
            fieldName = sb.toString();
        } else if (fieldNameType == 3) {
            // mAaBbCc
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.length; i++) {
                if (i == 0) {
                    sb.append("m");
                }
                sb.append(firstToUpperCase(names[i]));
            }
            fieldName = sb.toString();
        }
        return fieldName;
    }

    // 第一个字母大写
    public static String firstToUpperCase(String key) {
        return key.substring(0, 1).toUpperCase(Locale.CHINA) + key.substring(1);
    }
}
