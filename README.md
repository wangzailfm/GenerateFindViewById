# GenerateFindViewById

## 演示
![演示](http://img.blog.csdn.net/20161122145239231)

## 安装
- 下载项目里面的GenerateFindViewById.jar包，然后打开AS的Plugins，点击Install plugin from disk...安装
- 打开AS的Plugins，点击Browse repositories...然后搜索GenerateFindViewById，然后安装

## 说明
- 可输入布局字段，可选中布局文件字段，自动生成有id控件相应的代码
- Activity如果没有onCreate方法，会先生成onCreate方法，再重新操作一次才生成有id控件相应的代码。
- Fragment如果没有onCreateView方法，会先生成onCreateView方法，再重新操作一次才生成有id控件相应的代码。
- 可选生成的字段，可编辑变量名，可选择是否LayoutInflater类型。
- LayoutInflater类型生成的变量规则，如LayoutInflater的变量为mView，生成控件变量后面会加上"View"。

## 用法
1. 新建Activity或者Fragment后，选中布局按下快捷键Alt+Insert,然后选择FindViewById或者在菜单栏中的Code中选择FindViewById
2. 如果没有选中布局，会弹出输入框，输入布局
3. 插件会自动遍历布局列出所有带id的控件
4. 会自动检测是否已有代码，可选择是否生成、是否生成OnClick代码，可编辑变量名
5. 可选择是否生成View view = LayoutInflater.from(context).inflater()代码，可编辑生成的View的变量名
6. 点击确认生成

## 规则
1. Activity如果没有onCreate方法，会先生成onCreate方法，Fragment如果没有onCreateView方法，会先生成onCreateView方法
2. 没有id的控件是不会识别到的
3. 识别到的控件变量名为mAaBbCc命名
4. 识别到的控件中有clickable = true属性，自动生成setOnClickListener代码和onClick方法
5. 自动识别布局中的include标签, 读取对应布局中的控件
6. 识别到的控件中有text或者hint属性，会自动生成里面的值到字段注释
7. LayoutInflater生成的变量名规则为mAaBbCc+View的变量名(如mView会去掉m)

## 更新
- 1.0 支持Activity和Fragment，添加可选生成的字段，可编辑变量名，可选择是否LayoutInflater类型，添加快捷键Ctrl+Alt+E，text、hint的值添加到字段注释，添加LayoutInflater生成，添加OnClick生成代码。

## License
```
Copyright 2016 Jowan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```