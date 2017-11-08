package utils

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.command.WriteCommandAction.Simple
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import constant.Constant
import createFragmentInitViewMethod
import createFragmentOnCreateMethod
import createInitViewMethod
import createOnCreateMethod
import createOnCreateViewMethod
import createOnDestroyViewMethod
import isExtendsActivityOrActivityCompat
import isExtendsFragmentOrFragmentV4
import showPopupBalloon

/**
 * 生成Activity的onCreate方法和Fragment的onCreateView方法
 * @author Jowan
 */
class CreateMethodCreator<T>(private val mEditor: Editor,
                             private val mFile: PsiFile,
                             private val mClass: PsiClass,
                             command: String,
                             private val mSelectedText: String, // activity/fragment
                             private val mType: String,
                             private val mIsButterKnife: Boolean) :
        Simple<T>(mClass.project, command) {
    private val mProject: Project = mClass.project
    private val mFactory: PsiElementFactory

    init {
        // 获取Factory
        mFactory = JavaPsiFacade.getElementFactory(mProject)
    }

    @Throws(Throwable::class)
    override fun run() {
        try {
            createMethod()
        } catch (e: Exception) {
            mEditor.showPopupBalloon(e.message, 10)
            return
        }

        // 重写class
        val styleManager = JavaCodeStyleManager.getInstance(mProject)
        styleManager.optimizeImports(mFile)
        styleManager.shortenClassReferences(mClass)
        ReformatCodeProcessor(mProject, mClass.containingFile, null, false).runWithoutProgress()
        var actionName = Constant.ACTION_FINDVIEWBYID
        if (mIsButterKnife) {
            actionName = Constant.ACTION_BUTTERKNIFE
        }
        if (mType == Constant.CLASS_TYPE_BY_ACTIVITY) {
            mEditor.showPopupBalloon(Constant.Ext.CREATOR_NO_ONCREATE_METHOD + actionName, 10)
        } else if (mType == Constant.CLASS_TYPE_BY_FRAGMENT) {
            mEditor.showPopupBalloon(Constant.Ext.CREATOR_NO_ONCREATEVIEW_METHOD + actionName, 10)
        }
    }

    /**
     * 设置Activity的onCreate方法和Fragment的onCreateView方法
     */
    private fun createMethod() {
        if (mProject isExtendsActivityOrActivityCompat mClass) {
            // 判断是否有onCreate方法
            if (mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATE, false).isEmpty()) {
                // 添加
                mClass.add(mFactory.createMethodFromText(mSelectedText.createOnCreateMethod(mIsButterKnife), mClass))
                if (!mIsButterKnife && mClass.findMethodsByName(Constant.Ext.CREATOR_INITVIEW_NAME, false).isEmpty()) {
                    mClass.add(mFactory.createMethodFromText(createInitViewMethod(), mClass))
                }
            }

        } else if (mProject isExtendsFragmentOrFragmentV4 mClass) {
            var isViewExist = false
            var isUnbinderExist = false
            for (psiField in mClass.fields) {
                if (psiField.name != null && psiField.name == Constant.Ext.CREATOR_VIEW_NAME) {
                    isViewExist = true
                }
                if (psiField.name != null && psiField.name == Constant.Ext.CREATOR_UNBINDER_NAME) {
                    isUnbinderExist = true
                }
            }
            if (!isViewExist) {
                mClass.add(mFactory.createFieldFromText("private View view;", mClass))
            }
            if (mIsButterKnife && !isUnbinderExist) {
                mClass.add(mFactory.createFieldFromText("private Unbinder unbinder;", mClass))
            }
            // 判断是否有onCreate方法
            if (mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATE, false).isEmpty()) {
                mClass.add(mFactory.createMethodFromText(mSelectedText.createFragmentOnCreateMethod(), mClass))
            } else {
                // onCreate是否存在View.inflate方法
                var hasInflateStatement = false
                val onCreate = mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATE, false)[0]
                if (onCreate.body != null) {
                    for (psiStatement in onCreate.body!!.statements) {
                        if (psiStatement.text.contains("View.inflate(getActivity(), R.layout.$mSelectedText, null);")) {
                            hasInflateStatement = true
                            break
                        } else {
                            hasInflateStatement = false
                        }
                    }
                    if (!hasInflateStatement) {
                        onCreate.body?.add(mFactory.createStatementFromText("view = View.inflate(getActivity(), R.layout.$mSelectedText, null);", mClass))
                    }
                }
            }
            // 判断是否有onCreateView方法
            if (mClass.findMethodsByName(Constant.PSI_METHOD_BY_ONCREATEVIEW, false).isEmpty()) {
                // 添加
                mClass.add(mFactory.createMethodFromText(createOnCreateViewMethod(mIsButterKnife), mClass))
                if (!mIsButterKnife && mClass.findMethodsByName(Constant.Ext.CREATOR_INITVIEW_NAME, false).isEmpty()) {
                    mClass.add(mFactory.createMethodFromText(createFragmentInitViewMethod(), mClass))
                }
            }
            // ButterKnife判断是否有onDestroyView方法
            if (mIsButterKnife) {
                if (mClass.findMethodsByName(Constant.Ext.CREATOR_ONDESTROYVIEW_METHOD, false).isEmpty()) {
                    mClass.add(mFactory.createMethodFromText(createOnDestroyViewMethod(), mClass))
                } else {
                    // onDestroyView是否存在unbinder.unbind();
                    var hasUnbinderStatement = false
                    val onCreate = mClass.findMethodsByName(Constant.Ext.CREATOR_ONDESTROYVIEW_METHOD, false)[0]
                    if (onCreate.body != null) {
                        for (psiStatement in onCreate.body!!.statements) {
                            if (psiStatement.text.contains(Constant.Ext.CREATOR_UNBINDER_FIELD)) {
                                hasUnbinderStatement = true
                                break
                            } else {
                                hasUnbinderStatement = false
                            }
                        }
                        if (!hasUnbinderStatement) {
                            onCreate.body?.add(mFactory.createStatementFromText(Constant.Ext.CREATOR_UNBINDER_FIELD, mClass))
                        }
                    }
                }
            }

        }
    }
}
