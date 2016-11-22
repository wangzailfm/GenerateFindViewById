package Utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.JBColor;
import entity.Element;

import java.awt.*;
import java.awt.List;
import java.util.*;

/**
 * Created by pc on 2016/11/22.
 */
public class Utils {

    /**
     * 显示dialog
     *
     * @param editor
     * @param result 内容
     */
    public static void showPopupBalloon(final Editor editor, final String result) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                JBPopupFactory factory = JBPopupFactory.getInstance();
                factory.createHtmlTextBalloonBuilder(result, null, new JBColor(new Color(116, 214, 238), new Color(76, 112, 117)), null)
                        .setFadeoutTime(5000)
                        .createBalloon()
                        .show(factory.guessBestPopupLocation(editor), Balloon.Position.below);
            }
        });
    }

    /**
     * 获取所有id
     *
     * @param file
     * @param elements
     * @return
     */
    public static java.util.List<Element> getIDsFromLayout(final PsiFile file, final java.util.List<Element> elements) {
        // To iterate over the elements in a file
        // 遍历一个文件的所有元素
        file.accept(new XmlRecursiveElementVisitor() {

            @Override
            public void visitElement(PsiElement element) {
                super.visitElement(element);
                // 解析Xml标签
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    // 获取Tag的名字（TextView）或者自定义
                    String name = tag.getName();
                    // 如果有include
                    if (name.equalsIgnoreCase("include")) {
                        // 获取布局
                        XmlAttribute layout = tag.getAttribute("layout", null);
                        // 获取project
                        Project project = file.getProject();
                        // 布局文件
                        XmlFile include = null;
                        PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, getLayoutName(layout.getValue()) + ".xml", GlobalSearchScope.allScope(project));
                        if (psiFiles.length > 0) {
                            include = (XmlFile) psiFiles[0];
                        }
                        if (include != null) {
                            // 递归
                            getIDsFromLayout(include, elements);
                            return;
                        }
                    }
                    // 获取id字段属性
                    XmlAttribute id = tag.getAttribute("android:id", null);
                    if (id == null) {
                        return;
                    }
                    // 获取id的值
                    String idValue = id.getValue();
                    if (idValue == null) {
                        return;
                    }
                    XmlAttribute aClass = tag.getAttribute("class", null);
                    if (aClass != null) {
                        name = aClass.getValue();
                    }
                    // 添加到list
                    try {
                        Element e = new Element(name, idValue, tag);
                        elements.add(e);
                    } catch (IllegalArgumentException e) {

                    }
                }
            }
        });


        return elements;
    }

    /**
     * layout.getValue()返回的值为@layout/layout_view
     *
     * @param layout
     * @return
     */
    public static String getLayoutName(String layout) {
        if (layout == null || !layout.startsWith("@") || !layout.contains("/")) {
            return null;
        }

        // @layout layout_view
        String[] parts = layout.split("/");
        if (parts.length != 2) {
            return null;
        }
        // layout_view
        return parts[1];
    }
}
