package constant;

public final class Constant {
    public static final String actionFindViewById = "FindViewById";
    public static final String actionButterKnife = "ButterKnife";
    public static final String selectedTextSUFFIX = ".xml";
    public static final String psiMethodByOnCreate = "onCreate";
    public static final String psiMethodByOnCreateView = "onCreateView";
    public static final String creatorCommandName = "Generate Injections";
    public static final String classTypeByActivity = "activity";
    public static final String classTypeByFragment = "fragment";
    public static final String FieldOnClick = "OnClick";
    public static final String FieldonClick = "onClick";

    public static final class actions {
        public static final String selectedMessage = "布局内容：（不需要输入R.layout.）";
        public static final String selectedTitle = "未选中布局内容，请输入layout文件名";
        public static final String selectedErrorNoName = "未输入layout文件名";
        public static final String selectedErrorNoSelected = "未找到选中的布局文件";
        public static final String selectedErrorNoId = "未找到任何Id";
        public static final String selectedErrorNoPoint = "光标未在Class内";
        public static final String selectedSuccess = "生成成功";
    }

    public static final class dialogs {
        public static final String titleButterKnife = "ButterKnifeDialog";
        public static final String titleFindViewById = "FindViewByIdDialog";
        public static final String tableFieldViewWidget = "ViewWidget";
        public static final String tableFieldViewId = "ViewId";
        public static final String tableFieldViewFiled = "ViewFiled";
        public static final String fieldLayoutInflater = "LayoutInflater.from(context).inflater";
        public static final String fieldCheckAll = "CheckAll";
        public static final String viewHolderCheck = "Create ViewHolder";
        public static final String fieldButterKnifeBind = "ButterKnife.bind()";
        public static final String buttonConfirm = "确定";
        public static final String buttonCancel = "取消";
    }

    public static final class utils {
        public static final String creatorNoOnCreateMethod = "没有OnCreate方法，已创建OnCreate方法，请重新使用";
        public static final String creatorNoOnCreateViewMethod = "没有OnCreateView方法，已创建OnCreateView方法，请重新使用";
        public static final String creatorInitViewName = "initView";
        public static final String creatorViewName = "view";
        public static final String creatorUnbinderName = "unbinder";
        public static final String creatorSetContentViewMethod = "setContentView";
        public static final String creatorOnDestroyViewMethod = "onDestroyView";
        public static final String creatorUnbinderField = "unbinder.unbind();";
        public static final String fieldButterKnifeBind = "ButterKnife.bind";
    }

}
