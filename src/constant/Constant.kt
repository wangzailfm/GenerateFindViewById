package constant

/**
 * @author Jowan
 */
object Constant {
    const val GENERATEFINDVIEWBYID = "GenerateFindViewById"
    const val ACTION_FINDVIEWBYID = "FindViewById"
    const val ACTION_BUTTERKNIFE = "ButterKnife"
    const val SELECTED_TEXT_SUFFIX = ".xml"
    const val PSI_METHOD_BY_ONCREATE = "onCreate"
    const val PSI_METHOD_BY_ONCREATEVIEW = "onCreateView"
    const val CREATOR_COMMAND_NAME = "Generate Injections"
    const val CLASS_TYPE_BY_ACTIVITY = "activity"
    const val CLASS_TYPE_BY_FRAGMENT = "fragment"
    const val FIELD_ON_CLICK = "OnClick"
    const val FIELD_ONCLICK = "onClick"

    object Action {
        const val SELECTED_MESSAGE = "布局内容：（不需要输入R.layout.）"
        const val SELECTED_TITLE = "未选中布局内容，请输入layout文件名"
        const val SELECTED_ERROR_NO_NAME = "未输入layout文件名"
        const val SELECTED_ERROR_NO_SELECTED = "未找到选中的布局文件"
        const val SELECTED_ERROR_NO_ID = "未找到任何Id"
        const val SELECTED_ERROR_NO_POINT = "光标未在Class内"
        const val SELECTED_LAYOUT_FIELD_TEXT_NO_NULL = "LayoutInflater变量名不能为空"
        const val SELECTED_SUCCESS = "生成成功"
    }

    object Dialog {
        const val TITLE_BUTTERKNIFE = "ButterKnifeDialog"
        const val TITLE_FINDVIEWBYID = "FindViewByIdDialog"
        const val TABLE_FIELD_VIEW_WIDGET = "ViewWidget"
        const val TABLE_FIELD_VIEW_ID = "ViewId"
        const val TABLE_FIELD_VIEW_FILED = "ViewFiled"
        const val FIELD_LAYOUT_INFLATER = "LayoutInflater.from(context).inflater"
        const val FIELD_CHECK_ALL = "CheckAll"
        const val VIEWHOLDER_CHECK = "Create ViewHolder"
        const val NEED_CASTS = "Forced Casts(SupportLibrary uncheck the above version of 26)"
        const val FIELD_BUTTERKNIFE_BIND = "ButterKnife.bind()"
        const val BUTTON_CONFIRM = "确定"
        const val BUTTON_CANCEL = "取消"
    }

    object Ext {
        const val CREATOR_NO_ONCREATE_METHOD = "没有OnCreate方法，已创建OnCreate方法，请重新使用"
        const val CREATOR_NO_ONCREATEVIEW_METHOD = "没有OnCreateView方法，已创建OnCreateView方法，请重新使用"
        const val CREATOR_INITVIEW_NAME = "initView"
        const val CREATOR_VIEW_NAME = "view"
        const val CREATOR_UNBINDER_NAME = "unbinder"
        const val CREATOR_SETCONTENTVIEW_METHOD = "setContentView"
        const val CREATOR_ONDESTROYVIEW_METHOD = "onDestroyView"
        const val CREATOR_UNBINDER_FIELD = "unbinder.unbind();"
        const val FIELD_BUTTERKNIFE_BIND = "ButterKnife.bind"
        const val UNKNOWN_ERROR = "Unknown Error"
    }

}
